/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.AppVisualStyle
import com.nanzbeatles.nanas.constants.AppVisualStyleKey
import com.nanzbeatles.nanas.constants.DarkModeKey
import com.nanzbeatles.nanas.constants.DynamicIslandTheme
import com.nanzbeatles.nanas.constants.DynamicIslandThemeKey
import com.nanzbeatles.nanas.constants.DynamicThemeKey
import com.nanzbeatles.nanas.constants.LiquidGlassApplyNavBarKey
import com.nanzbeatles.nanas.constants.LiquidGlassApplyPlayerKey
import com.nanzbeatles.nanas.constants.LiquidGlassEffectKey
import com.nanzbeatles.nanas.constants.PureBlackKey
import com.nanzbeatles.nanas.constants.PureBlackMiniPlayerKey
import com.nanzbeatles.nanas.constants.SelectedThemeColorKey
import com.nanzbeatles.nanas.ui.theme.AuraMusicTheme
import com.nanzbeatles.nanas.ui.theme.DefaultThemeColor
import com.nanzbeatles.nanas.ui.theme.extractThemeColor
import com.nanzbeatles.nanas.ui.utils.frostedGlassSurface
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by (playerConnection?.mediaMetadata ?: remember { kotlinx.coroutines.flow.MutableStateFlow(null) }).collectAsState()

    val (darkMode, onDarkModeChange) = rememberEnumPreference(DarkModeKey, DarkMode.ON)
    val (visualStyle, onVisualStyleChange) = rememberEnumPreference(
        AppVisualStyleKey,
        AppVisualStyle.LIQUID_GLASS
    )
    val (_, onLiquidGlassEffectChange) = rememberPreference(LiquidGlassEffectKey, defaultValue = true)
    val (_, onLiquidGlassNavChange) = rememberPreference(LiquidGlassApplyNavBarKey, defaultValue = true)
    val (_, onLiquidGlassPlayerChange) = rememberPreference(LiquidGlassApplyPlayerKey, defaultValue = true)
    val (_, onDynamicIslandThemeChange) = rememberEnumPreference(
        DynamicIslandThemeKey,
        DynamicIslandTheme.LIQUID_GLASS
    )

    val (_, onPureBlackChangeRaw) = rememberPreference(PureBlackKey, defaultValue = true)
    val (_, onPureBlackMiniPlayerChange) = rememberPreference(
        PureBlackMiniPlayerKey,
        defaultValue = true
    )
    val (_, onSelectedThemeColorChange) = rememberPreference(
        SelectedThemeColorKey,
        DefaultThemeColor.toArgb()
    )
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(DynamicThemeKey, defaultValue = false)

    val isDarkSelected = darkMode != DarkMode.OFF

    val selectVisualStyle: (AppVisualStyle) -> Unit = { style ->
        onVisualStyleChange(style)
        val enableGlass = style == AppVisualStyle.LIQUID_GLASS
        onLiquidGlassEffectChange(enableGlass)
        onLiquidGlassNavChange(enableGlass)
        onLiquidGlassPlayerChange(enableGlass)
        onDynamicIslandThemeChange(
            if (enableGlass) DynamicIslandTheme.LIQUID_GLASS else DynamicIslandTheme.NORMAL
        )
    }

    val selectBlackTheme: () -> Unit = {
        onDarkModeChange(DarkMode.ON)
        onPureBlackChangeRaw(true)
        onPureBlackMiniPlayerChange(true)
        onSelectedThemeColorChange(DefaultThemeColor.toArgb())
    }

    val selectWhiteTheme: () -> Unit = {
        onDarkModeChange(DarkMode.OFF)
        onPureBlackChangeRaw(false)
        onPureBlackMiniPlayerChange(false)
        onSelectedThemeColorChange(DefaultThemeColor.toArgb())
    }

    var extractedAlbumColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(mediaMetadata?.thumbnailUrl) {
        val url = mediaMetadata?.thumbnailUrl
        if (url != null) {
            withContext(Dispatchers.IO) {
                try {
                    val result = context.imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(url)
                            .allowHardware(false)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build()
                    )
                    extractedAlbumColor = result.image?.toBitmap()?.extractThemeColor()
                } catch (_: Exception) {
                    extractedAlbumColor = null
                }
            }
        } else {
            extractedAlbumColor = null
        }
    }

    val effectivePreviewColor by animateColorAsState(
        targetValue = if (dynamicTheme && extractedAlbumColor != null) {
            extractedAlbumColor!!
        } else {
            DefaultThemeColor
        },
        animationSpec = tween(380),
        label = "effectivePreviewColor"
    )

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = systemBarsPadding.calculateTopPadding())
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Kostumisasi Tema",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${visualStyle.title} • ${if (isDarkSelected) "Hitam" else "Putih"}${if (dynamicTheme) " • Cover" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.testTag("theme_back_button")
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .heightIn(max = 320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThemeMockupPortrait(
                            isDark = isDarkSelected,
                            visualStyle = visualStyle,
                            themeColor = effectivePreviewColor
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    VisualStyleSelectorCard(
                        selectedStyle = visualStyle,
                        onSelectStyle = selectVisualStyle
                    )

                    BlackWhiteThemeSelectorCard(
                        isDarkSelected = isDarkSelected,
                        onSelectBlack = selectBlackTheme,
                        onSelectWhite = selectWhiteTheme
                    )

                    DynamicAlbumThemeCard(
                        dynamicTheme = dynamicTheme,
                        onDynamicThemeChange = onDynamicThemeChange,
                        songTitle = mediaMetadata?.title,
                        songArtist = mediaMetadata?.artists?.joinToString { it.name },
                        thumbnailUrl = mediaMetadata?.thumbnailUrl,
                        extractedColor = extractedAlbumColor
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 140.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Preview Mockup + Album Cover Preview Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(126.dp)
                            .height(236.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ThemeMockupPortrait(
                            isDark = isDarkSelected,
                            visualStyle = visualStyle,
                            themeColor = effectivePreviewColor
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DynamicAlbumThemeCard(
                            dynamicTheme = dynamicTheme,
                            onDynamicThemeChange = onDynamicThemeChange,
                            songTitle = mediaMetadata?.title,
                            songArtist = mediaMetadata?.artists?.joinToString { it.name },
                            thumbnailUrl = mediaMetadata?.thumbnailUrl,
                            extractedColor = extractedAlbumColor
                        )
                    }
                }

                // 1. GAYA TEMA VISUAL: 1. LIQUID GLASS / 2. EDITORIAL
                VisualStyleSelectorCard(
                    selectedStyle = visualStyle,
                    onSelectStyle = selectVisualStyle
                )

                // 2. WARNA DASAR: HITAM / PUTIH
                BlackWhiteThemeSelectorCard(
                    isDarkSelected = isDarkSelected,
                    onSelectBlack = selectBlackTheme,
                    onSelectWhite = selectWhiteTheme
                )

                // 3. WIDGET LAYAR UTAMA: VERSI LIQUID GLASS (4x2 & 4x1 PILL)
                LiquidGlassWidgetShowcaseCard(
                    songTitle = mediaMetadata?.title ?: "Belum Ada Lagu Diputar",
                    songArtist = mediaMetadata?.artists?.joinToString { it.name } ?: "Ketuk untuk memasang widget di Layar Utama",
                    thumbnailUrl = mediaMetadata?.thumbnailUrl
                )
            }
        }
    }
}

