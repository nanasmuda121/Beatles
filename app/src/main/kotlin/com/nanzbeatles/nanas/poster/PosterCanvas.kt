/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.poster

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nanzbeatles.nanas.R
import kotlin.math.roundToInt

private fun fmtTime(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%02d.%02d".format(s / 60, s % 60)
}

/**
 * Kartu pemutar musik Mode 1 (100% mirip referensi kartu kaca akrilik Beatles):
 * - Kaca akrilik gelap transparan (smoked glass acrylic) dengan border putih glowing + rim dalam
 * - Cover album di kiri dengan bingkai putih membulat glowing
 * - Label brand gelombang (~) + "Beatles" + judul tebal + artis + ikon hati (♡)
 * - Slider progress dengan titik bulatan putih (thumb) + durasi MM:SS
 * - Tombol kontrol pemutar lengkap: Shuffle, Rewind, Pause, FastForward, Repeat
 * - Baris pengatur volume di bagian bawah (VolumeMute - bar slider - VolumeUp)
 * - Kilau bintang (starflare) di sudut & tepi kartu
 */
@Composable
fun PosterCard(
    card: PosterCardData,
    widthPx: Float,
    sparkle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    fun sp(f: Float) = with(density) { (widthPx * f).toSp() }
    fun dp(f: Float): Dp = with(density) { (widthPx * f).toDp() }

    val baseColor = card.color?.let { Color(it) }
    val cardBg = if (baseColor != null) {
        baseColor.copy(alpha = 0.88f)
    } else {
        Color(0xE0111216) // Smoked glass acrylic transparan elegan sesuai referensi
    }
    val textColor = Color.White
    val cornerRadius = dp(0.082f)
    val shape = RoundedCornerShape(cornerRadius)
    val total = card.song.durationSec
    val elapsed = (total * card.progress).roundToInt()

    Box(modifier = modifier.fillMaxSize()) {
        // Layer ketebalan bezel kaca 3D di belakang kartu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = dp(0.008f), y = dp(-0.006f))
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFB8BDC7).copy(alpha = 0.70f),
                            Color(0xFF565A62).copy(alpha = 0.65f),
                            Color(0xFF2B2E34).copy(alpha = 0.70f),
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(dp(0.035f), shape, clip = false)
                .clip(shape)
                .background(cardBg)
                .border(
                    BorderStroke(
                        dp(0.009f),
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.98f),
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.96f),
                            )
                        )
                    ),
                    shape
                )
                .padding(horizontal = dp(0.046f), vertical = dp(0.034f)),
        ) {
            // Efek pantulan cahaya kaca diagonal + rim cahaya dalam
            Canvas(Modifier.fillMaxSize()) {
                val sheen = Path().apply {
                    moveTo(size.width * 0.14f, 0f)
                    lineTo(size.width * 0.56f, 0f)
                    lineTo(size.width * 0.24f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = sheen,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent,
                        ),
                    )
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Baris atas: Cover + Info Lagu + Heart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Cover dengan frame putih glowing
                    Box(
                        modifier = Modifier
                            .size(dp(0.225f))
                            .clip(RoundedCornerShape(dp(0.042f)))
                            .border(
                                BorderStroke(dp(0.0065f), Color.White.copy(alpha = 0.94f)),
                                RoundedCornerShape(dp(0.042f))
                            )
                            .background(Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val cover = card.cover
                        if (cover != null) {
                            Image(
                                bitmap = cover.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(dp(0.105f)),
                            )
                        }
                    }

                    Spacer(Modifier.width(dp(0.028f)))

                    Column(Modifier.weight(1f)) {
                        // Badge gelombang ~ Beatles
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandWaveIcon(
                                color = textColor.copy(alpha = 0.80f),
                                modifier = Modifier.size(width = dp(0.036f), height = dp(0.018f)),
                            )
                            Spacer(Modifier.width(dp(0.012f)))
                            Text(
                                text = "Beatles",
                                color = textColor.copy(alpha = 0.80f),
                                fontSize = sp(0.034f),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Text(
                            text = card.song.title,
                            color = textColor,
                            fontSize = sp(0.052f),
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = card.song.artist,
                            color = textColor.copy(alpha = 0.74f),
                            fontSize = sp(0.038f),
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Icon(
                        Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.92f),
                        modifier = Modifier.size(dp(0.062f)),
                    )
                }

                // Progress bar dengan thumb bulat & timestamps
                Column(modifier = Modifier.fillMaxWidth()) {
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(dp(0.030f))
                    ) {
                        val y = size.height / 2f
                        val stroke = size.height * 0.22f
                        drawLine(
                            textColor.copy(alpha = 0.28f),
                            Offset(0f, y),
                            Offset(size.width, y),
                            stroke,
                            StrokeCap.Round,
                        )
                        val x = size.width * card.progress.coerceIn(0f, 1f)
                        drawLine(
                            textColor,
                            Offset(0f, y),
                            Offset(x, y),
                            stroke,
                            StrokeCap.Round,
                        )
                        drawCircle(
                            textColor,
                            radius = size.height * 0.38f,
                            center = Offset(x, y),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = fmtMs(elapsed),
                            color = textColor.copy(alpha = 0.78f),
                            fontSize = sp(0.033f),
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = fmtMs(total),
                            color = textColor.copy(alpha = 0.78f),
                            fontSize = sp(0.033f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                // Tombol kontrol pemutar lengkap: Shuffle, FastRewind, Pause, FastForward, Repeat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dp(0.02f)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.78f),
                        modifier = Modifier.size(dp(0.058f)),
                    )
                    Icon(
                        Icons.Rounded.FastRewind,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(dp(0.078f)),
                    )
                    Icon(
                        Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(dp(0.102f)),
                    )
                    Icon(
                        Icons.Rounded.FastForward,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(dp(0.078f)),
                    )
                    Icon(
                        Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.78f),
                        modifier = Modifier.size(dp(0.058f)),
                    )
                }

                // Baris pengatur volume bawah (VolumeMute - slider thumb bulat - VolumeUp) sesuai referensi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.VolumeMute,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.80f),
                        modifier = Modifier.size(dp(0.042f)),
                    )
                    Canvas(
                        Modifier
                            .weight(1f)
                            .height(dp(0.024f))
                            .padding(horizontal = dp(0.016f))
                    ) {
                        val y = size.height / 2f
                        val stroke = size.height * 0.22f
                        drawLine(
                            textColor.copy(alpha = 0.28f),
                            Offset(0f, y),
                            Offset(size.width, y),
                            stroke,
                            StrokeCap.Round,
                        )
                        val volX = size.width * 0.64f
                        drawLine(
                            textColor.copy(alpha = 0.92f),
                            Offset(0f, y),
                            Offset(volX, y),
                            stroke,
                            StrokeCap.Round,
                        )
                        drawCircle(
                            color = textColor,
                            radius = size.height * 0.40f,
                            center = Offset(volX, y),
                        )
                    }
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.80f),
                        modifier = Modifier.size(dp(0.042f)),
                    )
                }
            }
        }

        // Pantulan cahaya optik bercahaya lembut (lebih glow, agak transparan, dan sedikit lebih besar)
        if (sparkle) {
            val flareSize = dp(0.185f)
            val half = flareSize / 2
            StarFlare(
                rimAngleDeg = -35f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = half - dp(0.016f), y = dp(0.012f) - half)
                    .size(flareSize)
            )
            StarFlare(
                rimAngleDeg = 45f,
                intensity = 0.78f,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = dp(0.018f) - half, y = half - dp(0.012f))
                    .size(flareSize * 0.84f)
            )
        }
    }
}

