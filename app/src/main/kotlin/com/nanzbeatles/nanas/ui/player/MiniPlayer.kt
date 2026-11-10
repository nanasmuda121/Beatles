/**
 * NanzBeatles Project (C) 2026
 * 
 * Performance optimized MiniPlayer - prevents unnecessary recomposition
 */

package com.nanzbeatles.nanas.ui.player

import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.LocalListenTogetherManager
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.CropAlbumArtKey
import com.nanzbeatles.nanas.constants.DarkModeKey
import com.nanzbeatles.nanas.constants.LiquidGlassEffectKey
import com.nanzbeatles.nanas.constants.LiquidGlassBlurRadiusKey
import com.nanzbeatles.nanas.constants.LiquidGlassCornerRadiusKey
import com.nanzbeatles.nanas.constants.LiquidGlassOpacityKey
import com.nanzbeatles.nanas.constants.MiniPlayerHeight
import com.nanzbeatles.nanas.constants.MiniPlayerHeightKey
import com.nanzbeatles.nanas.constants.MiniPlayerCornerRadiusKey
import com.nanzbeatles.nanas.constants.MiniPlayerShowFavoriteKey
import com.nanzbeatles.nanas.constants.MiniPlayerShowSubscribeKey
import com.nanzbeatles.nanas.constants.MiniPlayerShowPlayPauseKey
import com.nanzbeatles.nanas.constants.PureBlackMiniPlayerKey
import com.nanzbeatles.nanas.constants.SwipeSensitivityKey
import com.nanzbeatles.nanas.constants.SwipeThumbnailKey
import com.nanzbeatles.nanas.constants.ThumbnailCornerRadius
import com.nanzbeatles.nanas.constants.UseNewMiniPlayerDesignKey
import com.nanzbeatles.nanas.db.entities.ArtistEntity
import com.nanzbeatles.nanas.listentogether.ListenTogetherManager
import com.nanzbeatles.nanas.models.MediaMetadata
import com.nanzbeatles.nanas.playback.CastConnectionHandler
import com.nanzbeatles.nanas.playback.PlayerConnection
import com.nanzbeatles.nanas.ui.screens.settings.DarkMode
import com.nanzbeatles.nanas.ui.utils.crystalLiquidGlassPill
import com.nanzbeatles.nanas.ui.utils.liquidGlassPill
import com.nanzbeatles.nanas.ui.utils.resize
import com.nanzbeatles.nanas.ui.utils.resolveArtworkUrl
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.nanzbeatles.nanas.ui.component.Icon as MIcon

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.nanzbeatles.nanas.constants.MiniPlayerBallModeKey

/**
 * Stable wrapper for progress state - reads values only during draw phase
 * This prevents recomposition when position/duration change
 */
@Stable
class ProgressState(
    private val positionState: MutableLongState,
    private val durationState: MutableLongState
) {
    val progress: Float
        get() {
            val duration = durationState.longValue
            return if (duration > 0) (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
        }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    modifier: Modifier = Modifier,
    onHardwareIntegrationClick: (() -> Unit)? = null,
    onExpandPlayer: (() -> Unit)? = null,
) {
    val useNewMiniPlayerDesign by rememberPreference(UseNewMiniPlayerDesignKey, true)
    val (isBallMode, onBallModeChange) = rememberPreference(MiniPlayerBallModeKey, defaultValue = false)

    // Create stable progress state - doesn't cause recomposition on position changes
    val progressState = remember { ProgressState(positionState, durationState) }

    AnimatedContent(
        targetState = isBallMode,
        transitionSpec = {
            if (targetState) {
                // Morphing from Bar -> Ball docked above Home button (left side)
                (fadeIn(tween(220)) +
                    scaleIn(
                        initialScale = 0.72f,
                        transformOrigin = TransformOrigin(0.15f, 0.85f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 5 },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    ))
                    .togetherWith(
                        fadeOut(tween(160)) +
                            scaleOut(
                                targetScale = 0.45f,
                                transformOrigin = TransformOrigin(0.15f, 0.85f),
                                animationSpec = tween(180)
                            ) +
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                animationSpec = tween(180)
                            )
                    )
            } else {
                // Expanding from Ball -> Full MiniPlayer Bar
                (fadeIn(tween(220)) +
                    scaleIn(
                        initialScale = 0.65f,
                        transformOrigin = TransformOrigin(0.15f, 0.85f),
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ))
                    .togetherWith(
                        fadeOut(tween(150)) +
                            scaleOut(
                                targetScale = 0.75f,
                                transformOrigin = TransformOrigin(0.15f, 0.85f),
                                animationSpec = tween(150)
                            )
                    )
            }
        },
        label = "MiniPlayerBallModeTransition"
    ) { ballMode ->
        if (ballMode) {
            MiniPlayerDockedBall(
                progressState = progressState,
                modifier = modifier,
                onRestoreBar = { onBallModeChange(false) },
                onExpandPlayer = onExpandPlayer
            )
        } else if (useNewMiniPlayerDesign) {
            NewMiniPlayer(
                progressState = progressState,
                modifier = modifier,
                onHardwareIntegrationClick = onHardwareIntegrationClick,
                onExpandPlayer = onExpandPlayer,
                onCollapseToBall = { onBallModeChange(true) }
            )
        } else {
            Box(modifier = modifier.fillMaxWidth()) {
                LegacyMiniPlayer(
                    progressState = progressState,
                    modifier = Modifier.align(Alignment.Center),
                    onHardwareIntegrationClick = onHardwareIntegrationClick,
                    onExpandPlayer = onExpandPlayer,
                    onCollapseToBall = { onBallModeChange(true) }
                )
            }
        }
    }
}

