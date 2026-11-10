/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.theme

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score
import kotlin.math.max
import kotlin.math.min

val DefaultThemeColor = Color(0xFFE0E0E0)

/**
 * Provides the active visual theme style across the app:
 * - "LIQUID_GLASS": 3D Crystal-Clear ("Bening") Liquid Glass with water-lens refraction.
 * - "EDITORIAL": Swiss Magazine / Vinyl Editorial aesthetic with Space Grotesk display typography,
 *   warm paper/carbon surfaces, and crisp architectural hairline borders.
 */
val LocalAppVisualStyle = staticCompositionLocalOf { "LIQUID_GLASS" }

fun Color.isMonochromeSeed(): Boolean {
    val maxC = max(red, max(green, blue))
    val minC = min(red, min(green, blue))
    val sat = if (maxC > 0f) (maxC - minC) / maxC else 0f
    return sat < 0.08f
}

@Composable
fun NanzBeatlesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    selectedFont: String = "OUTFIT",
    fontScale: Float = 1f,
    fontBoldness: Float = 0f,
    visualStyle: String = "LIQUID_GLASS",
    content: @Composable () -> Unit,
) {
    val animatedSeedColor by animateColorAsState(
        targetValue = themeColor,
        animationSpec = tween(durationMillis = 380),
        label = "animatedThemeSeedColor"
    )

    val isMonochrome = remember(themeColor) {
        themeColor.isMonochromeSeed()
    }
    val isEditorial = visualStyle == "EDITORIAL"

    val baseColorScheme = rememberDynamicColorScheme(
        seedColor = if (isMonochrome) {
            val lum = animatedSeedColor.luminance()
            Color(lum, lum, lum, 1f)
        } else {
            animatedSeedColor
        },
        isDark = darkTheme,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = if (isMonochrome) PaletteStyle.Monochrome else PaletteStyle.Vibrant
    )

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme, isMonochrome, animatedSeedColor, isEditorial) {
        val vibrantPrimary = if (isMonochrome) {
            if (darkTheme) {
                if (isEditorial) Color(0xFFF5F2EB) else Color.White
            } else {
                if (isEditorial) Color(0xFF111111) else Color(0xFF0A0C10)
            }
        } else {
            if (darkTheme) {
                if (animatedSeedColor.luminance() < 0.38f) {
                    androidx.compose.ui.graphics.lerp(animatedSeedColor, Color.White, 0.36f)
                } else {
                    animatedSeedColor
                }
            } else {
                if (animatedSeedColor.luminance() > 0.55f) {
                    androidx.compose.ui.graphics.lerp(animatedSeedColor, Color.Black, 0.42f)
                } else {
                    animatedSeedColor
                }
            }
        }

        if (!darkTheme) {
            if (isEditorial) {
                // TEMA PUTIH + EDITORIAL (Warm Magazine Paper #FBF9F5 & Deep Ink #111111)
                val paperBg = Color(0xFFFBF9F5)
                val inkFg = Color(0xFF111111)
                val primaryCont = if (isMonochrome) {
                    Color(0xFF18181B)
                } else {
                    androidx.compose.ui.graphics.lerp(paperBg, vibrantPrimary, 0.18f)
                }
                val onPrimaryCont = if (isMonochrome) Color(0xFFFBF9F5) else inkFg
                val secondaryCont = if (isMonochrome) {
                    Color(0xFFEBE6DD)
                } else {
                    androidx.compose.ui.graphics.lerp(paperBg, vibrantPrimary, 0.14f)
                }

                baseColorScheme.copy(
                    primary = vibrantPrimary,
                    onPrimary = if (vibrantPrimary.luminance() > 0.52f) inkFg else paperBg,
                    primaryContainer = primaryCont,
                    onPrimaryContainer = onPrimaryCont,
                    secondary = if (isMonochrome) Color(0xFF27272A) else vibrantPrimary,
                    onSecondary = paperBg,
                    secondaryContainer = secondaryCont,
                    onSecondaryContainer = inkFg,
                    tertiary = if (isMonochrome) Color(0xFF3F3F46) else vibrantPrimary,
                    onTertiary = paperBg,
                    tertiaryContainer = Color(0xFFEFECE6),
                    onTertiaryContainer = inkFg,
                    background = paperBg,
                    onBackground = inkFg,
                    surface = paperBg,
                    onSurface = inkFg,
                    surfaceVariant = Color(0xFFEEEAE2),
                    onSurfaceVariant = Color(0xFF4A4843),
                    surfaceDim = Color(0xFFEFECE6),
                    surfaceBright = paperBg,
                    surfaceContainerLowest = Color.White,
                    surfaceContainerLow = Color(0xFFF6F3EC),
                    surfaceContainer = Color(0xFFF2EFE7),
                    surfaceContainerHigh = Color(0xFFEAE5DC),
                    surfaceContainerHighest = Color(0xFFE2DDD2),
                    outline = Color(0xFF8C887F),
                    outlineVariant = Color(0xFFD8D3C8),
                )
            } else {
                // TEMA PUTIH + LIQUID GLASS (#FFFFFF Bersih & Kontras Hitam Tajam)
                val primaryCont = if (isMonochrome) {
                    Color(0xFF14161C)
                } else {
                    androidx.compose.ui.graphics.lerp(Color.White, vibrantPrimary, 0.16f)
                }
                val onPrimaryCont = if (isMonochrome) {
                    Color.White
                } else {
                    Color(0xFF0A0C10)
                }
                val secondaryCont = if (isMonochrome) {
                    Color(0xFFE8EAF0)
                } else {
                    androidx.compose.ui.graphics.lerp(Color.White, vibrantPrimary, 0.14f)
                }

                baseColorScheme.copy(
                    primary = vibrantPrimary,
                    onPrimary = if (vibrantPrimary.luminance() > 0.52f) Color.Black else Color.White,
                    primaryContainer = primaryCont,
                    onPrimaryContainer = onPrimaryCont,
                    secondary = if (isMonochrome) Color(0xFF262932) else vibrantPrimary,
                    onSecondary = if (isMonochrome) Color.White else if (vibrantPrimary.luminance() > 0.52f) Color.Black else Color.White,
                    secondaryContainer = secondaryCont,
                    onSecondaryContainer = Color(0xFF0A0C10),
                    tertiary = if (isMonochrome) Color(0xFF3A3F4D) else vibrantPrimary,
                    onTertiary = Color.White,
                    tertiaryContainer = if (isMonochrome) Color(0xFFECEEF4) else secondaryCont,
                    onTertiaryContainer = Color(0xFF0A0C10),
                    background = Color.White,
                    onBackground = Color(0xFF0A0C10),
                    surface = Color.White,
                    onSurface = Color(0xFF0A0C10),
                    surfaceVariant = Color(0xFFECEEF3),
                    onSurfaceVariant = Color(0xFF4A4F5C),
                    surfaceDim = Color(0xFFF2F3F7),
                    surfaceBright = Color.White,
                    surfaceContainerLowest = Color.White,
                    surfaceContainerLow = Color(0xFFF7F8FA),
                    surfaceContainer = Color(0xFFF1F2F6),
                    surfaceContainerHigh = Color(0xFFE9EBF0),
                    surfaceContainerHighest = Color(0xFFE0E3EB),
                    outline = Color(0xFFB8BDC8),
                    outlineVariant = Color(0xFFE2E5EC),
                )
            }
        } else {
            if (isEditorial) {
                // TEMA HITAM + EDITORIAL (Carbon Press Black #08080A & Warm Ivory #F5F2EB)
                val carbonBg = Color(0xFF08080A)
                val ivoryFg = Color(0xFFF5F2EB)
                val primaryCont = if (isMonochrome) {
                    ivoryFg
                } else {
                    androidx.compose.ui.graphics.lerp(Color(0xFF141418), vibrantPrimary, 0.32f)
                }
                val onPrimaryCont = if (isMonochrome) {
                    carbonBg
                } else {
                    if (primaryCont.luminance() > 0.5f) carbonBg else ivoryFg
                }
                val secondaryCont = if (isMonochrome) {
                    Color(0xFF1E1E24)
                } else {
                    androidx.compose.ui.graphics.lerp(Color(0xFF141418), vibrantPrimary, 0.24f)
                }

                baseColorScheme.copy(
                    primary = vibrantPrimary,
                    onPrimary = if (vibrantPrimary.luminance() > 0.5f) carbonBg else ivoryFg,
                    primaryContainer = primaryCont,
                    onPrimaryContainer = onPrimaryCont,
                    secondary = if (isMonochrome) Color(0xFFD6D3CD) else vibrantPrimary,
                    onSecondary = carbonBg,
                    secondaryContainer = secondaryCont,
                    onSecondaryContainer = ivoryFg,
                    tertiary = if (isMonochrome) Color(0xFFA8A49E) else vibrantPrimary,
                    onTertiary = carbonBg,
                    tertiaryContainer = Color(0xFF24242B),
                    onTertiaryContainer = ivoryFg,
                    background = carbonBg,
                    onBackground = ivoryFg,
                    surface = carbonBg,
                    onSurface = ivoryFg,
                    surfaceVariant = Color(0xFF1B1B20),
                    onSurfaceVariant = Color(0xFFABA79F),
                    surfaceDim = carbonBg,
                    surfaceBright = Color(0xFF1E1E24),
                    surfaceContainerLowest = carbonBg,
                    surfaceContainerLow = Color(0xFF0E0E12),
                    surfaceContainer = Color(0xFF141418),
                    surfaceContainerHigh = Color(0xFF1B1B20),
                    surfaceContainerHighest = Color(0xFF24242B),
                    outline = Color(0xFF52504C),
                    outlineVariant = Color(0xFF2A2A30),
                )
            } else {
                // TEMA HITAM + LIQUID GLASS (#000000 Hitam Solid & Kontras Putih Tajam)
                val primaryCont = if (isMonochrome) {
                    Color(0xFFEAECEF)
                } else {
                    androidx.compose.ui.graphics.lerp(Color(0xFF14151A), vibrantPrimary, 0.30f)
                }
                val onPrimaryCont = if (isMonochrome) {
                    Color.Black
                } else {
                    if (primaryCont.luminance() > 0.5f) Color.Black else Color.White
                }
                val secondaryCont = if (isMonochrome) {
                    Color(0xFF22252D)
                } else {
                    androidx.compose.ui.graphics.lerp(Color(0xFF14151A), vibrantPrimary, 0.24f)
                }

                baseColorScheme.copy(
                    primary = vibrantPrimary,
                    onPrimary = if (vibrantPrimary.luminance() > 0.5f) Color.Black else Color.White,
                    primaryContainer = primaryCont,
                    onPrimaryContainer = onPrimaryCont,
                    secondary = if (isMonochrome) Color(0xFFD4D7E0) else vibrantPrimary,
                    onSecondary = if (isMonochrome) Color.Black else if (vibrantPrimary.luminance() > 0.5f) Color.Black else Color.White,
                    secondaryContainer = secondaryCont,
                    onSecondaryContainer = Color.White,
                    tertiary = if (isMonochrome) Color(0xFFB8BDC8) else vibrantPrimary,
                    onTertiary = Color.Black,
                    tertiaryContainer = if (isMonochrome) Color(0xFF262932) else secondaryCont,
                    onTertiaryContainer = Color.White,
                    background = Color.Black,
                    onBackground = Color.White,
                    surface = Color.Black,
                    onSurface = Color.White,
                    surfaceVariant = Color(0xFF1E2028),
                    onSurfaceVariant = Color(0xFFA6AAB4),
                    surfaceDim = Color.Black,
                    surfaceBright = Color(0xFF1E2028),
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = Color(0xFF0D0E12),
                    surfaceContainer = Color(0xFF14151A),
                    surfaceContainerHigh = Color(0xFF1C1E24),
                    surfaceContainerHighest = Color(0xFF252830),
                    outline = Color(0xFF444854),
                    outlineVariant = Color(0xFF262932),
                )
            }
        }
    }

    val typography = remember(selectedFont, fontScale, fontBoldness, isEditorial) {
        if (isEditorial && (selectedFont == "DEFAULT" || selectedFont == "OUTFIT")) {
            // Editorial Typography: Space Grotesk for bold magazine display/headlines/titles, Manrope for crisp body
            AppTypography
                .withEditorialFonts(displayAndHeadingFont = SpaceGrotesk, bodyFont = Manrope)
                .scaledBy(fontScale)
                .boldedBy(fontBoldness)
        } else {
            val fontFamily = when (selectedFont) {
                "DEFAULT" -> FontFamily.Default
                "MANROPE" -> Manrope
                "SPACE_GROTESK" -> SpaceGrotesk
                "POPPINS", "ROBOTO", "INTER", "OUTFIT" -> Outfit
                else -> Outfit
            }
            AppTypography.withFontFamily(fontFamily).scaledBy(fontScale).boldedBy(fontBoldness)
        }
    }

    CompositionLocalProvider(LocalAppVisualStyle provides visualStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}