@Composable
private fun BrandWaveIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path().apply {
            val hMid = size.height * 0.55f
            val amp = size.height * 0.44f
            moveTo(0f, hMid)
            cubicTo(
                size.width * 0.22f, hMid + amp,
                size.width * 0.36f, hMid - amp,
                size.width * 0.58f, hMid - amp * 0.35f,
            )
            quadraticTo(
                size.width * 0.78f, hMid + amp * 0.15f,
                size.width, hMid - amp * 0.25f,
            )
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = size.height * 0.21f, cap = StrokeCap.Round),
        )
    }
}

private fun fmtMs(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * Kartu pemutar vertikal Mode 2 (100% mirip referensi Mode 2 & kartu akrilik Beatles):
 * - Frame kaca akrilik metalik 3D gelap dengan ketebalan bezel 3D di sisi luar & kilau kaca diagonal
 * - Cover besar 1:1 membulat dengan border putih glowing + kilau bintang di sudut bingkai cover
 * - Baris brand: ikon gelombang (~) + "Beatles"
 * - Judul tebal + artis + ikon hati outline di kanan
 * - Slider progress dengan titik bulatan putih + waktu elapsed & durasi
 * - Tombol Shuffle, FastRewind, Pause, FastForward, Repeat
 * - Slider volume di bagian paling bawah (VolumeMute - bar - VolumeUp)
 * - Kilau bintang (starflare 8-sudut) di sudut & tepi kartu yang menembus keluar batas kartu
 */
@Composable
fun PosterCardVertical(
    card: PosterCardData,
    widthPx: Float,
    sparkle: Boolean,
    rotY: Float = 0f,
    sparkleStyle: Int = 0,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    fun sp(f: Float) = with(density) { (widthPx * f).toSp() }
    fun dp(f: Float): Dp = with(density) { (widthPx * f).toDp() }

    val baseColor = card.color?.let { Color(it) }
    val cardBg = if (baseColor != null) {
        baseColor.copy(alpha = 0.88f)
    } else {
        Color(0xE0111216) // Translucent smoked acrylic glass persis referensi kartu Beatles
    }
    val textColor = Color.White
    val cornerRadius = dp(0.095f)
    val shape = RoundedCornerShape(cornerRadius)
    val total = card.song.durationSec
    val elapsed = (total * card.progress).roundToInt()

    // Offset ketebalan bezel 3D mengikuti arah kemiringan kartu (kiri vs kanan)
    val bezelShiftX = when {
        rotY > 2f -> dp(-0.018f)
        rotY < -2f -> dp(0.018f)
        else -> dp(0.012f)
    }
    val bezelShiftY = dp(-0.010f)

    Box(modifier = modifier.fillMaxSize()) {
        // Layer ketebalan sisi 3D akrilik metalik di belakang kartu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = bezelShiftX, y = bezelShiftY)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFC4C9D2),
                            Color(0xFF6B7079),
                            Color(0xFF34373D),
                        )
                    )
                )
                .border(
                    width = dp(0.004f),
                    color = Color.White.copy(alpha = 0.55f),
                    shape = shape,
                )
        )

        // Badan utama kartu kaca akrilik
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(dp(0.050f), shape, clip = false)
                .clip(shape)
                .background(cardBg)
                .border(
                    width = dp(0.011f),
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.98f),
                            Color.White.copy(alpha = 0.50f),
                            Color.White.copy(alpha = 0.96f),
                        )
                    ),
                    shape = shape,
                )
        ) {
            // Efek pantulan kaca diagonal lembut + inner glass rim
            Canvas(Modifier.fillMaxSize()) {
                val sheen = Path().apply {
                    moveTo(size.width * 0.14f, 0f)
                    lineTo(size.width * 0.64f, 0f)
                    lineTo(size.width * 0.20f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = sheen,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.11f),
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height * 0.65f),
                    )
                )

                // Garis pantulan dalam tipis (inner glass highlight)
                val inset = size.width * 0.016f
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.25f),
                        )
                    ),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    cornerRadius = CornerRadius(size.width * 0.082f, size.width * 0.082f),
                    style = Stroke(width = size.width * 0.003f),
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = dp(0.058f), vertical = dp(0.048f))
            ) {
                // Cover Album Besar dengan frame putih glowing persis referensi kartu Beatles.
                // Menggunakan weight(1f) agar area kontrol bawah (Pause, Prev, Next, Volume) selalu tampil 100% utuh dan tidak pernah terpotong.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(dp(0.055f)))
                            .border(
                                BorderStroke(dp(0.008f), Color.White.copy(alpha = 0.94f)),
                                RoundedCornerShape(dp(0.055f))
                            )
                            .background(Color(0x26FFFFFF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val cover = card.cover
                        if (cover != null) {
                            Image(
                                bitmap = cover.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(dp(0.26f)),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(dp(0.024f)))

                // Bagian bawah kartu: Brand + Judul/Artis/Heart + Progress Bar + Tombol Pause/Prev/Next + Volume Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dp(0.016f)),
                ) {
                    // 1. Baris "~ Beatles" + Judul + Artis + Ikon Hati
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BrandWaveIcon(
                                    color = textColor.copy(alpha = 0.82f),
                                    modifier = Modifier.size(width = dp(0.044f), height = dp(0.020f)),
                                )
                                Spacer(Modifier.width(dp(0.014f)))
                                Text(
                                    text = "Beatles",
                                    color = textColor.copy(alpha = 0.82f),
                                    fontSize = sp(0.038f),
                                    lineHeight = sp(0.042f),
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Spacer(Modifier.height(dp(0.004f)))
                            Text(
                                text = card.song.title,
                                color = textColor,
                                fontSize = sp(0.068f),
                                lineHeight = sp(0.074f),
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = card.song.artist,
                                color = textColor.copy(alpha = 0.78f),
                                fontSize = sp(0.045f),
                                lineHeight = sp(0.050f),
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(dp(0.018f)))
                        Icon(
                            Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.95f),
                            modifier = Modifier.size(dp(0.072f)),
                        )
                    }

                    // 2. Progress bar dengan titik thumb bulat + durasi kiri & kanan
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Canvas(
                            Modifier
                                .fillMaxWidth()
                                .height(dp(0.034f))
                        ) {
                            val y = size.height / 2f
                            val stroke = size.height * 0.24f
                            drawLine(
                                textColor.copy(alpha = 0.28f),
                                Offset(0f, y),
                                Offset(size.width, y),
                                stroke,
                                StrokeCap.Round,
                            )
                            val x = size.width * card.progress.coerceIn(0f, 1f)
                            drawLine(
                                textColor.copy(alpha = 0.96f),
                                Offset(0f, y),
                                Offset(x, y),
                                stroke,
                                StrokeCap.Round,
                            )
                            drawCircle(
                                textColor,
                                radius = size.height * 0.40f,
                                center = Offset(x, y),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = fmtMs(elapsed),
                                color = textColor.copy(alpha = 0.80f),
                                fontSize = sp(0.039f),
                                lineHeight = sp(0.043f),
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = fmtMs(total),
                                color = textColor.copy(alpha = 0.80f),
                                fontSize = sp(0.039f),
                                lineHeight = sp(0.043f),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    // 3. Tombol kontrol pemutar: Shuffle, Prev (<<), Pause (||), Next (>>), Repeat persis referensi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dp(0.012f), vertical = dp(0.004f)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.82f),
                            modifier = Modifier.size(dp(0.064f)),
                        )
                        PosterDoubleTriangleIcon(
                            isForward = false,
                            color = textColor,
                            modifier = Modifier.size(width = dp(0.105f), height = dp(0.068f)),
                        )
                        PosterPauseBarsIcon(
                            color = textColor,
                            modifier = Modifier.size(width = dp(0.080f), height = dp(0.102f)),
                        )
                        PosterDoubleTriangleIcon(
                            isForward = true,
                            color = textColor,
                            modifier = Modifier.size(width = dp(0.105f), height = dp(0.068f)),
                        )
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.82f),
                            modifier = Modifier.size(dp(0.064f)),
                        )
                    }

                    // 4. Baris pengatur volume bawah (VolumeMute - bar slider dengan thumb bulat - VolumeUp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.VolumeMute,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.84f),
                            modifier = Modifier.size(dp(0.050f)),
                        )
                        Canvas(
                            Modifier
                                .weight(1f)
                                .height(dp(0.028f))
                                .padding(horizontal = dp(0.016f))
                        ) {
                            val y = size.height / 2f
                            val stroke = size.height * 0.24f
                            drawLine(
                                textColor.copy(alpha = 0.28f),
                                Offset(0f, y),
                                Offset(size.width, y),
                                stroke,
                                StrokeCap.Round,
                            )
                            val volX = size.width * 0.65f
                            drawLine(
                                textColor.copy(alpha = 0.94f),
                                Offset(0f, y),
                                Offset(volX, y),
                                stroke,
                                StrokeCap.Round,
                            )
                            drawCircle(
                                color = textColor,
                                radius = size.height * 0.42f,
                                center = Offset(volX, y),
                            )
                        }
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.84f),
                            modifier = Modifier.size(dp(0.050f)),
                        )
                    }
                }
            }
        }

        // Pantulan cahaya optik realistis di tepi bezel kaca (lebih glow, agak transparan, dan sedikit lebih besar)
        if (sparkle) {
            val flareSize = dp(0.26f)
            val half = flareSize / 2
            when (sparkleStyle % 3) {
                1 -> {
                    // Sudut kiri-atas bezel
                    StarFlare(
                        rimAngleDeg = 38f,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = dp(0.022f) - half, y = dp(0.018f) - half)
                            .size(flareSize)
                    )
                }
                2 -> {
                    // Sudut kanan-atas + pantulan halus di tepi kanan-bawah bezel
                    StarFlare(
                        rimAngleDeg = -38f,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = half - dp(0.022f), y = dp(0.018f) - half)
                            .size(flareSize)
                    )
                    StarFlare(
                        rimAngleDeg = 90f,
                        intensity = 0.82f,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = half - dp(0.008f), y = -dp(0.28f))
                            .size(flareSize * 0.86f)
                    )
                }
                else -> {
                    // Sudut kanan-atas bezel
                    StarFlare(
                        rimAngleDeg = -38f,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = half - dp(0.022f), y = dp(0.018f) - half)
                            .size(flareSize)
                    )
                }
            }
        }
    }
}