/**
 * Docked 3D Liquid Glass Ball Mode positioned directly above the Home button (leftmost nav item).
 * - Swipe right or tap the ball -> restores the full MiniPlayer bar
 * - Tap the mini play/pause badge -> toggles playback
 * - Long-press the ball -> expands the full BottomSheetPlayer
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiniPlayerDockedBall(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onRestoreBar: () -> Unit,
    onExpandPlayer: (() -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val isEditorial = com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL"
    val useDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val liquidGlassPref by rememberPreference(LiquidGlassEffectKey, defaultValue = true)
    val liquidGlassEnabled = !isEditorial && liquidGlassPref
    val liquidGlassBlurRadius by rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 35f)

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val coroutineScope = rememberCoroutineScope()
    val offsetXAnimatable = remember { Animatable(0f) }
    val springSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    }

    // Gentle vinyl rotation when music is playing
    val infiniteTransition = rememberInfiniteTransition(label = "ballVinylRotation")
    val discRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ballDiscAngle"
    )

    val accentColor = MaterialTheme.colorScheme.primary
    val ringColor = remember(accentColor, isEditorial, useDarkTheme) {
        if (isEditorial) {
            if (useDarkTheme) Color(0xFFF5F2EB) else Color(0xFF111111)
        } else {
            if (accentColor.luminance() < 0.25f) Color(0xFF1DB954) else accentColor
        }
    }

    // Align horizontally with the 1st slot (Home tab) of AppNavigationBar (14.dp + 6.dp = 20.dp padding, 4 equal slots)
    // and shift down slightly so the ball sits snugly right on top of ("nempel di atas") the Home button!
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .offset(y = 14.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                    .size(58.dp)
                    .pointerInput(Unit) {
                        var dragSumX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragSumX = 0f },
                            onDragCancel = {
                                coroutineScope.launch { offsetXAnimatable.animateTo(0f, springSpec) }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragSumX += dragAmount
                                coroutineScope.launch {
                                    offsetXAnimatable.snapTo((dragSumX * 0.65f).coerceIn(-24f, 96f))
                                }
                            },
                            onDragEnd = {
                                if (dragSumX > 28f) {
                                    // Swipe right on ball -> expand back to full MiniPlayer bar
                                    onRestoreBar()
                                }
                                coroutineScope.launch { offsetXAnimatable.animateTo(0f, springSpec) }
                            }
                        )
                    }
            ) {
                // Main 56.dp 3D Liquid Glass Orb
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = onRestoreBar,
                            onLongClick = { onExpandPlayer?.invoke() }
                        )
                        .border(
                            width = if (isEditorial) 1.2.dp else 1.4.dp,
                            brush = if (isEditorial) {
                                androidx.compose.ui.graphics.SolidColor(
                                    ringColor.copy(alpha = 0.28f)
                                )
                            } else {
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        ringColor.copy(alpha = 0.65f),
                                        Color.White.copy(alpha = 0.22f),
                                        ringColor.copy(alpha = 0.45f)
                                    )
                                )
                            },
                            shape = CircleShape
                        )
                        .drawWithContent {
                            drawContent()
                            // 360-degree circular progress ring around the perimeter of the ball
                            val progress = progressState.progress
                            val strokePx = 3.dp.toPx()
                            val inset = strokePx / 2f + 1.2.dp.toPx()
                            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)

                            // Subtle circular track
                            drawArc(
                                color = if (isEditorial) {
                                    ringColor.copy(alpha = 0.22f)
                                } else {
                                    Color.White.copy(alpha = if (useDarkTheme) 0.20f else 0.32f)
                                },
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                            // Active circular progress
                            if (progress > 0.005f) {
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = (progress * 360f).coerceIn(4f, 360f),
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                )
                            }
                        }
                ) {
                    // Backdrop inside the sphere
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .then(
                                if (liquidGlassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.blur(liquidGlassBlurRadius.coerceIn(12f, 35f).dp)
                                } else {
                                    Modifier
                                }
                            )
                            .background(
                                color = if (isEditorial) {
                                    if (useDarkTheme) Color(0xFF101014) else Color(0xFFF6F3EB)
                                } else if (useDarkTheme) {
                                    Color(0xFF0E121B).copy(alpha = 0.72f)
                                } else {
                                    Color(0xFF1A1F2C).copy(alpha = 0.78f)
                                },
                                shape = CircleShape
                            )
                            .then(
                                if (!isEditorial) {
                                    Modifier.background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.30f),
                                                ringColor.copy(alpha = 0.22f),
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.14f)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                } else Modifier
                            )
                    )

                    // Circular rotating album artwork inside the progress ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(5.5.dp)
                            .clip(CircleShape)
                    ) {
                        mediaMetadata?.let { metadata ->
                            val thumbnailUrl = remember(metadata.thumbnailUrl, metadata.id) {
                                resolveArtworkUrl(metadata.thumbnailUrl?.resize(120, 120), metadata.id)
                            }
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = metadata.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        rotationZ = if (effectiveIsPlaying) discRotation else 0f
                                    }
                                    .clip(CircleShape)
                                    .background(Color.DarkGray)
                            )
                        }

                        // Subtle dark glass center spindle / status overlay
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.52f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                        ) {
                            if (playbackState == Player.STATE_BUFFERING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(11.dp),
                                    color = Color.White,
                                    strokeWidth = 1.5.dp
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(if (effectiveIsPlaying) ringColor else Color.White.copy(alpha = 0.8f))
                                )
                            }
                        }
                    }
                }

                // Direct Play/Pause mini badge on bottom-right rim of the Ball
                val badgeBgBrush = if (isEditorial) {
                    androidx.compose.ui.graphics.SolidColor(
                        if (useDarkTheme) Color(0xFFF5F2EB) else Color(0xFF111111)
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(
                            ringColor.copy(alpha = 0.95f),
                            Color(0xFF0E121B).copy(alpha = 0.92f)
                        )
                    )
                }
                val badgeBorderColor = if (isEditorial) {
                    if (useDarkTheme) Color(0xFF08080A) else Color(0xFFFBF9F5)
                } else {
                    Color.White.copy(alpha = 0.72f)
                }
                val badgeIconColor = if (isEditorial) {
                    if (useDarkTheme) Color(0xFF08080A) else Color(0xFFFBF9F5)
                } else {
                    Color.White
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 1.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            brush = badgeBgBrush,
                            shape = CircleShape
                        )
                        .border(1.dp, badgeBorderColor, CircleShape)
                        .clickable {
                            if (isListenTogetherGuest) {
                                playerConnection.toggleMute()
                                return@clickable
                            }
                            if (isCasting) {
                                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                            } else if (playbackState == Player.STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.togglePlayPause()
                            }
                        }
                ) {
                    Icon(
                        painter = painterResource(
                            if (effectiveIsPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = null,
                        tint = badgeIconColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Empty right space above the remaining 3 navigation tabs (Search, Library, Settings)
        Spacer(modifier = Modifier.weight(3f))
    }
}

// ============================================================================
// NEW MINI PLAYER DESIGN
// ============================================================================

@Composable
private fun NewMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onHardwareIntegrationClick: (() -> Unit)? = null,
    onExpandPlayer: (() -> Unit)? = null,
    onCollapseToBall: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    
    // Theme settings - use actual surface luminance so AUTO and Editorial modes always match
    val pureBlack by rememberPreference(PureBlackMiniPlayerKey, defaultValue = false)
    val useDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    // Player states - only collect what's needed at this level
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    
    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }

    // Swipe settings
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    
    // Disable swipe for Listen Together guests
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    
    val configuration = LocalConfiguration.current
    val isTabletLandscape = remember(configuration.screenWidthDp, configuration.orientation) {
        configuration.screenWidthDp >= 600 && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    // Swipe animation state
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    }

    val autoSwipeThreshold = remember(swipeSensitivity) {
        (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    
    // Memoize colors
    // Memoize colors
    val liquidGlassPref by rememberPreference(LiquidGlassEffectKey, defaultValue = true)
    val isEditorial = com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL"
    val liquidGlassEnabled = !isEditorial && liquidGlassPref
    val liquidGlassBlurRadius by rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 35f)
    val liquidGlassCornerRadius by rememberPreference(LiquidGlassCornerRadiusKey, defaultValue = 18f)
    val liquidGlassOpacity by rememberPreference(LiquidGlassOpacityKey, defaultValue = 0.20f)

    // Mini player customization
    val miniPlayerHeight by rememberPreference(MiniPlayerHeightKey, defaultValue = 64f)
    val miniPlayerCornerPref by rememberPreference(MiniPlayerCornerRadiusKey, defaultValue = 32f)
    val miniPlayerCornerRadius = if (isEditorial) 10f else miniPlayerCornerPref
    val showFavorite by rememberPreference(MiniPlayerShowFavoriteKey, defaultValue = true)
    val showSubscribe by rememberPreference(MiniPlayerShowSubscribeKey, defaultValue = true)
    val showPlayPause by rememberPreference(MiniPlayerShowPlayPauseKey, defaultValue = true)

    val primaryColor = if (isEditorial) {
        if (useDarkTheme) Color(0xFFF5F2EB) else Color(0xFF111111)
    } else {
        if (useDarkTheme) Color.White else Color.Black
    }
    val outlineColor = if (isEditorial) {
        if (useDarkTheme) Color(0xFFF5F2EB).copy(alpha = 0.35f) else Color(0xFF111111).copy(alpha = 0.30f)
    } else {
        if (useDarkTheme) MaterialTheme.colorScheme.outline else Color(0x33000000)
    }
    val onSurfaceColor = if (isEditorial) {
        if (useDarkTheme) Color(0xFFF5F2EB) else Color(0xFF111111)
    } else {
        if (useDarkTheme) Color.White else Color.Black
    }
    val errorColor = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onExpandPlayer?.invoke() }
            )
            .pointerInput(swipeThumbnail) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragStartTime = System.currentTimeMillis()
                        totalDragDistance = 0f
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(0f, animationSpec)
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        val adjustedDragAmount =
                            if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                        val canSkipPrev = playerConnection.player.previousMediaItemIndex != -1
                        val tryingToSwipeRight = adjustedDragAmount > 0
                        val tryingToSwipeLeft = adjustedDragAmount < 0
                        // Swiping left is ALWAYS allowed so MiniPlayer can transform into a Ball above Home
                        val allowLeft = tryingToSwipeLeft
                        val allowRight = tryingToSwipeRight && swipeThumbnail && canSkipPrev

                        val canReturnToCenter =
                            (tryingToSwipeRight && offsetXAnimatable.value < 0) ||
                                (tryingToSwipeLeft && offsetXAnimatable.value > 0)

                        if (allowLeft || allowRight || canReturnToCenter) {
                            change.consume()
                            totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                            coroutineScope.launch {
                                offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                            }
                        }
                    },
                    onDragEnd = {
                        val dragDuration = System.currentTimeMillis() - dragStartTime
                        val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                        val currentOffset = offsetXAnimatable.value
                        val minDistanceThreshold = 42f
                        val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                        if (currentOffset < -minDistanceThreshold) {
                            // Swipe ke kiri -> ubah MiniPlayer menjadi Bola di atas tombol Home
                            onCollapseToBall()
                        } else {
                            val shouldChangeSong = (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                (kotlin.math.abs(currentOffset) > autoSwipeThreshold)
                            if (shouldChangeSong && currentOffset > 0 && swipeThumbnail && canSkipPrevious) {
                                playerConnection.player.seekToPreviousMediaItem()
                            }
                        }
                        coroutineScope.launch {
                            offsetXAnimatable.animateTo(0f, animationSpec)
                        }
                    }
                )
            }
    ) {
        val accentColor = MaterialTheme.colorScheme.primary
        val cyanTint = Color(0xFF64E9FF)
        val topGlintColor = if (useDarkTheme) Color.White.copy(alpha = 0.72f) else Color.White.copy(alpha = 0.98f)
        val bottomGlintColor = if (useDarkTheme) cyanTint.copy(alpha = 0.38f) else accentColor.copy(alpha = 0.28f)
        Box(
            modifier = Modifier
                .then(if (isTabletLandscape) Modifier.width(500.dp).align(Alignment.Center) else Modifier.fillMaxWidth())
                .height(miniPlayerHeight.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(miniPlayerCornerRadius.dp))
                .let { base ->
                    if (!liquidGlassEnabled) {
                        base.background(
                            color = if (isEditorial) {
                                if (useDarkTheme) Color(0xFF101014) else Color(0xFFF6F3EB)
                            } else {
                                if (pureBlack && useDarkTheme) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                    } else base
                }
                .drawWithContent {
                    if (liquidGlassEnabled) {
                        val topDomeDepth = size.height * 0.45f
                        val bottomCausticDepth = size.height * 0.38f
                        drawRect(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (useDarkTheme) 0.22f else 0.56f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = topDomeDepth
                            )
                        )
                        drawRect(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    cyanTint.copy(alpha = if (useDarkTheme) 0.16f else 0.10f)
                                ),
                                startY = size.height - bottomCausticDepth,
                                endY = size.height
                            )
                        )
                    }
                    drawContent()
                    if (liquidGlassEnabled) {
                        val glintWidth = size.width * 0.76f
                        val startX = (size.width - glintWidth) / 2f
                        drawLine(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, topGlintColor, Color.Transparent),
                                startX = startX,
                                endX = startX + glintWidth
                            ),
                            start = Offset(startX, 1.2.dp.toPx()),
                            end = Offset(startX + glintWidth, 1.2.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        val bottomWidth = size.width * 0.58f
                        val bottomStartX = (size.width - bottomWidth) / 2f
                        drawLine(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, bottomGlintColor, Color.Transparent),
                                startX = bottomStartX,
                                endX = bottomStartX + bottomWidth
                            ),
                            start = Offset(bottomStartX, size.height - 1.dp.toPx()),
                            end = Offset(bottomStartX + bottomWidth, size.height - 1.dp.toPx()),
                            strokeWidth = 1.15.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                .border(
                    width = if (isEditorial) 1.2.dp else 1.35.dp,
                    brush = if (liquidGlassEnabled) {
                        if (useDarkTheme) {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.78f),
                                    cyanTint.copy(alpha = 0.50f),
                                    Color.White.copy(alpha = 0.14f),
                                    accentColor.copy(alpha = 0.42f),
                                    Color.White.copy(alpha = 0.48f)
                                )
                            )
                        } else {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.16f),
                                    cyanTint.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.08f),
                                    accentColor.copy(alpha = 0.28f),
                                    Color.Black.copy(alpha = 0.14f)
                                )
                            )
                        }
                    } else if (isEditorial) {
                        androidx.compose.ui.graphics.SolidColor(
                            if (useDarkTheme) Color(0xFFF5F2EB).copy(alpha = 0.28f)
                            else Color(0xFF111111).copy(alpha = 0.24f)
                        )
                    } else {
                        androidx.compose.ui.graphics.SolidColor(outlineColor.copy(alpha = 0.3f))
                    },
                    shape = RoundedCornerShape(miniPlayerCornerRadius.dp)
                )
        ) {
            if (liquidGlassEnabled) {
                // 3D Liquid Glass blurred backdrop with refractive album artwork & caustic sheen
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(miniPlayerCornerRadius.dp))
                ) {
                    mediaMetadata?.let { meta ->
                        val resolvedGlassThumb = remember(meta.thumbnailUrl, meta.id) {
                            resolveArtworkUrl(meta.thumbnailUrl, meta.id)
                        }
                        resolvedGlassThumb?.let { thumbUrl ->
                        AsyncImage(
                            model = thumbUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Modifier.blur(liquidGlassBlurRadius.coerceIn(18f, 40f).dp)
                                    } else {
                                        Modifier
                                    }
                                )
                                .graphicsLayer { alpha = if (useDarkTheme) 0.28f else 0.18f }
                        )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = if (useDarkTheme) {
                                    Color(0xFF0A0D14).copy(alpha = 0.22f)
                                } else {
                                    Color(0xFFF3F4F8).copy(alpha = 0.82f)
                                },
                                shape = RoundedCornerShape(miniPlayerCornerRadius.dp)
                            )
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(
                                        if (useDarkTheme) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.88f),
                                        cyanTint.copy(alpha = if (useDarkTheme) 0.14f else 0.10f),
                                        Color.Transparent,
                                        accentColor.copy(alpha = if (useDarkTheme) 0.12f else 0.08f)
                                    )
                                ),
                                shape = RoundedCornerShape(miniPlayerCornerRadius.dp)
                            )
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                // Play button with progress - isolated composable
                NewMiniPlayerPlayButton(
                    progressState = progressState,
                    playbackState = playbackState,
                    isCasting = isCasting,
                    castHandler = castHandler,
                    playerConnection = playerConnection,
                    mediaMetadata = mediaMetadata,
                    primaryColor = primaryColor,
                    outlineColor = outlineColor,
                    listenTogetherManager = listenTogetherManager
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Song info - isolated composable
                NewMiniPlayerSongInfo(
                    mediaMetadata = mediaMetadata,
                    onSurfaceColor = onSurfaceColor,
                    errorColor = errorColor,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))
                
                // Cast indicator
                if (isCasting) {
                    Icon(
                        painter = painterResource(R.drawable.cast_connected),
                        contentDescription = "Casting",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Subscribe button - isolated composable
                if (showSubscribe) {
                    mediaMetadata?.artists?.firstOrNull()?.id?.let { artistId ->
                        SubscribeButton(artistId = artistId, metadata = mediaMetadata!!)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Favorite button - isolated composable
                if (showFavorite) {
                    mediaMetadata?.let { FavoriteButton(songId = it.id) }
                    if (showPlayPause) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Dedicated Play/Pause button
                if (showPlayPause) {
                    NewMiniPlayerPlayPauseButton(
                        playbackState = playbackState,
                        isCasting = isCasting,
                        castHandler = castHandler,
                        playerConnection = playerConnection,
                        listenTogetherManager = listenTogetherManager,
                        primaryColor = primaryColor,
                        outlineColor = outlineColor,
                        onSurfaceColor = onSurfaceColor
                    )
                }
            }
        }
    }
}

/**
 * Play button with circular progress indicator
 * Uses drawWithContent to update progress without recomposition
 */
