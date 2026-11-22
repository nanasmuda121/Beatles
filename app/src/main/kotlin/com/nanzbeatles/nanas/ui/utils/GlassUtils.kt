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
import com.nanzbeatles.nanas.ui.theme.LocalSoftLightMode
import com.nanzbeatles.nanas.utils.rememberPreference

/**
 * Ultra-lightweight Ambient Backdrop that adapts to Liquid Glass
 * (subtle dark obsidian or soft non-glaring pearl-slate) and Editorial.
 * Uses a single static color / subtle 2-stop gradient (zero real-time blur passes).
 */
@Composable
fun GlassmorphicAmbientBackdrop(
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    val isSoftLight = LocalSoftLightMode.current
    if (isEditorial) {
        val bgColor = when {
            isDark -> Color(0xFF08080A)
            isSoftLight -> Color(0xFFE5E1D8)
            else -> Color(0xFFFBF9F5)
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgColor)
        )
        return
    }

    val baseBrush = when {
        isDark -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF06090F),
                Color(0xFF04060A)
            )
        )
        isSoftLight -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE1E6EE),
                Color(0xFFE6EAEF)
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF2F5FA),
                Color(0xFFF6F8FC)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBrush)
    )
}

/**
 * Lightweight, subtle ("Ringan") Liquid Glass Surface Modifier (or crisp Editorial frame in EDITORIAL mode).
 * Single-pass translucent fill + thin 0.8dp hairline border without heavy dome/meniscus overdraw.
 */
@Composable
fun Modifier.crystalLiquidGlassSurface(
    shape: Shape = RoundedCornerShape(22.dp),
    isHighlighted: Boolean = false,
    baseTint: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isSoftLight = LocalSoftLightMode.current
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"

    if (isEditorial) {
        val editorialShape = RoundedCornerShape(10.dp)
        val editorialBg = baseTint ?: when {
            isDark -> if (isHighlighted) Color(0xFF1A1A20) else Color(0xFF111115)
            isSoftLight -> if (isHighlighted) Color(0xFFD5D0C5) else Color(0xFFDDD8CE)
            else -> if (isHighlighted) Color(0xFFEDE8DF) else Color(0xFFF5F1E9)
        }
        val editorialBorder = if (isDark) {
            Color(0xFFF5F2EB).copy(alpha = if (isHighlighted) 0.38f else 0.15f)
        } else {
            Color(0xFF1A1917).copy(alpha = if (isHighlighted) 0.35f else 0.14f)
        }
        return this
            .clip(editorialShape)
            .background(editorialBg, editorialShape)
            .border(
                width = if (isHighlighted) 1.1.dp else 0.8.dp,
                color = editorialBorder,
                shape = editorialShape
            )
    }

    val resolvedBaseTint = baseTint ?: when {
        isDark -> Color(0xFF10151F).copy(alpha = if (isHighlighted) 0.62f else 0.50f)
        isSoftLight -> Color(0xFFEDF0F6).copy(alpha = if (isHighlighted) 0.72f else 0.60f)
        else -> Color(0xFFF7F9FD).copy(alpha = if (isHighlighted) 0.76f else 0.65f)
    }

    val subtleBorderColor = when {
        isDark -> Color.White.copy(alpha = if (isHighlighted) 0.24f else 0.13f)
        isSoftLight -> Color(0xFF1C222D).copy(alpha = if (isHighlighted) 0.16f else 0.09f)
        else -> Color(0xFF1C222D).copy(alpha = if (isHighlighted) 0.14f else 0.08f)
    }

    return this
        .clip(shape)
        .background(resolvedBaseTint, shape)
        .border(
            width = if (isHighlighted) 1.dp else 0.75.dp,
            color = subtleBorderColor,
            shape = shape
        )
}

/**
 * Lightweight, subtle ("Ringan") Liquid Glass Pill Modifier (or Editorial badge in EDITORIAL mode).
 */
@Composable
fun Modifier.crystalLiquidGlassPill(
    isSelected: Boolean = false,
    shape: Shape = CircleShape,
    accentColor: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isSoftLight = LocalSoftLightMode.current
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"

    if (isEditorial) {
        val editorialShape = RoundedCornerShape(8.dp)
        val bg = when {
            isSelected && isDark -> Color(0xFFF5F2EB)
            isSelected && !isDark -> Color(0xFF1A1917)
            isDark -> Color(0xFF16161B)
            isSoftLight -> Color(0xFFDAD5CA)
            else -> Color(0xFFEDE9E0)
        }
        val border = when {
            isSelected && isDark -> Color(0xFFF5F2EB)
            isSelected && !isDark -> Color(0xFF1A1917)
            isDark -> Color(0xFFF5F2EB).copy(alpha = 0.20f)
            else -> Color(0xFF1A1917).copy(alpha = 0.16f)
        }
        return this
            .clip(editorialShape)
            .background(bg, editorialShape)
            .border(width = 0.8.dp, color = border, shape = editorialShape)
    }

    val accent = accentColor ?: Color(0xFF1E90FF)

    val pillBgColor = when {
        isSelected && isDark -> accent.copy(alpha = 0.22f)
        isSelected && !isDark -> accent.copy(alpha = 0.16f)
        isDark -> Color.White.copy(alpha = 0.07f)
        isSoftLight -> Color(0xFFF0F3F8).copy(alpha = 0.65f)
        else -> Color.White.copy(alpha = 0.72f)
    }

    val borderColor = when {
        isSelected && isDark -> accent.copy(alpha = 0.45f)
        isSelected && !isDark -> accent.copy(alpha = 0.38f)
        isDark -> Color.White.copy(alpha = 0.14f)
        else -> Color(0xFF1C222D).copy(alpha = 0.10f)
    }

    return this
        .clip(shape)
        .background(pillBgColor, shape)
        .border(width = if (isSelected) 1.dp else 0.75.dp, color = borderColor, shape = shape)
}