/** Ikon Pause (||) berupa 2 batang vertikal tebal membulat persis gambar referensi kartu Beatles. */
@Composable
private fun PosterPauseBarsIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val barW = size.width * 0.34f
        val barH = size.height * 0.92f
        val topY = (size.height - barH) / 2f
        val radius = CornerRadius(barW * 0.32f, barW * 0.32f)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.06f, topY),
            size = Size(barW, barH),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - size.width * 0.06f - barW, topY),
            size = Size(barW, barH),
            cornerRadius = radius,
        )
    }
}

/** Ikon Prev (<<) dan Next (>>) berupa 2 segitiga ganda tebal membulat persis gambar referensi kartu Beatles. */
@Composable
private fun PosterDoubleTriangleIcon(
    isForward: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val midY = h * 0.5f
        val topY = h * 0.08f
        val botY = h * 0.92f

        fun drawTriangle(xTip: Float, xBase: Float) {
            val path = Path().apply {
                moveTo(xTip, midY)
                lineTo(xBase, topY)
                lineTo(xBase, botY)
                close()
            }
            drawPath(path = path, color = color)
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = h * 0.12f, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            )
        }

        if (isForward) {
            // Segitiga kiri & kanan menghadap ke kanan (>>)
            drawTriangle(xTip = w * 0.50f, xBase = w * 0.08f)
            drawTriangle(xTip = w * 0.92f, xBase = w * 0.50f)
        } else {
            // Segitiga kiri & kanan menghadap ke kiri (<<)
            drawTriangle(xTip = w * 0.08f, xBase = w * 0.50f)
            drawTriangle(xTip = w * 0.50f, xBase = w * 0.92f)
        }
    }
}

