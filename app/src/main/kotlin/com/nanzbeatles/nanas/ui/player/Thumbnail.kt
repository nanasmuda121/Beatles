/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.player

import android.graphics.Matrix
import android.provider.Settings
import android.view.TextureView
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size as CoilSize
import com.nanzbeatles.innertube.YouTube
import com.nanzbeatles.nanas.LocalListenTogetherManager
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.CropAlbumArtKey
import com.nanzbeatles.nanas.constants.HidePlayerThumbnailKey
import com.nanzbeatles.nanas.constants.PlayerBackgroundStyle
import com.nanzbeatles.nanas.constants.PlayerBackgroundStyleKey
import com.nanzbeatles.nanas.constants.PlayerHorizontalPadding
import com.nanzbeatles.nanas.constants.SeekExtraSeconds
import com.nanzbeatles.nanas.constants.SubtitleLanguageKey
import com.nanzbeatles.nanas.constants.SubtitlesEnabledKey
import com.nanzbeatles.nanas.constants.SwipeSensitivityKey
import com.nanzbeatles.nanas.constants.SwipeThumbnailKey
import com.nanzbeatles.nanas.constants.ThumbnailCornerRadius
import com.nanzbeatles.nanas.constants.VideoFitModeKey
import com.nanzbeatles.nanas.constants.VideoLyricsEnabledKey
import com.nanzbeatles.nanas.constants.VideoQuality
import com.nanzbeatles.nanas.constants.VideoQualityKey
import com.nanzbeatles.nanas.listentogether.RoomRole
import com.nanzbeatles.nanas.ui.component.CastButton
import com.nanzbeatles.nanas.ui.utils.fallbackYtThumbnailUrl
import com.nanzbeatles.nanas.ui.utils.resize
import com.nanzbeatles.nanas.ui.utils.resolveArtworkUrl
import com.nanzbeatles.nanas.utils.BeatlesPlayerUtils
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Pre-calculated thumbnail dimensions to avoid repeated calculations during recomposition.
 * All values are computed once and cached.
 */
@Immutable
data class ThumbnailDimensions(
    val itemWidth: Dp,
    val containerSize: Dp,
    val thumbnailSize: Dp,
    val cornerRadius: Dp
)

/**
 * Cached media items data to prevent recalculation on every recomposition.
 */
@Immutable
data class MediaItemsData(
    val items: List<MediaItem>,
    val currentIndex: Int
)

/**
 * Calculate thumbnail dimensions once based on container size.
 * This function is marked as @Stable to indicate it produces stable results.
 * In landscape mode, uses the smaller dimension (height) to ensure square thumbnail fits.
 */
@Stable
private fun calculateThumbnailDimensions(
    containerWidth: Dp,
    containerHeight: Dp = containerWidth,
    horizontalPadding: Dp = PlayerHorizontalPadding,
    cornerRadius: Dp = ThumbnailCornerRadius,
    isLandscape: Boolean = false
): ThumbnailDimensions {
    val effectiveSize = if (isLandscape) {
        minOf(containerWidth, containerHeight) - (horizontalPadding * 2)
    } else {
        containerWidth - (horizontalPadding * 2)
    }
    return ThumbnailDimensions(
        itemWidth = containerWidth,
        containerSize = containerWidth,
        thumbnailSize = effectiveSize,
        cornerRadius = cornerRadius * 2
    )
}

/**
 * Get media items for the thumbnail carousel.
 * Calculates previous, current, and next items based on shuffle mode.
 */