@Composable
fun AuraMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    selectedFont: String = "OUTFIT",
    fontScale: Float = 1f,
    fontBoldness: Float = 0f,
    visualStyle: String = "LIQUID_GLASS",
    content: @Composable () -> Unit,
) = NanzBeatlesTheme(
    darkTheme = darkTheme,
    pureBlack = pureBlack,
    themeColor = themeColor,
    selectedFont = selectedFont,
    fontScale = fontScale,
    fontBoldness = fontBoldness,
    visualStyle = visualStyle,
    content = content
)

/**
 * Extracts the vibrant/dominant theme color from the currently playing song's album cover / thumbnail.
 */
fun Bitmap.extractThemeColor(): Color {
    val palette = Palette.from(this)
        .maximumColorCount(16)
        .generate()
    val colorsToPopulation = palette.swatches.associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    val bestColorInt = palette.vibrantSwatch?.rgb
        ?: palette.lightVibrantSwatch?.rgb
        ?: rankedColors.firstOrNull()
        ?: palette.dominantSwatch?.rgb
        ?: return DefaultThemeColor

    val raw = Color(bestColorInt)
    val maxC = max(raw.red, max(raw.green, raw.blue))
    val minC = min(raw.red, min(raw.green, raw.blue))
    val sat = if (maxC > 0f) (maxC - minC) / maxC else 0f

    if (sat < 0.08f) {
        return DefaultThemeColor
    }

    return if (raw.luminance() < 0.35f) {
        androidx.compose.ui.graphics.lerp(raw, Color.White, 0.35f)
    } else {
        raw
    }
}

