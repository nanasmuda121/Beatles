/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nanzbeatles.nanas.constants.LiquidGlassBlurRadiusKey
import com.nanzbeatles.nanas.constants.LiquidGlassCornerRadiusKey
import com.nanzbeatles.nanas.constants.LiquidGlassEffectKey
import com.nanzbeatles.nanas.constants.LiquidGlassOpacityKey
import com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle
import com.nanzbeatles.nanas.utils.rememberPreference

/**
 * Lightweight Ambient Backdrop that adapts to both Liquid Glass V2
 * (deep obsidian with subtle sapphire/cyan liquid light depth)
 * and Editorial (Carbon Black #08080A / Warm Magazine Paper #FBF9F5).
 * Uses zero-cost static GPU gradients (no real-time blur passes).
 */
@Composable
fun GlassmorphicAmbientBackdrop(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    if (isEditorial) {
        val bgColor = if (isDark) Color(0xFF08080A) else Color(0xFFFBF9F5)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgColor)
        )
        return
    }

    val baseBrush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF070B12),
                Color(0xFF04060A),
                Color(0xFF060A10)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF2F6FC),
                Color(0xFFF7FAFF),
                Color(0xFFEEF3FA)
            )
        )
    }

    val topAmbientGlow = if (isDark) {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E90FF).copy(alpha = 0.11f),
                Color(0xFF00E5FF).copy(alpha = 0.04f),
                Color.Transparent
            ),
            radius = 1100f
        )
    } else {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF1E90FF).copy(alpha = 0.08f),
                Color(0xFF00E5FF).copy(alpha = 0.03f),
                Color.Transparent
            ),
            radius = 1100f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBrush)
            .background(topAmbientGlow)
    )
}

/**
 * Crystal-Clear ("Bening") 3D Liquid Glass V2 Surface Modifier (or crisp Editorial frame in EDITORIAL mode).
 */
@Composable
fun Modifier.crystalLiquidGlassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    isHighlighted: Boolean = false,
    baseTint: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"

    if (isEditorial) {
        val editorialShape = RoundedCornerShape(10.dp)
        val editorialBg = baseTint ?: if (isDark) {
            if (isHighlighted) Color(0xFF1A1A20) else Color(0xFF111115)
        } else {
            if (isHighlighted) Color(0xFFEDE8DF) else Color(0xFFF5F1E9)
        }
        val editorialBorder = if (isDark) {
            Color(0xFFF5F2EB).copy(alpha = if (isHighlighted) 0.45f else 0.18f)
        } else {
            Color(0xFF111111).copy(alpha = if (isHighlighted) 0.45f else 0.18f)
        }
        return this
            .clip(editorialShape)
            .background(editorialBg, editorialShape)
            .border(
                width = if (isHighlighted) 1.4.dp else 1.dp,
                color = editorialBorder,
                shape = editorialShape
            )
    }

    // Liquid Glass V2 smoked translucent glass base with crisp silver-blue rim
    val resolvedBaseTint = baseTint ?: if (isDark) {
        Color(0xFF111621).copy(alpha = if (isHighlighted) 0.82f else 0.72f)
    } else {
        Color(0xFFF4F7FC).copy(alpha = if (isHighlighted) 0.88f else 0.78f)
    }

    val clearSheenGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isHighlighted) 0.18f else 0.12f),
                Color(0xFF1E90FF).copy(alpha = if (isHighlighted) 0.08f else 0.04f),
                Color.Black.copy(alpha = 0.16f),
                Color.White.copy(alpha = if (isHighlighted) 0.09f else 0.05f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.92f),
                Color(0xFF1E90FF).copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.65f)
            )
        }
    )

    val specularBorder = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isHighlighted) 0.58f else 0.38f),
                Color(0xFF1E90FF).copy(alpha = if (isHighlighted) 0.36f else 0.20f),
                Color(0xFF00E5FF).copy(alpha = if (isHighlighted) 0.28f else 0.14f),
                Color.White.copy(alpha = if (isHighlighted) 0.38f else 0.24f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFF1E90FF).copy(alpha = if (isHighlighted) 0.28f else 0.16f),
                Color.Black.copy(alpha = if (isHighlighted) 0.14f else 0.08f)
            )
        }
    )

    val topGlintColor = if (isDark) {
        Color.White.copy(alpha = if (isHighlighted) 0.68f else 0.48f)
    } else {
        Color.White.copy(alpha = 0.96f)
    }

    return this
        .clip(shape)
        .background(resolvedBaseTint, shape)
        .background(clearSheenGradient, shape)
        .drawWithContent {
            val topDomeHeight = (size.height * 0.42f).coerceAtMost(28.dp.toPx())
            if (topDomeHeight > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.15f else 0.48f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = topDomeHeight
                    )
                )
            }

            drawContent()

            // Crisp top meniscus catchlight
            val glintWidth = size.width * 0.72f
            val startX = (size.width - glintWidth) / 2f
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        topGlintColor,
                        Color.Transparent
                    ),
                    startX = startX,
                    endX = startX + glintWidth
                ),
                start = Offset(startX, 1.dp.toPx()),
                end = Offset(startX + glintWidth, 1.dp.toPx()),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        .border(
            width = if (isHighlighted) 1.25.dp else 1.05.dp,
            brush = specularBorder,
            shape = shape
        )
}

