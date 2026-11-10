/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.DynamicIslandAccentColorKey
import com.nanzbeatles.nanas.constants.DynamicIslandAutoEnableOnStartKey
import com.nanzbeatles.nanas.constants.DynamicIslandBgColorKey
import com.nanzbeatles.nanas.constants.DynamicIslandBlurEnabledKey
import com.nanzbeatles.nanas.constants.DynamicIslandBlurRadiusKey
import com.nanzbeatles.nanas.constants.DynamicIslandBorderGlowKey
import com.nanzbeatles.nanas.constants.DynamicIslandDismissedKey
import com.nanzbeatles.nanas.constants.DynamicIslandEnabledKey
import com.nanzbeatles.nanas.constants.DynamicIslandMode
import com.nanzbeatles.nanas.constants.DynamicIslandModeKey
import com.nanzbeatles.nanas.constants.DynamicIslandPermissionConfirmedKey
import com.nanzbeatles.nanas.constants.DynamicIslandTheme
import com.nanzbeatles.nanas.constants.DynamicIslandThemeKey
import com.nanzbeatles.nanas.constants.DynamicIslandUseDynamicAccentKey
import com.nanzbeatles.nanas.constants.DynamicIslandUseDynamicBgKey
import com.nanzbeatles.nanas.extensions.togglePlayPause
import com.nanzbeatles.nanas.extensions.toggleRepeatMode
import com.nanzbeatles.nanas.ui.utils.frostedGlassSurface
import com.nanzbeatles.nanas.ui.utils.liquidGlassPill
import com.nanzbeatles.nanas.utils.dataStore
import com.nanzbeatles.nanas.utils.makeTimeString
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shared runtime state between the in-app [DynamicIslandOverlay] and the outside-app
 * [DynamicIslandSystemOverlayManager] hosted by `MusicService`.
 */
object DynamicIslandOverlayState {
    private val _isAppInForeground = MutableStateFlow(false)
    val isAppInForeground: StateFlow<Boolean> = _isAppInForeground.asStateFlow()

    @Volatile
    private var hasInitializedProcessSession = false

    fun setAppInForeground(inForeground: Boolean) {
        _isAppInForeground.value = inForeground
    }

    /**
     * Called once when the APK process starts in `MainActivity.onCreate`.
     * Always starts with `DynamicIslandEnabledKey = false` after exiting/restarting the APK:
     * - If `DynamicIslandAutoEnableOnStartKey` (Otomatis) is `true`, Dynamic Island will automatically
     *   appear as soon as music is played.
     * - If `DynamicIslandAutoEnableOnStartKey` (Otomatis) is `false`, Dynamic Island will NOT appear
     *   when music is played unless manually enabled by the user.
     */
    suspend fun initializeOnAppProcessStart(context: Context) {
        if (hasInitializedProcessSession) return
        hasInitializedProcessSession = true
        withContext(Dispatchers.IO) {
            runCatching {
                context.dataStore.edit { settings ->
                    settings[DynamicIslandDismissedKey] = false
                    settings[DynamicIslandEnabledKey] = false
                }
            }
        }
    }

    /**
     * Automatically hides/resets Dynamic Island when the user exits the application.
     * Preserves `DynamicIslandAutoEnableOnStartKey` so Auto mode remains configured for next playback.
     */
    fun resetOnAppExit(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                context.dataStore.edit { settings ->
                    settings[DynamicIslandEnabledKey] = false
                    settings[DynamicIslandDismissedKey] = false
                }
            }
        }
    }
}

/**
 * Extracts a bright, vivid color from album artwork for Dynamic Island.
 * Unlike `Theme.kt`'s monochrome `extractThemeColor()`, this preserves saturation
 * and guarantees high luminance so the waveform and progress bar never look dark or grey.
 */
internal fun Bitmap.extractVibrantIslandColor(): Color {
    val palette = Palette.from(this).maximumColorCount(16).generate()
    val candidateInt = palette.vibrantSwatch?.rgb
        ?: palette.lightVibrantSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: return Color(0xFF1DB954)

    val raw = Color(candidateInt)
    val maxChannel = max(raw.red, max(raw.green, raw.blue))
    val minChannel = min(raw.red, min(raw.green, raw.blue))
    val saturation = if (maxChannel > 0f) (maxChannel - minChannel) / maxChannel else 0f

    // If the cover is monochrome / black & white (like B&W album art), fallback to bright Spotify Green
    if (saturation < 0.15f) {
        return Color(0xFF1DB954)
    }

    // Boost brightness if the extracted color is too dark
    return if (raw.luminance() < 0.42f) {
        lerp(raw, Color.White, 0.42f)
    } else {
        raw
    }
}

data class DynamicIslandColorPreset(
    val name: String,
    val colorInt: Int,
)

val DynamicIslandAccentPresets = listOf(
    DynamicIslandColorPreset("Spotify Green", 0xFF1DB954.toInt()),
    DynamicIslandColorPreset("Electric Cyan", 0xFF00E5FF.toInt()),
    DynamicIslandColorPreset("Neon Purple", 0xFFA855F7.toInt()),
    DynamicIslandColorPreset("Crimson Rose", 0xFFFF4D67.toInt()),
    DynamicIslandColorPreset("Amber Gold", 0xFFF59E0B.toInt()),
    DynamicIslandColorPreset("Ocean Blue", 0xFF3B82F6.toInt()),
    DynamicIslandColorPreset("Coral Sunset", 0xFFFF6B4A.toInt()),
    DynamicIslandColorPreset("Pure White", 0xFFFFFFFF.toInt()),
)

val DynamicIslandBgPresets = listOf(
    DynamicIslandColorPreset("Deep Black", 0xFF000000.toInt()),
    DynamicIslandColorPreset("Charcoal Glass", 0xFF12141C.toInt()),
    DynamicIslandColorPreset("Midnight Blue", 0xFF0B132B.toInt()),
    DynamicIslandColorPreset("Dark Emerald", 0xFF071C15.toInt()),
    DynamicIslandColorPreset("Dark Wine", 0xFF210912.toInt()),
    DynamicIslandColorPreset("Cosmic Violet", 0xFF160C28.toInt()),
)