fun Bitmap.extractGradientColors(): List<Color> {
    val palette = Palette.from(this)
        .maximumColorCount(16)
        .generate()
    val first = palette.vibrantSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: 0xFF3A3A3C.toInt()
    val second = palette.darkVibrantSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
        ?: 0xFF1C1C1E.toInt()
    return listOf(Color(first), Color(second))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}

private fun androidx.compose.material3.Typography.withEditorialFonts(
    displayAndHeadingFont: FontFamily,
    bodyFont: FontFamily
) = copy(
    displayLarge = displayLarge.copy(fontFamily = displayAndHeadingFont, letterSpacing = (-0.6).sp),
    displayMedium = displayMedium.copy(fontFamily = displayAndHeadingFont, letterSpacing = (-0.4).sp),
    displaySmall = displaySmall.copy(fontFamily = displayAndHeadingFont, letterSpacing = (-0.3).sp),
    headlineLarge = headlineLarge.copy(fontFamily = displayAndHeadingFont, letterSpacing = (-0.4).sp),
    headlineMedium = headlineMedium.copy(fontFamily = displayAndHeadingFont, letterSpacing = (-0.3).sp),
    headlineSmall = headlineSmall.copy(fontFamily = displayAndHeadingFont, letterSpacing = (-0.2).sp),
    titleLarge = titleLarge.copy(fontFamily = displayAndHeadingFont, letterSpacing = (-0.2).sp),
    titleMedium = titleMedium.copy(fontFamily = displayAndHeadingFont),
    titleSmall = titleSmall.copy(fontFamily = displayAndHeadingFont),
    bodyLarge = bodyLarge.copy(fontFamily = bodyFont),
    bodyMedium = bodyMedium.copy(fontFamily = bodyFont),
    bodySmall = bodySmall.copy(fontFamily = bodyFont),
    labelLarge = labelLarge.copy(fontFamily = displayAndHeadingFont),
    labelMedium = labelMedium.copy(fontFamily = bodyFont),
    labelSmall = labelSmall.copy(fontFamily = bodyFont)
)