@Composable
private fun VisualStyleSelectorCard(
    selectedStyle: AppVisualStyle,
    onSelectStyle: (AppVisualStyle) -> Unit,
) {
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassSurface(
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Pilih Gaya Tema",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Pilih antara 1. Liquid Glass (3D Bening & Lensa Air Prismatic) atau 2. Editorial (Tipografi Majalah Musik, Kertas Hangat & Garis Tegas).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. LIQUID GLASS
            VisualStyleOptionItem(
                numberBadge = "1",
                title = "Liquid Glass",
                subtitle = "3D Bening & Lensa Air",
                isLiquidGlass = true,
                isSelected = selectedStyle == AppVisualStyle.LIQUID_GLASS,
                isDarkSurface = isDarkSurface,
                onClick = { onSelectStyle(AppVisualStyle.LIQUID_GLASS) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("visual_style_liquid_glass")
            )

            // 2. EDITORIAL
            VisualStyleOptionItem(
                numberBadge = "2",
                title = "Editorial",
                subtitle = "Majalah & Garis Tegas",
                isLiquidGlass = false,
                isSelected = selectedStyle == AppVisualStyle.EDITORIAL,
                isDarkSurface = isDarkSurface,
                onClick = { onSelectStyle(AppVisualStyle.EDITORIAL) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("visual_style_editorial")
            )
        }
    }
}

@Composable
private fun VisualStyleOptionItem(
    numberBadge: String,
    title: String,
    subtitle: String,
    isLiquidGlass: Boolean,
    isSelected: Boolean,
    isDarkSurface: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "visualStyleScale"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.2.dp else 1.dp,
        label = "visualStyleBorderWidth"
    )
    val cardCorner = if (isLiquidGlass) 20.dp else 10.dp
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDarkSurface) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.14f)
    }

    val cardBg = if (isSelected) {
        if (isDarkSurface) Color(0xFF1A1D24) else Color(0xFFE8EBF2)
    } else {
        if (isDarkSurface) Color(0xFF121419) else Color(0xFFF4F5F8)
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(cardCorner))
            .background(cardBg)
            .border(borderWidth, borderColor, RoundedCornerShape(cardCorner))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isLiquidGlass) {
            // Preview Artwork for 1. LIQUID GLASS (3D Bening Lens + Cyan/Orange Chromatic Rim)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDarkSurface) Color(0xFF07090E) else Color(0xFFF1F3F8))
                    .border(
                        1.dp,
                        if (isDarkSurface) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.12f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                // Base capsule bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDarkSurface) Color(0xFF141822) else Color.White.copy(alpha = 0.85f)
                        )
                        .border(
                            0.8.dp,
                            if (isDarkSurface) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.12f),
                            CircleShape
                        )
                )
                // Protruding 3D Crystal-Clear ("Bening") Water-Lens Bubble with Cyan & Orange Rim
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDarkSurface) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.65f)
                        )
                        .border(
                            width = 1.8.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color.White,
                                    Color(0xFFFF4800)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3D",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkSurface) Color.White else Color(0xFF0A0C10)
                    )
                }
            }
        } else {
            // Preview Artwork for 2. EDITORIAL (Magazine Header + Hairline Grid + Inverted Dock)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDarkSurface) Color(0xFF0B0B0E) else Color(0xFFFBF9F5))
                    .border(
                        1.dp,
                        if (isDarkSurface) Color(0xFFF5F2EB).copy(alpha = 0.28f) else Color(0xFF111111).copy(alpha = 0.25f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(8.dp)
            ) {
                val inkColor = if (isDarkSurface) Color(0xFFF5F2EB) else Color(0xFF111111)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ISSUE 01",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = inkColor
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(inkColor.copy(alpha = 0.35f))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, inkColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(inkColor)
                        )
                        repeat(2) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(inkColor.copy(alpha = 0.45f))
                            )
                        }
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = numberBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BlackWhiteThemeSelectorCard(
    isDarkSelected: Boolean,
    onSelectBlack: () -> Unit,
    onSelectWhite: () -> Unit,
) {
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassSurface(
                shape = RoundedCornerShape(24.dp)
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Pilih Warna Dasar (Hitam / Putih)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Hanya tersedia warna dasar Hitam dan Putih. Dapat dikombinasikan dengan gaya Liquid Glass maupun Editorial.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Option 1: TEMA HITAM
            ThemeModeOptionCard(
                title = "Hitam",
                subtitle = "Hitam Solid #000000",
                previewBg = Color.Black,
                previewFg = Color.White,
                previewCard = Color(0xFF16181E),
                isSelected = isDarkSelected,
                isDarkSurface = isDarkSurface,
                onClick = onSelectBlack,
                modifier = Modifier
                    .weight(1f)
                    .testTag("theme_option_black")
            )

            // Option 2: TEMA PUTIH
            ThemeModeOptionCard(
                title = "Putih",
                subtitle = "Putih Bersih #FFFFFF",
                previewBg = Color.White,
                previewFg = Color(0xFF0A0C10),
                previewCard = Color(0xFFECEEF3),
                isSelected = !isDarkSelected,
                isDarkSurface = isDarkSurface,
                onClick = onSelectWhite,
                modifier = Modifier
                    .weight(1f)
                    .testTag("theme_option_white")
            )
        }
    }
}