/**
 * Pantulan cahaya optik realistis pada tepi kaca (optical specular rim reflection & lens diffraction):
 * - Menyatu dengan garis tepi kaca melalui bias cahaya sepanjang rim (bukan tempelan putih solid)
 * - Setiap berkas sinar memudar halus dengan gradasi eksponensial menuju transparan (0% di ujung)
 * - Menggunakan BlendMode.Plus agar bercampur secara optik dengan warna kaca dan latar di belakangnya.
 */
@Composable
private fun StarFlare(
    modifier: Modifier = Modifier,
    rimAngleDeg: Float = -35f,
    intensity: Float = 1f,
) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rMain = size.minDimension * 0.49f
        val rDiag = rMain * 0.42f
        val waist = rMain * 0.028f
        val waistGlow = rMain * 0.078f
        val k = intensity.coerceIn(0.3f, 1.25f)

        // 1a. Halo bloom atmosferik luar yang luas, lembut, dan bercahaya (outer luminous glow)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.42f * k),
                    0.20f to Color(0xFFEAF4FF).copy(alpha = 0.26f * k),
                    0.48f to Color(0xFFD6EAFF).copy(alpha = 0.11f * k),
                    0.78f to Color(0xFFC4E2FF).copy(alpha = 0.035f * k),
                    1.0f to Color.Transparent,
                ),
                center = c,
                radius = rMain * 0.96f,
            ),
            radius = rMain * 0.96f,
            center = c,
            blendMode = androidx.compose.ui.graphics.BlendMode.Plus,
        )

        // 1b. Halo glow menengah di sekitar pusat pantulan
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.54f * k),
                    0.35f to Color(0xFFF0F8FF).copy(alpha = 0.28f * k),
                    0.72f to Color(0xFFDCEEFF).copy(alpha = 0.08f * k),
                    1.0f to Color.Transparent,
                ),
                center = c,
                radius = rMain * 0.46f,
            ),
            radius = rMain * 0.46f,
            center = c,
            blendMode = androidx.compose.ui.graphics.BlendMode.Plus,
        )

        // 2. Pantulan memanjang mengikuti sudut tepi bezel kaca (specular rim streak, semi-transparan & glowing)
        val rad = Math.toRadians(rimAngleDeg.toDouble())
        val cosA = kotlin.math.cos(rad).toFloat()
        val sinA = kotlin.math.sin(rad).toFloat()
        val rimLen = rMain * 0.52f
        val rimP1 = Offset(c.x - cosA * rimLen, c.y - sinA * rimLen)
        val rimP2 = Offset(c.x + cosA * rimLen, c.y + sinA * rimLen)
        drawLine(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.25f to Color(0xFFEAF4FF).copy(alpha = 0.22f * k),
                    0.50f to Color.White.copy(alpha = 0.58f * k),
                    0.75f to Color(0xFFEAF4FF).copy(alpha = 0.22f * k),
                    1.0f to Color.Transparent,
                ),
                start = rimP1,
                end = rimP2,
            ),
            start = rimP1,
            end = rimP2,
            strokeWidth = rMain * 0.068f,
            cap = StrokeCap.Round,
            blendMode = androidx.compose.ui.graphics.BlendMode.Plus,
        )

        // 3. 4 berkas difraksi diagonal semi-transparan yang memudar penuh di ujung
        val d = rDiag * 0.7071f
        val dWaist = waist * 0.65f
        val diagStar = Path().apply {
            moveTo(c.x - d, c.y - d)
            quadraticTo(c.x, c.y - dWaist, c.x + d, c.y - d)
            quadraticTo(c.x + dWaist, c.y, c.x + d, c.y + d)
            quadraticTo(c.x, c.y + dWaist, c.x - d, c.y + d)
            quadraticTo(c.x - dWaist, c.y, c.x - d, c.y - d)
            close()
        }
        drawPath(
            path = diagStar,
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.48f * k),
                    0.28f to Color.White.copy(alpha = 0.20f * k),
                    0.68f to Color(0xFFE6F2FF).copy(alpha = 0.05f * k),
                    1.0f to Color.Transparent,
                ),
                center = c,
                radius = rDiag,
            ),
            blendMode = androidx.compose.ui.graphics.BlendMode.Plus,
        )

        // 4a. Lapisan glow lembut di sepanjang 4 berkas sinar utama (soft ray bloom)
        val glowStar = Path().apply {
            moveTo(c.x, c.y - rMain * 0.94f)
            quadraticTo(c.x + waistGlow, c.y - waistGlow, c.x + rMain * 0.94f, c.y)
            quadraticTo(c.x + waistGlow, c.y + waistGlow, c.x, c.y + rMain * 0.94f)
            quadraticTo(c.x - waistGlow, c.y + waistGlow, c.x - rMain * 0.94f, c.y)
            quadraticTo(c.x - waistGlow, c.y - waistGlow, c.x, c.y - rMain * 0.94f)
            close()
        }
        drawPath(
            path = glowStar,
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFFF2F8FF).copy(alpha = 0.36f * k),
                    0.22f to Color(0xFFE6F2FF).copy(alpha = 0.20f * k),
                    0.55f to Color(0xFFD6EAFF).copy(alpha = 0.07f * k),
                    1.0f to Color.Transparent,
                ),
                center = c,
                radius = rMain * 0.94f,
            ),
            blendMode = androidx.compose.ui.graphics.BlendMode.Plus,
        )

        // 4b. 4 berkas difraksi utama (vertikal & horizontal) agak transparan dengan gradasi halus ke ujung
        val mainStar = Path().apply {
            moveTo(c.x, c.y - rMain)
            quadraticTo(c.x + waist, c.y - waist, c.x + rMain, c.y)
            quadraticTo(c.x + waist, c.y + waist, c.x, c.y + rMain)
            quadraticTo(c.x - waist, c.y + waist, c.x - rMain, c.y)
            quadraticTo(c.x - waist, c.y - waist, c.x, c.y - rMain)
            close()
        }
        drawPath(
            path = mainStar,
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.76f * k),
                    0.15f to Color.White.copy(alpha = 0.54f * k),
                    0.38f to Color(0xFFF2F8FF).copy(alpha = 0.24f * k),
                    0.70f to Color(0xFFDCEEFF).copy(alpha = 0.07f * k),
                    1.0f to Color.Transparent,
                ),
                center = c,
                radius = rMain,
            ),
            blendMode = androidx.compose.ui.graphics.BlendMode.Plus,
        )

        // 5. Inti cahaya titik pantul semi-transparan yang menyatu lembut (translucent glowing hot-spot)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.80f * k),
                    0.42f to Color.White.copy(alpha = 0.48f * k),
                    1.0f to Color.Transparent,
                ),
                center = c,
                radius = rMain * 0.14f,
            ),
            radius = rMain * 0.14f,
            center = c,
            blendMode = androidx.compose.ui.graphics.BlendMode.Plus,
        )
    }
}