@Stable
private fun getMediaItems(
    player: Player,
    swipeThumbnail: Boolean
): MediaItemsData {
    val timeline = player.currentTimeline
    val currentIndex = player.currentMediaItemIndex
    val shuffleModeEnabled = player.shuffleModeEnabled

    val currentMediaItem = try {
        player.currentMediaItem
    } catch (e: Exception) { null }

    val previousMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val previousIndex = timeline.getPreviousWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (previousIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(previousIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val nextMediaItem = if (swipeThumbnail && !timeline.isEmpty) {
        val nextIndex = timeline.getNextWindowIndex(
            currentIndex,
            Player.REPEAT_MODE_OFF,
            shuffleModeEnabled
        )
        if (nextIndex != C.INDEX_UNSET) {
            try { player.getMediaItemAt(nextIndex) } catch (e: Exception) { null }
        } else null
    } else null

    val items = listOfNotNull(previousMediaItem, currentMediaItem, nextMediaItem)
    val currentMediaIndex = items.indexOf(currentMediaItem)

    return MediaItemsData(items, currentMediaIndex)
}

/**
 * Get text color based on player background style.
 * Computed once per background style change.
 */
@Stable
@Composable
private fun getTextColor(playerBackground: PlayerBackgroundStyle): Color {
    if (com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL") {
        return MaterialTheme.colorScheme.onBackground
    }
    return when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
        PlayerBackgroundStyle.ANIMATED_GRADIENT -> Color.White
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: () -> Boolean = { true },
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
    showHeader: Boolean = false,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    // Collect states
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    // Video mode state
    val videoModeEnabled by playerConnection.videoModeEnabled.collectAsState()
    val availableSubtitles by playerConnection.availableSubtitles.collectAsState()
    val selectedSubtitleIndex by playerConnection.selectedSubtitleIndex.collectAsState()
    val subtitlesEnabled by rememberPreference(SubtitlesEnabledKey, true)

    // Preferences - computed once
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, true)
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val textBackgroundColor = getTextColor(playerBackground)

    // Grid state
    val thumbnailLazyGridState = rememberLazyGridState()

    // Calculate media items data - memoized
    val mediaItemsData by remember(
        playerConnection.player.currentMediaItemIndex,
        playerConnection.player.shuffleModeEnabled,
        swipeThumbnail,
        mediaMetadata,
        videoModeEnabled
    ) {
        derivedStateOf {
            getMediaItems(playerConnection.player, swipeThumbnail)
        }
    }

    val mediaItems = mediaItemsData.items
    val currentMediaIndex = mediaItemsData.currentIndex

    val thumbnailSnapLayoutInfoProvider = remember(thumbnailLazyGridState, swipeSensitivity) {
        ThumbnailSnapLayoutInfoProvider(
            lazyGridState = thumbnailLazyGridState,
            positionInLayout = { layoutSize, itemSize ->
                (layoutSize / 2f - itemSize / 2f)
            },
            velocityThreshold = (1000f * (1.25f - swipeSensitivity.coerceIn(0.1f, 1f))).coerceAtLeast(150f)
        )
    }

    val currentItem by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemIndex } }
    val itemScrollOffset by remember { derivedStateOf { thumbnailLazyGridState.firstVisibleItemScrollOffset } }

    // Handle swipe to change song
    LaunchedEffect(itemScrollOffset) {
        if (!thumbnailLazyGridState.isScrollInProgress || !swipeThumbnail || itemScrollOffset != 0 || currentMediaIndex < 0) return@LaunchedEffect

        if (currentItem > currentMediaIndex && canSkipNext) {
            playerConnection.player.seekToNext()
        } else if (currentItem < currentMediaIndex && canSkipPrevious) {
            playerConnection.player.seekToPreviousMediaItem()
        }
    }

    // Update position when song changes
    LaunchedEffect(mediaMetadata, canSkipPrevious, canSkipNext) {
        val index = maxOf(0, currentMediaIndex)
        if (index >= 0 && index < mediaItems.size) {
            try {
                thumbnailLazyGridState.animateScrollToItem(index)
            } catch (e: Exception) {
                thumbnailLazyGridState.scrollToItem(index)
            }
        }
    }

    LaunchedEffect(playerConnection.player.currentMediaItemIndex) {
        val index = mediaItemsData.currentIndex
        if (index >= 0 && index != currentItem) {
            thumbnailLazyGridState.scrollToItem(index)
        }
    }

    // Seek effect state
    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }

    Box(
        modifier = modifier
    ) {
        // Error view
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { playbackError ->
                PlaybackError(
                    error = playbackError,
                    retry = playerConnection.player::prepare,
                )
            }
        }

        // Main thumbnail view
        AnimatedVisibility(
            visible = error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isLandscape && showHeader) Modifier.statusBarsPadding() else Modifier),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (!isLandscape && showHeader) {
                    ThumbnailHeader(
                        queueTitle = queueTitle,
                        albumTitle = mediaMetadata?.album?.title,
                        textColor = textBackgroundColor
                    )
                }

                // Thumbnail content
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = if (isLandscape) {
                        Modifier.weight(1f, false)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    val dimensions = remember(maxWidth, maxHeight, isLandscape) {
                        calculateThumbnailDimensions(
                            containerWidth = maxWidth,
                            containerHeight = maxHeight,
                            isLandscape = isLandscape
                        )
                    }

                    val onSeekCallback = remember {
                        { direction: String, showEffect: Boolean ->
                            seekDirection = direction
                            showSeekEffect = showEffect
                        }
                    }

                    val isScrollEnabled by remember(swipeThumbnail) {
                        derivedStateOf { swipeThumbnail && isPlayerExpanded() }
                    }

                    LazyHorizontalGrid(
                        state = thumbnailLazyGridState,
                        rows = GridCells.Fixed(1),
                        flingBehavior = rememberSnapFlingBehavior(thumbnailSnapLayoutInfoProvider),
                        userScrollEnabled = isScrollEnabled,
                        modifier = when {
                            videoModeEnabled -> Modifier.fillMaxSize()
                            isLandscape -> Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                            else -> Modifier.fillMaxSize()
                        }
                    ) {
                        items(
                            items = mediaItems,
                            key = { item ->
                                item.mediaId.ifEmpty { "unknown_${item.hashCode()}" }
                            }
                        ) { item ->
                            val activeMediaId = mediaMetadata?.id?.takeIf { it.isNotBlank() }
                                ?: playerConnection.player.currentMediaItem?.mediaId
                            ThumbnailItem(
                                item = item,
                                dimensions = dimensions,
                                hidePlayerThumbnail = hidePlayerThumbnail,
                                cropAlbumArt = cropAlbumArt,
                                textBackgroundColor = textBackgroundColor,
                                layoutDirection = layoutDirection,
                                onSeek = onSeekCallback,
                                playerConnection = playerConnection,
                                context = context,
                                isLandscape = isLandscape,
                                isListenTogetherGuest = isListenTogetherGuest,
                                currentMediaId = activeMediaId,
                                currentMediaTitle = mediaMetadata?.title,
                                currentMediaArtist = mediaMetadata?.artists?.joinToString(", ") { it.name },
                                currentMediaAlbum = mediaMetadata?.album?.title,
                                currentMediaDurationMs = mediaMetadata?.duration?.takeIf { it > 0 }?.times(1000L),
                                currentMediaThumbnail = mediaMetadata?.thumbnailUrl,
                                isPlaying = isPlaying,
                                isPlayerExpanded = isPlayerExpanded,
                                videoModeEnabled = videoModeEnabled,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Header component showing "Now Playing" and queue/album title.
 */
@Composable
private fun ThumbnailHeader(
    queueTitle: String?,
    albumTitle: String?,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = RoomRole.NONE)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
        ) {
            if (listenTogetherRoleState?.value != RoomRole.NONE) {
                Text(
                    text = if (listenTogetherRoleState?.value == RoomRole.HOST) "Hosting Listen Together" else "Listening Together",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            } else {
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
            }
            val playingFrom = queueTitle ?: albumTitle
            if (!playingFrom.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playingFrom,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

/**
 * Individual thumbnail item in the carousel.
 */
@Composable
private fun ThumbnailItem(
    item: MediaItem,
    dimensions: ThumbnailDimensions,
    hidePlayerThumbnail: Boolean,
    cropAlbumArt: Boolean,
    textBackgroundColor: Color,
    layoutDirection: LayoutDirection,
    onSeek: (String, Boolean) -> Unit,
    playerConnection: com.nanzbeatles.nanas.playback.PlayerConnection,
    context: android.content.Context,
    isLandscape: Boolean = false,
    isListenTogetherGuest: Boolean = false,
    currentMediaId: String? = null,
    currentMediaTitle: String? = null,
    currentMediaArtist: String? = null,
    currentMediaAlbum: String? = null,
    currentMediaDurationMs: Long? = null,
    currentMediaThumbnail: String? = null,
    isPlaying: Boolean = false,
    isPlayerExpanded: () -> Boolean = { true },
    videoModeEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val incrementalSeekSkipEnabled by rememberPreference(SeekExtraSeconds, defaultValue = false)
    var skipMultiplier by remember { mutableIntStateOf(1) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    val isCurrentSong = item.mediaId == currentMediaId || currentMediaId == null
    val isCurrentVideoMode = videoModeEnabled && isCurrentSong

    Box(
        modifier = modifier
            .then(
                if (isLandscape && !isCurrentVideoMode) {
                    Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                } else {
                    Modifier
                        .width(dimensions.itemWidth)
                        .fillMaxSize()
                }
            )
            .padding(horizontal = PlayerHorizontalPadding)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (isListenTogetherGuest) return@detectTapGestures

                        val currentPosition = playerConnection.player.currentPosition
                        val duration = playerConnection.player.duration

                        val now = System.currentTimeMillis()
                        if (incrementalSeekSkipEnabled && now - lastTapTime < 1000) {
                            skipMultiplier++
                        } else {
                            skipMultiplier = 1
                        }
                        lastTapTime = now

                        val skipAmount = 5000 * skipMultiplier

                        val isLeftSide = (layoutDirection == LayoutDirection.Ltr && offset.x < size.width / 2) ||
                                (layoutDirection == LayoutDirection.Rtl && offset.x > size.width / 2)

                        if (isLeftSide) {
                            playerConnection.player.seekTo((currentPosition - skipAmount).coerceAtLeast(0))
                            onSeek(context.getString(R.string.seek_backward_dynamic, skipAmount / 1000), true)
                        } else {
                            playerConnection.player.seekTo((currentPosition + skipAmount).coerceAtMost(duration))
                            onSeek(context.getString(R.string.seek_forward_dynamic, skipAmount / 1000), true)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val isEditorial = com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL"
        val effectiveCornerShape = RoundedCornerShape(if (isEditorial) 8.dp else dimensions.cornerRadius)
        Box(
            modifier = Modifier
                .then(
                    if (isCurrentVideoMode) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.size(dimensions.thumbnailSize)
                    }
                )
                .clip(effectiveCornerShape)
                .then(
                    if (isEditorial && !isCurrentVideoMode) {
                        Modifier.border(
                            width = 1.dp,
                            color = textBackgroundColor.copy(alpha = 0.20f),
                            shape = effectiveCornerShape
                        )
                    } else Modifier
                )
        ) {
            if (hidePlayerThumbnail && !isCurrentVideoMode) {
                HiddenThumbnailPlaceholder(textBackgroundColor = textBackgroundColor)
            } else {
                val resolvedMediaId = (if (isCurrentSong) currentMediaId else null) ?: item.mediaId
                val rawArtworkUri = if (isCurrentSong && !currentMediaThumbnail.isNullOrBlank()) {
                    currentMediaThumbnail
                } else {
                    item.mediaMetadata.artworkUri?.toString()
                } ?: item.mediaMetadata.artworkUri?.toString()
                val artworkUriToUse = resolveArtworkUrl(rawArtworkUri, resolvedMediaId)

                val resolvedTitle = (if (isCurrentSong) currentMediaTitle else null)
                    ?.takeIf { it.isNotBlank() }
                    ?: item.mediaMetadata.title?.toString()
                val resolvedArtist = (if (isCurrentSong) currentMediaArtist else null)
                    ?.takeIf { it.isNotBlank() }
                    ?: item.mediaMetadata.artist?.toString()
                val resolvedAlbum = (if (isCurrentSong) currentMediaAlbum else null)
                    ?.takeIf { it.isNotBlank() }
                    ?: item.mediaMetadata.albumTitle?.toString()

                ThumbnailImage(
                    artworkUri = artworkUriToUse,
                    cropArtwork = cropAlbumArt,
                    videoModeEnabled = isCurrentVideoMode,
                    isCurrentSong = isCurrentSong,
                    currentMediaId = (if (isCurrentSong) currentMediaId else null) ?: item.mediaId,
                    currentMediaTitle = resolvedTitle,
                    currentMediaArtist = resolvedArtist,
                    currentMediaAlbum = resolvedAlbum,
                    currentMediaDurationMs = if (isCurrentSong) currentMediaDurationMs else null,
                    isPlaying = isPlaying && isCurrentSong,
                    isPlayerExpanded = isPlayerExpanded,
                )
            }

            // Cast button at top corner of thumbnail
            CastButton(
                modifier = Modifier
                    .align(if (isCurrentVideoMode) Alignment.TopStart else Alignment.TopEnd)
                    .padding(8.dp),
                tintColor = textBackgroundColor
            )
        }
    }
}

/**
 * Placeholder shown when thumbnail is hidden.
 */
@Composable
private fun HiddenThumbnailPlaceholder(
    textBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = stringResource(R.string.hide_player_thumbnail),
            tint = textBackgroundColor.copy(alpha = 0.7f),
            modifier = Modifier.size(120.dp)
        )
    }
}

/**
 * Actual thumbnail image with caching and hardware layer rendering.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThumbnailImage(
    artworkUri: String?,
    cropArtwork: Boolean,
    videoModeEnabled: Boolean,
    isCurrentSong: Boolean = false,
    currentMediaId: String? = null,
    currentMediaTitle: String? = null,
    currentMediaArtist: String? = null,
    currentMediaAlbum: String? = null,
    currentMediaDurationMs: Long? = null,
    isPlaying: Boolean = false,
    isPlayerExpanded: () -> Boolean = { true },
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val isVideoSwitching by playerConnection.isVideoSwitching.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val (subtitlesEnabled, onSubtitlesEnabledChange) = rememberPreference(SubtitlesEnabledKey, true)

    // [4] Auto-hide controls state
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // [6] Pinch-to-zoom resize mode
    val (resizeMode, onResizeModeChange) = rememberPreference(VideoFitModeKey, AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH)

    val isFixedMode = resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH

    // [8] Brightness/Volume gesture state
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0.5f) }
    var volumeLevel by remember { mutableFloatStateOf(1f) }

    // Initialize brightness from system
    LaunchedEffect(Unit) {
        try {
            val systemBrightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, 128
            )
            brightnessLevel = systemBrightness / 255f
        } catch (_: Exception) {}
        volumeLevel = player.volume
    }

    // [4] Auto-hide controls after 3 seconds
    LaunchedEffect(showControls, lastInteractionTime, videoModeEnabled) {
        if (showControls && videoModeEnabled) {
            delay(3000)
            if (System.currentTimeMillis() - lastInteractionTime >= 2900) {
                showControls = false
            }
        }
    }

    // [7] Progress tracking for gradient bar
    var playerPosition by remember { mutableLongStateOf(0L) }
    var playerDuration by remember { mutableLongStateOf(1L) }
    LaunchedEffect(videoModeEnabled) {
        if (videoModeEnabled) {
            while (isActive) {
                playerPosition = player.currentPosition
                playerDuration = player.duration.coerceAtLeast(1L)
                delay(200)
            }
        } else {
            playerPosition = 0L
            playerDuration = 1L
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (videoModeEnabled) Color.Black else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (videoModeEnabled && !isVideoSwitching) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        this.resizeMode = resizeMode
                        setBackgroundColor(android.graphics.Color.BLACK)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = true
                        setShowSubtitleButton(true)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { playerView ->
                    playerView.player = player
                    playerView.resizeMode = resizeMode
                    playerView.requestLayout()
                }
            )

            // [4] Tap to show/hide controls + [8] Brightness/Volume swipe gestures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                showControls = !showControls
                                lastInteractionTime = System.currentTimeMillis()
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                lastInteractionTime = System.currentTimeMillis()
                                showControls = true
                                val isLeftSide = offset.x < size.width / 2
                                if (isLeftSide) showBrightnessOverlay = true
                                else showVolumeOverlay = true
                            },
                            onDragEnd = {
                                showBrightnessOverlay = false
                                showVolumeOverlay = false
                            },
                            onDragCancel = {
                                showBrightnessOverlay = false
                                showVolumeOverlay = false
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount.y / size.height
                                if (showBrightnessOverlay) {
                                    brightnessLevel = (brightnessLevel + delta).coerceIn(0.01f, 1f)
                                    val window = (context as? android.app.Activity)?.window
                                    window?.let {
                                        val params = it.attributes
                                        params.screenBrightness = brightnessLevel
                                        it.attributes = params
                                    }
                                } else if (showVolumeOverlay) {
                                    volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                                    player.volume = volumeLevel
                                }
                            }
                        )
                    }
            )

            // [8] Brightness overlay (left side)
            AnimatedVisibility(
                visible = showBrightnessOverlay,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
            ) {
                GestureIndicator(
                    icon = R.drawable.contrast,
                    level = brightnessLevel,
                    label = "${(brightnessLevel * 100).roundToInt()}%"
                )
            }

            // [8] Volume overlay (right side)
            AnimatedVisibility(
                visible = showVolumeOverlay,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(300)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                GestureIndicator(
                    icon = R.drawable.volume_up,
                    level = volumeLevel,
                    label = "${(volumeLevel * 100).roundToInt()}%"
                )
            }

            // [7] Video progress gradient bar at top
            val progress by animateFloatAsState(
                targetValue = if (playerDuration > 0) playerPosition.toFloat() / playerDuration.toFloat() else 0f,
                animationSpec = tween(250, easing = LinearEasing),
                label = "progress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                )
            }

            // [4] Auto-hide settings button
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(400)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
            ) {
                VideoSettingsButton(
                    resizeMode = resizeMode,
                    onResizeModeChange = onResizeModeChange,
                    onInteraction = { lastInteractionTime = System.currentTimeMillis() },
                    subtitlesEnabled = subtitlesEnabled,
                    onSubtitlesToggle = { onSubtitlesEnabledChange(!subtitlesEnabled) }
                )
            }

            VideoLyricsOverlay(
                isFixedMode = isFixedMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        } else {
            // Cover Mode: Displays static high-res album art with automatic video thumbnail fallback
            Box(modifier = Modifier.fillMaxSize()) {
                val primaryArtworkUri = remember(artworkUri, currentMediaId) {
                    resolveArtworkUrl(artworkUri?.resize(1280, 1280) ?: artworkUri, currentMediaId)
                }
                val fallbackArtworkUri = remember(artworkUri, currentMediaId) {
                    fallbackYtThumbnailUrl(currentMediaId, artworkUri)
                }
                var useFallbackUri by remember(primaryArtworkUri, currentMediaId) {
                    mutableStateOf(false)
                }
                val activeArtworkUri = if (useFallbackUri && !fallbackArtworkUri.isNullOrBlank()) {
                    fallbackArtworkUri
                } else {
                    primaryArtworkUri ?: fallbackArtworkUri
                }

                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(activeArtworkUri)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .networkCachePolicy(CachePolicy.ENABLED)
                        .size(CoilSize.ORIGINAL)
                        .crossfade(300)
                        .build(),
                    contentDescription = null,
                    contentScale = if (cropArtwork) ContentScale.Crop else ContentScale.Fit,
                    onError = {
                        if (!useFallbackUri && !fallbackArtworkUri.isNullOrBlank() && fallbackArtworkUri != primaryArtworkUri) {
                            useFallbackUri = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle gradient overlay for depth and modern look
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.08f),
                                    Color.Black.copy(alpha = 0.28f)
                                ),
                                startY = 0f
                            )
                        )
                )

                // Loading indicator with smooth animation
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeArtworkUri.isNullOrBlank(),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Memuat sampul...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * [8] Gesture indicator for brightness/volume swipe
 */
@Composable
private fun GestureIndicator(
    icon: Int,
    level: Float,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(80.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(level)
                    .align(Alignment.BottomCenter)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Single settings button for video controls.
 */
@Composable
private fun VideoSettingsButton(
    resizeMode: Int,
    onResizeModeChange: (Int) -> Unit,
    onInteraction: () -> Unit,
    subtitlesEnabled: Boolean = true,
    onSubtitlesToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    val (videoQuality, onVideoQualityChange) = rememberEnumPreference(
        VideoQualityKey,
        defaultValue = VideoQuality.QUALITY_720P
    )

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                expanded = true
                showQualityMenu = false
                onInteraction()
            },
            modifier = Modifier
                .size(36.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
        ) {
            Icon(
                painter = painterResource(R.drawable.settings),
                contentDescription = "Pengaturan video",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        if (!showQualityMenu) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.settings),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                    tint = Color.White
                                )
                                Text("Kualitas")
                            }
                            Text(
                                text = when (videoQuality) {
                                    VideoQuality.QUALITY_360P -> "360p"
                                    VideoQuality.QUALITY_480P -> "480p"
                                    VideoQuality.QUALITY_720P -> "720p"
                                    VideoQuality.QUALITY_1080P -> "1080p"
                                },
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    },
                    onClick = {
                        showQualityMenu = true
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_subtitles),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                    tint = Color.White
                                )
                                Text(stringResource(R.string.captions))
                            }
                            Text(
                                text = if (subtitlesEnabled) stringResource(R.string.captions_on) else stringResource(R.string.captions_off),
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    },
                    onClick = {
                        onSubtitlesToggle()
                        expanded = false
                    }
                )

                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.fullscreen),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                    tint = Color.White
                                )
                                Text("Kesesuaian video")
                            }
                            Text(
                                text = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Regangkan"
                                    AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> "Tetap"
                                    else -> "Tetap"
                                },
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    },
                    onClick = {
                        val nextMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                        }
                        onResizeModeChange(nextMode)
                        expanded = false
                    }
                )
            }
        } else {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    showQualityMenu = false
                }
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp).padding(end = 12.dp),
                                tint = Color.White
                            )
                            Text("Kualitas")
                        }
                    },
                    onClick = {
                        showQualityMenu = false
                    }
                )

                VideoQuality.entries.forEach { quality ->
                    val label = when (quality) {
                        VideoQuality.QUALITY_360P -> "360p"
                        VideoQuality.QUALITY_480P -> "480p"
                        VideoQuality.QUALITY_720P -> "720p"
                        VideoQuality.QUALITY_1080P -> "1080p"
                    }
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label)
                                if (quality == videoQuality) {
                                    Icon(
                                        painter = painterResource(R.drawable.check),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onVideoQualityChange(quality)
                            BeatlesPlayerUtils.setPreferredVideoQuality(quality)
                            expanded = false
                            showQualityMenu = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * [5] Enhanced seek effect overlay with ripple circles and seek arrows
 */
@Composable
private fun SeekEffectOverlay(
    seekDirection: String,
    isForward: Boolean = true,
    modifier: Modifier = Modifier
) {
    val rippleAlpha = remember { Animatable(0.6f) }
    val rippleScale = remember { Animatable(0.5f) }

    LaunchedEffect(seekDirection) {
        rippleScale.snapTo(0.5f)
        rippleAlpha.snapTo(0.6f)
        launch { rippleScale.animateTo(1.5f, tween(600)) }
        rippleAlpha.animateTo(0f, tween(600))
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Canvas(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    scaleX = rippleScale.value
                    scaleY = rippleScale.value
                    alpha = rippleAlpha.value
                }
        ) {
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = size.minDimension / 2
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isForward) R.drawable.fast_forward else R.drawable.fast_forward
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { if (!isForward) scaleX = -1f }
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = seekDirection,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Video lyrics overlay displayed at bottom like YouTube subtitles.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideoLyricsOverlay(
    isFixedMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val player = playerConnection.player

    val videoModeEnabled by playerConnection.videoModeEnabled.collectAsState()
    if (!videoModeEnabled) return

    val bottomPadding = if (isFixedMode) 24.dp else 16.dp

    val (subtitlesEnabled, _) = rememberPreference(
        SubtitlesEnabledKey,
        defaultValue = true
    )
    val (videoLyricsEnabled, _) = rememberPreference(
        VideoLyricsEnabledKey,
        defaultValue = true
    )
    val subtitleLanguage by rememberPreference(
        SubtitleLanguageKey,
        defaultValue = "auto"
    )

    if (!videoLyricsEnabled || !subtitlesEnabled) return

    var transcriptText by remember { mutableStateOf<String?>(null) }
    var isLoadingCaptions by remember { mutableStateOf(false) }
    var captionError by remember { mutableStateOf<String?>(null) }
    var videoDurationMs by remember { mutableLongStateOf(0L) }

    val activeVideoId by playerConnection.currentVideoId.collectAsState()

    LaunchedEffect(activeVideoId) {
        val videoId = activeVideoId ?: return@LaunchedEffect
        val cached = playerConnection.captionCache[videoId]
        if (cached != null) {
            transcriptText = cached
        }
    }

    LaunchedEffect(activeVideoId, subtitleLanguage) {
        val videoId = activeVideoId ?: return@LaunchedEffect

        val cachedCaption = playerConnection.captionCache[videoId]
        if (cachedCaption != null) {
            transcriptText = cachedCaption
            return@LaunchedEffect
        }

        if (videoId in playerConnection.captionAttemptedIds) {
            return@LaunchedEffect
        }

        playerConnection.captionAttemptedIds.add(videoId)

        transcriptText = null
        captionError = null
        isLoadingCaptions = true
        videoDurationMs = 0L

        delay(500)
        withContext(Dispatchers.IO) {
            try {
                val captionResult = YouTube.getCaptionTracksWithDuration(videoId)
                val captionResultPair = captionResult.getOrNull()
                val captionTracks = captionResultPair?.first
                videoDurationMs = captionResultPair?.second ?: 0L

                if (!captionTracks.isNullOrEmpty()) {
                    val captionTrack = if (subtitleLanguage != "auto") {
                        captionTracks.firstOrNull { it.languageCode == subtitleLanguage }
                    } else {
                        null
                    }
                        ?: captionTracks.firstOrNull { it.languageCode == "en" }
                        ?: captionTracks.firstOrNull { it.kind == "asr" }
                        ?: captionTracks.firstOrNull()
                    if (captionTrack != null) {
                        val fetchResult = YouTube.fetchSubtitleFromCaptionTrack(captionTrack.baseUrl)
                        fetchResult.onSuccess { text ->
                            if (text.isNotEmpty()) {
                                transcriptText = text
                            }
                        }.onFailure { e ->
                            captionError = e.message
                        }
                    }
                } else {
                    captionError = "Trek teks tidak tersedia"
                }

                if (transcriptText == null) {
                    val transcriptResult = YouTube.transcript(videoId)
                    transcriptResult.onSuccess { text ->
                        transcriptText = text
                    }.onFailure { e ->
                        if (captionError == null) {
                            captionError = "Transcript: ${e.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                captionError = e.message
            } finally {
                isLoadingCaptions = false
                withContext(Dispatchers.Main) {
                    if (transcriptText != null && transcriptText!!.isNotEmpty()) {
                        playerConnection.captionCache[videoId] = transcriptText!!
                        Toast.makeText(context, "Teks berhasil dimuat", Toast.LENGTH_SHORT).show()
                    } else if (captionError != null) {
                        Toast.makeText(context, "Gagal memuat teks: $captionError", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val lyricsText = remember(transcriptText) {
        if (!transcriptText.isNullOrEmpty()) transcriptText else null
    }

    data class LyricLine(val timeMs: Long, val text: String)

    val parsedLines = remember(lyricsText) {
        if (lyricsText.isNullOrBlank()) return@remember emptyList<LyricLine>()
        val result = mutableListOf<LyricLine>()
        val regex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\](.*)""")
        for (line in lyricsText.lines()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            val match = regex.find(trimmedLine) ?: continue
            val minutes = match.groupValues[1].toLongOrNull() ?: 0
            val seconds = match.groupValues[2].toLongOrNull() ?: 0
            val millis = match.groupValues[3].padEnd(3, '0').toLongOrNull() ?: 0
            val timeMs = minutes * 60_000 + seconds * 1000 + millis
            val text = match.groupValues[4].trim()
            if (text.isNotEmpty()) {
                result.add(LyricLine(timeMs, text))
            }
        }
        result.sortedBy { it.timeMs }
    }

    var playerPosition by remember { mutableLongStateOf(player.currentPosition) }
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val lyricsOffset = currentSong?.song?.lyricsOffset ?: 0

    LaunchedEffect(mediaMetadata?.id, lyricsOffset) {
        while (isActive) {
            playerPosition = player.currentPosition - lyricsOffset
            delay(16)
        }
    }

    val currentIndex = remember(parsedLines, playerPosition) {
        if (parsedLines.isEmpty()) return@remember -1
        var bestIdx = -1
        for (i in parsedLines.indices) {
            if (playerPosition >= parsedLines[i].timeMs) {
                bestIdx = i
            } else break
        }
        bestIdx
    }

    val currentLine = if (currentIndex >= 0) parsedLines[currentIndex].text else null
    val nextLine = if (currentIndex >= 0 && currentIndex + 1 < parsedLines.size) parsedLines[currentIndex + 1].text else null

    val glowStyle = TextStyle(
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        shadow = Shadow(
            color = Color.Black,
            offset = Offset(0f, 0f),
            blurRadius = 12f
        )
    )

    if (isLoadingCaptions) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.fillMaxWidth()
        ) {
            ContainedLoadingIndicator()
        }
    } else if (currentLine != null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(bottom = bottomPadding)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.55f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = currentLine,
                        style = glowStyle.copy(
                            shadow = Shadow(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                offset = Offset(0f, 0f),
                                blurRadius = 20f
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = currentLine,
                        style = glowStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (nextLine != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = nextLine,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(0f, 0f),
                            blurRadius = 8f
                        )
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
        }
    }
}
