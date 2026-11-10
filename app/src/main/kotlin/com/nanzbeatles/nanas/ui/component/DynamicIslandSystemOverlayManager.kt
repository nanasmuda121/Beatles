/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.Player
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.nanzbeatles.nanas.MainActivity
import com.nanzbeatles.nanas.constants.DynamicIslandAccentColorKey
import com.nanzbeatles.nanas.constants.DynamicIslandBgColorKey
import com.nanzbeatles.nanas.constants.DynamicIslandBlurEnabledKey
import com.nanzbeatles.nanas.constants.DynamicIslandBlurRadiusKey
import com.nanzbeatles.nanas.constants.DynamicIslandBorderGlowKey
import com.nanzbeatles.nanas.constants.DynamicIslandDismissedKey
import com.nanzbeatles.nanas.constants.DynamicIslandEnabledKey
import com.nanzbeatles.nanas.constants.DynamicIslandMode
import com.nanzbeatles.nanas.constants.DynamicIslandModeKey
import com.nanzbeatles.nanas.constants.DynamicIslandTheme
import com.nanzbeatles.nanas.constants.DynamicIslandThemeKey
import com.nanzbeatles.nanas.constants.DynamicIslandUseDynamicAccentKey
import com.nanzbeatles.nanas.constants.DynamicIslandUseDynamicBgKey
import com.nanzbeatles.nanas.extensions.togglePlayPause
import com.nanzbeatles.nanas.extensions.toggleRepeatMode
import com.nanzbeatles.nanas.playback.MusicService
import com.nanzbeatles.nanas.ui.theme.NanzBeatlesTheme
import com.nanzbeatles.nanas.utils.dataStore
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

import androidx.datastore.preferences.core.edit
import com.nanzbeatles.nanas.constants.DynamicIslandAutoEnableOnStartKey
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Manages the system-wide floating Spotify Dynamic Island overlay (`TYPE_APPLICATION_OVERLAY`)
 * when the user is outside the app (e.g., on the Home screen or using another app) while
 * `MusicService` is running.
 *
 * Automatically hides the system window while `MainActivity` is in the foreground so the in-app
 * Compose `DynamicIslandOverlay` handles rendering without duplication.
 */