/**
 * Crystal-Clear ("Bening") Liquid Glass V2 Pill Modifier (or Editorial badge in EDITORIAL mode).
 */
@Composable
fun Modifier.crystalLiquidGlassPill(
    isSelected: Boolean = false,
    shape: Shape = CircleShape,
    accentColor: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"

    if (isEditorial) {
        val editorialShape = RoundedCornerShape(8.dp)
        val bg = when {
            isSelected && isDark -> Color(0xFFF5F2EB)
            isSelected && !isDark -> Color(0xFF111111)
            isDark -> Color(0xFF16161B)
            else -> Color(0xFFEDE9E0)
        }
        val border = when {
            isSelected && isDark -> Color(0xFFF5F2EB)
            isSelected && !isDark -> Color(0xFF111111)
            isDark -> Color(0xFFF5F2EB).copy(alpha = 0.22f)
            else -> Color(0xFF111111).copy(alpha = 0.20f)
        }
        return this
            .clip(editorialShape)
            .background(bg, editorialShape)
            .border(width = 1.dp, color = border, shape = editorialShape)
    }

    val accent = accentColor ?: Color(0xFF1E90FF)

    val pillFillBrush = when {
        isSelected && isDark -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                accent.copy(alpha = 0.24f),
                Color(0xFF0E1522).copy(alpha = 0.85f)
            )
        )
        isSelected && !isDark -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                accent.copy(alpha = 0.18f),
                Color(0xFFE4EEFC).copy(alpha = 0.90f)
            )
        )
        isDark -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.13f),
                Color(0xFF131822).copy(alpha = 0.68f),
                Color.White.copy(alpha = 0.05f)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.88f),
                Color(0xFFEEF3FA).copy(alpha = 0.78f),
                Color.White.copy(alpha = 0.65f)
            )
        )
    }

    val rimBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isSelected) 0.72f else 0.36f),
                accent.copy(alpha = if (isSelected) 0.55f else 0.18f),
                Color(0xFF00E5FF).copy(alpha = if (isSelected) 0.38f else 0.12f),
                Color.White.copy(alpha = if (isSelected) 0.42f else 0.20f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.96f),
                accent.copy(alpha = if (isSelected) 0.45f else 0.18f),
                Color.Black.copy(alpha = if (isSelected) 0.16f else 0.08f)
            )
        }
    )

    return this
        .clip(shape)
        .background(pillFillBrush, shape)
        .border(width = if (isSelected) 1.25.dp else 0.95.dp, brush = rimBrush, shape = shape)
}

/**
 * Lightweight Liquid Glass V2 Card Surface for general app UI (Settings, Menus, Dialogs, Cards).
 * Uses zero-allocation GPU gradient fills + silver-blue specular borders (no expensive blur passes).
 * Adapts to Liquid Glass V2 (rounded 22dp translucent smoked glass) vs Editorial (sharp 10dp architectural frames).
 */
