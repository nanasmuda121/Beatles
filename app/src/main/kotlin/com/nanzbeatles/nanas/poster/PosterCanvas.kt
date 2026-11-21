/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.poster

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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

/** Kartu pemutar musik. Semua ukuran diturunkan dari [widthPx] supaya proporsional di ukuran poster apa pun. */
@Composable
fun PosterCard(card: PosterCardData, widthPx: Float, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    fun sp(f: Float) = with(density) { (widthPx * f).toSp() }
    fun dp(f: Float): Dp = with(density) { (widthPx * f).toDp() }

    val bg = Color(card.effectiveColor)
    val textColor = if (bg.luminance() > 0.6f) Color(0xFF111111) else Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(dp(0.13f)))
            .background(bg)
            .padding(horizontal = dp(0.05f), vertical = dp(0.045f)),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(dp(0.17f))
                    .clip(RoundedCornerShape(dp(0.02f)))
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
                        modifier = Modifier.size(dp(0.09f)),
                    )
                }
            }
            Spacer(Modifier.width(dp(0.04f)))
            Column(Modifier.weight(1f)) {
                Text(
                    text = card.song.title,
                    color = textColor,
                    fontSize = sp(0.062f),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = card.song.artist,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = sp(0.05f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(dp(0.011f))
                .clip(RoundedCornerShape(50))
                .background(textColor.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(card.progress.coerceIn(0.01f, 1f))
                    .fillMaxHeight()
                    .background(textColor.copy(alpha = 0.9f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val total = card.song.durationSec
            Text(
                text = fmtTime((total * card.progress).roundToInt()),
                color = textColor,
                fontSize = sp(0.046f),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = fmtTime(total),
                color = textColor,
                fontSize = sp(0.046f),
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dp(0.02f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AddCircleOutline, null, tint = textColor, modifier = Modifier.size(dp(0.085f)))
            Icon(Icons.Rounded.SkipPrevious, null, tint = textColor.copy(alpha = 0.8f), modifier = Modifier.size(dp(0.085f)))
            Icon(Icons.Rounded.Pause, null, tint = textColor, modifier = Modifier.size(dp(0.095f)))
            Icon(Icons.Rounded.SkipNext, null, tint = textColor.copy(alpha = 0.8f), modifier = Modifier.size(dp(0.085f)))
        }
    }
}


private fun fmtMs(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

/** Kartu pemutar vertikal bergaya referensi Mode 2: cover besar, judul, progress, kontrol, volume. */
@Composable
fun PosterCardVertical(
    card: PosterCardData,
    widthPx: Float,
    sparkle: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    fun sp(f: Float) = with(density) { (widthPx * f).toSp() }
    fun dp(f: Float): Dp = with(density) { (widthPx * f).toDp() }

    val base = Color(card.effectiveColor)
    val textColor = if (base.luminance() > 0.6f) Color(0xFF111111) else Color.White
    val shape = RoundedCornerShape(dp(0.10f))
    val total = card.song.durationSec
    val elapsed = (total * card.progress).roundToInt()

    Box(
        modifier = modifier
            .fillMaxSize()
            .shadow(dp(0.035f), shape, clip = false)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(base, Color.White, 0.10f), base)))
            .border(dp(0.012f), Color.White.copy(alpha = 0.30f), shape)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(dp(0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(dp(0.05f)))
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
                        modifier = Modifier.size(dp(0.3f)),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(dp(0.05f)),
                    )
                    Text(
                        text = "Beatles",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = sp(0.04f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = card.song.title,
                            color = textColor,
                            fontSize = sp(0.085f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = card.song.artist,
                            color = textColor.copy(alpha = 0.7f),
                            fontSize = sp(0.06f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(dp(0.075f)),
                    )
                }
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(dp(0.05f))
                ) {
                    val y = size.height / 2f
                    val stroke = size.height * 0.16f
                    drawLine(textColor.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
                    val x = size.width * card.progress.coerceIn(0f, 1f)
                    drawLine(textColor.copy(alpha = 0.9f), Offset(0f, y), Offset(x, y), stroke, StrokeCap.Round)
                    drawCircle(textColor, radius = size.height * 0.30f, center = Offset(x, y))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(fmtMs(elapsed), color = textColor.copy(alpha = 0.75f), fontSize = sp(0.05f))
                    Text("-" + fmtMs(total - elapsed), color = textColor.copy(alpha = 0.75f), fontSize = sp(0.05f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.FastRewind, null, tint = textColor, modifier = Modifier.size(dp(0.12f)))
                    Icon(Icons.Rounded.Pause, null, tint = textColor, modifier = Modifier.size(dp(0.14f)))
                    Icon(Icons.Rounded.FastForward, null, tint = textColor, modifier = Modifier.size(dp(0.12f)))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VolumeMute, null, tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(dp(0.06f)))
                    Canvas(
                        Modifier
                            .weight(1f)
                            .height(dp(0.04f))
                            .padding(horizontal = dp(0.02f))
                    ) {
                        val y = size.height / 2f
                        val stroke = size.height * 0.2f
                        drawLine(textColor.copy(alpha = 0.25f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
                        drawLine(textColor.copy(alpha = 0.85f), Offset(0f, y), Offset(size.width * 0.62f, y), stroke, StrokeCap.Round)
                    }
                    Icon(Icons.Rounded.VolumeUp, null, tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(dp(0.06f)))
                }
            }
        }

        if (sparkle) {
            Canvas(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(dp(0.2f))
            ) {
                val c = Offset(size.width * 0.62f, size.height * 0.38f)
                val r = size.minDimension * 0.34f
                val path = Path().apply {
                    moveTo(c.x, c.y - r)
                    quadraticTo(c.x, c.y, c.x + r, c.y)
                    quadraticTo(c.x, c.y, c.x, c.y + r)
                    quadraticTo(c.x, c.y, c.x - r, c.y)
                    quadraticTo(c.x, c.y, c.x, c.y - r)
                    close()
                }
                drawCircle(Color.White.copy(alpha = 0.25f), radius = r * 0.55f, center = c)
                drawPath(path, Color.White.copy(alpha = 0.95f))
            }
        }
    }
}

private fun Color.luminance(): Float {
    val c = this.toArgb()
    val r = ((c shr 16) and 255) / 255f
    val g = ((c shr 8) and 255) / 255f
    val b = (c and 255) / 255f
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

/** Transformasi elemen: posisi tengah, skala, rotasi, flip. Dipakai konten dan overlay seleksi. */
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
    cameraDistance = 14f * density
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
                            if (mode == PosterMode.PHOTO3D) PosterCardVertical(card = it, widthPx = bw, sparkle = sparkle)
                            else PosterCard(card = it, widthPx = bw)
                        }
                        ElementKind.CUTOUT -> cutouts.getOrNull(el.refIndex)?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                contentScale = if (el.fill) ContentScale.Crop else ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }

        Watermark(cw = cw, ch = ch, pill = mode == PosterMode.COLLAGE)
    }
}

/** Logo + watermark Beatles: selalu di paling bawah dan paling atas, tidak bisa diedit. */
@Composable
private fun BoxScope.Watermark(cw: Float, ch: Float, pill: Boolean) {
    val density = LocalDensity.current
    val iconDp = with(density) { (cw * 0.05f).toDp() }
    val padBottom = with(density) { (ch * 0.022f).toDp() }
    val textSp = with(density) { (cw * 0.034f).toSp() }
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = padBottom)
            .clip(RoundedCornerShape(50))
            .background(if (pill) Color.Black.copy(alpha = 0.35f) else Color.Transparent)
            .padding(horizontal = iconDp * 0.6f, vertical = iconDp * 0.3f)
            .alpha(0.9f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(iconDp),
        )
        Spacer(Modifier.width(iconDp * 0.35f))
        Text(
            text = "Beatles",
            color = Color.White,
            fontSize = textSp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Permukaan editor: poster + overlay seleksi + gesture (tap pilih, drag/pinch/putar elemen terpilih).
 * [onSize] melaporkan ukuran piksel kanvas.
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