class DynamicIslandSystemOverlayManager(
    private val service: MusicService,
) {
    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitorJob: Job? = null

    private var composeView: ComposeView? = null
    private var overlayLifecycleOwner: OverlayWindowLifecycleOwner? = null
    private var isWindowAttached = false

    private val isPlayerPlayingFlow = MutableStateFlow(false)
    private var playerListener: Player.Listener? = null

    fun start() {
        if (monitorJob != null) return

        isPlayerPlayingFlow.value = service.player.isPlaying
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayerPlayingFlow.value = isPlaying
                if (isPlaying) {
                    // When music starts playing, reset dismissed state if Auto Mode is active
                    scope.launch(Dispatchers.IO) {
                        runCatching {
                            service.dataStore.edit { prefs ->
                                val autoMode = prefs[DynamicIslandAutoEnableOnStartKey] ?: false
                                if (autoMode) {
                                    prefs[DynamicIslandDismissedKey] = false
                                    prefs[DynamicIslandEnabledKey] = true
                                }
                            }
                        }
                    }
                }
            }

            override fun onEvents(player: Player, events: Player.Events) {
                isPlayerPlayingFlow.value = player.isPlaying
            }
        }
        playerListener = listener
        service.player.addListener(listener)

        monitorJob = scope.launch {
            val prefsFlow = service.dataStore.data
                .map { prefs ->
                    val enabled = prefs[DynamicIslandEnabledKey] ?: false
                    val autoMode = prefs[DynamicIslandAutoEnableOnStartKey] ?: false
                    val dismissed = prefs[DynamicIslandDismissedKey] ?: false
                    Triple(enabled, autoMode, dismissed)
                }
                .distinctUntilChanged()

            combine(
                prefsFlow,
                isPlayerPlayingFlow,
                service.currentMediaMetadata,
                DynamicIslandOverlayState.isAppInForeground
            ) { (enabled, _, dismissed), _, _, inForeground ->
                val shouldShow = enabled && !dismissed
                shouldShow to inForeground
            }.collect { (shouldShow, inForeground) ->
                updateOverlayVisibility(shouldShow = shouldShow, inForeground = inForeground)
            }
        }

        // Also periodically check overlay permission when user is outside app
        scope.launch {
            while (isActive) {
                delay(1500L)
                val inForeground = DynamicIslandOverlayState.isAppInForeground.value
                if (!inForeground && isWindowAttached && !canDrawOverlays()) {
                    hideOverlayWindow()
                }
            }
        }
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(service)
        } else {
            true
        }
    }

    private fun updateOverlayVisibility(shouldShow: Boolean, inForeground: Boolean) {
        if (shouldShow && !inForeground && canDrawOverlays()) {
            showOverlayWindow()
        } else {
            hideOverlayWindow()
        }
    }

    private fun showOverlayWindow() {
        if (isWindowAttached) return
        if (!canDrawOverlays()) return

        try {
            val lifecycleOwner = OverlayWindowLifecycleOwner().apply {
                onCreate()
                onStart()
                onResume()
            }
            overlayLifecycleOwner = lifecycleOwner

            val view = ComposeView(service).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Prevent MIUI / Poco / Android system Force-Dark from dimming white overlay text
                    isForceDarkAllowed = false
                }
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setContent {
                    NanzBeatlesTheme(
                        darkTheme = true,
                        pureBlack = true
                    ) {
                        SystemDynamicIslandContent(
                            service = service,
                            onOpenApp = { openMainActivity() }
                        )
                    }
                }
            }

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.RGBA_8888
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dpToPx(8)
                alpha = 1.0f
            }

            windowManager.addView(view, params)
            composeView = view
            isWindowAttached = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to attach Dynamic Island system overlay")
        }
    }

    private fun hideOverlayWindow() {
        if (!isWindowAttached) return
        try {
            composeView?.let { view ->
                windowManager.removeViewImmediate(view)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to remove Dynamic Island system overlay")
        } finally {
            overlayLifecycleOwner?.onDestroy()
            overlayLifecycleOwner = null
            composeView = null
            isWindowAttached = false
        }
    }

    private fun openMainActivity() {
        runCatching {
            val intent = Intent(service, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            service.startActivity(intent)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * service.resources.displayMetrics.density).toInt()
    }

    fun release() {
        playerListener?.let { service.player.removeListener(it) }
        playerListener = null
        monitorJob?.cancel()
        monitorJob = null
        hideOverlayWindow()
        scope.cancel()
    }
}

@Composable
private fun SystemDynamicIslandContent(
    service: MusicService,
    onOpenApp: () -> Unit,
) {
    val (enabled, _) = rememberPreference(DynamicIslandEnabledKey, defaultValue = false)
    val (autoMode, _) = rememberPreference(DynamicIslandAutoEnableOnStartKey, defaultValue = false)
    val (isDismissed, onDismissedChange) = rememberPreference(
        DynamicIslandDismissedKey,
        defaultValue = false
    )
    val (mode, onModeChange) = rememberEnumPreference(
        DynamicIslandModeKey,
        defaultValue = DynamicIslandMode.KECIL
    )
    val (islandTheme, _) = rememberEnumPreference(
        DynamicIslandThemeKey,
        defaultValue = DynamicIslandTheme.LIQUID_GLASS
    )
    val (accentColorInt, _) = rememberPreference(
        DynamicIslandAccentColorKey,
        defaultValue = 0xFF1DB954.toInt()
    )
    val (useDynamicAccent, _) = rememberPreference(
        DynamicIslandUseDynamicAccentKey,
        defaultValue = false
    )
    val (bgColorInt, _) = rememberPreference(
        DynamicIslandBgColorKey,
        defaultValue = 0xFF000000.toInt()
    )
    val (useDynamicBg, _) = rememberPreference(
        DynamicIslandUseDynamicBgKey,
        defaultValue = false
    )
    val (borderGlow, _) = rememberPreference(
        DynamicIslandBorderGlowKey,
        defaultValue = true
    )
    val (blurRadius, _) = rememberPreference(
        DynamicIslandBlurRadiusKey,
        defaultValue = 22
    )

    val metadata by service.currentMediaMetadata.collectAsState()
    val currentSong by service.currentSong.collectAsState()

    var isPlaying by remember { mutableStateOf(service.player.isPlaying) }
    var playbackState by remember { mutableIntStateOf(service.player.playbackState) }
    var canSkipPrevious by remember { mutableStateOf(service.player.hasPreviousMediaItem()) }
    var canSkipNext by remember { mutableStateOf(service.player.hasNextMediaItem()) }
    var shuffleModeEnabled by remember { mutableStateOf(service.player.shuffleModeEnabled) }
    var repeatMode by remember { mutableIntStateOf(service.player.repeatMode) }

    DisposableEffect(service.player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onShuffleModeEnabledChanged(shuffleEnabled: Boolean) {
                shuffleModeEnabled = shuffleEnabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }

            override fun onEvents(player: Player, events: Player.Events) {
                isPlaying = player.isPlaying
                playbackState = player.playbackState
                canSkipPrevious = player.hasPreviousMediaItem() || player.currentPosition > 3000L
                canSkipNext = player.hasNextMediaItem()
                shuffleModeEnabled = player.shuffleModeEnabled
                repeatMode = player.repeatMode
            }
        }
        service.player.addListener(listener)
        onDispose {
            service.player.removeListener(listener)
        }
    }

    val shouldShow = enabled && !isDismissed
    if (!shouldShow) return

    var extractedArtworkColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(metadata?.thumbnailUrl, useDynamicAccent, useDynamicBg) {
        if (!useDynamicAccent && !useDynamicBg) return@LaunchedEffect
        val thumb = metadata?.thumbnailUrl
        if (thumb.isNullOrBlank()) {
            extractedArtworkColor = null
            return@LaunchedEffect
        }
        withContext(Dispatchers.IO) {
            runCatching {
                val result = service.imageLoader.execute(
                    ImageRequest.Builder(service)
                        .data(thumb)
                        .allowHardware(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                )
                extractedArtworkColor = result.image?.toBitmap()?.extractVibrantIslandColor()
            }
        }
    }

    val effectiveAccentColor = remember(accentColorInt, useDynamicAccent, extractedArtworkColor) {
        if (useDynamicAccent && extractedArtworkColor != null) {
            extractedArtworkColor!!
        } else {
            Color(accentColorInt)
        }
    }

    val effectiveBgColor = remember(bgColorInt, useDynamicBg, extractedArtworkColor) {
        if (useDynamicBg && extractedArtworkColor != null) {
            lerp(Color.Black, extractedArtworkColor!!, 0.20f).copy(alpha = 1f)
        } else {
            Color(bgColorInt).copy(alpha = 1f)
        }
    }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(playbackState, isPlaying, metadata?.id) {
        if (playbackState == Player.STATE_READY || isPlaying) {
            while (isActive) {
                position = service.player.currentPosition.coerceAtLeast(0L)
                duration = service.player.duration.coerceAtLeast(0L)
                delay(250L)
            }
        } else {
            position = service.player.currentPosition.coerceAtLeast(0L)
            duration = service.player.duration.coerceAtLeast(0L)
        }
    }

    val isLiked = currentSong?.song?.liked == true
    val displayTitle = metadata?.title ?: "Dynamic Island Aktif"
    val displayArtist = metadata?.artists?.joinToString(", ") { it.name } ?: "Ketuk untuk kembali ke aplikasi"
    val displayDuration = if (metadata != null) {
        duration.takeIf { it > 0L } ?: ((metadata!!.duration.toLong()) * 1000L)
    } else {
        0L
    }

    val handleDismissWithoutStoppingMusic = {
        onDismissedChange(true)
        Toast.makeText(
            service,
            "Dynamic Island disembunyikan (musik tetap berputar)",
            Toast.LENGTH_SHORT
        ).show()
    }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        DynamicIslandCapsuleSurface(
            mode = mode,
            islandTheme = islandTheme,
            effectiveBgColor = effectiveBgColor,
            effectiveAccentColor = effectiveAccentColor,
            borderGlow = borderGlow,
            blurRadius = blurRadius,
            isSystemOverlay = true,
            displayTitle = displayTitle,
            displayArtist = displayArtist,
            thumbnailUrl = metadata?.thumbnailUrl,
            isPlaying = isPlaying,
            isLiked = isLiked,
            shuffleModeEnabled = shuffleModeEnabled,
            repeatMode = repeatMode,
            position = if (metadata != null) position else 0L,
            duration = displayDuration,
            canSkipPrevious = canSkipPrevious && metadata != null,
            canSkipNext = canSkipNext && metadata != null,
            onModeChange = onModeChange,
            onDismissIsland = handleDismissWithoutStoppingMusic,
            onTogglePlayPause = {
                if (metadata != null) {
                    service.player.togglePlayPause()
                } else {
                    onOpenApp()
                }
            },
            onSeekTo = { newPos ->
                if (metadata != null) {
                    service.player.seekTo(newPos)
                }
            },
            onPrevious = { service.player.seekToPrevious() },
            onNext = { service.player.seekToNext() },
            onToggleLike = { service.toggleLike() },
            onToggleShuffle = {
                service.player.shuffleModeEnabled = !service.player.shuffleModeEnabled
            },
            onToggleRepeat = {
                service.player.toggleRepeatMode()
            },
            onOpenFullPlayer = { onOpenApp() },
            onOpenCustomizeOrApp = { onOpenApp() }
        )
    }
}

private class OverlayWindowLifecycleOwner :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
