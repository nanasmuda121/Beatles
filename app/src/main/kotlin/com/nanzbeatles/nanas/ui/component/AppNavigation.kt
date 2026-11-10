/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.nanzbeatles.nanas.constants.LiquidGlassApplyNavBarKey
import com.nanzbeatles.nanas.constants.LiquidGlassEffectKey
import com.nanzbeatles.nanas.ui.screens.Screens
import com.nanzbeatles.nanas.ui.utils.crystalLiquidGlassSurface
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlin.math.abs

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (screenRoute == Screens.Search.route &&
        (currentRoute.startsWith("search/") || currentRoute == "search_input")
    ) {
        return true
    }
    return navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")
}

/**
 * Shared state that exposes the real-time finger drag progress inside the 3D Liquid Glass Navbar
 * so the main screen viewport (`NavHost`) can smoothly follow the user's finger as it slides
 * across Beranda (0), Cari (1), and Pustaka (2).
 */
@Stable
class NavDragTransitionState(
    initialSlot: Int = 0
) {
    var isDragging by mutableStateOf(false)
        internal set
    var fractionalIndex by mutableFloatStateOf(initialSlot.toFloat())
        internal set
    var activeSlotIndex by mutableIntStateOf(initialSlot)
        internal set
    var velocityNorm by mutableFloatStateOf(0f)
        internal set
}

@Composable
fun rememberNavDragTransitionState(initialSlot: Int = 0): NavDragTransitionState {
    return remember { NavDragTransitionState(initialSlot) }
}


/**
 * Custom minimalist bookshelf/records icon (`|||\`) matching the Library ("Pustaka") icon in the video.
 */