/**
 * Floating Dynamic Island overlay anchored at the top-center of the screen inside the app.
 * When the user leaves the app, [DynamicIslandSystemOverlayManager] in `MusicService` seamlessly
 * displays the exact same Dynamic Island over the Home screen / other apps if overlay permission is granted.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandOverlay(
    isPlayerExpanded: Boolean,
    onExpandPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    val (enabled, onEnabledChange) = rememberPreference(DynamicIslandEnabledKey, defaultValue = false)
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

    var showSettingsDialog by remember { mutableStateOf(false) }
    if (showSettingsDialog) {
        DynamicIslandSettingsDialog(onDismiss = { showSettingsDialog = false })
    }

    val mediaMetadata = playerConnection?.mediaMetadata?.collectAsState()?.value
    val isPlaying = playerConnection?.isPlaying?.collectAsState()?.value ?: false
    val playbackState = playerConnection?.playbackState?.collectAsState()?.value ?: Player.STATE_IDLE
    val currentSong = playerConnection?.currentSong?.collectAsState(initial = null)?.value
    val canSkipPrevious = playerConnection?.canSkipPrevious?.collectAsState()?.value ?: false
    val canSkipNext = playerConnection?.canSkipNext?.collectAsState()?.value ?: false
    val shuffleModeEnabled = playerConnection?.shuffleModeEnabled?.collectAsState()?.value ?: false
    val repeatMode = playerConnection?.repeatMode?.collectAsState()?.value ?: Player.REPEAT_MODE_OFF

    val metadata = mediaMetadata

    // Ketika musik diputar:
    // - Jika fitur Otomatis aktif (autoMode == true), Dynamic Island otomatis muncul.
    // - Jika fitur Otomatis tidak aktif (autoMode == false), Dynamic Island TIDAK akan muncul otomatis.
    LaunchedEffect(isPlaying, metadata?.id, autoMode) {
        if (autoMode && isPlaying && metadata != null) {
            if (isDismissed) {
                onDismissedChange(false)
            }
            if (!enabled) {
                onEnabledChange(true)
            }
        }
    }

    // Saat musik di-pause, Dynamic Island TIDAK hilang (tetap tampil selama enabled && !isDismissed).
    // Dynamic Island hanya otomatis hilang saat keluar dari aplikasi.
    val shouldShowIsland = enabled && !isDismissed

    if (!shouldShowIsland) return

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
                val result = context.imageLoader.execute(
                    ImageRequest.Builder(context)
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

    LaunchedEffect(playerConnection, playbackState, isPlaying, metadata?.id) {
        val conn = playerConnection ?: return@LaunchedEffect
        if (playbackState == Player.STATE_READY || isPlaying) {
            while (isActive) {
                position = conn.player.currentPosition.coerceAtLeast(0L)
                duration = conn.player.duration.coerceAtLeast(0L)
                delay(250L)
            }
        } else {
            position = conn.player.currentPosition.coerceAtLeast(0L)
            duration = conn.player.duration.coerceAtLeast(0L)
        }
    }

    val topStatusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val isLiked = currentSong?.song?.liked == true
    val displayTitle = metadata?.title ?: "Dynamic Island Aktif"
    val displayArtist = metadata?.artists?.joinToString(", ") { it.name } ?: "Putar lagu untuk mulai"
    val displayDuration = if (metadata != null) {
        duration.takeIf { it > 0L } ?: ((metadata.duration.toLong()) * 1000L)
    } else {
        0L
    }

    val handleDismissWithoutStoppingMusic = {
        onDismissedChange(true)
        Toast.makeText(
            context,
            "Dynamic Island disembunyikan (musik tetap berputar)",
            Toast.LENGTH_SHORT
        ).show()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = (topStatusBarPadding - 2.dp).coerceAtLeast(8.dp)),
        contentAlignment = Alignment.TopCenter
    ) {
        DynamicIslandCapsuleSurface(
            mode = mode,
            islandTheme = islandTheme,
            effectiveBgColor = effectiveBgColor,
            effectiveAccentColor = effectiveAccentColor,
            borderGlow = borderGlow,
            blurRadius = blurRadius,
            isSystemOverlay = false,
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
                if (playerConnection != null && metadata != null) {
                    playerConnection.player.togglePlayPause()
                } else {
                    showSettingsDialog = true
                }
            },
            onSeekTo = { newPos ->
                if (playerConnection != null && metadata != null) {
                    playerConnection.player.seekTo(newPos)
                }
            },
            onPrevious = { playerConnection?.seekToPrevious() },
            onNext = { playerConnection?.seekToNext() },
            onToggleLike = { playerConnection?.toggleLike() },
            onToggleShuffle = {
                playerConnection?.let { conn ->
                    conn.player.shuffleModeEnabled = !conn.player.shuffleModeEnabled
                }
            },
            onToggleRepeat = {
                playerConnection?.player?.toggleRepeatMode()
            },
            onOpenFullPlayer = {
                if (metadata != null && !isPlayerExpanded) {
                    onExpandPlayer()
                } else {
                    onModeChange(DynamicIslandMode.KECIL)
                }
            },
            onOpenCustomizeOrApp = { showSettingsDialog = true }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DynamicIslandCapsuleSurface(
    mode: DynamicIslandMode,
    islandTheme: DynamicIslandTheme,
    effectiveBgColor: Color,
    effectiveAccentColor: Color,
    borderGlow: Boolean,
    blurRadius: Int,
    isSystemOverlay: Boolean,
    displayTitle: String,
    displayArtist: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    isLiked: Boolean,
    shuffleModeEnabled: Boolean = false,
    repeatMode: Int = Player.REPEAT_MODE_OFF,
    position: Long,
    duration: Long,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onModeChange: (DynamicIslandMode) -> Unit,
    onDismissIsland: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onOpenFullPlayer: () -> Unit,
    onOpenCustomizeOrApp: () -> Unit,
) {
    val isLiquidGlass = islandTheme == DynamicIslandTheme.LIQUID_GLASS
    val cornerRadius by animateDpAsState(
        targetValue = if (mode == DynamicIslandMode.SEDANG) 30.dp else 50.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "islandCorner"
    )
    val shape = RoundedCornerShape(cornerRadius)

    var horizontalDragOffset by remember(mode) { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = horizontalDragOffset,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "islandDragOffsetX"
    )

    Surface(
        shape = shape,
        color = if (isLiquidGlass) {
            effectiveBgColor.copy(alpha = 0.24f)
        } else {
            effectiveBgColor.copy(alpha = 1f)
        },
        contentColor = Color.White,
        // Avoid rectangular shadow clipping inside WRAP_CONTENT WindowManager overlays
        shadowElevation = if (isSystemOverlay) 0.dp else 16.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
            .padding(horizontal = if (isSystemOverlay) 4.dp else 12.dp)
            .pointerInput(mode) {
                var horizontalDragSum = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        horizontalDragSum = 0f
                    },
                    onDragCancel = {
                        horizontalDragSum = 0f
                        horizontalDragOffset = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (!change.isConsumed) {
                            change.consume()
                            horizontalDragSum += dragAmount
                            horizontalDragOffset = (horizontalDragSum * 0.45f).coerceIn(-72f, 48f)
                        }
                    },
                    onDragEnd = {
                        horizontalDragOffset = 0f
                        if (horizontalDragSum < -35f) {
                            // Geser ke kiri -> ubah Dynamic Island menjadi Bola
                            if (mode != DynamicIslandMode.BOLA) {
                                onModeChange(DynamicIslandMode.BOLA)
                            }
                        } else if (horizontalDragSum > 35f && mode == DynamicIslandMode.BOLA) {
                            // Geser ke kanan saat mode Bola -> kembali normal (Mode Kecil)
                            onModeChange(DynamicIslandMode.KECIL)
                        }
                    }
                )
            }
            .pointerInput(mode) {
                var verticalDragSum = 0f
                detectVerticalDragGestures(
                    onDragStart = { verticalDragSum = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        verticalDragSum += dragAmount
                    },
                    onDragEnd = {
                        if (verticalDragSum < -40f) {
                            if (mode == DynamicIslandMode.SEDANG) {
                                onModeChange(DynamicIslandMode.KECIL)
                            } else {
                                onDismissIsland()
                            }
                        } else if (verticalDragSum > 40f) {
                            if (mode == DynamicIslandMode.BOLA) {
                                onModeChange(DynamicIslandMode.KECIL)
                            } else if (mode == DynamicIslandMode.KECIL) {
                                onModeChange(DynamicIslandMode.SEDANG)
                            }
                        }
                    }
                )
            }
            .then(
                if (isLiquidGlass) {
                    Modifier.border(
                        width = if (borderGlow) 1.6.dp else 1.25.dp,
                        brush = Brush.linearGradient(
                            colors = if (borderGlow) {
                                listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    effectiveAccentColor.copy(alpha = 0.78f),
                                    Color.White.copy(alpha = 0.25f),
                                    effectiveAccentColor.copy(alpha = 0.62f)
                                )
                            } else {
                                listOf(
                                    Color.White.copy(alpha = 0.68f),
                                    Color.White.copy(alpha = 0.16f),
                                    Color.White.copy(alpha = 0.36f)
                                )
                            }
                        ),
                        shape = shape
                    )
                } else if (borderGlow) {
                    Modifier.border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                effectiveAccentColor.copy(alpha = 0.85f),
                                Color.White.copy(alpha = 0.35f),
                                effectiveAccentColor.copy(alpha = 0.55f)
                            )
                        ),
                        shape = shape
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = shape
                    )
                }
            )
    ) {
        Box(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .height(IntrinsicSize.Max)
        ) {
            // Tema Liquid Glass: Frosted glass backdrop + blurred artwork + inner glow & specular reflection
            if (isLiquidGlass) {
                val safeBlurDp = blurRadius.coerceIn(8, 40).dp
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(shape)
                ) {
                    if (!thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(safeBlurDp)
                                .alpha(0.55f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            effectiveAccentColor.copy(alpha = 0.45f),
                                            effectiveBgColor.copy(alpha = 0.65f),
                                            effectiveAccentColor.copy(alpha = 0.30f)
                                        )
                                    )
                                )
                                .blur(safeBlurDp)
                        )
                    }
                    // Crystal-Clear ("Bening") Liquid Glass tint + top meniscus catchlight
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.28f),
                                        Color(0xFF0A0E18).copy(alpha = 0.22f),
                                        effectiveBgColor.copy(alpha = 0.34f)
                                    )
                                )
                            )
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.18f),
                                        Color.White.copy(alpha = 0.24f),
                                        Color.Transparent,
                                        Color(0xFFFF5500).copy(alpha = 0.14f)
                                    )
                                )
                            )
                    )
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.88f, animationSpec = tween(220)))
                        .togetherWith(fadeOut(tween(150)) + scaleOut(targetScale = 0.88f, animationSpec = tween(150)))
                },
                label = "DynamicIslandModeContent"
            ) { targetMode ->
                when (targetMode) {
                    DynamicIslandMode.BOLA -> {
                        DynamicIslandBallContent(
                            title = displayTitle,
                            thumbnailUrl = thumbnailUrl,
                            isPlaying = isPlaying,
                            position = position,
                            duration = duration,
                            accentColor = effectiveAccentColor,
                            onTap = {
                                // Klik pada bola mengembalikan Dynamic Island ke tampilan normal (Kecil)
                                onModeChange(DynamicIslandMode.KECIL)
                            },
                            onLongPress = {
                                onOpenFullPlayer()
                            }
                        )
                    }

                    DynamicIslandMode.KECIL -> {
                        DynamicIslandSmallContent(
                            title = displayTitle,
                            artist = displayArtist,
                            thumbnailUrl = thumbnailUrl,
                            isPlaying = isPlaying,
                            position = position,
                            duration = duration,
                            accentColor = effectiveAccentColor,
                            onTap = {
                                onModeChange(DynamicIslandMode.SEDANG)
                            },
                            onLongPress = {
                                onOpenFullPlayer()
                            },
                            onDismissIsland = onDismissIsland
                        )
                    }

                    DynamicIslandMode.SEDANG -> {
                        DynamicIslandMediumContent(
                            title = displayTitle,
                            artist = displayArtist,
                            thumbnailUrl = thumbnailUrl,
                            isPlaying = isPlaying,
                            isLiked = isLiked,
                            shuffleModeEnabled = shuffleModeEnabled,
                            repeatMode = repeatMode,
                            position = position,
                            duration = duration,
                            canSkipPrevious = canSkipPrevious,
                            canSkipNext = canSkipNext,
                            accentColor = effectiveAccentColor,
                            onTogglePlayPause = onTogglePlayPause,
                            onSeekTo = onSeekTo,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onToggleLike = onToggleLike,
                            onToggleShuffle = onToggleShuffle,
                            onToggleRepeat = onToggleRepeat,
                            onCollapseToSmall = { onModeChange(DynamicIslandMode.KECIL) },
                            onDismissIsland = onDismissIsland,
                            onOpenFullPlayer = onOpenFullPlayer,
                            onOpenCustomize = onOpenCustomizeOrApp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DynamicIslandBallContent(
    title: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    accentColor: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val safeDuration = duration.coerceAtLeast(1L)
    val rawProgress = if (duration > 0L) {
        (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "ballRingProgress"
    )
    val ringColor = remember(accentColor) {
        if (accentColor.luminance() < 0.25f) Color(0xFF1DB954) else accentColor
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        // Circular progress ring encircling the ball
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 2.8.dp.toPx()
            val inset = strokePx / 2f + 1.2.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            // Subtle circular track
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // Active circular music progress
            if (animatedProgress > 0.005f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = (animatedProgress * 360f).coerceIn(4f, 360f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.5.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.DarkGray)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = if (isPlaying) 0.38f else 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicIslandWaveform(
                        isPlaying = isPlaying,
                        accentColor = accentColor,
                        barCount = 3,
                        modifier = Modifier.size(width = 15.dp, height = 13.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.26f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        DynamicIslandWaveform(
                            isPlaying = true,
                            accentColor = accentColor,
                            barCount = 3,
                            modifier = Modifier.size(width = 16.dp, height = 14.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DynamicIslandSmallContent(
    title: String,
    artist: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    accentColor: Color,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDismissIsland: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val pillWidth = (configuration.screenWidthDp.dp * 0.80f).coerceIn(248.dp, 340.dp)

    val safeDuration = duration.coerceAtLeast(1L)
    val rawProgress = if (duration > 0L) {
        (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "smallLineProgress"
    )
    val barColor = remember(accentColor) {
        if (accentColor.luminance() < 0.25f) Color(0xFF1DB954) else accentColor
    }

    Box(
        modifier = Modifier
            .width(pillWidth)
            .height(46.dp)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Album art thumbnail or music note badge when idle
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(27.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(27.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Center: Pure bright #FFFFFF SansSerif Bold text
            val fullSmallLabel = remember(title, artist) {
                if (artist.isNotBlank() && artist != "Putar lagu untuk mulai" && artist != "Ketuk untuk kembali ke aplikasi") {
                    "$title • $artist"
                } else {
                    title
                }
            }
            Text(
                text = fullSmallLabel,
                color = Color(0xFFFFFFFF),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFFFFF),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Right: Animated audio waveform bars + Close/Hide button (keeps music playing)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                DynamicIslandWaveform(
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    barCount = 5,
                    modifier = Modifier.size(width = 22.dp, height = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.24f))
                        .clickable { onDismissIsland() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Tutup Dynamic Island (Musik Tetap Berputar)",
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Bottom horizontal music progress line (garis progres musik untuk Mode Kecil)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.5.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.24f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                barColor.copy(alpha = 0.88f),
                                barColor,
                                Color.White.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DynamicIslandMediumContent(
    title: String,
    artist: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    isLiked: Boolean,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    position: Long,
    duration: Long,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    accentColor: Color,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onCollapseToSmall: () -> Unit,
    onDismissIsland: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onOpenCustomize: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val cardWidth = (configuration.screenWidthDp.dp * 0.92f).coerceIn(300.dp, 388.dp)

    val safeDuration = duration.coerceAtLeast(1L)
    val progress = (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val remainingMs = (safeDuration - position).coerceAtLeast(0L)
    val visibleAccent = remember(accentColor) {
        if (accentColor.luminance() < 0.25f) Color(0xFF1DB954) else accentColor
    }

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable { onOpenFullPlayer() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Top Row: Album Art + Song Title & Artist + Customize (Palette) + Equalizer Waveform (Collapse) + Close/Hide Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.DarkGray)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color(0xFFFFFFFF),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFFFFF),
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = artist.ifBlank { "Unknown Artist" },
                    color = Color(0xFFF2F2F7),
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF2F2F7),
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Clicking the waveform collapses back to Mode Kecil
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onCollapseToSmall() }
                    .padding(horizontal = 5.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                DynamicIslandWaveform(
                    isPlaying = isPlaying,
                    accentColor = accentColor,
                    barCount = 5,
                    modifier = Modifier.size(width = 20.dp, height = 17.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Customize / Settings button
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .clickable { onOpenCustomize() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.palette),
                    contentDescription = "Kostumisasi Dynamic Island",
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(15.dp)
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            // Close / Hide Dynamic Island button (without stopping music)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable { onDismissIsland() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Tutup Dynamic Island (Musik Tetap Berputar)",
                    tint = Color(0xFFFFFFFF),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Middle Row: Elapsed time + Custom slim progress bar + Remaining time (-mm:ss)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = makeTimeString(position),
                color = Color(0xFFF2F2F7),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF2F2F7),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Interactive slim progress track matching the reference Spotify Dynamic Island
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .pointerInput(safeDuration) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeekTo((frac * safeDuration).toLong())
                        }
                    }
                    .pointerInput(safeDuration) {
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            onSeekTo((frac * safeDuration).toLong())
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.28f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(visibleAccent)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "-${makeTimeString(remainingMs)}",
                color = Color(0xFFF2F2F7),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF2F2F7),
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Row: Complete Playback Controls
        // Left: Shuffle + Rewind 10s + Previous | Center: Play/Pause | Right: Next + Repeat (Off/All/One) + Like
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Shuffle Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (shuffleModeEnabled) visibleAccent.copy(alpha = 0.22f) else Color.Transparent
                    )
                    .clickable { onToggleShuffle() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(
                            if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle
                        ),
                        contentDescription = "Acak Lagu (Shuffle)",
                        tint = if (shuffleModeEnabled) visibleAccent else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(19.dp)
                    )
                    if (shuffleModeEnabled) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .clip(CircleShape)
                                .background(visibleAccent)
                        )
                    }
                }
            }

            // 2. Rewind 10s Button
            IconButton(
                onClick = { onSeekTo((position - 10_000L).coerceAtLeast(0L)) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.replay),
                    contentDescription = "Mundur 10 Detik",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp)
                )
            }

            // 3. Previous Button
            IconButton(
                onClick = onPrevious,
                enabled = canSkipPrevious,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = "Sebelumnya",
                    tint = if (canSkipPrevious) Color.White else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 4. Center Play/Pause Button (White circle with black icon)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onTogglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 5. Next Button
            IconButton(
                onClick = onNext,
                enabled = canSkipNext,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = "Selanjutnya",
                    tint = if (canSkipNext) Color.White else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // 6. Repeat Button (cycles Off -> Repeat All -> Repeat One)
            val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
            val repeatIconRes = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                else -> R.drawable.repeat
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRepeatActive) visibleAccent.copy(alpha = 0.22f) else Color.Transparent
                    )
                    .clickable { onToggleRepeat() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(repeatIconRes),
                        contentDescription = "Ulangi Lagu (Repeat)",
                        tint = if (isRepeatActive) visibleAccent else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(19.dp)
                    )
                    if (isRepeatActive) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .clip(CircleShape)
                                .background(visibleAccent)
                        )
                    }
                }
            }

            // 7. Right button: Like / Favorite heart
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLiked) visibleAccent.copy(alpha = 0.20f) else Color.Transparent
                    )
                    .clickable { onToggleLike() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = "Sukai",
                    tint = if (isLiked) visibleAccent else Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun DynamicIslandWaveform(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 5,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "islandWave")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val phase3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(510, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val phase4 by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(390, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )
    val phase5 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar5"
    )

    val phases = listOf(phase1, phase2, phase3, phase4, phase5)

    // Guarantee the waveform color is always bright & visible even if a dark custom color was picked
    val visibleWaveColor = remember(accentColor) {
        if (accentColor.luminance() < 0.25f) {
            Color(0xFF1DB954)
        } else {
            accentColor
        }
    }

    Canvas(modifier = modifier) {
        val count = barCount.coerceIn(3, 5)
        val spacing = size.width * 0.11f
        val totalSpacing = spacing * (count - 1)
        val barWidth = ((size.width - totalSpacing) / count).coerceAtLeast(2.2f)

        for (i in 0 until count) {
            val fraction = if (isPlaying) phases[i % phases.size] else 0.32f
            val barHeight = (size.height * fraction).coerceAtLeast(size.height * 0.28f)
            val x = i * (barWidth + spacing)
            val y = (size.height - barHeight) / 2f
            drawRoundRect(
                color = visibleWaveColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Full settings & customization dialog for Dynamic Island:
 * - Opt-in activation toggle + "Otomatis Aktif Saat Buka / Restart APK" toggle
 * - Background Blur (Glassmorphism) toggle & blur intensity selector
 * - Mode selector: Kecil (Compact Pill) & Sedang (Expanded Card)
 * - Live interactive preview of the Dynamic Island
 * - Accent Color & Background Color customization + Dynamic Album Art color sync + Border Glow
 */