private fun androidx.compose.material3.Typography.withFontFamily(fontFamily: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily)
)

private fun androidx.compose.material3.Typography.scaledBy(scale: Float) =
    if (scale <= 0f || scale == 1f) {
        this
    } else {
        copy(
            displayLarge = displayLarge.scaledBy(scale),
            displayMedium = displayMedium.scaledBy(scale),
            displaySmall = displaySmall.scaledBy(scale),
            headlineLarge = headlineLarge.scaledBy(scale),
            headlineMedium = headlineMedium.scaledBy(scale),
            headlineSmall = headlineSmall.scaledBy(scale),
            titleLarge = titleLarge.scaledBy(scale),
            titleMedium = titleMedium.scaledBy(scale),
            titleSmall = titleSmall.scaledBy(scale),
            bodyLarge = bodyLarge.scaledBy(scale),
            bodyMedium = bodyMedium.scaledBy(scale),
            bodySmall = bodySmall.scaledBy(scale),
            labelLarge = labelLarge.scaledBy(scale),
            labelMedium = labelMedium.scaledBy(scale),
            labelSmall = labelSmall.scaledBy(scale)
        )
    }

private fun TextStyle.scaledBy(scale: Float) =
    copy(
        fontSize = (fontSize.value * scale.coerceIn(0.5f, 3f)).sp,
        lineHeight = (lineHeight.value * scale.coerceIn(0.5f, 3f)).sp,
        letterSpacing = (letterSpacing.value * scale.coerceIn(0.5f, 3f)).sp,
    )

