/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.poster

import android.graphics.Bitmap
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
 * Kartu pemutar musik Mode 1 (100% mirip referensi):
 * - Kaca akrilik gelap transparan (smoked glass acrylic) dengan border putih glowing
 * - Cover album di kiri dengan bingkai putih membulat
 * - Label brand "~ Beatles" + judul tebal + artis + ikon hati (♡)
 * - Slider progress dengan titik bulatan putih (thumb) + durasi MM:SS
 * - Tombol kontrol pemutar lengkap: Shuffle, Rewind, Pause, FastForward, Repeat
 */
@Composable
fun PosterCard(card: PosterCardData, widthPx: Float, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    fun sp(f: Float) = with(density) { (widthPx * f).toSp() }
    fun dp(f: Float): Dp = with(density) { (widthPx * f).toDp() }

    val baseColor = card.color?.let { Color(it) }
    val cardBg = if (baseColor != null) {
        baseColor.copy(alpha = 0.88f)
    } else {
        Color(0xDD121318) // Smoked glass acrylic transparan elegan
    }
    val textColor = Color.White
    val cornerRadius = dp(0.080f)
    val shape = RoundedCornerShape(cornerRadius)
    val total = card.song.durationSec
    val elapsed = (total * card.progress).roundToInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(cardBg)
            .border(
                BorderStroke(
                    dp(0.008f),
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.90f),
                        )
                    )
                ),
                shape
            )
            .padding(horizontal = dp(0.048f), vertical = dp(0.040f)),
    ) {
        // Efek pantulan cahaya kaca diagonal lembut
        Canvas(Modifier.fillMaxSize()) {
            val sheen = Path().apply {
                moveTo(size.width * 0.15f, 0f)
                lineTo(size.width * 0.55f, 0f)
                lineTo(size.width * 0.25f, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = sheen,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
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
                        .size(dp(0.24f))
                        .clip(RoundedCornerShape(dp(0.040f)))
                        .border(BorderStroke(dp(0.006f), Color.White.copy(alpha = 0.88f)), RoundedCornerShape(dp(0.040f)))
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
                            modifier = Modifier.size(dp(0.11f)),
                        )
                    }
                }

                Spacer(Modifier.width(dp(0.030f)))

                Column(Modifier.weight(1f)) {
                    // Badge ~ Beatles
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "~",
                            color = textColor.copy(alpha = 0.80f),
                            fontSize = sp(0.045f),
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(dp(0.010f)))
                        Text(
                            text = "Beatles",
                            color = textColor.copy(alpha = 0.80f),
                            fontSize = sp(0.038f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        text = card.song.title,
                        color = textColor,
                        fontSize = sp(0.054f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = card.song.artist,
                        color = textColor.copy(alpha = 0.70f),
                        fontSize = sp(0.040f),
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Icon(
                    Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.90f),
                    modifier = Modifier.size(dp(0.065f)),
                )
            }

            // Progress bar dengan thumb bulat & timestamps
            Column(modifier = Modifier.fillMaxWidth()) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(dp(0.035f))
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
                        fontSize = sp(0.036f),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = fmtMs(total),
                        color = textColor.copy(alpha = 0.78f),
                        fontSize = sp(0.036f),
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
                    tint = textColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(dp(0.068f)),
                )
                Icon(
                    Icons.Rounded.FastRewind,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(dp(0.085f)),
                )
                Icon(
                    Icons.Rounded.Pause,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(dp(0.115f)),
                )
                Icon(
                    Icons.Rounded.FastForward,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(dp(0.085f)),
                )
                Icon(
                    Icons.Rounded.Repeat,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.75f),
                    modifier = Modifier.size(dp(0.068f)),
                )
            }
        }
    }
}