@Composable
fun LibraryBookshelfIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.105f
        val topY = h * 0.16f
        val bottomY = h * 0.84f

        val x1 = w * 0.18f
        val x2 = w * 0.36f
        val x3 = w * 0.54f
        drawLine(
            color = tint,
            start = Offset(x1, topY),
            end = Offset(x1, bottomY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(x2, topY),
            end = Offset(x2, bottomY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(x3, topY),
            end = Offset(x3, bottomY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.70f, topY + h * 0.03f),
            end = Offset(w * 0.86f, bottomY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    NavigationRail(
        modifier = modifier
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .crystalLiquidGlassSurface(
                shape = RoundedCornerShape(32.dp),
                isHighlighted = true
            ),
        containerColor = Color.Transparent
    ) {
        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            NavigationRailItem(
                selected = isSelected,
                onClick = { onItemClick(screen, currentRoute == screen.route) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = if (isDark) Color.White else Color.Black,
                    selectedTextColor = if (isDark) Color.White else Color.Black,
                    unselectedIconColor = if (isDark) Color(0xFF9296A2) else Color(0xFF5A5E6B),
                    unselectedTextColor = if (isDark) Color(0xFF9296A2) else Color(0xFF5A5E6B),
                    indicatorColor = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)
                ),
                icon = {
                    if (screen == Screens.Library) {
                        LibraryBookshelfIcon(
                            tint = if (isSelected) {
                                if (isDark) Color.White else Color.Black
                            } else {
                                if (isDark) Color(0xFF9296A2) else Color(0xFF5A5E6B)
                            }
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = stringResource(screen.titleId)
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(screen.titleId),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

/**
 * 100% Video-Accurate Crystal-Clear ("Bening") 3D Liquid Glass Navigation Bar:
 * - Translucent dark glass capsule bar (`CircleShape`)
 * - Vertical icon + label layout for each tab (Beranda, Cari, Pustaka)
 * - At rest (`exp = 0f`): Sleek dark-glass inner pill with subtle Cyan-left and Orange-right chromatic rim
 * - On touch / press / drag / tab switch (`exp -> 1f`): Swells into a tall, protruding
 *   Crystal-Clear ("Bening") 3D Water-Lens Bubble (`barHeight + 28.dp`) that extends `14.dp` above
 *   and `14.dp` below the navbar capsule, clearing the dark bar fill inside the lens with
 *   `BlendMode.DstOut` so it is truly transparent ("bening"), refracting the navbar's top & bottom
 *   glass edges inside the bubble, and rendering sharp Cyan-left (`#00E5FF`) & Orange-right (`#FF4800`)
 *   chromatic crescents along the curved ends.
 * - All lens animations run in the GPU Draw phase (`drawWithContent` + `graphicsLayer`) for locked 120fps.
 */
@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    dragTransitionState: NavDragTransitionState? = null,
) {
    val liquidGlassEnabled by rememberPreference(LiquidGlassEffectKey, defaultValue = true)
    val liquidGlassApplyNavBar by rememberPreference(LiquidGlassApplyNavBarKey, defaultValue = true)
    val isEditorial = com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL"
    val isGlass = !isEditorial && liquidGlassEnabled && liquidGlassApplyNavBar
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val haptic = LocalHapticFeedback.current

    val itemCount = navigationItems.size.coerceAtLeast(1)
    val selectedIndex = remember(currentRoute, navigationItems) {
        navigationItems.indexOfFirst { screen ->
            isRouteSelected(currentRoute, screen.route, navigationItems)
        }.let { if (it == -1) 0 else it }
    }

    // Always keep updated references for pointerInput so clicks NEVER read a stale route or callback
    val currentRouteState by rememberUpdatedState(currentRoute)
    val onItemClickState by rememberUpdatedState(onItemClick)
    val navItemsState by rememberUpdatedState(navigationItems)
    val dragStateUpdated by rememberUpdatedState(dragTransitionState)

    var isInteracting by remember { mutableStateOf(false) }
    var isTapMorphing by remember { mutableStateOf(false) }
    var morphTriggerToken by remember { mutableIntStateOf(0) }
    var dragFractionalIndex by remember { mutableFloatStateOf(selectedIndex.toFloat()) }
    var rawVelocityNorm by remember { mutableFloatStateOf(0f) }

    // Sync dragFractionalIndex when route changes externally (e.g. system back button)
    LaunchedEffect(selectedIndex) {
        if (!isInteracting) {
            if (abs(dragFractionalIndex - selectedIndex.toFloat()) > 0.02f) {
                dragFractionalIndex = selectedIndex.toFloat()
                morphTriggerToken++
            } else {
                dragFractionalIndex = selectedIndex.toFloat()
            }
            dragStateUpdated?.let { state ->
                state.activeSlotIndex = selectedIndex
                state.fractionalIndex = selectedIndex.toFloat()
            }
        }
    }

    // Keep the 3D water-lens bubble inflated for 380ms on every tap or tab transition
    // so quick taps visibly swell into the full crystal-clear 3D lens just like the video
    LaunchedEffect(morphTriggerToken, isInteracting) {
        if (isInteracting) {
            isTapMorphing = true
        } else if (morphTriggerToken > 0) {
            isTapMorphing = true
            delay(380L)
            isTapMorphing = false
        }
    }

    val targetFractionalIndex = if (isInteracting) {
        dragFractionalIndex.coerceIn(0f, (itemCount - 1).toFloat())
    } else {
        selectedIndex.toFloat()
    }

    val animatedIndex by animateFloatAsState(
        targetValue = targetFractionalIndex,
        animationSpec = if (isInteracting) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        } else {
            spring(
                dampingRatio = 0.74f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "navLensIndex"
    )

    val animatedVelocityNorm by animateFloatAsState(
        targetValue = if (isInteracting) rawVelocityNorm.coerceIn(-1f, 1f) else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navLensVelocityStretch"
    )

    val lensExpanded = isInteracting || isTapMorphing
    val lensExpansionProgress by animateFloatAsState(
        targetValue = if (lensExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lensExpansionProgress"
    )

    val barHeight = if (slimNav) 54.dp else 60.dp

    // Chromatic dispersion colors matching the reference video 100%
    val cyanPrism = Color(0xFF00E5FF)
    val deepBluePrism = Color(0xFF0088FF)
    val orangePrism = Color(0xFFFF4800)
    val amberPrism = Color(0xFFFF8800)

    val lensClipPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
            .graphicsLayer { clip = false }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .height(barHeight)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    clip = false
                }
                .pointerInput(itemCount) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val widthPx = size.width.toFloat().coerceAtLeast(1f)
                        val slotWidthPx = widthPx / itemCount

                        val downX = down.position.x
                        val tappedSlot = (downX / slotWidthPx).toInt().coerceIn(0, itemCount - 1)

                        isInteracting = true
                        morphTriggerToken++
                        rawVelocityNorm = 0f
                        // Snap cleanly to the tapped slot center until horizontal drag exceeds touchSlop
                        dragFractionalIndex = tappedSlot.toFloat()
                        var lastHapticSlot = tappedSlot
                        var lastNavigatedSlot = navItemsState.indexOfFirst { screen ->
                            isRouteSelected(currentRouteState, screen.route, navItemsState)
                        }.let { if (it == -1) tappedSlot else it }

                        dragStateUpdated?.let { state ->
                            state.isDragging = true
                            state.activeSlotIndex = lastNavigatedSlot
                            state.fractionalIndex = tappedSlot.toFloat()
                            state.velocityNorm = 0f
                        }

                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        var hasDragged = false
                        var lastX = downX
                        var prevEventX = downX
                        var pointerId = down.id

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                    ?: event.changes.firstOrNull()
                                if (change == null || !change.pressed) {
                                    break
                                }
                                pointerId = change.id
                                lastX = change.position.x
                                val deltaX = lastX - prevEventX
                                prevEventX = lastX

                                if (!hasDragged && abs(lastX - downX) > touchSlop * 0.55f) {
                                    hasDragged = true
                                    // If user started dragging from a different slot than the currently active tab,
                                    // switch immediately so the screen follows the finger right from the start
                                    if (tappedSlot != lastNavigatedSlot && tappedSlot in navItemsState.indices) {
                                        lastNavigatedSlot = tappedSlot
                                        onItemClickState(navItemsState[tappedSlot], false)
                                    }
                                }

                                if (hasDragged) {
                                    val newFrac = ((lastX - slotWidthPx / 2f) / slotWidthPx)
                                        .coerceIn(0f, (itemCount - 1).toFloat())
                                    dragFractionalIndex = newFrac

                                    // Compute normalized horizontal velocity for elastic water-droplet stretch
                                    val instVel = (deltaX / (slotWidthPx * 0.22f)).coerceIn(-1f, 1f)
                                    rawVelocityNorm = rawVelocityNorm * 0.55f + instVel * 0.45f

                                    val currentSlot = (lastX / slotWidthPx).toInt().coerceIn(0, itemCount - 1)
                                    if (currentSlot != lastHapticSlot) {
                                        lastHapticSlot = currentSlot
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }

                                    // Live mid-drag tab switching: transition screen immediately as finger crosses slots
                                    if (currentSlot != lastNavigatedSlot && currentSlot in navItemsState.indices) {
                                        lastNavigatedSlot = currentSlot
                                        onItemClickState(navItemsState[currentSlot], false)
                                    }

                                    dragStateUpdated?.let { state ->
                                        state.isDragging = true
                                        state.activeSlotIndex = lastNavigatedSlot
                                        state.fractionalIndex = newFrac
                                        state.velocityNorm = rawVelocityNorm
                                    }
                                }
                                change.consume()
                            }
                        } finally {
                            isInteracting = false
                            rawVelocityNorm = 0f
                            dragStateUpdated?.let { state ->
                                state.isDragging = false
                                state.velocityNorm = 0f
                            }
                        }

                        val finalIdx = if (hasDragged) {
                            (lastX / slotWidthPx).toInt().coerceIn(0, itemCount - 1)
                        } else {
                            tappedSlot
                        }
                        dragFractionalIndex = finalIdx.toFloat()

                        dragStateUpdated?.let { state ->
                            state.activeSlotIndex = finalIdx
                            state.fractionalIndex = finalIdx.toFloat()
                        }

                        val items = navItemsState
                        if (finalIdx in items.indices) {
                            val targetScreen = items[finalIdx]
                            if (!hasDragged) {
                                val wasSelected = currentRouteState == targetScreen.route
                                onItemClickState(targetScreen, wasSelected)
                            } else if (finalIdx != lastNavigatedSlot) {
                                onItemClickState(targetScreen, false)
                            }
                        }
                    }
                }
                .drawWithContent {
                    val barW = size.width
                    val barH = size.height
                    val horizontalPadPx = 5.dp.toPx()
                    val usableW = (barW - horizontalPadPx * 2f).coerceAtLeast(1f)
                    val slotW = usableW / itemCount

                    if (isEditorial) {
                        // =========================================================================
                        // EDITORIAL ARCHITECTURAL DOCK (Swiss Magazine / Vinyl Print Style)
                        // =========================================================================
                        val editorialCorner = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                        val editorialBarBg = if (isDark) Color(0xFF101014) else Color(0xFFF6F3EB)
                        val editorialBorder = if (isDark) Color(0xFFF5F2EB).copy(alpha = 0.28f) else Color(0xFF111111).copy(alpha = 0.24f)

                        drawRoundRect(
                            color = editorialBarBg,
                            topLeft = Offset.Zero,
                            size = Size(barW, barH),
                            cornerRadius = editorialCorner
                        )

                        drawRoundRect(
                            color = editorialBorder,
                            topLeft = Offset.Zero,
                            size = Size(barW, barH),
                            cornerRadius = editorialCorner,
                            style = Stroke(width = 1.2.dp.toPx())
                        )

                        val pillW = (slotW - 6.dp.toPx()).coerceAtLeast(60.dp.toPx())
                        val pillH = (barH - 10.dp.toPx()).coerceAtLeast(42.dp.toPx())
                        val slotCenterX = horizontalPadPx + slotW * (animatedIndex + 0.5f)
                        val pillLeft = (slotCenterX - pillW / 2f).coerceIn(horizontalPadPx, barW - pillW - horizontalPadPx)
                        val pillTop = (barH - pillH) / 2f
                        val pillCr = CornerRadius(8.dp.toPx(), 8.dp.toPx())

                        val activeBlockColor = if (isDark) Color(0xFFF5F2EB) else Color(0xFF111111)
                        drawRoundRect(
                            color = activeBlockColor,
                            topLeft = Offset(pillLeft, pillTop),
                            size = Size(pillW, pillH),
                            cornerRadius = pillCr
                        )

                        drawContent()
                    } else {
                        val barCorner = CornerRadius(barH / 2f, barH / 2f)
                        val exp = lensExpansionProgress.coerceIn(0f, 1f)
                        val speedMag = abs(animatedVelocityNorm).coerceIn(0f, 1f)

                        // Bubble dimensions with surface-tension velocity stretch:
                        val restW = (slotW - 8.dp.toPx()).coerceAtLeast(62.dp.toPx())
                        val expandedW = (slotW + 16.dp.toPx()).coerceAtMost(usableW * 0.52f)
                        val velocityStretchW = 14.dp.toPx() * speedMag * exp
                        val bubbleW = restW + (expandedW - restW) * exp + velocityStretchW

                        val restH = (barH - 12.dp.toPx()).coerceAtLeast(42.dp.toPx())
                        val expandedH = barH + 24.dp.toPx()
                        val velocityCompressH = 4.dp.toPx() * speedMag * exp
                        val bubbleH = (restH + (expandedH - restH) * exp - velocityCompressH).coerceAtLeast(restH)

                        val slotCenterX = horizontalPadPx + slotW * (animatedIndex + 0.5f)
                        val bubbleLeft = (slotCenterX - bubbleW / 2f)
                            .coerceIn(-4.dp.toPx() * exp, barW - bubbleW + 4.dp.toPx() * exp)
                        val bubbleTop = (barH - bubbleH) / 2f
                        val bubbleRadius = bubbleH / 2f
                        val bubbleCr = CornerRadius(bubbleRadius, bubbleRadius)

                        val iceCyan = Color(0xFF7DD3FC)
                        val skyCaustic = Color(0xFF38BDF8)
                        val violetCaustic = Color(0xFFC4B5FD)
                        val warmAmberCaustic = Color(0xFFFDBA74)

                        // =========================================================================
                        // 1. ULTRA-CLEAR ("BENING") FLUID GLASS CAPSULE BAR (React Bits Style)
                        // =========================================================================
                        // Center is nearly 100% see-through ("bening") so background content shines right through
                        val barBgColor = if (isDark) {
                            Color(0xFF0A101D).copy(alpha = if (isGlass) 0.06f else 0.90f)
                        } else {
                            Color.White.copy(alpha = if (isGlass) 0.10f else 0.92f)
                        }
                        drawRoundRect(
                            color = barBgColor,
                            topLeft = Offset.Zero,
                            size = Size(barW, barH),
                            cornerRadius = barCorner
                        )

                        // Layer A: Thick Curved 3D Glass Torus / Meniscus Wall Depth Band (inset 2.8.dp)
                        val bevelInset = 2.8.dp.toPx()
                        val bevelStroke = 5.2.dp.toPx()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to if (isDark) Color(0xFF020612).copy(alpha = 0.28f) else Color(0xFF334155).copy(alpha = 0.12f),
                                    0.25f to if (isDark) Color(0xFF0F172A).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.25f),
                                    0.50f to if (isDark) Color(0xFF020612).copy(alpha = 0.22f) else Color(0xFF475569).copy(alpha = 0.10f),
                                    0.75f to if (isDark) Color(0xFF0F172A).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.25f),
                                    1.00f to if (isDark) Color(0xFF020612).copy(alpha = 0.30f) else Color(0xFF334155).copy(alpha = 0.14f)
                                )
                            ),
                            topLeft = Offset(bevelInset, bevelInset),
                            size = Size((barW - bevelInset * 2f).coerceAtLeast(1f), (barH - bevelInset * 2f).coerceAtLeast(1f)),
                            cornerRadius = CornerRadius((barH / 2f - bevelInset).coerceAtLeast(1f), (barH / 2f - bevelInset).coerceAtLeast(1f)),
                            style = Stroke(width = bevelStroke)
                        )

                        // Layer B: Prismatic Caustic Meniscus Wave inside the curved glass wall (inset 4.2.dp)
                        val causticInset = 4.2.dp.toPx()
                        val causticStroke = 3.4.dp.toPx()
                        val causticShift = (animatedIndex / (itemCount - 1).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.00f to iceCyan.copy(alpha = if (isDark) 0.28f else 0.22f),
                                    0.18f to Color.White.copy(alpha = if (isDark) 0.18f else 0.45f),
                                    (0.32f + causticShift * 0.18f) to skyCaustic.copy(alpha = if (isDark) 0.32f else 0.24f),
                                    (0.52f + causticShift * 0.14f) to warmAmberCaustic.copy(alpha = if (isDark) 0.22f else 0.18f),
                                    0.78f to violetCaustic.copy(alpha = if (isDark) 0.24f else 0.18f),
                                    1.00f to iceCyan.copy(alpha = if (isDark) 0.30f else 0.24f)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(barW, barH)
                            ),
                            topLeft = Offset(causticInset, causticInset),
                            size = Size((barW - causticInset * 2f).coerceAtLeast(1f), (barH - causticInset * 2f).coerceAtLeast(1f)),
                            cornerRadius = CornerRadius((barH / 2f - causticInset).coerceAtLeast(1f), (barH / 2f - causticInset).coerceAtLeast(1f)),
                            style = Stroke(width = causticStroke)
                        )

                        // Layer C: Signature Inner Concentric Meniscus Ridge (inset 7.2.dp — the double-ring lens look)
                        val innerRidgeInset = 7.2.dp.toPx()
                        val innerRidgeStroke = 1.7.dp.toPx()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.White.copy(alpha = if (isDark) 0.28f else 0.55f),
                                    0.20f to iceCyan.copy(alpha = if (isDark) 0.18f else 0.20f),
                                    0.50f to Color.White.copy(alpha = if (isDark) 0.10f else 0.25f),
                                    0.80f to skyCaustic.copy(alpha = if (isDark) 0.18f else 0.20f),
                                    1.00f to Color.White.copy(alpha = if (isDark) 0.25f else 0.50f)
                                )
                            ),
                            topLeft = Offset(innerRidgeInset, innerRidgeInset),
                            size = Size((barW - innerRidgeInset * 2f).coerceAtLeast(1f), (barH - innerRidgeInset * 2f).coerceAtLeast(1f)),
                            cornerRadius = CornerRadius((barH / 2f - innerRidgeInset).coerceAtLeast(1f), (barH / 2f - innerRidgeInset).coerceAtLeast(1f)),
                            style = Stroke(width = innerRidgeStroke)
                        )

                        // Layer D: Second Inner Whisper Caustic Ring (inset 10.4.dp) fading into the clear center
                        val whisperInset = 10.4.dp.toPx()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDark) 0.12f else 0.28f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (isDark) 0.10f else 0.24f)
                                )
                            ),
                            topLeft = Offset(whisperInset, whisperInset),
                            size = Size((barW - whisperInset * 2f).coerceAtLeast(1f), (barH - whisperInset * 2f).coerceAtLeast(1f)),
                            cornerRadius = CornerRadius((barH / 2f - whisperInset).coerceAtLeast(1f), (barH / 2f - whisperInset).coerceAtLeast(1f)),
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Layer E: Outer Crisp Specular Glass Border around the capsule perimeter
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to if (isDark) Color.White.copy(alpha = 0.52f) else Color.White.copy(alpha = 0.88f),
                                    0.35f to if (isDark) Color.White.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.12f),
                                    0.65f to if (isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.10f),
                                    1.00f to if (isDark) Color.White.copy(alpha = 0.44f) else Color.White.copy(alpha = 0.78f)
                                )
                            ),
                            topLeft = Offset.Zero,
                            size = Size(barW, barH),
                            cornerRadius = barCorner,
                            style = Stroke(width = 1.25.dp.toPx())
                        )

                        // Top & Bottom Curved Meniscus Specular Glints on the Base Capsule
                        val baseGlintInset = (barH * 0.42f).coerceAtLeast(18.dp.toPx())
                        if (barW > baseGlintInset * 2f) {
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Transparent,
                                        0.20f to Color.White.copy(alpha = if (isDark) 0.42f else 0.75f),
                                        0.55f to iceCyan.copy(alpha = if (isDark) 0.38f else 0.45f),
                                        0.80f to Color.White.copy(alpha = if (isDark) 0.46f else 0.75f),
                                        1.00f to Color.Transparent
                                    ),
                                    startX = baseGlintInset,
                                    endX = barW - baseGlintInset
                                ),
                                start = Offset(baseGlintInset, 1.4.dp.toPx()),
                                end = Offset(barW - baseGlintInset, 1.4.dp.toPx()),
                                strokeWidth = 1.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colorStops = arrayOf(
                                        0.00f to Color.Transparent,
                                        0.25f to iceCyan.copy(alpha = if (isDark) 0.30f else 0.35f),
                                        0.50f to Color.White.copy(alpha = if (isDark) 0.38f else 0.68f),
                                        0.75f to warmAmberCaustic.copy(alpha = if (isDark) 0.26f else 0.30f),
                                        1.00f to Color.Transparent
                                    ),
                                    startX = baseGlintInset,
                                    endX = barW - baseGlintInset
                                ),
                                start = Offset(baseGlintInset, barH - 1.4.dp.toPx()),
                                end = Offset(barW - baseGlintInset, barH - 1.4.dp.toPx()),
                                strokeWidth = 1.1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // =========================================================================
                        // 2. CRYSTAL-CLEAR ("BENING") 3D ACTIVE FLUID LENS DROPLET
                        // =========================================================================
                        // Clear out any base meniscus lines inside the active lens when expanded
                        if (exp > 0.01f && isGlass) {
                            drawRoundRect(
                                color = Color.Black.copy(alpha = 0.75f * exp),
                                topLeft = Offset(bubbleLeft, bubbleTop),
                                size = Size(bubbleW, bubbleH),
                                cornerRadius = bubbleCr,
                                blendMode = BlendMode.DstOut
                            )
                        }

                        // Ultra-transparent ("bening") droplet fill both at rest and when expanded!
                        val bubbleFillColor = if (isDark) {
                            Color.White.copy(alpha = 0.04f + 0.03f * exp)
                        } else {
                            Color.White.copy(alpha = 0.14f + 0.06f * exp)
                        }
                        drawRoundRect(
                            color = bubbleFillColor,
                            topLeft = Offset(bubbleLeft, bubbleTop),
                            size = Size(bubbleW, bubbleH),
                            cornerRadius = bubbleCr
                        )

                        // Inner curved meniscus ring inside the active fluid droplet
                        val dropletInnerInset = 3.2.dp.toPx()
                        val dropletInnerStroke = (2.2f + 1.4f * exp).dp.toPx()
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0.00f to cyanPrism.copy(alpha = 0.28f + 0.26f * exp),
                                    0.25f to Color.White.copy(alpha = 0.16f + 0.18f * exp),
                                    0.50f to Color.Transparent,
                                    0.75f to Color.White.copy(alpha = 0.16f + 0.18f * exp),
                                    1.00f to orangePrism.copy(alpha = 0.26f + 0.26f * exp)
                                ),
                                start = Offset(bubbleLeft, bubbleTop),
                                end = Offset(bubbleLeft + bubbleW, bubbleTop + bubbleH)
                            ),
                            topLeft = Offset(bubbleLeft + dropletInnerInset, bubbleTop + dropletInnerInset),
                            size = Size(
                                (bubbleW - dropletInnerInset * 2f).coerceAtLeast(1f),
                                (bubbleH - dropletInnerInset * 2f).coerceAtLeast(1f)
                            ),
                            cornerRadius = CornerRadius(
                                (bubbleRadius - dropletInnerInset).coerceAtLeast(1f),
                                (bubbleRadius - dropletInnerInset).coerceAtLeast(1f)
                            ),
                            style = Stroke(width = dropletInnerStroke)
                        )

                        if (exp > 0.06f) {
                            lensClipPath.reset()
                            lensClipPath.addRoundRect(
                                RoundRect(
                                    left = bubbleLeft,
                                    top = bubbleTop,
                                    right = bubbleLeft + bubbleW,
                                    bottom = bubbleTop + bubbleH,
                                    cornerRadius = bubbleCr
                                )
                            )
                            clipPath(lensClipPath) {
                                val refractInset = 2.5.dp.toPx() * exp
                                val refractedTopY = refractInset
                                val refractedBottomY = barH - refractInset

                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colorStops = arrayOf(
                                            0.0f to cyanPrism.copy(alpha = 0.42f * exp),
                                            0.25f to Color.White.copy(alpha = 0.28f * exp),
                                            0.50f to Color.White.copy(alpha = 0.16f * exp),
                                            0.75f to Color.White.copy(alpha = 0.28f * exp),
                                            1.0f to orangePrism.copy(alpha = 0.42f * exp)
                                        ),
                                        startX = bubbleLeft,
                                        endX = bubbleLeft + bubbleW
                                    ),
                                    start = Offset(bubbleLeft + 2.dp.toPx(), refractedTopY),
                                    end = Offset(bubbleLeft + bubbleW - 2.dp.toPx(), refractedTopY),
                                    strokeWidth = 1.1.dp.toPx()
                                )

                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colorStops = arrayOf(
                                            0.0f to cyanPrism.copy(alpha = 0.36f * exp),
                                            0.25f to Color.White.copy(alpha = 0.22f * exp),
                                            0.50f to Color.White.copy(alpha = 0.12f * exp),
                                            0.75f to Color.White.copy(alpha = 0.22f * exp),
                                            1.0f to orangePrism.copy(alpha = 0.36f * exp)
                                        ),
                                        startX = bubbleLeft,
                                        endX = bubbleLeft + bubbleW
                                    ),
                                    start = Offset(bubbleLeft + 2.dp.toPx(), refractedBottomY),
                                    end = Offset(bubbleLeft + bubbleW - 2.dp.toPx(), refractedBottomY),
                                    strokeWidth = 1.1.dp.toPx()
                                )
                            }
                        }

                        // =========================================================================
                        // 3. FOREGROUND TAB ICONS & LABELS
                        // =========================================================================
                        drawContent()

                        // =========================================================================
                        // 4. FRONT 3D LIQUID LENS SPECULAR DOME & CAUSTIC CRESCENT RIMS
                        // =========================================================================
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.White.copy(alpha = 0.14f + 0.16f * exp),
                                    0.22f to Color.White.copy(alpha = 0.02f + 0.03f * exp),
                                    0.50f to Color.Transparent,
                                    0.82f to Color.White.copy(alpha = 0.02f + 0.03f * exp),
                                    1.00f to Color.White.copy(alpha = 0.08f + 0.12f * exp)
                                ),
                                startY = bubbleTop,
                                endY = bubbleTop + bubbleH
                            ),
                            topLeft = Offset(bubbleLeft, bubbleTop),
                            size = Size(bubbleW, bubbleH),
                            cornerRadius = bubbleCr
                        )

                        val midRimColor = if (isDark) Color.White else Color(0xFF475569)
                        val outerStrokeW = (1.15f + 0.95f * exp).dp.toPx()
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.00f to cyanPrism.copy(alpha = 0.55f + 0.38f * exp),
                                    0.16f to iceCyan.copy(alpha = 0.45f + 0.35f * exp),
                                    0.32f to midRimColor.copy(alpha = 0.42f + 0.38f * exp),
                                    0.50f to midRimColor.copy(alpha = 0.52f + 0.38f * exp),
                                    0.68f to midRimColor.copy(alpha = 0.42f + 0.38f * exp),
                                    0.84f to warmAmberCaustic.copy(alpha = 0.45f + 0.35f * exp),
                                    1.00f to orangePrism.copy(alpha = 0.55f + 0.38f * exp)
                                ),
                                startX = bubbleLeft,
                                endX = bubbleLeft + bubbleW
                            ),
                            topLeft = Offset(bubbleLeft, bubbleTop),
                            size = Size(bubbleW, bubbleH),
                            cornerRadius = bubbleCr,
                            style = Stroke(width = outerStrokeW)
                        )

                        val topGlintW = bubbleW * 0.56f
                        val topGlintStartX = bubbleLeft + (bubbleW - topGlintW) / 2f
                        val topGlintY = bubbleTop + 1.8.dp.toPx()
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.52f + 0.38f * exp),
                                    Color.Transparent
                                ),
                                startX = topGlintStartX,
                                endX = topGlintStartX + topGlintW
                            ),
                            start = Offset(topGlintStartX, topGlintY),
                            end = Offset(topGlintStartX + topGlintW, topGlintY),
                            strokeWidth = (1.1f + 0.6f * exp).dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        val botGlintW = bubbleW * 0.48f
                        val botGlintStartX = bubbleLeft + (bubbleW - botGlintW) / 2f
                        val botGlintY = bubbleTop + bubbleH - 1.8.dp.toPx()
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    iceCyan.copy(alpha = 0.28f + 0.32f * exp),
                                    Color.White.copy(alpha = 0.42f + 0.38f * exp),
                                    warmAmberCaustic.copy(alpha = 0.28f + 0.32f * exp),
                                    Color.Transparent
                                ),
                                startX = botGlintStartX,
                                endX = botGlintStartX + botGlintW
                            ),
                            start = Offset(botGlintStartX, botGlintY),
                            end = Offset(botGlintStartX + botGlintW, botGlintY),
                            strokeWidth = (1.0f + 0.5f * exp).dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val horizontalInnerPad = 5.dp

            // Foreground Tab Items Row (Icon on top + Label text below)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalInnerPad),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navigationItems.forEachIndexed { index, screen ->
                    val distanceFromBubble = abs(animatedIndex - index.toFloat())
                    val isHoveredOrSelected = distanceFromBubble < 0.5f
                    val iconRes = remember(isHoveredOrSelected, screen) {
                        if (isHoveredOrSelected) screen.iconIdActive else screen.iconIdInactive
                    }

                    val contentColor by animateColorAsState(
                        targetValue = if (isEditorial) {
                            if (isHoveredOrSelected) {
                                if (isDark) Color(0xFF0A0A0D) else Color(0xFFFBF9F5)
                            } else {
                                if (isDark) Color(0xFFA8A49E) else Color(0xFF52504A)
                            }
                        } else {
                            if (isHoveredOrSelected) {
                                if (isDark) Color.White else Color(0xFF0A0D14)
                            } else {
                                if (isDark) Color.White.copy(alpha = 0.78f) else Color(0xFF334155)
                            }
                        },
                        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                        label = "navItemColor_$index"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                val dist = abs(animatedIndex - index.toFloat()).coerceIn(0f, 1f)
                                val proximity = 1f - dist
                                val scale = 1f + proximity * (0.04f + 0.11f * lensExpansionProgress)
                                scaleX = scale
                                scaleY = scale
                                translationY = -proximity * 2.5.dp.toPx() * lensExpansionProgress
                            }
                            .testTag("nav_item_${screen.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (screen == Screens.Library) {
                                LibraryBookshelfIcon(
                                    tint = contentColor,
                                    modifier = Modifier.size(if (slimNav) 20.dp else 22.dp)
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = stringResource(screen.titleId),
                                    tint = contentColor,
                                    modifier = Modifier.size(if (slimNav) 20.dp else 22.dp)
                                )
                            }

                            if (!slimNav) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = stringResource(screen.titleId),
                                    color = contentColor,
                                    fontSize = 11.sp,
                                    fontWeight = if (isHoveredOrSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}