private fun androidx.compose.material3.Typography.boldedBy(boldness: Float) =
    if (boldness <= 0f) this
    else copy(
        displayLarge = displayLarge.boldedBy(boldness),
        displayMedium = displayMedium.boldedBy(boldness),
        displaySmall = displaySmall.boldedBy(boldness),
        headlineLarge = headlineLarge.boldedBy(boldness),
        headlineMedium = headlineMedium.boldedBy(boldness),
        headlineSmall = headlineSmall.boldedBy(boldness),
        titleLarge = titleLarge.boldedBy(boldness),
        titleMedium = titleMedium.boldedBy(boldness),
        titleSmall = titleSmall.boldedBy(boldness),
        bodyLarge = bodyLarge.boldedBy(boldness),
        bodyMedium = bodyMedium.boldedBy(boldness),
        bodySmall = bodySmall.boldedBy(boldness),
        labelLarge = labelLarge.boldedBy(boldness),
        labelMedium = labelMedium.boldedBy(boldness),
        labelSmall = labelSmall.boldedBy(boldness)
    )

private fun TextStyle.boldedBy(boldness: Float) = copy(
    fontWeight = when (fontWeight) {
        FontWeight.Thin -> lerp(FontWeight.Thin, FontWeight.Black, boldness)
        FontWeight.Light -> lerp(FontWeight.Light, FontWeight.Black, boldness)
        FontWeight.Normal -> lerp(FontWeight.Normal, FontWeight.Black, boldness)
        FontWeight.Medium -> lerp(FontWeight.Medium, FontWeight.Black, boldness)
        FontWeight.SemiBold -> lerp(FontWeight.SemiBold, FontWeight.Black, boldness)
        FontWeight.Bold -> FontWeight.Bold
        FontWeight.Black -> FontWeight.Black
        else -> fontWeight
    }
)

private fun lerp(a: FontWeight, b: FontWeight, fraction: Float): FontWeight {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return when {
        clampedFraction <= 0f -> a
        clampedFraction >= 1f -> b
        else -> FontWeight((a.weight + (b.weight - a.weight) * clampedFraction).toInt())
    }
}