@Composable
fun Modifier.frostedGlassSurface(
    shape: Shape = RoundedCornerShape(22.dp),
    isHighlighted: Boolean = false,
    baseTint: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"

    if (isEditorial) {
        val editorialShape = RoundedCornerShape(10.dp)
        val bg = baseTint ?: if (isDark) {
            if (isHighlighted) Color(0xFF1D1D22) else Color(0xFF121216)
        } else {
            if (isHighlighted) Color(0xFFEAE5DC) else Color(0xFFF3EFE7)
        }
        val borderColor = if (isDark) {
            Color(0xFFF5F2EB).copy(alpha = if (isHighlighted) 0.35f else 0.16f)
        } else {
            Color(0xFF111111).copy(alpha = if (isHighlighted) 0.35f else 0.16f)
        }
        return this
            .clip(editorialShape)
            .background(bg, editorialShape)
            .border(width = 1.dp, color = borderColor, shape = editorialShape)
    }

    // Liquid Glass V2 smoked translucent glass surface (ultra-lightweight GPU gradient)
    val baseColor = baseTint ?: if (isDark) {
        if (isHighlighted) Color(0xFF151C28).copy(alpha = 0.84f) else Color(0xFF10141D).copy(alpha = 0.76f)
    } else {
        if (isHighlighted) Color(0xFFEAF0FA).copy(alpha = 0.90f) else Color(0xFFF4F7FC).copy(alpha = 0.84f)
    }

    val glassSheenBrush = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isHighlighted) 0.14f else 0.08f),
                Color(0xFF1E90FF).copy(alpha = if (isHighlighted) 0.06f else 0.02f),
                Color.Transparent,
                Color.White.copy(alpha = if (isHighlighted) 0.05f else 0.025f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.88f),
                Color(0xFF1E90FF).copy(alpha = if (isHighlighted) 0.05f else 0.02f),
                Color.White.copy(alpha = 0.25f),
                Color.White.copy(alpha = 0.55f)
            )
        }
    )

    val borderBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isHighlighted) 0.42f else 0.22f),
                Color(0xFF1E90FF).copy(alpha = if (isHighlighted) 0.32f else 0.12f),
                Color(0xFF00E5FF).copy(alpha = if (isHighlighted) 0.22f else 0.08f),
                Color.White.copy(alpha = if (isHighlighted) 0.24f else 0.12f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFF1E90FF).copy(alpha = if (isHighlighted) 0.24f else 0.12f),
                Color.Black.copy(alpha = if (isHighlighted) 0.12f else 0.07f)
            )
        }
    )

    return this
        .clip(shape)
        .background(baseColor, shape)
        .background(glassSheenBrush, shape)
        .border(width = if (isHighlighted) 1.2.dp else 1.dp, brush = borderBrush, shape = shape)
}

/**
 * Lightweight Liquid Glass V2 Pill Modifier for chips, buttons, and action badges across the app.
 */
@Composable
fun Modifier.liquidGlassPill(
    isSelected: Boolean = false,
    shape: Shape = CircleShape,
    accentColor: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    val effectiveShape = if (isEditorial) RoundedCornerShape(8.dp) else shape

    if (isEditorial) {
        val bg = when {
            isSelected && isDark -> Color(0xFFF5F2EB)
            isSelected && !isDark -> Color(0xFF111111)
            isDark -> Color(0xFF141418)
            else -> Color(0xFFEDE9E0)
        }
        val border = when {
            isSelected -> Color.Transparent
            isDark -> Color(0xFFF5F2EB).copy(alpha = 0.20f)
            else -> Color(0xFF111111).copy(alpha = 0.18f)
        }
        return this
            .clip(effectiveShape)
            .background(bg, effectiveShape)
            .border(width = 1.dp, color = border, shape = effectiveShape)
    }

    val accent = accentColor ?: Color(0xFF1E90FF)

    val fillBrush = when {
        isSelected && isDark -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                accent.copy(alpha = 0.24f),
                Color(0xFF101724).copy(alpha = 0.86f)
            )
        )
        isSelected && !isDark -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f),
                accent.copy(alpha = 0.18f),
                Color(0xFFE4EEFC).copy(alpha = 0.90f)
            )
        )
        isDark -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.13f),
                Color(0xFF141923).copy(alpha = 0.72f),
                Color.White.copy(alpha = 0.05f)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFFEEF3FA).copy(alpha = 0.80f),
                Color.White.copy(alpha = 0.65f)
            )
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isSelected) 0.65f else 0.30f),
                accent.copy(alpha = if (isSelected) 0.50f else 0.16f),
                Color(0xFF00E5FF).copy(alpha = if (isSelected) 0.34f else 0.10f),
                Color.White.copy(alpha = if (isSelected) 0.38f else 0.16f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.95f),
                accent.copy(alpha = if (isSelected) 0.42f else 0.16f),
                Color.Black.copy(alpha = if (isSelected) 0.15f else 0.08f)
            )
        }
    )

    return this
        .clip(effectiveShape)
        .background(fillBrush, effectiveShape)
        .border(width = if (isSelected) 1.2.dp else 1.dp, brush = borderBrush, shape = effectiveShape)
}