@Composable
private fun NewMiniPlayerPlayButton(
    progressState: ProgressState,
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata?,
    primaryColor: Color,
    outlineColor: Color,
    listenTogetherManager: ListenTogetherManager?
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsState()

    
    val trackColor = outlineColor.copy(alpha = 0.2f)
    val strokeWidth = 3.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .drawWithContent {
                drawContent()
                // Draw progress arc - this reads progressState.progress during draw phase only
                val progress = progressState.progress
                val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                val startAngle = -90f
                val sweepAngle = 360f * progress
                val diameter = size.minDimension
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                
                // Draw track
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = stroke
                )
                // Draw progress
                drawArc(
                    color = primaryColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = stroke
                )
            }
    ) {
        // Thumbnail with play/pause overlay
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, outlineColor.copy(alpha = 0.3f), CircleShape)
                .clickable {
                    if (isListenTogetherGuest) {
                        playerConnection.toggleMute()
                        return@clickable
                    }
                    if (isCasting) {
                        if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                    } else if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.togglePlayPause()
                    }
                }
        ) {
            mediaMetadata?.let { metadata ->
                val thumbnailUrl = remember(metadata.thumbnailUrl, metadata.id) {
                    resolveArtworkUrl(metadata.thumbnailUrl?.resize(120, 120), metadata.id)
                }
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
    }
}