/**
 * Lightweight Liquid Glass Card Surface for general app UI (Settings, Menus, Dialogs, Cards).
 * Single-pass translucent fill + thin subtle border for smooth scrolling & clean appearance.
 */
@Composable
fun Modifier.frostedGlassSurface(
    shape: Shape = RoundedCornerShape(20.dp),
    isHighlighted: Boolean = false,
    baseTint: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isSoftLight = LocalSoftLightMode.current
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"

    if (isEditorial) {
        val editorialShape = RoundedCornerShape(10.dp)
        val bg = baseTint ?: when {
            isDark -> if (isHighlighted) Color(0xFF1D1D22) else Color(0xFF121216)
            isSoftLight -> if (isHighlighted) Color(0xFFD5D0C5) else Color(0xFFDDD8CE)
            else -> if (isHighlighted) Color(0xFFEAE5DC) else Color(0xFFF3EFE7)
        }
        val borderColor = if (isDark) {
            Color(0xFFF5F2EB).copy(alpha = if (isHighlighted) 0.30f else 0.14f)
        } else {
            Color(0xFF1A1917).copy(alpha = if (isHighlighted) 0.28f else 0.13f)
        }
        return this
            .clip(editorialShape)
            .background(bg, editorialShape)
            .border(width = 0.8.dp, color = borderColor, shape = editorialShape)
    }

    val baseColor = baseTint ?: when {
        isDark -> if (isHighlighted) Color(0xFF161C27).copy(alpha = 0.66f) else Color(0xFF10141D).copy(alpha = 0.54f)
        isSoftLight -> if (isHighlighted) Color(0xFFE6EBF3).copy(alpha = 0.78f) else Color(0xFFEDF0F6).copy(alpha = 0.68f)
        else -> if (isHighlighted) Color(0xFFEBF0F9).copy(alpha = 0.82f) else Color(0xFFF5F7FC).copy(alpha = 0.72f)
    }

    val borderColor = when {
        isDark -> Color.White.copy(alpha = if (isHighlighted) 0.22f else 0.11f)
        else -> Color(0xFF1C222D).copy(alpha = if (isHighlighted) 0.16f else 0.08f)
    }

    return this
        .clip(shape)
        .background(baseColor, shape)
        .border(width = if (isHighlighted) 1.dp else 0.75.dp, color = borderColor, shape = shape)
}

/**
 * Lightweight Liquid Glass Pill Modifier for chips, buttons, and action badges across the app.
 */
@Composable
fun Modifier.liquidGlassPill(
    isSelected: Boolean = false,
    shape: Shape = CircleShape,
    accentColor: Color? = null,
): Modifier {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val isSoftLight = LocalSoftLightMode.current
    val isEditorial = LocalAppVisualStyle.current == "EDITORIAL"
    val effectiveShape = if (isEditorial) RoundedCornerShape(8.dp) else shape

    if (isEditorial) {
        val bg = when {
            isSelected && isDark -> Color(0xFFF5F2EB)
            isSelected && !isDark -> Color(0xFF1A1917)
            isDark -> Color(0xFF141418)
            isSoftLight -> Color(0xFFDAD5CA)
            else -> Color(0xFFEDE9E0)
        }
        val border = when {
            isSelected -> Color.Transparent
            isDark -> Color(0xFFF5F2EB).copy(alpha = 0.18f)
            else -> Color(0xFF1A1917).copy(alpha = 0.15f)
        }
        return this
            .clip(effectiveShape)
            .background(bg, effectiveShape)
            .border(width = 0.8.dp, color = border, shape = effectiveShape)
    }

    val accent = accentColor ?: Color(0xFF1E90FF)

    val fillColor = when {
        isSelected && isDark -> accent.copy(alpha = 0.22f)
        isSelected && !isDark -> accent.copy(alpha = 0.16f)
        isDark -> Color.White.copy(alpha = 0.07f)
        isSoftLight -> Color(0xFFEDF0F6).copy(alpha = 0.66f)
        else -> Color.White.copy(alpha = 0.72f)
    }

    val borderColor = when {
        isSelected && isDark -> accent.copy(alpha = 0.42f)
        isSelected && !isDark -> accent.copy(alpha = 0.35f)
        isDark -> Color.White.copy(alpha = 0.13f)
        else -> Color(0xFF1C222D).copy(alpha = 0.09f)
    }

    return this
        .clip(effectiveShape)
        .background(fillColor, effectiveShape)
        .border(width = if (isSelected) 1.dp else 0.75.dp, color = borderColor, shape = effectiveShape)
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