/**
 * Lightweight list item surface (zero overhead on unselected rows; Liquid Glass V2 highlight on active row).
 */
@Composable
fun ModifierFrostedGlassBridge(
    shape: Shape = RoundedCornerShape(16.dp),
    isHighlighted: Boolean = false,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    val effectiveShape = if (isEditorial) RoundedCornerShape(8.dp) else shape
    if (!isHighlighted) return Modifier

    if (isEditorial) {
        val bg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
        return Modifier
            .clip(effectiveShape)
            .background(bg, effectiveShape)
    }

    val fillBrush = Brush.horizontalGradient(
        colors = if (isDark) {
            listOf(
                Color(0xFF1E90FF).copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.08f),
                Color(0xFF00E5FF).copy(alpha = 0.10f)
            )
        } else {
            listOf(
                Color(0xFF1E90FF).copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.70f),
                Color(0xFF00E5FF).copy(alpha = 0.08f)
            )
        }
    )
    val rimBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = 0.32f),
                Color(0xFF1E90FF).copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.16f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.90f),
                Color(0xFF1E90FF).copy(alpha = 0.25f),
                Color.Black.copy(alpha = 0.08f)
            )
        }
    )
    return Modifier
        .clip(effectiveShape)
        .background(fillBrush, effectiveShape)
        .border(width = 1.dp, brush = rimBrush, shape = effectiveShape)
}

@Composable
fun ModifierLiquidGlassPillBridge(
    isSelected: Boolean = false,
    shape: Shape = CircleShape,
): Modifier = Modifier.liquidGlassPill(isSelected = isSelected, shape = shape)

@Composable
fun Modifier.liquidGlass(
    enabled: Boolean,
    cornerRadius: Dp = 20.dp,
    alpha: Float = 0.85f,
    blurRadius: Dp = 35.dp
): Modifier {
    return this.then(
        if (enabled) {
            Modifier.frostedGlassSurface(shape = RoundedCornerShape(cornerRadius))
        } else {
            Modifier
        }
    )
}

@Composable
fun LiquidGlassContainer(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    blurRadius: Dp = 35.dp,
    content: @Composable BoxScope.() -> Unit
) {
    if (enabled) {
        val shape = RoundedCornerShape(cornerRadius)
        Box(
            modifier = modifier
                .frostedGlassSurface(shape = shape, isHighlighted = false)
        ) {
            content()
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
fun FrostedGlassCard(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    LiquidGlassContainer(
        enabled = enabled,
        modifier = modifier,
        cornerRadius = cornerRadius,
        blurRadius = 35.dp,
        content = content
    )
}

@Composable
fun Modifier.liquidGlassFromPrefs(): Modifier {
    val enabled by rememberPreference(LiquidGlassEffectKey, defaultValue = true)
    val blurRadius by rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 35f)
    val cornerRadius by rememberPreference(LiquidGlassCornerRadiusKey, defaultValue = 20f)
    val opacity by rememberPreference(LiquidGlassOpacityKey, defaultValue = 0.20f)
    return this.liquidGlass(
        enabled = enabled,
        cornerRadius = cornerRadius.dp,
        alpha = opacity,
        blurRadius = blurRadius.dp
    )
}

@Composable
fun LiquidGlassContainerFromPrefs(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val enabled by rememberPreference(LiquidGlassEffectKey, defaultValue = true)
    val blurRadius by rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 35f)
    LiquidGlassContainer(
        enabled = enabled,
        modifier = modifier,
        cornerRadius = cornerRadius,
        blurRadius = blurRadius.dp,
        content = content
    )
}

/**
 * Returns the appropriate high-contrast foreground (icon/text) color for elements placed
 * inside [crystalLiquidGlassPill] or [liquidGlassPill].
 *
 * In EDITORIAL mode, selected pills use a solid inverted fill (`#F5F2EB` in dark mode,
 * `#111111` in light mode), so the content inside must also invert (`#08080A` in dark mode,
 * `#FBF9F5` in light mode) to remain clearly visible.
 */
@Composable
fun editorialPillContentColor(
    isSelected: Boolean,
    defaultColor: Color,
    isDark: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
): Color {
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    if (!isEditorial) return defaultColor
    return if (isSelected) {
        if (isDark) Color(0xFF08080A) else Color(0xFFFBF9F5)
    } else {
        if (isDark) Color(0xFFF5F2EB) else Color(0xFF111111)
    }
}