/**
 * Song info display - title and artist
 */
@Composable
private fun NewMiniPlayerSongInfo(
    mediaMetadata: MediaMetadata?,
    onSurfaceColor: Color,
    errorColor: Color,
    modifier: Modifier = Modifier
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        mediaMetadata?.let { metadata ->
            Text(
                text = metadata.title,
                color = onSurfaceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (metadata.explicit) MIcon.Explicit()
                if (metadata.artists.any { it.name.isNotBlank() }) {
                    Text(
                        text = metadata.artists.joinToString { it.name },
                        color = onSurfaceColor.copy(alpha = 0.86f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                    )
                }
            }

            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.error_playing),
                    color = errorColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// LEGACY MINI PLAYER DESIGN
// ============================================================================

@Composable
private fun LegacyMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onHardwareIntegrationClick: (() -> Unit)? = null,
    onExpandPlayer: (() -> Unit)? = null,
    onCollapseToBall: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val pureBlack by rememberPreference(PureBlackMiniPlayerKey, defaultValue = false)
    val isEditorial = com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL"
    val useDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val liquidGlassPref by rememberPreference(LiquidGlassEffectKey, defaultValue = true)
    val liquidGlassEnabled = !isEditorial && liquidGlassPref
    val liquidGlassBlurRadius by rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 35f)
    
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    
    val castHandler = remember(playerConnection) {
        try {
            playerConnection.service.castConnectionHandler
        } catch (e: Exception) {
            null
        }
    }
    val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }

    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    
    // Disable swipe for Listen Together guests
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    
    val configuration = LocalConfiguration.current
    val isTabletLandscape = remember(configuration.screenWidthDp, configuration.orientation) {
        configuration.screenWidthDp >= 600 && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    }

    val autoSwipeThreshold = remember(swipeSensitivity) {
        (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    
    val isDark = useDarkTheme
    val primaryColor = if (isDark) Color.White else Color.Black
    val trackColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .then(if (isTabletLandscape) Modifier.width(500.dp) else Modifier.fillMaxWidth())
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onExpandPlayer?.invoke() }
            )
            .let { base ->
                if (!liquidGlassEnabled) {
                    base.background(
                        if (isEditorial) {
                            if (isDark) Color(0xFF101014) else Color(0xFFF6F3EB)
                        } else if (pureBlack && isDark) Color.Black
                        else MaterialTheme.colorScheme.surfaceContainer
                    ).then(
                        if (isEditorial) {
                            Modifier.border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFFF5F2EB).copy(alpha = 0.24f) else Color(0xFF111111).copy(alpha = 0.20f),
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                            )
                        } else Modifier
                    )
                } else {
                    base.border(
                        width = 1.dp,
                        brush = if (isDark) {
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.20f), Color.Transparent)
                            )
                        } else {
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent)
                            )
                        },
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                }
            }
            .pointerInput(swipeThumbnail) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragStartTime = System.currentTimeMillis()
                        totalDragDistance = 0f
                    },
                    onDragCancel = {
                        coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        val adjustedDragAmount =
                            if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                        val canSkipPrev = playerConnection.player.previousMediaItemIndex != -1
                        val tryingToSwipeRight = adjustedDragAmount > 0
                        val tryingToSwipeLeft = adjustedDragAmount < 0
                        val allowLeft = tryingToSwipeLeft
                        val allowRight = tryingToSwipeRight && swipeThumbnail && canSkipPrev

                        val canReturnToCenter =
                            (tryingToSwipeRight && offsetXAnimatable.value < 0) ||
                                (tryingToSwipeLeft && offsetXAnimatable.value > 0)

                        if (allowLeft || allowRight || canReturnToCenter) {
                            change.consume()
                            totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                            coroutineScope.launch {
                                offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                            }
                        }
                    },
                    onDragEnd = {
                        val dragDuration = System.currentTimeMillis() - dragStartTime
                        val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                        val currentOffset = offsetXAnimatable.value
                        val minDistanceThreshold = 42f
                        val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                        if (currentOffset < -minDistanceThreshold) {
                            // Swipe ke kiri -> ubah MiniPlayer menjadi Bola di atas tombol Home
                            onCollapseToBall()
                        } else {
                            val shouldChangeSong = (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                (kotlin.math.abs(currentOffset) > autoSwipeThreshold)
                            if (shouldChangeSong && currentOffset > 0 && swipeThumbnail && canSkipPrevious) {
                                playerConnection.player.seekToPreviousMediaItem()
                            }
                        }
                        coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                    }
                )
            }
    ) {
        if (liquidGlassEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(liquidGlassBlurRadius.dp)
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        color = if (isDark) {
                            Color(0xFF141418).copy(alpha = 0.62f)
                        } else {
                            Color(0xFFF7F7FA).copy(alpha = 0.80f)
                        }
                    )
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.85f),
                                if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.50f),
                                if (isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.70f)
                            )
                        )
                    )
            )
        }

        // Progress bar - uses drawWithContent to avoid recomposition
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.BottomCenter)
                .drawWithContent {
                    val progress = progressState.progress
                    drawRect(trackColor)
                    drawRect(primaryColor, size = Size(size.width * progress, size.height))
                }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .padding(end = 12.dp),
        ) {
            Box(Modifier.weight(1f)) {
                mediaMetadata?.let {
                    LegacyMiniMediaInfo(
                        mediaMetadata = it,
                        pureBlack = pureBlack,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }

            LegacyPlayPauseButton(
                playbackState = playbackState,
                isCasting = isCasting,
                castHandler = castHandler,
                playerConnection = playerConnection,
                listenTogetherManager = listenTogetherManager
            )

            IconButton(
                    enabled = canSkipNext && !isListenTogetherGuest,
                    onClick = if (isListenTogetherGuest) ({}) else ({ playerConnection.seekToNext() }),
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = if (isDark) Color.White else Color.Black
                )
            }
        }

        // Swipe indicator
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = primaryColor.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun LegacyPlayPauseButton(
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    listenTogetherManager: ListenTogetherManager?
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsState()


    IconButton(
        onClick = {
            if (isListenTogetherGuest) {
                playerConnection.toggleMute()
                return@IconButton
            }
            if (isCasting) {
                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
            } else if (playbackState == Player.STATE_ENDED) {
                playerConnection.player.seekTo(0, 0)
                playerConnection.player.playWhenReady = true
            } else {
                playerConnection.togglePlayPause()
            }
        },
    ) {
        Icon(
            painter = painterResource(
                when {
                    isListenTogetherGuest -> if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                    playbackState == Player.STATE_ENDED -> R.drawable.replay
                    effectiveIsPlaying -> R.drawable.pause
                    else -> R.drawable.play
                }
            ),
            contentDescription = null,
            tint = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color.White else Color.Black,
        )
    }
}

@Composable
private fun LegacyMiniMediaInfo(
    mediaMetadata: MediaMetadata,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }
    val cropAlbumArt by rememberPreference(CropAlbumArtKey, true)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .padding(6.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(ThumbnailCornerRadius))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            val thumbnailUrl = remember(mediaMetadata.thumbnailUrl, mediaMetadata.id) {
                resolveArtworkUrl(mediaMetadata.thumbnailUrl?.resize(144, 144), mediaMetadata.id)
            }
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            androidx.compose.animation.AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = if (pureBlack) Color.Black else Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        ) {
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            Text(
                text = mediaMetadata.title,
                color = if (isDark) Color.White else Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )

            if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                Text(
                    text = mediaMetadata.artists.joinToString { it.name },
                    color = if (isDark) Color(0xFFCCCCCC) else Color(0xFF333333),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// ISOLATED BUTTON COMPOSABLES - Prevent parent recomposition
// ============================================================================

@Composable
private fun SubscribeButton(
    artistId: String,
    metadata: MediaMetadata
) {
    val database = LocalDatabase.current
    val libraryArtist by database.artist(artistId).collectAsState(initial = null)
    val isSubscribed = libraryArtist?.artist?.bookmarkedAt != null
    
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val primaryColor = if (isDark) Color.White else Color.Black
    val onSurfaceColor = if (isDark) Color.White else Color.Black
    val iconTint = com.nanzbeatles.nanas.ui.utils.editorialPillContentColor(
        isSelected = isSubscribed,
        defaultColor = if (isSubscribed) primaryColor else onSurfaceColor.copy(alpha = 0.8f),
        isDark = isDark
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .crystalLiquidGlassPill(isSelected = isSubscribed, shape = CircleShape)
            .clickable {
                database.transaction {
                    val artist = libraryArtist?.artist
                    if (artist != null) {
                        update(artist.toggleLike())
                    } else {
                        metadata.artists.firstOrNull()?.let { artistInfo ->
                            insert(
                                ArtistEntity(
                                    id = artistInfo.id ?: "",
                                    name = artistInfo.name,
                                    channelId = null,
                                    thumbnailUrl = null,
                                ).toggleLike()
                            )
                        }
                    }
                }
            }
    ) {
        Icon(
            painter = painterResource(if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun NewMiniPlayerPlayPauseButton(
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    listenTogetherManager: ListenTogetherManager?,
    primaryColor: Color,
    outlineColor: Color,
    onSurfaceColor: Color
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsState()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val (iconRes, contentDesc) = when {
        isListenTogetherGuest -> if (isMuted) R.drawable.volume_off to "Unmute" else R.drawable.volume_up to "Mute"
        playbackState == Player.STATE_ENDED -> R.drawable.replay to "Replay"
        effectiveIsPlaying -> R.drawable.pause to "Pause"
        else -> R.drawable.play to "Play"
    }

    val iconTint = com.nanzbeatles.nanas.ui.utils.editorialPillContentColor(
        isSelected = effectiveIsPlaying,
        defaultColor = onSurfaceColor,
        isDark = isDark
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .crystalLiquidGlassPill(isSelected = effectiveIsPlaying, shape = CircleShape)
            .clickable {
                if (isListenTogetherGuest) {
                    playerConnection.toggleMute()
                    return@clickable
                }
                if (isCasting) {
                    if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                } else if (playbackState == Player.STATE_ENDED) {
                    playerConnection.player.seekTo(0, 0)
                    playerConnection.player.playWhenReady = true
                } else {
                    playerConnection.togglePlayPause()
                }
            }
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDesc,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FavoriteButton(songId: String) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(songId).collectAsState(initial = null)
    val isLiked = librarySong?.song?.liked == true

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL"
    val errorColor = if (isEditorial) Color(0xFFE03E2D) else MaterialTheme.colorScheme.error
    val onSurfaceColor = if (isDark) Color.White else Color.Black
    val unlikedTint = com.nanzbeatles.nanas.ui.utils.editorialPillContentColor(
        isSelected = false,
        defaultColor = onSurfaceColor.copy(alpha = 0.8f),
        isDark = isDark
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .crystalLiquidGlassPill(isSelected = isLiked, shape = CircleShape)
            .clickable { playerConnection.service.toggleLike() }
    ) {
        Icon(
            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
            contentDescription = null,
            tint = if (isLiked) errorColor else unlikedTint,
            modifier = Modifier.size(20.dp)
        )
    }
}
