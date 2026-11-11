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
 * Clean Backdrop that adapts to both Liquid Glass (#000000 / #FFFFFF)
 * and Editorial (Carbon Black #08080A / Warm Magazine Paper #FBF9F5).
 */
@Composable
fun GlassmorphicAmbientBackdrop(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    val bgColor = when {
        isEditorial && isDark -> Color(0xFF08080A)
        isEditorial && !isDark -> Color(0xFFFBF9F5)
        isDark -> Color.Black
        else -> Color.White
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    )
}

/**
 * Crystal-Clear ("Bening") 3D Liquid Glass Surface Modifier (or crisp Editorial frame in EDITORIAL mode).
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

    // Very clear ("bening") translucent glass base
    val resolvedBaseTint = baseTint ?: if (isDark) {
        Color(0xFF0C0E14).copy(alpha = if (isHighlighted) 0.52f else 0.42f)
    } else {
        Color.White.copy(alpha = if (isHighlighted) 0.68f else 0.56f)
    }

    val clearSheenGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isHighlighted) 0.18f else 0.12f),
                Color.White.copy(alpha = 0.03f),
                Color.Transparent,
                Color.White.copy(alpha = if (isHighlighted) 0.07f else 0.04f)
            )
        } else {
            listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.55f)
            )
        }
    )

    val specularBorder = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isHighlighted) 0.52f else 0.34f),
                Color.White.copy(alpha = if (isHighlighted) 0.18f else 0.10f),
                Color.White.copy(alpha = if (isHighlighted) 0.32f else 0.20f)
            )
        } else {
            listOf(
                Color.Black.copy(alpha = if (isHighlighted) 0.16f else 0.10f),
                Color.Black.copy(alpha = 0.04f),
                Color.Black.copy(alpha = if (isHighlighted) 0.12f else 0.08f)
            )
        }
    )

    val topGlintColor = if (isDark) {
        Color.White.copy(alpha = if (isHighlighted) 0.62f else 0.42f)
    } else {
        Color.White.copy(alpha = 0.95f)
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
                            Color.White.copy(alpha = if (isDark) 0.14f else 0.45f),
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
            width = if (isHighlighted) 1.2.dp else 1.dp,
            brush = specularBorder,
            shape = shape
        )
}

/**
 * Crystal-Clear ("Bening") Liquid Glass Pill Modifier (or Editorial badge in EDITORIAL mode).
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

    val accent = accentColor ?: if (isDark) Color.White else Color.Black

    val baseColor = when {
        isSelected && isDark -> Color.White.copy(alpha = 0.20f)
        isSelected && !isDark -> Color.Black.copy(alpha = 0.12f)
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color.Black.copy(alpha = 0.05f)
    }

    val rimBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(
                Color.White.copy(alpha = if (isSelected) 0.65f else 0.32f),
                Color.White.copy(alpha = if (isSelected) 0.20f else 0.08f),
                accent.copy(alpha = if (isSelected) 0.42f else 0.18f)
            )
        } else {
            listOf(
                Color.Black.copy(alpha = if (isSelected) 0.28f else 0.12f),
                Color.Black.copy(alpha = 0.04f),
                Color.Black.copy(alpha = if (isSelected) 0.20f else 0.08f)
            )
        }
    )

    return this
        .clip(shape)
        .background(baseColor, shape)
        .drawWithContent {
            val topDepth = (size.height * 0.45f).coerceAtMost(18.dp.toPx())
            if (topDepth > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) (if (isSelected) 0.22f else 0.12f) else 0.55f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = topDepth
                    )
                )
            }
            drawContent()
        }
        .border(width = if (isSelected) 1.2.dp else 0.9.dp, brush = rimBrush, shape = shape)
}

/**
 * Card Surface for general app UI (Settings, Menus, Dialogs, Cards).
 * Adapts to Liquid Glass (rounded 22dp soft cards) vs Editorial (sharp 10dp architectural frames).
 */
@Composable
fun Modifier.frostedGlassSurface(
    shape: Shape = RoundedCornerShape(22.dp),
    isHighlighted: Boolean = false,
    baseTint: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    val effectiveShape = if (isEditorial) RoundedCornerShape(10.dp) else shape

    val bg = baseTint ?: if (isEditorial) {
        if (isDark) {
            if (isHighlighted) Color(0xFF1D1D22) else Color(0xFF121216)
        } else {
            if (isHighlighted) Color(0xFFEAE5DC) else Color(0xFFF3EFE7)
        }
    } else {
        if (isDark) {
            if (isHighlighted) Color(0xFF1C1D22) else Color(0xFF121316)
        } else {
            if (isHighlighted) Color(0xFFE8EAEF) else Color(0xFFF3F4F7)
        }
    }

    val borderColor = if (isEditorial) {
        if (isDark) {
            Color(0xFFF5F2EB).copy(alpha = if (isHighlighted) 0.35f else 0.16f)
        } else {
            Color(0xFF111111).copy(alpha = if (isHighlighted) 0.35f else 0.16f)
        }
    } else {
        if (isDark) {
            Color.White.copy(alpha = if (isHighlighted) 0.16f else 0.07f)
        } else {
            Color.Black.copy(alpha = if (isHighlighted) 0.14f else 0.06f)
        }
    }

    return this
        .clip(effectiveShape)
        .background(bg, effectiveShape)
        .border(width = 1.dp, color = borderColor, shape = effectiveShape)
}

/**
 * Pill Modifier for general chips/buttons outside the 4 glass zones.
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

    val bg = when {
        isEditorial -> when {
            isSelected && isDark -> Color(0xFFF5F2EB)
            isSelected && !isDark -> Color(0xFF111111)
            isDark -> Color(0xFF141418)
            else -> Color(0xFFEDE9E0)
        }
        isSelected -> if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.14f)
        isDark -> Color.White.copy(alpha = 0.10f)
        else -> Color.Black.copy(alpha = 0.06f)
    }
    val border = when {
        isEditorial -> when {
            isSelected -> Color.Transparent
            isDark -> Color(0xFFF5F2EB).copy(alpha = 0.20f)
            else -> Color(0xFF111111).copy(alpha = 0.18f)
        }
        isSelected -> if (isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.22f)
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color.Black.copy(alpha = 0.07f)
    }
    return this
        .clip(effectiveShape)
        .background(bg, effectiveShape)
        .border(width = 1.dp, color = border, shape = effectiveShape)
}

/**
 * Clean list item surface (no heavy glass border on every song row).
 */
@Composable
fun ModifierFrostedGlassBridge(
    shape: Shape = RoundedCornerShape(16.dp),
    isHighlighted: Boolean = false,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    val effectiveShape = if (isEditorial) RoundedCornerShape(8.dp) else shape
    return if (isHighlighted) {
        thisOrModifier(
            shape = effectiveShape,
            bg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
        )
    } else {
        Modifier
    }
}

private fun thisOrModifier(shape: Shape, bg: Color): Modifier =
    Modifier
        .clip(shape)
        .background(bg, shape)

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