@Composable
fun DynamicIslandSettingsDialog(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val (enabled, onEnabledChange) = rememberPreference(DynamicIslandEnabledKey, defaultValue = false)
    val (autoEnableOnStart, onAutoEnableOnStartChange) = rememberPreference(
        DynamicIslandAutoEnableOnStartKey,
        defaultValue = false
    )
    val (isDismissed, onDismissedChange) = rememberPreference(
        DynamicIslandDismissedKey,
        defaultValue = false
    )
    val (mode, onModeChange) = rememberEnumPreference(
        DynamicIslandModeKey,
        defaultValue = DynamicIslandMode.KECIL
    )
    val (islandTheme, onIslandThemeChange) = rememberEnumPreference(
        DynamicIslandThemeKey,
        defaultValue = DynamicIslandTheme.LIQUID_GLASS
    )
    val (accentColorInt, onAccentColorChange) = rememberPreference(
        DynamicIslandAccentColorKey,
        defaultValue = 0xFF1DB954.toInt()
    )
    val (useDynamicAccent, onUseDynamicAccentChange) = rememberPreference(
        DynamicIslandUseDynamicAccentKey,
        defaultValue = false
    )
    val (bgColorInt, onBgColorChange) = rememberPreference(
        DynamicIslandBgColorKey,
        defaultValue = 0xFF000000.toInt()
    )
    val (useDynamicBg, onUseDynamicBgChange) = rememberPreference(
        DynamicIslandUseDynamicBgKey,
        defaultValue = false
    )
    val (borderGlow, onBorderGlowChange) = rememberPreference(
        DynamicIslandBorderGlowKey,
        defaultValue = true
    )
    val (blurRadius, onBlurRadiusChange) = rememberPreference(
        DynamicIslandBlurRadiusKey,
        defaultValue = 22
    )
    val (permissionConfirmed, onPermissionConfirmedChange) = rememberPreference(
        DynamicIslandPermissionConfirmedKey,
        defaultValue = false
    )

    var canDrawOverlays by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    // Refresh permission states whenever user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else {
                    true
                }
                hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                if (canDrawOverlays) {
                    onPermissionConfirmedChange(true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val playerConnection = LocalPlayerConnection.current
    val isMusicPlayingNow =
        (playerConnection?.isPlaying?.collectAsState()?.value == true) &&
            (playerConnection.mediaMetadata.collectAsState().value != null)

    var showPermissionPromptDialog by remember { mutableStateOf(false) }
    var pendingManualActivation by remember { mutableStateOf(false) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        onPermissionConfirmedChange(true)
        onDismissedChange(false)
        if (pendingManualActivation || isMusicPlayingNow) {
            onEnabledChange(true)
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        onPermissionConfirmedChange(true)
        onDismissedChange(false)
        if (pendingManualActivation || isMusicPlayingNow) {
            onEnabledChange(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            runCatching {
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                )
            }.onFailure {
                runCatching {
                    overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
            }
        }
    }

    val launchOverlaySettings = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                overlayPermissionLauncher.launch(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                )
            }.onFailure {
                runCatching {
                    overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
            }
        }
    }

    val previewAccent = Color(accentColorInt)
    val previewBg = Color(bgColorInt)

    if (showPermissionPromptDialog) {
        Dialog(
            onDismissRequest = { showPermissionPromptDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val permDialogShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 400.dp)
                    .clip(permDialogShape)
                    .frostedGlassSurface(
                        shape = permDialogShape,
                        isHighlighted = true,
                        baseTint = Color(0xFF0A1120).copy(alpha = 0.44f)
                    )
            ) {
                // Translucent Liquid Glass backdrop with hardware blur & refractive light orbs
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(permDialogShape)
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Modifier.blur(28.dp)
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.22f),
                                    previewAccent.copy(alpha = 0.16f),
                                    Color(0xFF38BDF8).copy(alpha = 0.12f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            )
                        )
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                previewAccent.copy(alpha = 0.34f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.18f, size.height * 0.15f),
                            radius = size.width * 0.62f
                        ),
                        center = Offset(size.width * 0.18f, size.height * 0.15f),
                        radius = size.width * 0.62f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.82f),
                            radius = size.width * 0.56f
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.82f),
                        radius = size.width * 0.56f
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .liquidGlassPill(
                                    shape = CircleShape,
                                    isSelected = true,
                                    accentColor = Color(0xFF1DB954)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.notification),
                                contentDescription = null,
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Izin Dynamic Island",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Muncul di dalam & di luar aplikasi",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Agar Dynamic Island dapat tampil dengan optimal baik di dalam maupun di luar aplikasi, periksa izin berikut:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Overlay Permission Card (Outside App)
                    val permCardShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(permCardShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .frostedGlassSurface(
                                shape = permCardShape,
                                isHighlighted = canDrawOverlays,
                                baseTint = if (canDrawOverlays) Color(0xFF1DB954).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.12f)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(if (canDrawOverlays) R.drawable.check else R.drawable.info),
                                    contentDescription = null,
                                    tint = if (canDrawOverlays) Color(0xFF1DB954) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tampilkan di Atas Aplikasi Lain",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Diperlukan agar kapsul Dynamic Island tetap melayang di layar saat kamu membuka Home atau aplikasi lain.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Notification & Media Session Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(permCardShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .frostedGlassSurface(
                                shape = permCardShape,
                                isHighlighted = true,
                                baseTint = Color(0xFF1DB954).copy(alpha = 0.1f)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Notifikasi & Sesi Media Aktif",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Menjaga sinkronisasi pemutaran musik, sampul album, dan durasi lagu secara real-time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showPermissionPromptDialog = false
                                onPermissionConfirmedChange(true)
                                onDismissedChange(false)
                                if (pendingManualActivation || isMusicPlayingNow) {
                                    onEnabledChange(true)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else if (!canDrawOverlays) {
                                    launchOverlaySettings()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .frostedGlassSurface(
                                    shape = RoundedCornerShape(14.dp),
                                    isHighlighted = true,
                                    baseTint = Color(0xFF1DB954).copy(alpha = 0.35f)
                                ),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1DB954).copy(alpha = 0.85f),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = if (!canDrawOverlays) "Berikan Izin Overlay" else "Izinkan & Aktifkan",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        TextButton(
                            onClick = {
                                showPermissionPromptDialog = false
                                onPermissionConfirmedChange(true)
                                onDismissedChange(false)
                                if (pendingManualActivation || isMusicPlayingNow) {
                                    onEnabledChange(true)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .liquidGlassPill(
                                    shape = RoundedCornerShape(14.dp),
                                    isSelected = false
                                ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Aktifkan di Dalam Aplikasi Saja",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val mainDialogShape = RoundedCornerShape(30.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 440.dp)
                .padding(vertical = 18.dp)
                .clip(mainDialogShape)
                .frostedGlassSurface(
                    shape = mainDialogShape,
                    isHighlighted = true,
                    baseTint = Color(0xFF0A1222).copy(alpha = 0.38f)
                )
        ) {
            // Frosted 3D Liquid Glass translucent backdrop layer with hardware blur & prismatic sheen
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(mainDialogShape)
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(blurRadius.dp.coerceIn(16.dp, 36.dp))
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                previewAccent.copy(alpha = 0.16f),
                                Color(0xFF38BDF8).copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    )
            )

            // Refractive Liquid Glass ambient glow orbs behind the dialog content
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            previewAccent.copy(alpha = 0.34f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.1f),
                        radius = size.width * 0.7f
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.1f),
                    radius = size.width * 0.7f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF38BDF8).copy(alpha = 0.24f),
                            Color(0xFF6366F1).copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.45f),
                        radius = size.width * 0.65f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.45f),
                    radius = size.width * 0.65f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            previewAccent.copy(alpha = 0.24f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.25f, size.height * 0.88f),
                        radius = size.width * 0.6f
                    ),
                    center = Offset(size.width * 0.25f, size.height * 0.88f),
                    radius = size.width * 0.6f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Pengaturan Dynamic Island",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Kapsul musik interaktif • Liquid Glass 3D",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .liquidGlassPill(
                                shape = CircleShape,
                                isSelected = true,
                                accentColor = previewAccent
                            )
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Tutup",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Interactive Preview Box (Liquid Glass stage)
                val previewStageShape = RoundedCornerShape(22.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(previewStageShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    previewAccent.copy(alpha = 0.18f),
                                    Color(0xFF38BDF8).copy(alpha = 0.12f)
                                )
                            )
                        )
                        .frostedGlassSurface(
                            shape = previewStageShape,
                            isHighlighted = true,
                            baseTint = Color.White.copy(alpha = 0.10f)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val previewModeLabel = when (mode) {
                            DynamicIslandMode.BOLA -> "MODE BOLA"
                            DynamicIslandMode.KECIL -> "MODE KECIL"
                            DynamicIslandMode.SEDANG -> "MODE SEDANG"
                        }
                        val previewThemeLabel = when (islandTheme) {
                            DynamicIslandTheme.NORMAL -> "TEMA NORMAL"
                            DynamicIslandTheme.LIQUID_GLASS -> "TEMA LIQUID GLASS"
                        }
                        Text(
                            text = "PRATINJAU LANGSUNG • $previewModeLabel • $previewThemeLabel",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Render the selected mode & theme inside the preview container
                        val previewShape = RoundedCornerShape(
                            if (mode == DynamicIslandMode.SEDANG) 26.dp else 50.dp
                        )
                        val isPreviewGlass = islandTheme == DynamicIslandTheme.LIQUID_GLASS
                        Surface(
                            shape = previewShape,
                            color = if (isPreviewGlass) previewBg.copy(alpha = 0.45f) else previewBg,
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .pointerInput(mode) {
                                    var dragSum = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragSum = 0f },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            dragSum += dragAmount
                                        },
                                        onDragEnd = {
                                            if (dragSum < -30f && mode != DynamicIslandMode.BOLA) {
                                                onModeChange(DynamicIslandMode.BOLA)
                                            } else if (dragSum > 30f && mode == DynamicIslandMode.BOLA) {
                                                onModeChange(DynamicIslandMode.KECIL)
                                            }
                                        }
                                    )
                                }
                                .then(
                                    if (isPreviewGlass) {
                                        Modifier
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.25f),
                                                        previewAccent.copy(alpha = 0.22f),
                                                        Color.White.copy(alpha = 0.08f)
                                                    )
                                                ),
                                                shape = previewShape
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.85f),
                                                        previewAccent.copy(alpha = 0.75f),
                                                        Color.White.copy(alpha = 0.25f)
                                                    )
                                                ),
                                                shape = previewShape
                                            )
                                    } else if (borderGlow) {
                                        Modifier.border(
                                            1.2.dp,
                                            previewAccent.copy(alpha = 0.72f),
                                            previewShape
                                        )
                                    } else {
                                        Modifier.border(
                                            0.85.dp,
                                            Color.White.copy(alpha = 0.22f),
                                            previewShape
                                        )
                                    }
                                )
                        ) {
                            if (mode == DynamicIslandMode.BOLA) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .clickable { onModeChange(DynamicIslandMode.KECIL) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokePx = 2.6.dp.toPx()
                                        val inset = strokePx / 2f + 1.dp.toPx()
                                        val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                                        drawArc(
                                            color = Color.White.copy(alpha = 0.22f),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            topLeft = Offset(inset, inset),
                                            size = arcSize,
                                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = previewAccent,
                                            startAngle = -90f,
                                            sweepAngle = 210f,
                                            useCenter = false,
                                            topLeft = Offset(inset, inset),
                                            size = arcSize,
                                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(5.5.dp)
                                            .clip(CircleShape)
                                            .background(previewAccent.copy(alpha = 0.28f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DynamicIslandWaveform(
                                            isPlaying = true,
                                            accentColor = previewAccent,
                                            barCount = 3,
                                            modifier = Modifier.size(width = 15.dp, height = 13.dp)
                                        )
                                    }
                                }
                            } else if (mode == DynamicIslandMode.KECIL) {
                                Box(
                                    modifier = Modifier
                                        .width(230.dp)
                                        .height(44.dp)
                                        .clickable { onModeChange(DynamicIslandMode.SEDANG) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 10.dp, end = 7.dp, top = 4.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(previewAccent.copy(alpha = 0.28f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.music_note),
                                                contentDescription = null,
                                                tint = previewAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = "Passionfruit • Drake",
                                            color = Color(0xFFFFFFFF),
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFFFFFF)
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            DynamicIslandWaveform(
                                                isPlaying = true,
                                                accentColor = previewAccent,
                                                barCount = 5,
                                                modifier = Modifier.size(width = 18.dp, height = 15.dp)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.22f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.close),
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                            }
                                        }
                                    }
                                    // Bottom progress line (garis progres musik)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 3.dp)
                                            .height(2.8.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color.White.copy(alpha = 0.24f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.56f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(50))
                                                .background(previewAccent)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onModeChange(DynamicIslandMode.KECIL) }
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(previewAccent.copy(alpha = 0.28f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.music_note),
                                                contentDescription = null,
                                                tint = previewAccent,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Passionfruit",
                                                color = Color(0xFFFFFFFF),
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFFFFFF)
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Drake",
                                                color = Color(0xFFF2F2F7),
                                                style = TextStyle(
                                                    fontFamily = FontFamily.SansSerif,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFF2F2F7)
                                                ),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        DynamicIslandWaveform(
                                            isPlaying = true,
                                            accentColor = previewAccent,
                                            barCount = 5,
                                            modifier = Modifier.size(width = 20.dp, height = 16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.palette),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.close),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "1:10",
                                            color = Color(0xFFF2F2F7),
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFF2F2F7)
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(4.5.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(Color.White.copy(alpha = 0.28f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.38f)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(50))
                                                    .background(previewAccent)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "-2:10",
                                            color = Color(0xFFF2F2F7),
                                            style = TextStyle(
                                                fontFamily = FontFamily.SansSerif,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFF2F2F7)
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.shuffle),
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.replay),
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.skip_previous),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.pause),
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(R.drawable.skip_next),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.repeat_on),
                                            contentDescription = null,
                                            tint = previewAccent,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.favorite),
                                            contentDescription = null,
                                            tint = previewAccent,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // If Dynamic Island was hidden via the X button while enabled, offer a 1-tap restore banner
                if (enabled && isDismissed) {
                    val restoreShape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(restoreShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .frostedGlassSurface(
                                shape = restoreShape,
                                isHighlighted = true,
                                baseTint = previewAccent.copy(alpha = 0.2f)
                            )
                            .clickable { onDismissedChange(false) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dynamic Island Disembunyikan",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ketuk untuk menampilkan kembali kapsul di atas layar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .liquidGlassPill(
                                        shape = RoundedCornerShape(50),
                                        isSelected = true,
                                        accentColor = previewAccent
                                    )
                                    .clickable { onDismissedChange(false) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Munculkan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // 1. Enable / Disable Toggle (Manual vs Otomatis Saat Musik Diputar) — Liquid Glass Card
                val toggleCardShape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(toggleCardShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .frostedGlassSurface(
                            shape = toggleCardShape,
                            isHighlighted = true,
                            baseTint = if (enabled || autoEnableOnStart) {
                                previewAccent.copy(alpha = 0.16f)
                            } else {
                                Color.White.copy(alpha = 0.08f)
                            }
                        )
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!enabled) {
                                        pendingManualActivation = true
                                        onDismissedChange(false)
                                        if (!canDrawOverlays || !permissionConfirmed || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)) {
                                            showPermissionPromptDialog = true
                                        } else {
                                            onEnabledChange(true)
                                        }
                                    } else {
                                        onEnabledChange(false)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Aktifkan Manual",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (enabled) {
                                        "Aktif • Tetap tampil saat musik di-pause & otomatis hilang saat keluar aplikasi"
                                    } else {
                                        "Nonaktif • Nyalakan manual atau gunakan fitur Otomatis di bawah"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = enabled,
                                onCheckedChange = { wantEnable ->
                                    if (wantEnable) {
                                        pendingManualActivation = true
                                        onDismissedChange(false)
                                        if (!canDrawOverlays || !permissionConfirmed || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)) {
                                            showPermissionPromptDialog = true
                                        } else {
                                            onEnabledChange(true)
                                        }
                                    } else {
                                        onEnabledChange(false)
                                    }
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = Color.White.copy(alpha = 0.14f)
                        )

                        // Auto-Trigger on Music Playback toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val nextAuto = !autoEnableOnStart
                                    onAutoEnableOnStartChange(nextAuto)
                                    if (nextAuto) {
                                        pendingManualActivation = false
                                        onDismissedChange(false)
                                        if (!canDrawOverlays || !permissionConfirmed || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)) {
                                            showPermissionPromptDialog = true
                                        } else {
                                            onEnabledChange(isMusicPlayingNow)
                                        }
                                    } else {
                                        onEnabledChange(false)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Otomatis Saat Memutar Musik",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (autoEnableOnStart) {
                                        "Aktif • Muncul otomatis saat musik diputar & hilang saat keluar aplikasi"
                                    } else {
                                        "Nonaktif • Dynamic Island hanya muncul jika diaktifkan manual"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = autoEnableOnStart,
                                onCheckedChange = { nextAuto ->
                                    onAutoEnableOnStartChange(nextAuto)
                                    if (nextAuto) {
                                        pendingManualActivation = false
                                        onDismissedChange(false)
                                        if (!canDrawOverlays || !permissionConfirmed || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission)) {
                                            showPermissionPromptDialog = true
                                        } else {
                                            onEnabledChange(isMusicPlayingNow)
                                        }
                                    } else {
                                        onEnabledChange(false)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Overlay Permission status card (For Outside-App display) — Liquid Glass & zero squished text
                val overlayPermShape = RoundedCornerShape(16.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(overlayPermShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .frostedGlassSurface(
                            shape = overlayPermShape,
                            isHighlighted = true,
                            baseTint = if (canDrawOverlays) {
                                Color(0xFF1DB954).copy(alpha = 0.12f)
                            } else {
                                Color(0xFFF59E0B).copy(alpha = 0.16f)
                            }
                        )
                        .clickable {
                            if (!canDrawOverlays) {
                                launchOverlaySettings()
                            } else {
                                showPermissionPromptDialog = true
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (canDrawOverlays) R.drawable.check else R.drawable.info
                                ),
                                contentDescription = null,
                                tint = if (canDrawOverlays) Color(0xFF1DB954) else Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (canDrawOverlays) {
                                    "Izin Overlay Luar Aplikasi"
                                } else {
                                    "Izin Tampil di Atas Aplikasi"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .liquidGlassPill(
                                        shape = RoundedCornerShape(50),
                                        isSelected = true,
                                        accentColor = if (canDrawOverlays) Color(0xFF1DB954) else Color(0xFFF59E0B)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (canDrawOverlays) "Aktif" else "Berikan Izin",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (canDrawOverlays) Color(0xFF4ADE80) else Color(0xFFFDE68A),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (canDrawOverlays) {
                                "Kapsul dapat tetap melayang saat kamu membuka layar utama atau aplikasi lain."
                            } else {
                                "Saat ini hanya tampil di dalam aplikasi. Ketuk di sini untuk memberikan izin overlay."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Mode Selection (Bola vs Kecil vs Sedang) — Liquid Glass Cards
                Text(
                    text = "Ukuran & Mode Dynamic Island",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = previewAccent
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Geser kapsul ke kiri untuk jadi Bola • Ketuk Bola untuk kembali normal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modeCardShape = RoundedCornerShape(16.dp)
                    val isBall = mode == DynamicIslandMode.BOLA
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(modeCardShape)
                            .background(Color.White.copy(alpha = if (isBall) 0.1f else 0.05f))
                            .frostedGlassSurface(
                                shape = modeCardShape,
                                isHighlighted = isBall,
                                baseTint = if (isBall) previewAccent.copy(alpha = 0.22f) else null
                            )
                            .clickable {
                                onDismissedChange(false)
                                onModeChange(DynamicIslandMode.BOLA)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokePx = 1.8.dp.toPx()
                                    val inset = strokePx / 2f + 0.5.dp.toPx()
                                    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                                    drawArc(
                                        color = Color.White.copy(alpha = 0.25f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = arcSize,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = previewAccent,
                                        startAngle = -90f,
                                        sweepAngle = 220f,
                                        useCenter = false,
                                        topLeft = Offset(inset, inset),
                                        size = arcSize,
                                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(previewAccent)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Bola",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Ring progres",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val isSmall = mode == DynamicIslandMode.KECIL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(modeCardShape)
                            .background(Color.White.copy(alpha = if (isSmall) 0.1f else 0.05f))
                            .frostedGlassSurface(
                                shape = modeCardShape,
                                isHighlighted = isSmall,
                                baseTint = if (isSmall) previewAccent.copy(alpha = 0.22f) else null
                            )
                            .clickable {
                                onDismissedChange(false)
                                onModeChange(DynamicIslandMode.KECIL)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(52.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 5.dp, end = 5.dp, top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(previewAccent)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(10.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(previewAccent)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                        .height(2.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.25f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(50))
                                            .background(previewAccent)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Kecil",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Garis progres",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val isMedium = mode == DynamicIslandMode.SEDANG
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(modeCardShape)
                            .background(Color.White.copy(alpha = if (isMedium) 0.1f else 0.05f))
                            .frostedGlassSurface(
                                shape = modeCardShape,
                                isHighlighted = isMedium,
                                baseTint = if (isMedium) previewAccent.copy(alpha = 0.22f) else null
                            )
                            .clickable {
                                onDismissedChange(false)
                                onModeChange(DynamicIslandMode.SEDANG)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(previewAccent)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sedang",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Kontrol penuh",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                Spacer(modifier = Modifier.height(14.dp))

                // 3. Tema Dynamic Island (Normal vs Liquid Glass)
                Text(
                    text = "Tema Dynamic Island",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = previewAccent
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Pilih gaya visual kapsul antara Normal (hitam pekat) atau Liquid Glass (kaca buram transparan)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val themeCardShape = RoundedCornerShape(16.dp)
                    val isNormalTheme = islandTheme == DynamicIslandTheme.NORMAL
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(themeCardShape)
                            .background(Color.White.copy(alpha = if (isNormalTheme) 0.1f else 0.05f))
                            .frostedGlassSurface(
                                shape = themeCardShape,
                                isHighlighted = isNormalTheme,
                                baseTint = if (isNormalTheme) previewAccent.copy(alpha = 0.22f) else null
                            )
                            .clickable {
                                onIslandThemeChange(DynamicIslandTheme.NORMAL)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black)
                                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(previewAccent)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Normal",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Kapsul gelap solid",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val isGlassTheme = islandTheme == DynamicIslandTheme.LIQUID_GLASS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(themeCardShape)
                            .background(Color.White.copy(alpha = if (isGlassTheme) 0.1f else 0.05f))
                            .frostedGlassSurface(
                                shape = themeCardShape,
                                isHighlighted = isGlassTheme,
                                baseTint = if (isGlassTheme) previewAccent.copy(alpha = 0.22f) else null
                            )
                            .clickable {
                                onIslandThemeChange(DynamicIslandTheme.LIQUID_GLASS)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(22.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.38f),
                                                previewAccent.copy(alpha = 0.35f),
                                                Color.White.copy(alpha = 0.16f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.2.dp,
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.85f),
                                                previewAccent.copy(alpha = 0.7f)
                                            )
                                        ),
                                        RoundedCornerShape(50)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Liquid Glass",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Kaca transparan 3D",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = islandTheme == DynamicIslandTheme.LIQUID_GLASS) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val blurOptions = listOf(
                            "Halus" to 12,
                            "Sedang" to 22,
                            "Kuat" to 34
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            blurOptions.forEach { (label, radiusVal) ->
                                val selected = abs(blurRadius - radiusVal) <= 4
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .liquidGlassPill(
                                            shape = RoundedCornerShape(12.dp),
                                            isSelected = selected,
                                            accentColor = previewAccent
                                        )
                                        .clickable { onBlurRadiusChange(radiusVal) }
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Blur $label",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                Spacer(modifier = Modifier.height(14.dp))

                // 4, 5, 6. Color Customization & Border Glow inside a unified Liquid Glass panel
                val colorPanelShape = RoundedCornerShape(18.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(colorPanelShape)
                        .background(Color.White.copy(alpha = 0.10f))
                        .frostedGlassSurface(
                            shape = colorPanelShape,
                            isHighlighted = true,
                            baseTint = previewAccent.copy(alpha = 0.12f)
                        )
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 4. Accent Color
                        Text(
                            text = "Warna Aksen (Waveform & Progress)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = previewAccent
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUseDynamicAccentChange(!useDynamicAccent) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Otomatis ikuti warna sampul album",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = useDynamicAccent,
                                onCheckedChange = onUseDynamicAccentChange
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DynamicIslandAccentPresets.forEach { preset ->
                                val isSelected = !useDynamicAccent && accentColorInt == preset.colorInt
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        onUseDynamicAccentChange(false)
                                        onAccentColorChange(preset.colorInt)
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.colorInt))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(R.drawable.check),
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preset.name.substringBefore(" "),
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // 5. Island Background Color
                        Text(
                            text = "Warna Latar Kapsul",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = previewAccent
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUseDynamicBgChange(!useDynamicBg) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nuansa gelap sampul album otomatis",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = useDynamicBg,
                                onCheckedChange = onUseDynamicBgChange
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DynamicIslandBgPresets.forEach { preset ->
                                val isSelected = !useDynamicBg && bgColorInt == preset.colorInt
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable {
                                        onUseDynamicBgChange(false)
                                        onBgColorChange(preset.colorInt)
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(preset.colorInt))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) previewAccent else Color.White.copy(alpha = 0.25f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                painter = painterResource(R.drawable.check),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preset.name.substringAfter(" "),
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // 6. Border Glow Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBorderGlowChange(!borderGlow) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Garis Tepi Berkilau (Border Glow)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tampilkan aksen garis tepi halus di sekeliling Dynamic Island",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = borderGlow,
                                onCheckedChange = onBorderGlowChange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .frostedGlassSurface(
                            shape = RoundedCornerShape(16.dp),
                            isHighlighted = true,
                            baseTint = previewAccent.copy(alpha = 0.35f)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = previewAccent.copy(alpha = 0.88f),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "Selesai",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