@Composable
private fun ThemeModeOptionCard(
    title: String,
    subtitle: String,
    previewBg: Color,
    previewFg: Color,
    previewCard: Color,
    isSelected: Boolean,
    isDarkSurface: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "themeCardScale"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.2.dp else 1.dp,
        label = "themeCardBorderWidth"
    )
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDarkSurface) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)
    }

    val cardBg = if (isSelected) {
        if (isDarkSurface) Color(0xFF1A1D24) else Color(0xFFE8EBF2)
    } else {
        if (isDarkSurface) Color(0xFF121419) else Color(0xFFF4F5F8)
    }

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(borderWidth, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Mini visual preview of Hitam / Putih
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(previewBg)
                .border(
                    width = 1.dp,
                    color = if (previewBg.luminance() > 0.5f) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(previewFg)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(previewFg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = null,
                                tint = previewBg,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(previewCard)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(previewCard)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DynamicAlbumThemeCard(
    dynamicTheme: Boolean,
    onDynamicThemeChange: (Boolean) -> Unit,
    songTitle: String?,
    songArtist: String?,
    thumbnailUrl: String?,
    extractedColor: Color?,
) {
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val activeAccent = extractedColor ?: MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassSurface(
                shape = RoundedCornerShape(24.dp)
            )
            .clickable { onDynamicThemeChange(!dynamicTheme) }
            .padding(16.dp)
            .testTag("dynamic_album_theme_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val iconBadgeBg = if (dynamicTheme && extractedColor != null) {
                        extractedColor
                    } else {
                        if (isDarkSurface) Color(0xFF22252E) else Color(0xFFE2E5EC)
                    }
                    val iconBadgeTint = if (iconBadgeBg.luminance() > 0.55f) Color.Black else Color.White

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(iconBadgeBg)
                            .border(
                                1.dp,
                                if (isDarkSurface) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.14f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.palette),
                            contentDescription = null,
                            tint = iconBadgeTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Ikuti Warna Cover / Album",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (dynamicTheme) "Aktif (Warna Cover Lagu)" else "Nonaktif (Murni Hitam/Putih)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dynamicTheme) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = dynamicTheme,
                    onCheckedChange = onDynamicThemeChange,
                    modifier = Modifier.testTag("dynamic_album_theme_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (activeAccent.luminance() > 0.5f) Color.Black else Color.White,
                        checkedTrackColor = activeAccent
                    )
                )
            }

            Text(
                text = "Satu-satunya warna aksen tambahan: mengikuti warna cover/album lagu yang sedang diputar secara otomatis.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            if (thumbnailUrl != null && !songTitle.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f))
                        .border(
                            1.dp,
                            activeAccent.copy(alpha = 0.38f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = songTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = songTitle,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!songArtist.isNullOrBlank()) {
                            Text(
                                text = songArtist,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (extractedColor != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(extractedColor)
                                .border(
                                    1.5.dp,
                                    if (isDarkSurface) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.25f),
                                    CircleShape
                                )
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Putar lagu untuk mendeteksi warna cover otomatis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeMockupPortrait(
    isDark: Boolean,
    visualStyle: AppVisualStyle,
    themeColor: Color
) {
    val isEditorial = visualStyle == AppVisualStyle.EDITORIAL
    AuraMusicTheme(
        darkTheme = isDark,
        pureBlack = isDark,
        themeColor = themeColor,
        visualStyle = visualStyle.name
    ) {
        val borderTint = if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.18f)
        val itemShape = if (isEditorial) RoundedCornerShape(4.dp) else RoundedCornerShape(8.dp)
        val navShape = if (isEditorial) RoundedCornerShape(6.dp) else CircleShape

        Card(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(9f / 17.5f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!isEditorial) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    themeColor.copy(alpha = if (isDark) 0.24f else 0.14f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.25f, size.height * 0.2f),
                                radius = size.width * 0.75f
                            ),
                            center = Offset(size.width * 0.25f, size.height * 0.2f),
                            radius = size.width * 0.75f
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mock Dynamic Island Pill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(14.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(if (isEditorial) RoundedCornerShape(4.dp) else CircleShape)
                            .background(Color(0xFF101216))
                            .border(
                                0.8.dp,
                                borderTint,
                                if (isEditorial) RoundedCornerShape(4.dp) else CircleShape
                            )
                    )

                    // Mock Hero Card + Grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isEditorial) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(borderTint)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(itemShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        )
                                    )
                                )
                                .border(0.8.dp, borderTint, itemShape)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(itemShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(0.6.dp, borderTint, itemShape)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(itemShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(0.6.dp, borderTint, itemShape)
                            )
                        }
                    }

                    // Mock MiniPlayer + 3-Item Navbar
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(itemShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
                                .border(0.6.dp, borderTint, itemShape)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(22.dp)
                                .clip(navShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .border(0.8.dp, borderTint, navShape)
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(26.dp)
                                    .fillMaxHeight()
                                    .clip(navShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .then(
                                        if (!isEditorial) {
                                            Modifier.border(
                                                width = 1.dp,
                                                brush = Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF00E5FF),
                                                        Color.White,
                                                        Color(0xFFFF4800)
                                                    )
                                                ),
                                                shape = navShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                            )
                            repeat(2) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(navShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassWidgetShowcaseCard(
    songTitle: String,
    songArtist: String,
    thumbnailUrl: String?,
) {
    val context = LocalContext.current

    fun requestPinWidget(receiverClass: Class<*>, label: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val provider = android.content.ComponentName(context, receiverClass)
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val pinned = appWidgetManager.requestPinAppWidget(provider, null, null)
                if (!pinned) {
                    android.widget.Toast.makeText(
                        context,
                        "Tambahkan widget '$label' melalui menu Widget di Layar Utama",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Launcher tidak mendukung pin otomatis. Tekan lama Layar Utama > Widget > $label",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        } else {
            android.widget.Toast.makeText(
                context,
                "Tekan lama Layar Utama > Widget > $label",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedGlassSurface(shape = RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Widget Layar Utama — Liquid Glass",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0x3864E9FF), Color(0x388B5CF6))
                            )
                        )
                        .border(0.8.dp, Color(0x8864E9FF), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "3D GLASS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF64E9FF)
                    )
                }
            }
            Text(
                text = "Tersedia 2 varian widget 3D Liquid Glass transparan dengan efek refraksi sampul lagu, kubah kilau kaca, dan rim prismatik.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        // Preview 1: Liquid Glass Player (4x2) — Ultra-clear crystal glass
        val onSurfaceText = MaterialTheme.colorScheme.onSurface
        val onSurfaceSub = MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color(0xFF64E9FF).copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.92f),
                            Color(0xFF64E9FF).copy(alpha = 0.68f),
                            Color.White.copy(alpha = 0.30f),
                            Color(0xFFA78BFA).copy(alpha = 0.62f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable {
                    requestPinWidget(
                        com.nanzbeatles.nanas.widget.LiquidGlassWidgetReceiver::class.java,
                        "Liquid Glass Player"
                    )
                }
                .testTag("pin_liquid_glass_player_widget")
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .border(1.2.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                tint = Color(0xFF64E9FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = songTitle,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = onSurfaceText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "4×2 BENING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF64E9FF)
                            )
                        }
                        Text(
                            text = songArtist,
                            style = MaterialTheme.typography.labelSmall,
                            color = onSurfaceSub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(onSurfaceText.copy(alpha = 0.16f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.62f)
                                    .fillMaxHeight()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.White, Color(0xFF64E9FF), Color(0xFFA78BFA))
                                        )
                                    )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ketuk kartu ini untuk pasang Widget 4×2",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceSub
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+ Pasang 4×2",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = onSurfaceText
                        )
                    }
                }
            }
        }

        // Preview 2: Liquid Glass Pill (4x1 Wide) — Ultra-clear crystal glass
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color(0xFF64E9FF).copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.92f),
                            Color(0xFF64E9FF).copy(alpha = 0.68f),
                            Color.White.copy(alpha = 0.30f),
                            Color(0xFFA78BFA).copy(alpha = 0.62f)
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
                .clickable {
                    requestPinWidget(
                        com.nanzbeatles.nanas.widget.LiquidGlassWideWidgetReceiver::class.java,
                        "Liquid Glass Pill"
                    )
                }
                .testTag("pin_liquid_glass_pill_widget")
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .border(1.5.dp, Color(0xFF64E9FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            tint = onSurfaceText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Liquid Glass Pill (4×1)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = onSurfaceText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Kapsul kaca air super bening",
                        style = MaterialTheme.typography.labelSmall,
                        color = onSurfaceSub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "+ Pasang 4×1",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = onSurfaceText
                    )
                }
            }
        }
    }
}

