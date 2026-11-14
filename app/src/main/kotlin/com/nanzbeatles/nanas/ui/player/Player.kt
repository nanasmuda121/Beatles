/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.LocalDownloadUtil
import com.nanzbeatles.nanas.LocalListenTogetherManager
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.AudioStyleMode
import com.nanzbeatles.nanas.constants.CropAlbumArtKey
import com.nanzbeatles.nanas.constants.DarkModeKey
import com.nanzbeatles.nanas.constants.HidePlayerThumbnailKey
import com.nanzbeatles.nanas.constants.KeepScreenOn
import com.nanzbeatles.nanas.constants.PlayerBackgroundStyle
import com.nanzbeatles.nanas.constants.PlayerBackgroundStyleKey
import com.nanzbeatles.nanas.constants.PlayerButtonsStyle
import com.nanzbeatles.nanas.constants.PlayerButtonsStyleKey
import com.nanzbeatles.nanas.constants.PlayerHorizontalPadding
import com.nanzbeatles.nanas.constants.ShowLyricsKey
import com.nanzbeatles.nanas.constants.SliderStyle
import com.nanzbeatles.nanas.constants.SliderStyleKey
import com.nanzbeatles.nanas.constants.SquigglySliderKey
import com.nanzbeatles.nanas.constants.UseNewPlayerDesignKey
import com.nanzbeatles.nanas.db.entities.LyricsEntity
import com.nanzbeatles.nanas.extensions.togglePlayPause
import com.nanzbeatles.nanas.extensions.toggleRepeatMode
import com.nanzbeatles.nanas.listentogether.RoomRole
import com.nanzbeatles.nanas.ui.utils.crystalLiquidGlassPill
import com.nanzbeatles.nanas.ui.utils.crystalLiquidGlassSurface
import com.nanzbeatles.nanas.ui.utils.frostedGlassSurface
import com.nanzbeatles.nanas.ui.utils.liquidGlassPill
import com.nanzbeatles.nanas.models.MediaMetadata
import com.nanzbeatles.nanas.playback.ExoDownloadService
import com.nanzbeatles.nanas.ui.component.AudioVisualizerSlider
import com.nanzbeatles.nanas.ui.component.BottomSheet
import com.nanzbeatles.nanas.ui.component.BottomSheetState
import com.nanzbeatles.nanas.ui.component.Icon as MIcon
import com.nanzbeatles.nanas.ui.component.LocalBottomSheetPageState
import com.nanzbeatles.nanas.ui.component.LocalMenuState
import com.nanzbeatles.nanas.ui.component.Lyrics
import com.nanzbeatles.nanas.ui.component.PlayerSliderTrack
import com.nanzbeatles.nanas.ui.component.SamsungSlider
import com.nanzbeatles.nanas.ui.component.ShareSongBottomSheet
import com.nanzbeatles.nanas.ui.component.SquigglySlider
import com.nanzbeatles.nanas.ui.component.WavySlider
import com.nanzbeatles.nanas.ui.component.rememberBottomSheetState
import com.nanzbeatles.nanas.ui.menu.LyricsMenu
import com.nanzbeatles.nanas.ui.menu.PlayerMenu
import com.nanzbeatles.nanas.ui.screens.settings.DarkMode
import com.nanzbeatles.nanas.ui.theme.PlayerColorExtractor
import com.nanzbeatles.nanas.ui.theme.PlayerSliderColors
import com.nanzbeatles.nanas.ui.utils.ShowMediaInfo
import com.nanzbeatles.nanas.ui.utils.ShowOffsetDialog
import com.nanzbeatles.nanas.ui.utils.resolveArtworkUrl
import com.nanzbeatles.nanas.ui.utils.toHighQualityThumbnail
import com.nanzbeatles.nanas.utils.ShareUtils
import com.nanzbeatles.nanas.utils.makeTimeString
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, UnstableApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val useNewPlayerDesign by rememberPreference(UseNewPlayerDesignKey, true)
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, true)
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.BLUR
    )
    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val isEditorial = com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL"
    val effectivePlayerBackground = if (isEditorial) PlayerBackgroundStyle.DEFAULT else playerBackground

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val audioStyleMode by playerConnection.audioStyleMode.collectAsState()
    val isKeepScreenOn by rememberPreference(KeepScreenOn, false)
    val keepScreenOn = isPlaying && isKeepScreenOn

    DisposableEffect(effectivePlayerBackground, state.isExpanded, useDarkTheme, keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (window != null && state.isExpanded) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)

            when (effectivePlayerBackground) {
                PlayerBackgroundStyle.BLUR,
                PlayerBackgroundStyle.GRADIENT,
                PlayerBackgroundStyle.ANIMATED_GRADIENT -> {
                    insetsController.isAppearanceLightStatusBars = false
                }
                PlayerBackgroundStyle.DEFAULT -> {
                    insetsController.isAppearanceLightStatusBars = !useDarkTheme
                }
            }

            if (keepScreenOn && state.isExpanded)
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !useDarkTheme
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val onBackgroundColor = when (effectivePlayerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDark =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDark && pureBlack
        }

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val isMuted by playerConnection.isMuted.collectAsState()
    val playerVolume by playerConnection.service.playerVolume.collectAsState()

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val squigglySlider by rememberPreference(SquigglySliderKey, defaultValue = false)

    // Video mode state
    val videoModeEnabled by playerConnection.videoModeEnabled.collectAsState()
    val videoFetchError by playerConnection.videoFetchError.collectAsState()
    val videoModeMessage by playerConnection.videoModeMessage.collectAsState()

    LaunchedEffect(videoFetchError, videoModeMessage) {
        videoFetchError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            playerConnection.clearVideoError()
        }
        videoModeMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            playerConnection.clearVideoModeMessage()
        }
    }

    // Listen Together state
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    // Cast state
    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castPosition by castHandler?.castPosition?.collectAsState() ?: remember { mutableLongStateOf(0L) }
    val castDuration by castHandler?.castDuration?.collectAsState() ?: remember { mutableLongStateOf(0L) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsState() ?: remember { mutableFloatStateOf(1f) }

    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val positionState = remember { mutableLongStateOf(0L) }
    val durationState = remember { mutableLongStateOf(0L) }

    var position by positionState
    var duration by durationState

    val effectivePosition by remember {
        derivedStateOf {
            if (isCasting) castPosition else position
        }
    }

    val sliderPositionState = remember { mutableStateOf<Long?>(null) }
    var sliderPosition by sliderPositionState
    var lastManualSeekTime by remember { mutableLongStateOf(0L) }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata?.id, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT ||
            playerBackground == PlayerBackgroundStyle.ANIMATED_GRADIENT
        ) {
            val currentMetadata = mediaMetadata
            val resolvedThumb = currentMetadata?.let { resolveArtworkUrl(it.thumbnailUrl, it.id) }
            if (currentMetadata != null && resolvedThumb != null) {
                val cachedColors = gradientColorsCache[currentMetadata.id]
                if (cachedColors != null) {
                    gradientColors = cachedColors
                    return@LaunchedEffect
                }
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(resolvedThumb)
                        .size(100, 100)
                        .allowHardware(false)
                        .memoryCacheKey("gradient_${currentMetadata.id}")
                        .build()

                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    if (result != null) {
                        val bitmap = result.image?.toBitmap()
                        if (bitmap != null) {
                            val palette = withContext(Dispatchers.Default) {
                                Palette.from(bitmap)
                                    .maximumColorCount(8)
                                    .resizeBitmapArea(100 * 100)
                                    .generate()
                            }
                            val extractedColors = PlayerColorExtractor.extractGradientColors(
                                palette = palette,
                                fallbackColor = fallbackColor
                            )
                            gradientColorsCache[currentMetadata.id] = extractedColors
                            withContext(Dispatchers.Main) { gradientColors = extractedColors }
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val TextBackgroundColor by animateColorAsState(
        targetValue = if (isEditorial) {
            if (useDarkTheme) Color(0xFFF5F2EB) else Color(0xFF111111)
        } else {
            when (effectivePlayerBackground) {
                PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
                PlayerBackgroundStyle.BLUR -> Color.White
                PlayerBackgroundStyle.GRADIENT -> Color.White
                PlayerBackgroundStyle.ANIMATED_GRADIENT -> Color.White
            }
        },
        label = "TextBackgroundColor"
    )

    val (textButtonColor, iconButtonColor) = when {
        isEditorial -> {
            if (useDarkTheme) Pair(Color(0xFFF5F2EB), Color(0xFF08080A))
            else Pair(Color(0xFF111111), Color(0xFFFBF9F5))
        }
        effectivePlayerBackground == PlayerBackgroundStyle.BLUR ||
            effectivePlayerBackground == PlayerBackgroundStyle.GRADIENT ||
            effectivePlayerBackground == PlayerBackgroundStyle.ANIMATED_GRADIENT -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT -> Pair(Color.White, Color.Black)
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            }
        }
        else -> {
            when (playerButtonsStyle) {
                PlayerButtonsStyle.DEFAULT ->
                    if (useDarkTheme) Pair(Color.White, Color.Black)
                    else Pair(Color.Black, Color.White)
                PlayerButtonsStyle.PRIMARY -> Pair(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.onPrimary
                )
                PlayerButtonsStyle.TERTIARY -> Pair(
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.onTertiary
                )
            }
        }
    }

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    var showSleepTimerDialog by remember { mutableStateOf(false) }

    if (showSleepTimerDialog) {
        com.nanzbeatles.nanas.ui.component.SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false }
        )
    }

    var showHardwareIntegrationDialog by remember { mutableStateOf(false) }

    if (showHardwareIntegrationDialog) {
        com.nanzbeatles.nanas.ui.component.HardwareIntegrationDialog(
            onDismiss = { showHardwareIntegrationDialog = false },
        )
    }

    var showInlineLyricsState by rememberSaveable { mutableStateOf(false) }
    val showInlineLyrics = showInlineLyricsState
    val onShowInlineLyricsChange: (Boolean) -> Unit = { showInlineLyricsState = it }
    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showLyricsProviderDialog by rememberSaveable { mutableStateOf(false) }

    val (preferredLyricsProvider, _) = rememberEnumPreference(
        key = com.nanzbeatles.nanas.constants.PreferredLyricsProviderKey,
        defaultValue = com.nanzbeatles.nanas.constants.PreferredLyricsProvider.SIMPMUSIC
    )
    val isSwitchingLyricsProvider by com.nanzbeatles.nanas.ui.component.LyricsProviderSwitchState.isSwitching.collectAsState()
    val switchingLyricsProviderName by com.nanzbeatles.nanas.ui.component.LyricsProviderSwitchState.switchingProviderName.collectAsState()

    if (showLyricsProviderDialog) {
        com.nanzbeatles.nanas.ui.component.LyricsProviderSwitcherDialog(
            currentPreferred = preferredLyricsProvider,
            activeLoadedProviderName = currentLyrics?.provider,
            onSelectProvider = { newProvider ->
                showLyricsProviderDialog = false
                com.nanzbeatles.nanas.ui.component.LyricsProviderSwitchState.switchProviderAsync(
                    context = context,
                    database = database,
                    playerConnection = playerConnection,
                    mediaMetadata = mediaMetadata,
                    newProvider = newProvider,
                    showToast = true
                )
            },
            onDismiss = { showLyricsProviderDialog = false }
        )
    }

    // Exit fullscreen lyrics if player sheet collapses
    LaunchedEffect(state.isExpanded) {
        if (!state.isExpanded && isFullScreen) {
            isFullScreen = false
        }
    }

    BackHandler(enabled = isFullScreen && state.isExpanded) {
        isFullScreen = false
    }

    fun onLyricsToggle() {
        onShowInlineLyricsChange(!showInlineLyrics)
    }

    LaunchedEffect(videoModeEnabled) {
        if (videoModeEnabled && showInlineLyrics) {
            onShowInlineLyricsChange(false)
        }
    }

    fun stepLyricsByLine(lineDelta: Int) {
        val currentMeta = mediaMetadata ?: return
        val existingSong = currentSong?.song
        val currentOffset = existingSong?.lyricsOffset ?: 0
        val currentPos = (sliderPosition ?: effectivePosition).coerceAtLeast(0L)

        val rawLyrics = currentLyrics?.lyrics?.trim()
        val syncedLines = if (!rawLyrics.isNullOrEmpty() && rawLyrics.startsWith("[")) {
            com.nanzbeatles.nanas.lyrics.LyricsUtils.parseLyrics(rawLyrics)
                .filter { it.text.isNotBlank() && !it.isBackground }
        } else {
            emptyList()
        }

        val newOffset: Int
        val toastText: String
        if (syncedLines.isNotEmpty()) {
            val effectiveLyricTime = currentPos + currentOffset
            val activeIdx = syncedLines.indexOfLast { it.time <= effectiveLyricTime }.coerceAtLeast(0)
            val targetIdx = (activeIdx + lineDelta).coerceIn(0, syncedLines.lastIndex)
            val targetLine = syncedLines[targetIdx]
            // Set offset so currentPos + newOffset lands 80ms inside targetLine
            newOffset = ((targetLine.time + 80L) - currentPos).toInt().coerceIn(-600000, 600000)
            val preview = targetLine.text.lineSequence().firstOrNull()?.trim().orEmpty().take(28)
            val dirLabel = if (lineDelta > 0) "Maju 1 baris" else "Mundur 1 baris"
            toastText = if (preview.isNotEmpty()) "$dirLabel: \"$preview\"" else dirLabel
        } else {
            newOffset = (currentOffset + (lineDelta * 1500)).coerceIn(-600000, 600000)
            val sign = if (newOffset > 0) "+" else ""
            toastText = "Sync lirik: ${sign}${newOffset}ms"
        }

        database.query {
            if (existingSong != null) {
                upsert(existingSong.copy(lyricsOffset = newOffset))
            } else {
                insert(currentMeta)
            }
        }
        com.nanzbeatles.nanas.lyrics.LyricsResyncHelper.triggerResync()
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
    }

    fun openLyricsMenu() {
        val currentMeta = mediaMetadata ?: return
        menuState.show {
            LyricsMenu(
                lyricsProvider = { currentLyrics },
                songProvider = { currentSong?.song },
                mediaMetadataProvider = { currentMeta },
                onDismiss = menuState::dismiss,
                onShowOffsetDialog = {
                    bottomSheetPageState.show {
                        ShowOffsetDialog(
                            songProvider = { currentSong?.song }
                        )
                    }
                }
            )
        }
    }

    // Position update - only for local playback
    LaunchedEffect(isPlaying, isCasting) {
        if (!isCasting && isPlaying) {
            while (isActive) {
                delay(100)
                if (sliderPosition == null) {
                    position = playerConnection.player.currentPosition
                    duration = playerConnection.player.duration
                }
            }
        }
    }

    LaunchedEffect(playbackState, mediaMetadata?.id) {
        if (!isCasting) {
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration
        }
    }

    LaunchedEffect(isCasting, castPosition, castDuration) {
        if (isCasting && sliderPosition == null) {
            val timeSinceManualSeek = System.currentTimeMillis() - lastManualSeekTime
            if (timeSinceManualSeek > 1500) {
                position = castPosition
                if (castDuration > 0) duration = castDuration
            }
        }
    }

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        expandedBound = state.expandedBound,
        collapsedBound = 0.dp,
        initialAnchor = 1
    )

    BackHandler(enabled = queueSheetState.isExpanded) {
        queueSheetState.collapseSoft()
    }

    val bottomSheetBackgroundColor = if (isEditorial) {
        if (useDarkTheme) {
            if (useBlackBackground) Color.Black else Color(0xFF08080A)
        } else {
            Color(0xFFFBF9F5)
        }
    } else {
        when (effectivePlayerBackground) {
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.ANIMATED_GRADIENT ->
                Color(0xFF0A0B0E)
            else ->
                if (useBlackBackground) Color.Black
                else MaterialTheme.colorScheme.surfaceContainer
        }
    }

    val backgroundAlpha = state.progress.coerceIn(0f, 1f)

    fun openPlayerMoreMenu(meta: MediaMetadata) {
        menuState.show {
            PlayerMenu(
                mediaMetadata = meta,
                navController = navController,
                playerBottomSheetState = state,
                onShowDetailsDialog = {
                    meta.id.let {
                        bottomSheetPageState.show {
                            ShowMediaInfo(it)
                        }
                    }
                },
                onShowShareSheet = { showShareSheet = true },
                onShowSleepTimer = { showSleepTimerDialog = true },
                onOpenQueue = { queueSheetState.expandSoft() },
                onDismiss = menuState::dismiss,
            )
        }
    }

    fun toggleDownload(meta: MediaMetadata) {
        when (download?.state) {
            Download.STATE_COMPLETED, Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                DownloadService.sendRemoveDownload(
                    context,
                    ExoDownloadService::class.java,
                    meta.id,
                    false,
                )
                Toast.makeText(context, "Unduhan offline dihapus", Toast.LENGTH_SHORT).show()
            }
            else -> {
                database.transaction {
                    insert(meta)
                }
                val downloadRequest = DownloadRequest
                    .Builder(meta.id, meta.id.toUri())
                    .setCustomCacheKey(meta.id)
                    .setData(meta.title.toByteArray())
                    .build()
                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    downloadRequest,
                    false,
                )
                Toast.makeText(context, "Mengunduh lagu untuk mode offline...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val (miniPlayerBallMode, _) = rememberPreference(
        com.nanzbeatles.nanas.constants.MiniPlayerBallModeKey,
        defaultValue = false
    )

    BottomSheet(
        state = state,
        modifier = modifier,
        collapsedContentHandlesClick = true,
        collapsedTouchMaxWidthDp = if (miniPlayerBallMode) 115.dp else null,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomSheetBackgroundColor)
            ) {
                when (effectivePlayerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        val resolvedBlurThumb = remember(mediaMetadata?.thumbnailUrl, mediaMetadata?.id) {
                            resolveArtworkUrl(mediaMetadata?.thumbnailUrl, mediaMetadata?.id)
                        }
                        AnimatedContent(
                            targetState = resolvedBlurThumb,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "blurBackground"
                        ) { thumbnailUrl ->
                            if (thumbnailUrl != null) {
                                Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(thumbnailUrl)
                                            .size(100, 100)
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.85f) }),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .blur(if (useDarkTheme) 140.dp else 100.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.45f),
                                                        Color.Black.copy(alpha = 0.72f),
                                                        Color(0xFF07080A).copy(alpha = 0.94f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "gradientBackground"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                val gradientColorStops = if (colors.size >= 3) {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.5f to colors[1],
                                        1.0f to Color(0xFF07080A)
                                    )
                                } else {
                                    arrayOf(
                                        0.0f to colors[0],
                                        0.55f to colors[0].copy(alpha = 0.65f),
                                        1.0f to Color(0xFF07080A)
                                    )
                                }
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                        .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                        .background(Color.Black.copy(alpha = 0.28f))
                                )
                            }
                        }
                    }
                    PlayerBackgroundStyle.ANIMATED_GRADIENT -> {
                        AnimatedContent(
                            targetState = gradientColors,
                            transitionSpec = {
                                fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                            },
                            label = "animatedGradientBackground"
                        ) { colors ->
                            if (colors.isNotEmpty()) {
                                AnimatedGradientBackground(
                                    colors = colors,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(backgroundAlpha)
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
        },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                positionState = positionState,
                durationState = durationState,
                onHardwareIntegrationClick = { showHardwareIntegrationDialog = true },
                onExpandPlayer = { state.expandSoft() }
            )
        },
    ) {
        val seekSliderContent: @Composable (Modifier) -> Unit = { sliderModifier ->
            val sliderColors = PlayerSliderColors.getSliderColors(textButtonColor, effectivePlayerBackground, useDarkTheme)
            val currentSliderValue = (sliderPositionState.value ?: effectivePosition).toFloat()
            val currentSliderRange = 0f..(if (duration == C.TIME_UNSET || duration <= 0L) 1f else duration.toFloat())

            val handleSeekValueChange: (Float) -> Unit = { newVal ->
                if (!isListenTogetherGuest) {
                    sliderPositionState.value = newVal.toLong()
                }
            }

            val handleSeekFinished: () -> Unit = {
                if (!isListenTogetherGuest) {
                    sliderPositionState.value?.let { targetPos ->
                        if (isCasting) {
                            castHandler?.seekTo(targetPos)
                            lastManualSeekTime = System.currentTimeMillis()
                        } else {
                            playerConnection.player.seekTo(targetPos)
                        }
                        position = targetPos
                    }
                    sliderPositionState.value = null
                }
            }

            when (sliderStyle) {
                SliderStyle.DEFAULT -> {
                    Slider(
                        value = currentSliderValue,
                        valueRange = currentSliderRange,
                        onValueChange = handleSeekValueChange,
                        onValueChangeFinished = handleSeekFinished,
                        enabled = !isListenTogetherGuest,
                        colors = sliderColors,
                        modifier = sliderModifier,
                    )
                }

                SliderStyle.WAVY -> {
                    if (squigglySlider) {
                        SquigglySlider(
                            value = currentSliderValue,
                            valueRange = currentSliderRange,
                            onValueChange = handleSeekValueChange,
                            onValueChangeFinished = handleSeekFinished,
                            enabled = !isListenTogetherGuest,
                            modifier = sliderModifier,
                            colors = sliderColors,
                            isPlaying = effectiveIsPlaying,
                        )
                    } else {
                        WavySlider(
                            value = currentSliderValue,
                            valueRange = currentSliderRange,
                            onValueChange = handleSeekValueChange,
                            onValueChangeFinished = handleSeekFinished,
                            enabled = !isListenTogetherGuest,
                            colors = sliderColors,
                            modifier = sliderModifier,
                            isPlaying = effectiveIsPlaying
                        )
                    }
                }

                SliderStyle.SLIM -> {
                    Slider(
                        value = currentSliderValue,
                        valueRange = currentSliderRange,
                        onValueChange = handleSeekValueChange,
                        onValueChangeFinished = handleSeekFinished,
                        enabled = !isListenTogetherGuest,
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = sliderColors
                            )
                        },
                        colors = sliderColors,
                        modifier = sliderModifier
                    )
                }

                SliderStyle.LIQUID -> {
                    SamsungSlider(
                        value = currentSliderValue,
                        valueRange = currentSliderRange,
                        onValueChange = handleSeekValueChange,
                        onValueChangeFinished = handleSeekFinished,
                        enabled = !isListenTogetherGuest,
                        isPlaying = effectiveIsPlaying,
                        colors = sliderColors,
                        modifier = sliderModifier,
                    )
                }

                SliderStyle.VISUALIZER -> {
                    val audioSessionId = try {
                        playerConnection.player.audioSessionId
                    } catch (e: Exception) {
                        0
                    }
                    AudioVisualizerSlider(
                        value = currentSliderValue,
                        valueRange = currentSliderRange,
                        onValueChange = handleSeekValueChange,
                        onValueChangeFinished = handleSeekFinished,
                        enabled = !isListenTogetherGuest,
                        colors = sliderColors,
                        audioSessionId = audioSessionId,
                        isPlaying = effectiveIsPlaying,
                        modifier = sliderModifier,
                    )
                }
            }
        }

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { meta ->
            // 1. Song Title, Artist & 2 Circular Action Buttons (Offline/Download + Favorite/Heart)
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    AnimatedContent(
                        targetState = meta.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "songTitle",
                    ) { title ->
                        Text(
                            text = title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            color = TextBackgroundColor,
                            modifier = Modifier
                                .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2500, spacing = MarqueeSpacing(48.dp), velocity = 30.dp)
                                .combinedClickable(
                                    enabled = true,
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        if (meta.album != null) {
                                            navController.navigate("album/${meta.album.id}")
                                            state.collapseSoft()
                                        }
                                    },
                                    onLongClick = {
                                        val clip = ClipData.newPlainText(context.getString(R.string.copied_title), title)
                                        clipboardManager.setPrimaryClip(clip)
                                        Toast
                                            .makeText(context, context.getString(R.string.copied_title), Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (meta.explicit) MIcon.Explicit()

                        if (meta.artists.any { it.name.isNotBlank() }) {
                            val annotatedString = buildAnnotatedString {
                                meta.artists.forEachIndexed { index, artist ->
                                    val tag = "artist_${artist.id.orEmpty()}"
                                    pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                                    withStyle(SpanStyle(color = TextBackgroundColor.copy(alpha = 0.72f), fontSize = 15.sp)) {
                                        append(artist.name)
                                    }
                                    pop()
                                    if (index != meta.artists.lastIndex) append(", ")
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2500, spacing = MarqueeSpacing(48.dp), velocity = 30.dp)
                            ) {
                                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                                var clickOffset by remember { mutableStateOf<Offset?>(null) }
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextBackgroundColor.copy(alpha = 0.72f),
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip,
                                    onTextLayout = { layoutResult = it },
                                    modifier = Modifier
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    val event = awaitPointerEvent()
                                                    val tapPosition = event.changes.firstOrNull()?.position
                                                    if (tapPosition != null) {
                                                        clickOffset = tapPosition
                                                    }
                                                }
                                            }
                                        }
                                        .combinedClickable(
                                            enabled = true,
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {
                                                val tapPosition = clickOffset
                                                val layout = layoutResult
                                                if (tapPosition != null && layout != null) {
                                                    val offset = layout.getOffsetForPosition(tapPosition)
                                                    annotatedString
                                                        .getStringAnnotations(offset, offset)
                                                        .firstOrNull()
                                                        ?.let { ann ->
                                                            val artistId = ann.item
                                                            if (artistId.isNotBlank()) {
                                                                navController.navigate("artist/$artistId")
                                                                state.collapseSoft()
                                                            }
                                                        }
                                                }
                                            },
                                            onLongClick = {
                                                val clip = ClipData.newPlainText(
                                                    context.getString(R.string.copied_artist),
                                                    annotatedString
                                                )
                                                clipboardManager.setPrimaryClip(clip)
                                                Toast
                                                    .makeText(
                                                        context,
                                                        context.getString(R.string.copied_artist),
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                // Right circular buttons: Offline/Download + Favorite/Heart
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDownloaded = download?.state == Download.STATE_COMPLETED || currentSong?.song?.isDownloaded == true
                    val isDownloading = download?.state == Download.STATE_QUEUED || download?.state == Download.STATE_DOWNLOADING

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .liquidGlassPill(isSelected = isDownloaded, shape = CircleShape)
                            .clickable { toggleDownload(meta) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = TextBackgroundColor
                            )
                        } else {
                            Icon(
                                painter = painterResource(
                                    if (isDownloaded) R.drawable.offline else R.drawable.download
                                ),
                                contentDescription = stringResource(R.string.action_download),
                                tint = if (isDownloaded) {
                                    if (isEditorial) iconButtonColor else MaterialTheme.colorScheme.primary
                                } else {
                                    TextBackgroundColor.copy(alpha = 0.9f)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val isLiked = currentSong?.song?.liked == true
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .liquidGlassPill(isSelected = isLiked, shape = CircleShape)
                            .clickable { playerConnection.toggleLike() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                            ),
                            contentDescription = stringResource(R.string.like),
                            tint = if (isLiked) {
                                if (isEditorial) Color(0xFFE03E2D) else Color(0xFFFF4D67)
                            } else {
                                TextBackgroundColor.copy(alpha = 0.9f)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // 2. Inline Seekbar Row (0:03 — Slider — 3:32)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding),
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: effectivePosition),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBackgroundColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = 36.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    seekSliderContent(Modifier.fillMaxWidth())
                }

                Text(
                    text = if (duration != C.TIME_UNSET && duration > 0L) makeTimeString(duration) else "0:00",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextBackgroundColor.copy(alpha = 0.72f),
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 36.dp)
                )
            }

            // 3. Inline Volume Slider Row (Shown in New Player Design)
            if (useNewPlayerDesign) {
                val currentVolume = if (isCasting) castVolume else playerVolume
                val sleepFadeMult = playerConnection.service.sleepTimer.fadeVolumeMultiplier
                val effectiveDisplayVolume = if (isMuted) 0f else (currentVolume * sleepFadeMult).coerceIn(0f, 1f)
                val volumeSliderColors = PlayerSliderColors.getSliderColors(textButtonColor, effectivePlayerBackground, useDarkTheme)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PlayerHorizontalPadding),
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(min = 36.dp)
                            .clickable { playerConnection.toggleMute() }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Icon(
                            painter = painterResource(
                                when {
                                    isMuted || effectiveDisplayVolume == 0f -> R.drawable.volume_off
                                    effectiveDisplayVolume < 0.5f -> R.drawable.volume_down
                                    else -> R.drawable.volume_up
                                }
                            ),
                            contentDescription = null,
                            tint = TextBackgroundColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Slider(
                        value = effectiveDisplayVolume,
                        valueRange = 0f..1f,
                        onValueChange = { newVol ->
                            if (isMuted && newVol > 0f) {
                                playerConnection.toggleMute()
                            }
                            if (isCasting) {
                                castHandler?.setVolume(newVol)
                            } else {
                                playerConnection.service.playerVolume.value = newVol
                            }
                        },
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(textButtonColor)
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(4.dp),
                                colors = volumeSliderColors,
                                thumbTrackGapSize = 0.dp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )

                    Text(
                        text = "${(effectiveDisplayVolume * 100).roundToInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextBackgroundColor.copy(alpha = 0.72f),
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.widthIn(min = 36.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // 3b. Gaya Suara (Normal / Speed Up / Slowed + Reverb)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .crystalLiquidGlassSurface(
                        shape = RoundedCornerShape(24.dp),
                        isHighlighted = audioStyleMode != AudioStyleMode.NORMAL
                    )
                    .padding(horizontal = 5.dp, vertical = 4.dp)
            ) {
                AudioStyleMode.entries.forEach { mode ->
                    val isSelectedMode = audioStyleMode == mode
                    val modeFgColor = if (isSelectedMode) {
                        if (isEditorial) iconButtonColor else TextBackgroundColor
                    } else {
                        TextBackgroundColor.copy(alpha = 0.68f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .crystalLiquidGlassPill(
                                isSelected = isSelectedMode,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable(enabled = !isListenTogetherGuest) {
                                playerConnection.setAudioStyleMode(mode)
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    when (mode) {
                                        AudioStyleMode.NORMAL -> R.drawable.music_note
                                        AudioStyleMode.SPEED_UP -> R.drawable.speed
                                        AudioStyleMode.SLOWED_REVERB -> R.drawable.graphic_eq
                                    }
                                ),
                                contentDescription = mode.label,
                                tint = modeFgColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = mode.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelectedMode) FontWeight.Bold else FontWeight.Medium,
                                color = modeFgColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // 4. Main Playback Controls Row
            val dockModifier = if (useNewPlayerDesign) {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
                    .crystalLiquidGlassSurface(shape = RoundedCornerShape(36.dp), isHighlighted = false)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding, vertical = 6.dp)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = dockModifier,
            ) {
                // Shuffle
                val isShuffleSelected = shuffleModeEnabled && !isListenTogetherGuest
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .liquidGlassPill(
                            isSelected = isShuffleSelected,
                            shape = CircleShape
                        )
                        .clickable(enabled = !isListenTogetherGuest) {
                            playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle
                        ),
                        contentDescription = stringResource(R.string.shuffle),
                        tint = if (isShuffleSelected) {
                            if (isEditorial) iconButtonColor else textButtonColor
                        } else {
                            TextBackgroundColor.copy(alpha = 0.82f)
                        },
                        modifier = Modifier
                            .size(21.dp)
                            .alpha(if (isListenTogetherGuest) 0.5f else 1f)
                    )
                }

                // Previous
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .liquidGlassPill(isSelected = false, shape = CircleShape)
                        .clickable(enabled = canSkipPrevious && !isListenTogetherGuest) {
                            playerConnection.seekToPrevious()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = stringResource(R.string.previous),
                        tint = TextBackgroundColor,
                        modifier = Modifier
                            .size(28.dp)
                            .alpha(if (canSkipPrevious && !isListenTogetherGuest) 1f else 0.45f)
                    )
                }

                // Large Play/Pause Orb respecting PlayerButtonsStyleKey & Editorial geometry
                val playBtnShape = if (isEditorial) RoundedCornerShape(12.dp) else CircleShape
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(playBtnShape)
                        .background(
                            if (isEditorial) {
                                androidx.compose.ui.graphics.SolidColor(textButtonColor)
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        textButtonColor,
                                        textButtonColor.copy(alpha = 0.90f),
                                        textButtonColor.copy(alpha = 0.96f)
                                    )
                                )
                            },
                            playBtnShape
                        )
                        .border(
                            width = if (isEditorial) 1.dp else 1.5.dp,
                            brush = if (isEditorial) {
                                androidx.compose.ui.graphics.SolidColor(textButtonColor)
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        textButtonColor,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                        textButtonColor.copy(alpha = 0.8f)
                                    )
                                )
                            },
                            shape = playBtnShape
                        )
                        .clickable {
                            if (isListenTogetherGuest) {
                                playerConnection.toggleMute()
                                return@clickable
                            }
                            if (isCasting) {
                                if (castIsPlaying) {
                                    castHandler?.pause()
                                } else {
                                    castHandler?.play()
                                }
                            } else if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isListenTogetherGuest) {
                                if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                            } else if (playbackState == STATE_ENDED) {
                                R.drawable.replay
                            } else if (effectiveIsPlaying) {
                                R.drawable.pause
                            } else {
                                R.drawable.play
                            }
                        ),
                        contentDescription = if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = iconButtonColor,
                        modifier = Modifier.size(32.dp),
                    )
                }

                // Next
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .liquidGlassPill(isSelected = false, shape = CircleShape)
                        .clickable(enabled = canSkipNext && !isListenTogetherGuest) {
                            playerConnection.seekToNext()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = stringResource(R.string.next),
                        tint = TextBackgroundColor,
                        modifier = Modifier
                            .size(28.dp)
                            .alpha(if (canSkipNext && !isListenTogetherGuest) 1f else 0.45f)
                    )
                }

                // Repeat
                val isRepeatSelected = repeatMode != Player.REPEAT_MODE_OFF && !isListenTogetherGuest
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .liquidGlassPill(
                            isSelected = isRepeatSelected,
                            shape = CircleShape
                        )
                        .clickable(enabled = !isListenTogetherGuest) {
                            playerConnection.player.toggleRepeatMode()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                else -> R.drawable.repeat
                            }
                        ),
                        contentDescription = stringResource(R.string.repeat),
                        tint = if (isRepeatSelected) {
                            if (isEditorial) iconButtonColor else textButtonColor
                        } else {
                            TextBackgroundColor.copy(alpha = 0.82f)
                        },
                        modifier = Modifier
                            .size(21.dp)
                            .alpha(if (isListenTogetherGuest) 0.5f else 1f)
                    )
                }
            }
        }

        // Fullscreen Lyrics View vs Normal Player View
        AnimatedContent(
            targetState = isFullScreen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "FullscreenLyricsTransition"
        ) { fullScreenLyrics ->
            if (fullScreenLyrics && mediaMetadata != null) {
                val meta = mediaMetadata!!
                val fullLyricsBgColor = if (isEditorial) bottomSheetBackgroundColor else Color(0xFF08090C).copy(alpha = 0.94f)
                val fullLyricsFgColor = if (isEditorial) TextBackgroundColor else Color.White
                // MODE 3: FULLSCREEN LYRICS VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(fullLyricsBgColor)
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    // Fullscreen Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        if (hidePlayerThumbnail) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(if (isEditorial) 8.dp else 10.dp))
                                    .background(fullLyricsFgColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.music_note),
                                    contentDescription = null,
                                    tint = fullLyricsFgColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            AsyncImage(
                                model = resolveArtworkUrl(meta.thumbnailUrl?.toHighQualityThumbnail(), meta.id),
                                contentDescription = null,
                                contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(if (isEditorial) 8.dp else 10.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = meta.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = fullLyricsFgColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2500, spacing = MarqueeSpacing(48.dp), velocity = 30.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = meta.artists.joinToString { it.name },
                                fontSize = 14.sp,
                                color = fullLyricsFgColor.copy(alpha = 0.65f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Lyrics 3-dot menu button
                        IconButton(
                            onClick = { openLyricsMenu() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = stringResource(R.string.more_options),
                                tint = fullLyricsFgColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Collapse Fullscreen Lyrics Button (v)
                        val btnCorner = if (isEditorial) RoundedCornerShape(8.dp) else CircleShape
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(btnCorner)
                                .background(fullLyricsFgColor.copy(alpha = 0.08f))
                                .border(1.dp, fullLyricsFgColor.copy(alpha = 0.16f), btnCorner)
                                .clickable { isFullScreen = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.expand_more),
                                contentDescription = "Tutup Lirik Penuh",
                                tint = fullLyricsFgColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = fullLyricsFgColor.copy(alpha = 0.10f), thickness = 1.dp)

                    // Fullscreen Sub-controls Row: Equalizer on Left, Provider Switcher in Center, Sync [-][+] Pill on Right
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        val subBtnShape = if (isEditorial) RoundedCornerShape(8.dp) else CircleShape
                        val pillContainerShape = if (isEditorial) RoundedCornerShape(8.dp) else RoundedCornerShape(50)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(subBtnShape)
                                .background(fullLyricsFgColor.copy(alpha = 0.08f))
                                .border(1.dp, fullLyricsFgColor.copy(alpha = 0.16f), subBtnShape)
                                .clickable {
                                    state.collapseSoft()
                                    navController.navigate("equalizer")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.graphic_eq),
                                contentDescription = stringResource(R.string.equalizer),
                                tint = fullLyricsFgColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Direct Lyrics Provider Switcher Pill
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            com.nanzbeatles.nanas.ui.component.LyricsProviderQuickPill(
                                activeProviderName = currentLyrics?.provider,
                                preferredProvider = preferredLyricsProvider,
                                isSwitching = isSwitchingLyricsProvider,
                                switchingToName = switchingLyricsProviderName,
                                onClick = { showLyricsProviderDialog = true }
                            )
                        }

                        // Sync [-][+] Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(pillContainerShape)
                                .background(fullLyricsFgColor.copy(alpha = 0.05f))
                                .border(1.dp, fullLyricsFgColor.copy(alpha = 0.16f), pillContainerShape)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(subBtnShape)
                                    .background(fullLyricsFgColor.copy(alpha = 0.10f))
                                    .clickable { stepLyricsByLine(-1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.remove),
                                    contentDescription = "Mundurkan 1 Baris Lirik",
                                    tint = fullLyricsFgColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(subBtnShape)
                                    .background(fullLyricsFgColor.copy(alpha = 0.10f))
                                    .clickable { stepLyricsByLine(1) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = "Majukan 1 Baris Lirik",
                                    tint = fullLyricsFgColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Full-height Synced Lyrics Body
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        InlineLyricsView(
                            mediaMetadata = meta,
                            showLyrics = true,
                            sliderPositionProvider = { sliderPosition }
                        )
                    }
                }
            } else {
                // MODE 1 & 2: NORMAL FULL PLAYER (Cover Mode & Inline Lyrics Card Mode)
                when (LocalConfiguration.current.orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> {
                        val density = LocalDensity.current
                        val verticalPadding = max(
                            WindowInsets.systemBars.getTop(density),
                            WindowInsets.systemBars.getBottom(density)
                        )
                        val verticalPaddingDp = with(density) { verticalPadding.toDp() }
                        val verticalWindowInsets = WindowInsets(
                            left = 0.dp,
                            top = verticalPaddingDp,
                            right = 0.dp,
                            bottom = verticalPaddingDp
                        )

                        Row(
                            modifier = Modifier
                                .windowInsetsPadding(
                                    WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(verticalWindowInsets)
                                )
                                .padding(bottom = 16.dp)
                                .fillMaxSize()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .nestedScroll(state.preUpPostDownNestedScrollConnection)
                            ) {
                                val currentSliderPosition by rememberUpdatedState(sliderPosition)
                                val sliderPositionProvider = remember { { currentSliderPosition } }
                                val isExpandedProvider = remember(state) { { state.isExpanded } }
                                AnimatedContent(
                                    targetState = showInlineLyrics,
                                    label = "Lyrics",
                                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                                ) { showLyrics ->
                                    if (showLyrics) {
                                        InlineLyricsView(
                                            mediaMetadata = mediaMetadata,
                                            showLyrics = showLyrics,
                                            sliderPositionProvider = sliderPositionProvider
                                        )
                                    } else {
                                        Thumbnail(
                                            sliderPositionProvider = sliderPositionProvider,
                                            modifier = Modifier.animateContentSize(),
                                            isPlayerExpanded = isExpandedProvider,
                                            isLandscape = true,
                                            isListenTogetherGuest = isListenTogetherGuest,
                                            showHeader = false,
                                        )
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(if (showInlineLyrics) 0.65f else 1f, false)
                                    .animateContentSize()
                                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                            ) {
                                Spacer(Modifier.weight(1f))

                                mediaMetadata?.let {
                                    controlsContent(it)
                                }

                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.systemBars)
                                .padding(bottom = 16.dp)
                                .animateContentSize(),
                        ) {
                            // 1. TOP HEADER BAR (Matching Screenshots 1 & 2)
                            val subtitleText = remember(mediaMetadata, queueTitle) {
                                val artistsText = mediaMetadata?.artists?.joinToString { it.name }.orEmpty()
                                when {
                                    artistsText.isNotBlank() -> artistsText
                                    !queueTitle.isNullOrBlank() -> queueTitle!!
                                    else -> mediaMetadata?.album?.title.orEmpty()
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                // Left: Collapse Down Chevron (v)
                                IconButton(
                                    onClick = { state.collapseSoft() },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.expand_more),
                                        contentDescription = "Tutup Pemutar",
                                        tint = TextBackgroundColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // Spacer to balance right side (since right side has 2 icons: Equalizer + 3-dot)
                                Spacer(modifier = Modifier.width(4.dp))

                                // Center: "SEDANG DIPUTAR" + Artist/Context Subtitle
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (listenTogetherRoleState?.value != RoomRole.NONE && listenTogetherRoleState?.value != null) {
                                            "LISTEN TOGETHER"
                                        } else {
                                            "SEDANG DIPUTAR"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.6.sp,
                                        color = TextBackgroundColor.copy(alpha = 0.62f),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (subtitleText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subtitleText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextBackgroundColor,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Right: Sleep Timer button + Circular Equalizer button + Top-right 3-dot Menu button (⋮)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val isSleepTimerActive = playerConnection.service.sleepTimer.isActive
                                    val isSleepFading = playerConnection.service.sleepTimer.isFadingOut
                                    val topBtnShape = if (isEditorial) RoundedCornerShape(8.dp) else CircleShape
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .crystalLiquidGlassPill(
                                                isSelected = isSleepTimerActive,
                                                shape = topBtnShape
                                            )
                                            .clickable { showSleepTimerDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isSleepFading) R.drawable.volume_down else R.drawable.bedtime
                                            ),
                                            contentDescription = stringResource(R.string.sleep_timer),
                                            tint = if (isSleepTimerActive) {
                                                if (isEditorial) iconButtonColor else MaterialTheme.colorScheme.primary
                                            } else {
                                                TextBackgroundColor
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .crystalLiquidGlassPill(
                                                isSelected = false,
                                                shape = topBtnShape
                                            )
                                            .clickable {
                                                state.collapseSoft()
                                                navController.navigate("equalizer")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.graphic_eq),
                                            contentDescription = stringResource(R.string.equalizer),
                                            tint = TextBackgroundColor,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            mediaMetadata?.let { openPlayerMoreMenu(it) }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = stringResource(R.string.more_options),
                                            tint = TextBackgroundColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 2. SEGMENTED PILL SWITCHER: Cover | Lirik
                            val isLyricsTabSelected = showInlineLyrics
                            val isCoverTabSelected = !isLyricsTabSelected
                            val selectedTabFgColor = if (isEditorial) iconButtonColor else TextBackgroundColor
                            val unselectedTabFgColor = TextBackgroundColor.copy(alpha = 0.68f)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .crystalLiquidGlassSurface(shape = RoundedCornerShape(50), isHighlighted = false)
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (isCoverTabSelected) {
                                                Modifier.crystalLiquidGlassPill(isSelected = true, shape = RoundedCornerShape(50))
                                            } else {
                                                Modifier.clip(RoundedCornerShape(50))
                                            }
                                        )
                                        .clickable {
                                            onShowInlineLyricsChange(false)
                                        }
                                        .padding(horizontal = 22.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Cover",
                                        fontSize = 13.sp,
                                        fontWeight = if (isCoverTabSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isCoverTabSelected) selectedTabFgColor else unselectedTabFgColor
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (isLyricsTabSelected) {
                                                Modifier.crystalLiquidGlassPill(isSelected = true, shape = RoundedCornerShape(50))
                                            } else {
                                                Modifier.clip(RoundedCornerShape(50))
                                            }
                                        )
                                        .clickable {
                                            onShowInlineLyricsChange(true)
                                        }
                                        .padding(horizontal = 22.dp, vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Lirik",
                                        fontSize = 13.sp,
                                        fontWeight = if (isLyricsTabSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isLyricsTabSelected) selectedTabFgColor else unselectedTabFgColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3. CENTER CONTENT AREA (Cover with [|] Lirik pill OR Inline Lyrics Card)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            ) {
                                val currentSliderPosition by rememberUpdatedState(sliderPosition)
                                val sliderPositionProvider = remember { { currentSliderPosition } }
                                val isExpandedProvider = remember(state) { { state.isExpanded } }

                                AnimatedContent(
                                    targetState = isLyricsTabSelected,
                                    label = "CenterCoverOrLyricsCard",
                                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) }
                                ) { showLyricsCard ->
                                    if (showLyricsCard && mediaMetadata != null) {
                                        val meta = mediaMetadata!!
                                        val cardFgColor = if (isEditorial) TextBackgroundColor else Color.White
                                        val cardPillShape = if (isEditorial) RoundedCornerShape(8.dp) else RoundedCornerShape(50)
                                        val cardBtnShape = if (isEditorial) RoundedCornerShape(6.dp) else CircleShape
                                        // INLINE LYRICS CARD
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = PlayerHorizontalPadding, vertical = 4.dp)
                                                .crystalLiquidGlassSurface(shape = RoundedCornerShape(26.dp), isHighlighted = true)
                                        ) {
                                            // Card Header: Small Album Art + Title/Artist on Left | [|] Cover pill + Lyrics 3-dot on Right
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                                            ) {
                                                if (hidePlayerThumbnail) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(RoundedCornerShape(if (isEditorial) 6.dp else 10.dp))
                                                            .background(cardFgColor.copy(alpha = 0.1f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.music_note),
                                                            contentDescription = null,
                                                            tint = cardFgColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                } else {
                                                    AsyncImage(
                                                        model = resolveArtworkUrl(meta.thumbnailUrl?.toHighQualityThumbnail(), meta.id),
                                                        contentDescription = null,
                                                        contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(RoundedCornerShape(if (isEditorial) 6.dp else 10.dp))
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = meta.title,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = cardFgColor,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Clip,
                                                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 2500, spacing = MarqueeSpacing(48.dp), velocity = 30.dp)
                                                    )
                                                    Text(
                                                        text = meta.artists.joinToString { it.name },
                                                        fontSize = 12.sp,
                                                        color = cardFgColor.copy(alpha = 0.65f),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                // [|] Cover Pill Button
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier
                                                        .clip(cardPillShape)
                                                        .background(cardFgColor.copy(alpha = 0.08f))
                                                        .border(1.dp, cardFgColor.copy(alpha = 0.18f), cardPillShape)
                                                        .clickable { onShowInlineLyricsChange(false) }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    SplitCardIcon(tint = cardFgColor)
                                                    Text(
                                                        text = "Cover",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = cardFgColor
                                                    )
                                                }

                                                // Lyrics 3-dot Menu Button
                                                IconButton(
                                                    onClick = { openLyricsMenu() },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = stringResource(R.string.more_options),
                                                        tint = cardFgColor.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            HorizontalDivider(
                                                color = cardFgColor.copy(alpha = 0.10f),
                                                thickness = 1.dp,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )

                                            // Scrollable Synced Lyrics inside Card
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                            ) {
                                                InlineLyricsView(
                                                    mediaMetadata = meta,
                                                    showLyrics = true,
                                                    sliderPositionProvider = { sliderPosition }
                                                )
                                            }

                                            HorizontalDivider(
                                                color = cardFgColor.copy(alpha = 0.10f),
                                                thickness = 1.dp,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )

                                            // Card Footer: Sync [-][+] Pill on Left | Provider Switcher in Center | "Lirik Penuh" Pill on Right
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(54.dp)
                                                    .padding(horizontal = 10.dp)
                                            ) {
                                                // Sync [-][+] Pill (steps backward/forward by 1 lyric line)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .clip(cardPillShape)
                                                        .background(cardFgColor.copy(alpha = 0.06f))
                                                        .border(1.dp, cardFgColor.copy(alpha = 0.16f), cardPillShape)
                                                        .padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "Sync",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = cardFgColor.copy(alpha = 0.75f),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.clickable {
                                                            bottomSheetPageState.show {
                                                                ShowOffsetDialog(
                                                                    songProvider = { currentSong?.song }
                                                                )
                                                            }
                                                        }
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(cardBtnShape)
                                                            .background(cardFgColor.copy(alpha = 0.12f))
                                                            .clickable { stepLyricsByLine(-1) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "-",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = cardFgColor
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(cardBtnShape)
                                                            .background(cardFgColor.copy(alpha = 0.12f))
                                                            .clickable { stepLyricsByLine(1) },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "+",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = cardFgColor
                                                        )
                                                    }
                                                }

                                                // Direct Lyrics Provider Switcher Pill (flexible center space, never overlaps neighbors)
                                                Box(
                                                    modifier = Modifier.weight(1f),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    com.nanzbeatles.nanas.ui.component.LyricsProviderQuickPill(
                                                        activeProviderName = currentLyrics?.provider,
                                                        preferredProvider = preferredLyricsProvider,
                                                        isSwitching = isSwitchingLyricsProvider,
                                                        switchingToName = switchingLyricsProviderName,
                                                        onClick = { showLyricsProviderDialog = true }
                                                    )
                                                }

                                                // "Lirik Penuh" Pill Button
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .clip(cardPillShape)
                                                        .background(cardFgColor.copy(alpha = 0.08f))
                                                        .border(1.dp, cardFgColor.copy(alpha = 0.18f), cardPillShape)
                                                        .clickable { isFullScreen = true }
                                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.fullscreen),
                                                        contentDescription = "Lirik Penuh",
                                                        tint = cardFgColor,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "Lirik Penuh",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = cardFgColor,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        // COVER MODE
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Thumbnail(
                                                sliderPositionProvider = sliderPositionProvider,
                                                modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                                isPlayerExpanded = isExpandedProvider,
                                                isListenTogetherGuest = isListenTogetherGuest,
                                                showHeader = false,
                                            )

                                            // Floating "[|] Lirik" quick pill overlaid at bottom-right of cover
                                            val overlayPillShape = if (isEditorial) RoundedCornerShape(8.dp) else RoundedCornerShape(50)
                                            val overlayPillBg = if (isEditorial) {
                                                bottomSheetBackgroundColor.copy(alpha = 0.90f)
                                            } else {
                                                Color.Black.copy(alpha = 0.68f)
                                            }
                                            val overlayPillBorder = if (isEditorial) {
                                                TextBackgroundColor.copy(alpha = 0.25f)
                                            } else {
                                                Color.White.copy(alpha = 0.22f)
                                            }
                                            val overlayPillFg = if (isEditorial) TextBackgroundColor else Color.White
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(end = PlayerHorizontalPadding + 16.dp, bottom = 22.dp)
                                                    .clip(overlayPillShape)
                                                    .background(overlayPillBg)
                                                    .border(1.dp, overlayPillBorder, overlayPillShape)
                                                    .clickable { onShowInlineLyricsChange(true) }
                                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                            ) {
                                                SplitCardIcon(tint = overlayPillFg)
                                                Text(
                                                    text = "Lirik",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = overlayPillFg
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            mediaMetadata?.let {
                                controlsContent(it)
                            }
                        }
                    }
                }
            }
        }

        // Queue sheet (expandable from top-right 3-dot PlayerMenu)
        AnimatedVisibility(
            visible = !isFullScreen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
                navController = navController,
                background =
                if (isEditorial) {
                    bottomSheetBackgroundColor
                } else if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                onBackgroundColor = onBackgroundColor,
                TextBackgroundColor = TextBackgroundColor,
                textButtonColor = textButtonColor,
                iconButtonColor = iconButtonColor,
                pureBlack = pureBlack,
                showInlineLyrics = showInlineLyrics,
                playerBackground = effectivePlayerBackground,
                onToggleLyrics = {
                    onLyricsToggle()
                },
            )
        }
    }

    val currentMetadata = mediaMetadata
    if (showShareSheet && currentMetadata != null) {
        ShareSongBottomSheet(
            songData = ShareUtils.SongShareData(
                id = currentMetadata.id,
                title = currentMetadata.title,
                artist = currentMetadata.artists.joinToString(", ") { it.name },
                album = currentMetadata.album?.title,
                thumbnailUrl = currentMetadata.thumbnailUrl
            ),
            onDismiss = { showShareSheet = false }
        )
    }
}

/**
 * Custom split-card icon `[|]` matching the `[|] Lirik` and `[|] Cover` buttons in the reference design.
 */
@Composable
private fun SplitCardIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(14.dp)) {
        val strokeWidth = 1.4.dp.toPx()
        val corner = 2.5.dp.toPx()
        drawRoundRect(
            color = tint,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )
        drawLine(
            color = tint,
            start = Offset(size.width / 2f, strokeWidth),
            end = Offset(size.width / 2f, size.height - strokeWidth),
            strokeWidth = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 1.5.dp.toPx()), 0f)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InlineLyricsView(
    mediaMetadata: MediaMetadata?,
    showLyrics: Boolean,
    sliderPositionProvider: () -> Long? = { null }
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val videoModeEnabled by playerConnection.videoModeEnabled.collectAsState()
    val lyrics = remember(currentLyrics) { currentLyrics?.lyrics?.trim() }
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val (preferredLyricsProvider, _) = rememberEnumPreference(
        key = com.nanzbeatles.nanas.constants.PreferredLyricsProviderKey,
        defaultValue = com.nanzbeatles.nanas.constants.PreferredLyricsProvider.SIMPMUSIC
    )
    var showInlineProviderDialog by rememberSaveable { mutableStateOf(false) }
    val resolvedProviderBySong = remember { androidx.compose.runtime.mutableStateMapOf<String, com.nanzbeatles.nanas.constants.PreferredLyricsProvider>() }

    if (showInlineProviderDialog) {
        com.nanzbeatles.nanas.ui.component.LyricsProviderSwitcherDialog(
            currentPreferred = preferredLyricsProvider,
            activeLoadedProviderName = currentLyrics?.provider,
            onSelectProvider = { newProvider ->
                showInlineProviderDialog = false
                mediaMetadata?.id?.let { resolvedProviderBySong[it] = newProvider }
                coroutineScope.launch {
                    com.nanzbeatles.nanas.ui.component.LyricsProviderSwitchState.switchProviderImmediately(
                        context = context,
                        database = database,
                        playerConnection = playerConnection,
                        mediaMetadata = mediaMetadata,
                        newProvider = newProvider,
                        showToast = true
                    )
                }
            },
            onDismiss = { showInlineProviderDialog = false }
        )
    }

    LaunchedEffect(mediaMetadata?.id, currentLyrics?.provider, preferredLyricsProvider) {
        val meta = mediaMetadata ?: return@LaunchedEffect
        val existingProvider = currentLyrics?.provider
        val isManual = existingProvider.equals("Manual", ignoreCase = true)
        val matchesPreferred = existingProvider != null &&
            com.nanzbeatles.nanas.lyrics.LyricsHelper.fromProviderName(existingProvider) == preferredLyricsProvider
        val alreadyAttemptedForThisProvider = resolvedProviderBySong[meta.id] == preferredLyricsProvider

        if (currentLyrics == null || (!isManual && !matchesPreferred && !alreadyAttemptedForThisProvider)) {
            if (currentLyrics == null) {
                delay(300)
            }
            resolvedProviderBySong[meta.id] = preferredLyricsProvider
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.nanzbeatles.nanas.di.LyricsHelperEntryPoint::class.java
                    )
                    val lyricsHelper = entryPoint.lyricsHelper()
                    val fetchedLyricsWithProvider = lyricsHelper.getLyricsFromProvider(meta, preferredLyricsProvider)
                    val isTv = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
                    if (isTv) {
                        playerConnection.updateTvLyrics(meta.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider)
                    } else if (currentLyrics == null || fetchedLyricsWithProvider.lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                        database.query {
                            upsert(
                                LyricsEntity(
                                    meta.id,
                                    fetchedLyricsWithProvider.lyrics,
                                    fetchedLyricsWithProvider.provider
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    if (currentLyrics == null) {
                        database.query {
                            upsert(
                                LyricsEntity(
                                    meta.id,
                                    LyricsEntity.LYRICS_NOT_FOUND,
                                    "Unknown"
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            lyrics == null -> {
                ContainedLoadingIndicator()
            }
            lyrics == LyricsEntity.LYRICS_NOT_FOUND -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    val notFoundTextColor = if (com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL") {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    } else {
                        Color.White.copy(alpha = 0.65f)
                    }
                    Text(
                        text = stringResource(R.string.lyrics_not_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = notFoundTextColor,
                        textAlign = TextAlign.Center
                    )
                    com.nanzbeatles.nanas.ui.component.LyricsProviderQuickPill(
                        activeProviderName = null,
                        preferredProvider = preferredLyricsProvider,
                        isSwitching = false,
                        switchingToName = null,
                        onClick = { showInlineProviderDialog = true }
                    )
                }
            }
            else -> {
                val lyricsContent: @Composable () -> Unit = {
                    if (!videoModeEnabled) {
                        Lyrics(
                            sliderPositionProvider = sliderPositionProvider,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            showLyrics = showLyrics,
                        )
                    }
                }
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        textAlign = TextAlign.Start
                    )
                ) {
                    lyricsContent()
                }
            }
        }
    }
}

@Composable
fun MoreActionsButton(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    state: BottomSheetState,
    textButtonColor: Color,
    iconButtonColor: Color
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(textButtonColor)
            .clickable {
                menuState.show {
                    PlayerMenu(
                        mediaMetadata = mediaMetadata,
                        navController = navController,
                        playerBottomSheetState = state,
                        onShowDetailsDialog = {
                            mediaMetadata.id.let {
                                bottomSheetPageState.show {
                                    ShowMediaInfo(it)
                                }
                            }
                        },
                        onDismiss = menuState::dismiss
                    )
                }
            }
    ) {
        Image(
            painter = painterResource(R.drawable.more_horiz),
            contentDescription = null,
            colorFilter = ColorFilter.tint(iconButtonColor)
        )
    }
}

@Composable
private fun AnimatedGradientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier,
) {
    if (colors.isEmpty()) return

    val baseColor = Color(0xFF0C0C0C)
    val c0 = colors.getOrNull(0) ?: baseColor
    val c1 = colors.getOrNull(1) ?: c0
    val c2 = colors.getOrNull(2) ?: c1
    val cHighlight = Color(
        red = (c0.red * 1.15f).coerceIn(0f, 1f),
        green = (c0.green * 1.15f).coerceIn(0f, 1f),
        blue = (c0.blue * 1.15f).coerceIn(0f, 1f),
        alpha = 1f
    )

    val transition = rememberInfiniteTransition(label = "animatedGradient")

    val t1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "t1"
    )
    val t2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 21000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "t2"
    )
    val t3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 27000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "t3"
    )
    val t4 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 33000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "t4"
    )
    val breath by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath"
    )

    Box(
        modifier = modifier
            .background(baseColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
        ) {
            val w = size.width
            val h = size.height
            val maxDim = kotlin.math.max(w, h)
            val radius = maxDim * 0.78f * breath

            val cx1 = w * (0.28f + 0.20f * kotlin.math.cos(t1))
            val cy1 = h * (0.30f + 0.20f * kotlin.math.sin(t1 * 0.7f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c0.copy(alpha = 0.95f), c0.copy(alpha = 0f)),
                    center = Offset(cx1, cy1),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx1, cy1),
            )

            val cx2 = w * (0.72f + 0.22f * kotlin.math.sin(t2))
            val cy2 = h * (0.32f + 0.20f * kotlin.math.cos(t2 * 0.9f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c1.copy(alpha = 0.85f), c1.copy(alpha = 0f)),
                    center = Offset(cx2, cy2),
                    radius = radius * 0.95f
                ),
                radius = radius * 0.95f,
                center = Offset(cx2, cy2),
            )

            val cx3 = w * (0.50f + 0.28f * kotlin.math.cos(t3 * 0.6f))
            val cy3 = h * (0.82f + 0.16f * kotlin.math.sin(t3))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c2.copy(alpha = 0.80f), c2.copy(alpha = 0f)),
                    center = Offset(cx3, cy3),
                    radius = radius * 0.95f
                ),
                radius = radius * 0.95f,
                center = Offset(cx3, cy3),
            )

            val cx4 = w * (0.50f + 0.32f * kotlin.math.sin(t4))
            val cy4 = h * (0.55f + 0.18f * kotlin.math.sin(t4 * 2f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(cHighlight.copy(alpha = 0.45f), cHighlight.copy(alpha = 0f)),
                    center = Offset(cx4, cy4),
                    radius = radius * 0.7f
                ),
                radius = radius * 0.7f,
                center = Offset(cx4, cy4),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f))
        )
    }
}