private fun fmtMs(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/**
 * Kartu pemutar vertikal Mode 2 (100% mirip referensi Mode 2):
 * - Frame kaca metalik 3D gelap dengan ketebalan bezel 3D di sisi luar & kilau kaca diagonal
 * - Cover besar 1:1 membulat
 * - Baris brand: ikon gelombang (~) + "Beatles"
 * - Judul tebal + artis + ikon hati outline di kanan
 * - Slider progress dengan titik bulatan putih + waktu elapsed & -remaining
 * - Tombol FastRewind, Pause, FastForward
 * - Slider volume di bagian paling bawah (VolumeMute - bar - VolumeUp)
 * - Kilau bintang (starflare 8-sudut) di sudut kartu yang menembus keluar batas kartu
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
        Color(0xDD111216) // Translucent smoked acrylic glass persis referensi beatles_player_card.jpg
    }
    val textColor = Color.White
    val cornerRadius = dp(0.095f)
    val shape = RoundedCornerShape(cornerRadius)
    val total = card.song.durationSec
    val elapsed = (total * card.progress).roundToInt()

    // Offset ketebalan bezel 3D mengikuti arah kemiringan kartu (kiri vs kanan)
    val bezelShiftX = when {
        rotY > 2f -> dp(-0.015f)
        rotY < -2f -> dp(0.015f)
        else -> dp(0.010f)
    }
    val bezelShiftY = dp(-0.008f)

    Box(modifier = modifier.fillMaxSize()) {
        // Layer ketebalan sisi 3D metalik di belakang kartu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = bezelShiftX, y = bezelShiftY)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF9EA3AB),
                            Color(0xFF565A61),
                            Color(0xFF34373D),
                        )
                    )
                )
        )

        // Badan utama kartu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(dp(0.045f), shape, clip = false)
                .clip(shape)
                .background(cardBg)
                .border(
                    width = dp(0.010f),
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.95f),
                        )
                    ),
                    shape = shape,
                )
        ) {
            // Efek pantulan kaca diagonal lembut seperti referensi
            Canvas(Modifier.fillMaxSize()) {
                val sheen = Path().apply {
                    moveTo(size.width * 0.15f, 0f)
                    lineTo(size.width * 0.62f, 0f)
                    lineTo(size.width * 0.20f, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = sheen,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height * 0.6f),
                    )
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = dp(0.062f), vertical = dp(0.058f))
            ) {
                // Cover Album Besar dengan frame putih glowing persis beatles_player_card.jpg
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(dp(0.055f)))
                        .border(
                            BorderStroke(dp(0.007f), Color.White.copy(alpha = 0.90f)),
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
                            modifier = Modifier.size(dp(0.28f)),
                        )
                    }
                }

                Spacer(Modifier.height(dp(0.032f)))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Baris "~ Beatles" + Judul + Artis + Ikon Hati
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Ikon gelombang (~) persis seperti di referensi
                            Canvas(Modifier.size(width = dp(0.046f), height = dp(0.024f))) {
                                val path = Path().apply {
                                    val hMid = size.height * 0.55f
                                    val amp = size.height * 0.42f
                                    moveTo(0f, hMid)
                                    cubicTo(
                                        size.width * 0.22f, hMid + amp,
                                        size.width * 0.35f, hMid - amp,
                                        size.width * 0.58f, hMid - amp * 0.4f,
                                    )
                                    quadraticTo(
                                        size.width * 0.78f, hMid,
                                        size.width, hMid - amp * 0.25f,
                                    )
                                }
                                drawPath(
                                    path = path,
                                    color = textColor.copy(alpha = 0.78f),
                                    style = Stroke(width = size.height * 0.20f, cap = StrokeCap.Round),
                                )
                            }
                            Spacer(Modifier.width(dp(0.018f)))
                            Text(
                                text = "Beatles",
                                color = textColor.copy(alpha = 0.78f),
                                fontSize = sp(0.038f),
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        Spacer(Modifier.height(dp(0.008f)))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = card.song.title,
                                    color = textColor,
                                    fontSize = sp(0.074f),
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = card.song.artist,
                                    color = textColor.copy(alpha = 0.74f),
                                    fontSize = sp(0.050f),
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(dp(0.02f)))
                            Icon(
                                Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.90f),
                                modifier = Modifier.size(dp(0.072f)),
                            )
                        }
                    }

                    // Progress bar dengan titik thumb bulat + durasi kiri & kanan
                    Column {
                        Canvas(
                            Modifier
                                .fillMaxWidth()
                                .height(dp(0.042f))
                        ) {
                            val y = size.height / 2f
                            val stroke = size.height * 0.19f
                            drawLine(
                                textColor.copy(alpha = 0.28f),
                                Offset(0f, y),
                                Offset(size.width, y),
                                stroke,
                                StrokeCap.Round,
                            )
                            val x = size.width * card.progress.coerceIn(0f, 1f)
                            drawLine(
                                textColor.copy(alpha = 0.95f),
                                Offset(0f, y),
                                Offset(x, y),
                                stroke,
                                StrokeCap.Round,
                            )
                            drawCircle(
                                textColor,
                                radius = size.height * 0.34f,
                                center = Offset(x, y),
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = fmtMs(elapsed),
                                color = textColor.copy(alpha = 0.76f),
                                fontSize = sp(0.044f),
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = fmtMs(total),
                                color = textColor.copy(alpha = 0.76f),
                                fontSize = sp(0.044f),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    // Tombol kontrol pemutar (Shuffle, <<, ||, >>, Repeat) persis sesuai beatles_player_card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dp(0.015f)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(dp(0.075f)),
                        )
                        Icon(
                            Icons.Rounded.FastRewind,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(dp(0.098f)),
                        )
                        Icon(
                            Icons.Rounded.Pause,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(dp(0.120f)),
                        )
                        Icon(
                            Icons.Rounded.FastForward,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(dp(0.098f)),
                        )
                        Icon(
                            Icons.Rounded.Repeat,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(dp(0.075f)),
                        )
                    }

                    // Baris pengatur volume (VolumeMute - bar slider dengan thumb bulat - VolumeUp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.VolumeMute,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(dp(0.052f)),
                        )
                        Canvas(
                            Modifier
                                .weight(1f)
                                .height(dp(0.032f))
                                .padding(horizontal = dp(0.018f))
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
                            val volX = size.width * 0.65f
                            drawLine(
                                textColor.copy(alpha = 0.90f),
                                Offset(0f, y),
                                Offset(volX, y),
                                stroke,
                                StrokeCap.Round,
                            )
                            drawCircle(
                                color = textColor,
                                radius = size.height * 0.38f,
                                center = Offset(volX, y),
                            )
                        }
                        Icon(
                            Icons.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(dp(0.052f)),
                        )
                    }
                }
            }
        }

        // Kilau bintang (Starflare) di sudut kartu persis seperti beatles_player_card.jpg
        if (sparkle) {
            val flareSize = dp(0.24f)
            val half = flareSize / 2
            // Kiri-atas
            StarFlare(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = dp(0.015f) - half, y = dp(0.015f) - half)
                    .size(flareSize * 1.1f)
            )
            // Kanan-atas
            StarFlare(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = half - dp(0.015f), y = dp(0.015f) - half)
                    .size(flareSize)
            )
            // Kiri-bawah
            StarFlare(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = dp(0.015f) - half, y = half - dp(0.015f))
                    .size(flareSize * 0.9f)
            )
            // Kanan-bawah
            StarFlare(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = half - dp(0.015f), y = half - dp(0.020f))
                    .size(flareSize * 1.15f)
            )
        }
    }
}

/** Kilau bintang kamera (lens starflare) dengan halo lembut + 4 sinar utama tajam + 4 sinar diagonal. */
@Composable
private fun StarFlare(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rMain = size.minDimension * 0.48f
        val rDiag = rMain * 0.52f
        val core = rMain * 0.085f

        // Halo cahaya lembut
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.25f),
                    Color.Transparent,
                ),
                center = c,
                radius = rMain * 0.55f,
            ),
            radius = rMain * 0.55f,
            center = c,
        )

        // 4 sinar diagonal tipis
        val d = rDiag * 0.7071f
        drawLine(Color.White.copy(alpha = 0.55f), Offset(c.x - d, c.y - d), Offset(c.x + d, c.y + d), strokeWidth = core * 0.45f, cap = StrokeCap.Round)
        drawLine(Color.White.copy(alpha = 0.55f), Offset(c.x - d, c.y + d), Offset(c.x + d, c.y - d), strokeWidth = core * 0.45f, cap = StrokeCap.Round)

        // 4 sinar utama tajam (vertikal & horizontal)
        val mainStar = Path().apply {
            moveTo(c.x, c.y - rMain)
            quadraticTo(c.x + core * 0.35f, c.y - core * 0.35f, c.x + rMain, c.y)
            quadraticTo(c.x + core * 0.35f, c.y + core * 0.35f, c.x, c.y + rMain)
            quadraticTo(c.x - core * 0.35f, c.y + core * 0.35f, c.x - rMain, c.y)
            quadraticTo(c.x - core * 0.35f, c.y - core * 0.35f, c.x, c.y - rMain)
            close()
        }
        drawPath(mainStar, Color.White.copy(alpha = 0.96f))
        drawCircle(Color.White, radius = core * 0.95f, center = c)
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
                                PosterCard(card = it, widthPx = bw)
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
            .aspectRatio(vm.aspect.ratio)
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