private fun Color.luminance(): Float {
    val c = this.toArgb()
    val r = ((c shr 16) and 255) / 255f
    val g = ((c shr 8) and 255) / 255f
    val b = (c and 255) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

/** Transformasi elemen: posisi tengah, skala, rotasi, flip, dan kemiringan perspektif 3D. */
private fun Modifier.elementTransform(
    el: PosterElement,
    bw: Float,
    bh: Float,
    cw: Float,
    ch: Float,
): Modifier = this.graphicsLayer {
    translationX = el.cx * cw - bw / 2f
    translationY = el.cy * ch - bh / 2f
    scaleX = el.scale * (if (el.flipX) -1f else 1f)
    scaleY = el.scale
    rotationZ = el.rotation
    rotationX = el.rotX
    rotationY = el.rotY
    cameraDistance = 11f * density
}

/**
 * Isi poster (tanpa UI seleksi). Hasil export diambil dari composable ini lewat [captureLayer],
 * sehingga file yang disimpan identik dengan pratinjau.
 */
@Composable
fun PosterContent(
    cw: Float,
    ch: Float,
    elements: List<PosterElement>,
    cards: List<PosterCardData>,
    cutouts: List<Bitmap?>,
    backgrounds: List<Bitmap>,
    bgMode: BackgroundMode,
    solidColor: Int,
    gradient: Pair<Int, Int>,
    baseSize: (PosterElement, Float, Float) -> Pair<Float, Float>,
    modifier: Modifier = Modifier,
    captureLayer: GraphicsLayer? = null,
    mode: PosterMode = PosterMode.COLLAGE,
    sparkle: Boolean = true,
) {
    val density = LocalDensity.current
    val captureModifier = if (captureLayer != null) {
        Modifier.drawWithContent {
            captureLayer.record { this@drawWithContent.drawContent() }
            drawLayer(captureLayer)
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(captureModifier)
            .clipToBounds()
            .background(Color(0xFF101217)),
    ) {
        when (bgMode) {
            BackgroundMode.SOLID -> Box(Modifier.fillMaxSize().background(Color(solidColor)))
            BackgroundMode.GRADIENT -> Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(gradient.first), Color(gradient.second)))
                )
            )
            BackgroundMode.PHOTO -> Unit
        }

        elements.forEach { el ->
            key(el.id) {
                val (bw, bh) = baseSize(el, cw, ch)
                val wDp = with(density) { bw.toDp() }
                val hDp = with(density) { bh.toDp() }
                Box(
                    modifier = Modifier
                        .requiredSize(wDp, hDp)
                        .elementTransform(el, bw, bh, cw, ch)
                ) {
                    when (el.kind) {
                        ElementKind.BACKGROUND -> backgrounds.getOrNull(el.refIndex)?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        ElementKind.CARD -> cards.getOrNull(el.refIndex)?.let {
                            if (mode == PosterMode.PHOTO3D) {
                                PosterCardVertical(
                                    card = it,
                                    widthPx = bw,
                                    sparkle = sparkle,
                                    rotY = el.rotY,
                                    sparkleStyle = el.sparkleStyle,
                                )
                            } else {
                                PosterCard(
                                    card = it,
                                    widthPx = bw,
                                    sparkle = sparkle,
                                )
                            }
                        }
                        ElementKind.CUTOUT -> cutouts.getOrNull(el.refIndex)?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                contentScale = if (el.fill) ContentScale.Crop else ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        ElementKind.STICKER -> {
                            val resId = STAR_STICKER_RES.getOrNull(el.refIndex) ?: STAR_STICKER_RES[0]
                            Image(
                                painter = painterResource(resId),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }

        // Referensi Mode 1 tidak memakai watermark bawah; Referensi Mode 2 memiliki badge logo + nama aplikasi di bawah.
        if (mode == PosterMode.PHOTO3D) {
            WatermarkMode2(cw = cw, ch = ch)
        }
    }
}

/** Badge logo persegi membulat + teks tebal di bagian bawah Mode 2, 100% mirip referensi Mode 2. */
@Composable
private fun BoxScope.WatermarkMode2(cw: Float, ch: Float) {
    val density = LocalDensity.current
    val badgeDp = with(density) { (cw * 0.056f).toDp() }
    val padBottom = with(density) { (ch * 0.025f).toDp() }
    val textSp = with(density) { (cw * 0.048f).toSp() }
    val badgeShape = RoundedCornerShape(with(density) { (cw * 0.015f).toDp() })

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = padBottom),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(badgeDp)
                .shadow(with(density) { (cw * 0.01f).toDp() }, badgeShape, clip = false)
                .clip(badgeShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0E2233), Color(0xFF061018))
                    )
                )
                .border(with(density) { (cw * 0.002f).toDp() }, Color.White.copy(alpha = 0.22f), badgeShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(badgeDp * 0.72f),
            )
        }
        Spacer(Modifier.width(badgeDp * 0.28f))
        Text(
            text = "Beatles",
            color = Color.White,
            fontSize = textSp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.65f),
                    offset = Offset(0f, 3f),
                    blurRadius = 8f,
                )
            ),
        )
    }
}

/**
 * Permukaan editor: poster + overlay seleksi + gesture (tap pilih, drag/pinch/putar elemen terpilih).
 */
@Composable
fun PosterEditorSurface(
    vm: PosterViewModel,
    captureLayer: GraphicsLayer,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(vm.effectiveAspectRatio)
    ) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        val cutouts = vm.cutoutBitmaps

        PosterContent(
            cw = cw,
            ch = ch,
            elements = vm.elements,
            cards = vm.cards,
            cutouts = cutouts,
            backgrounds = vm.canvasBackgrounds,
            bgMode = vm.effectiveBgMode,
            solidColor = vm.solidColor,
            gradient = GRADIENT_BG_PRESETS[vm.gradientIndex.coerceIn(0, GRADIENT_BG_PRESETS.lastIndex)],
            baseSize = vm::baseSize,
            captureLayer = captureLayer,
            mode = vm.mode,
            sparkle = vm.sparkle,
        )

        // Overlay seleksi (di luar area capture, jadi tidak ikut ter-export)
        val sel = vm.selected
        if (sel != null) {
            val (bw, bh) = vm.baseSize(sel, cw, ch)
            Box(
                modifier = Modifier
                    .requiredSize(with(density) { bw.toDp() }, with(density) { bh.toDp() })
                    .elementTransform(sel, bw, bh, cw, ch)
                    .border(2.dp, Color(0xFF4FC3F7), RoundedCornerShape(6.dp))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cw, ch) {
                    detectTapGestures { off -> vm.select(vm.hitTest(off.x, off.y, cw, ch)) }
                }
                .pointerInput(cw, ch) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        vm.transformSelected(pan.x / cw, pan.y / ch, zoom, rotation)
                    }
                }
        )
    }
}
