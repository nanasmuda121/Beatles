/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.Manifest
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioVisualizerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    enabled: Boolean = true,
    audioSessionId: Int = 0,
    isPlaying: Boolean = true,
    trackHeight: Dp = 6.dp,
    onPermissionRequired: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val rangeSpan = (valueRange.endInclusive - valueRange.start).let { if (it <= 0f) 1f else it }
    val normalizedValue = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    var waveformData by remember { mutableStateOf<ByteArray?>(null) }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val currentValueRange by rememberUpdatedState(valueRange)

    val displayValue = if (isDragging) dragValue else normalizedValue

    DisposableEffect(audioSessionId) {
        if (audioSessionId == 0) {
            return@DisposableEffect onDispose { }
        }

        val hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            onPermissionRequired?.invoke()
            return@DisposableEffect onDispose { }
        }

        val visualizerInstance = try {
            Visualizer(audioSessionId).apply {
                this.enabled = false
                val captureRange = Visualizer.getCaptureSizeRange()
                if (captureRange != null && captureRange.size >= 2) {
                    this.captureSize = captureRange[1].coerceAtMost(256)
                }
            }
        } catch (e: Exception) {
            return@DisposableEffect onDispose { }
        }

        try {
            visualizerInstance.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveformData = waveform?.copyOf()
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                    }
                },
                Visualizer.getMaxCaptureRate() / 2,
                true,
                false
            )
            visualizerInstance.enabled = true
        } catch (_: Exception) {
        }

        onDispose {
            try {
                visualizerInstance.enabled = false
                visualizerInstance.release()
            } catch (_: Exception) {
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val playAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.15f,
        animationSpec = tween(durationMillis = 300),
        label = "playAmplitude"
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor
    val thumbColor = colors.thumbColor

    val baseModifier = modifier
        .fillMaxWidth()
        .height(40.dp)

    val interactiveModifier = if (enabled) {
        baseModifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val newValue = (offset.x / width).coerceIn(0f, 1f)
                    val range = currentValueRange
                    val mappedValue = range.start + newValue * (range.endInclusive - range.start)
                    dragValue = newValue
                    currentOnValueChange(mappedValue)
                    currentOnValueChangeFinished?.invoke()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        dragValue = (offset.x / width).coerceIn(0f, 1f)
                        val range = currentValueRange
                        val mappedValue = range.start + dragValue * (range.endInclusive - range.start)
                        currentOnValueChange(mappedValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        currentOnValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        currentOnValueChangeFinished?.invoke()
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val width = size.width.toFloat().coerceAtLeast(1f)
                        dragValue = (dragValue + dragAmount / width).coerceIn(0f, 1f)
                        val range = currentValueRange
                        val mappedValue = range.start + dragValue * (range.endInclusive - range.start)
                        currentOnValueChange(mappedValue)
                    }
                )
            }
    } else {
        baseModifier
    }

    Box(
        modifier = interactiveModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(38.dp)) {
            val width = size.width
            val centerY = size.height / 2f
            val barHeight = trackHeight.toPx()
            val progressWidth = (width * displayValue).coerceIn(0f, width)

            // Draw spectrum equalizer bars + ocean wave directly anchored to centerY
            drawVisualizerBarsAndWave(
                width = width,
                centerY = centerY,
                barHeight = barHeight,
                progress = displayValue,
                waveOffset = waveOffset,
                waveformData = waveformData,
                amplitudeScale = playAmplitude,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )

            // Base progress pill
            drawSmoothProgressBar(
                width = width,
                centerY = centerY,
                barHeight = barHeight * 0.75f,
                progress = displayValue,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )

            if (isDragging || enabled) {
                val thumbRadius = barHeight * 1.15f
                val clampedX = progressWidth.coerceIn(thumbRadius, (width - thumbRadius).coerceAtLeast(thumbRadius))
                drawCircle(
                    color = thumbColor.copy(alpha = 0.3f),
                    radius = thumbRadius * 1.45f,
                    center = Offset(clampedX, centerY)
                )
                drawCircle(
                    color = thumbColor,
                    radius = thumbRadius,
                    center = Offset(clampedX, centerY)
                )
            }
        }
    }
}

private fun DrawScope.drawSmoothProgressBar(
    width: Float,
    centerY: Float,
    barHeight: Float,
    progress: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    val progressWidth = width * progress

    drawRoundRect(
        color = inactiveColor,
        topLeft = Offset(0f, centerY - barHeight / 2f),
        size = Size(width, barHeight),
        cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
    )

    if (progressWidth > 0f) {
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(0f, centerY - barHeight / 2f),
            size = Size(progressWidth.coerceAtMost(width), barHeight),
            cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
        )
    }
}

private fun DrawScope.drawVisualizerBarsAndWave(
    width: Float,
    centerY: Float,
    barHeight: Float,
    progress: Float,
    waveOffset: Float,
    waveformData: ByteArray?,
    amplitudeScale: Float,
    activeColor: Color,
    inactiveColor: Color,
) {
    val progressWidth = width * progress
    val barCount = 42
    val spacing = width / barCount
    val barWidth = (spacing * 0.56f).coerceAtLeast(2.dp.toPx())
    val maxAmplitude = barHeight * 2.4f

    val hasValidHardwareWaveform = waveformData != null && waveformData.any {
        val unsigned = it.toInt() and 0xFF
        abs(unsigned - 128) > 4 && unsigned != 0
    }

    for (i in 0 until barCount) {
        val barCenterX = i * spacing + spacing / 2f
        val fraction = i.toFloat() / barCount

        val sampleMagnitude = if (hasValidHardwareWaveform && waveformData != null) {
            val idx = (fraction * waveformData.size).toInt().coerceIn(0, waveformData.size - 1)
            val raw = abs(((waveformData[idx].toInt() and 0xFF) - 128) / 128f)
            val harmonic = (sin(fraction * 10f - waveOffset * 1.5f) + 1f) * 0.25f
            (raw * 1.4f + harmonic).coerceIn(0.15f, 1f)
        } else {
            val w1 = (sin(fraction * 4f * Math.PI.toFloat() - waveOffset * 2f) + 1f) * 0.5f
            val w2 = (cos(fraction * 9f * Math.PI.toFloat() + waveOffset * 1.3f) + 1f) * 0.5f
            val w3 = (sin(fraction * 15f * Math.PI.toFloat() - waveOffset * 0.8f) + 1f) * 0.5f
            (0.2f + 0.45f * w1 + 0.2f * w2 + 0.15f * w3).coerceIn(0.18f, 1f)
        }

        val isPlayed = barCenterX <= progressWidth
        val effectiveAmp = sampleMagnitude * maxAmplitude * (if (isPlayed) amplitudeScale else (amplitudeScale * 0.35f).coerceAtLeast(0.12f))
        val totalBarH = (barHeight + effectiveAmp * 2f).coerceAtLeast(barHeight)

        drawRoundRect(
            color = if (isPlayed) activeColor.copy(alpha = 0.72f) else inactiveColor.copy(alpha = 0.35f),
            topLeft = Offset(barCenterX - barWidth / 2f, centerY - totalBarH / 2f),
            size = Size(barWidth, totalBarH),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
        )
    }

    if (progressWidth > 2f) {
        val wavePath = Path()
        wavePath.moveTo(0f, centerY)
        val steps = 60
        val stepPx = progressWidth / steps
        for (i in 0..steps) {
            val x = i * stepPx
            val phase = (x / width) * 3f * 2f * Math.PI.toFloat() - waveOffset
            val y = centerY - (sin(phase) * 0.5f + 0.5f) * maxAmplitude * 0.85f * amplitudeScale
            wavePath.lineTo(x, y)
        }
        wavePath.lineTo(progressWidth, centerY)
        wavePath.close()

        drawPath(
            path = wavePath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    activeColor.copy(alpha = 0.55f),
                    activeColor.copy(alpha = 0.08f)
                ),
                startY = centerY - maxAmplitude,
                endY = centerY
            )
        )
    }
}

@Composable
fun AudioVisualizerPreview(
    modifier: Modifier = Modifier,
    waveColor: Color = Color.White,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wavePreview")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val barHeight = 5.dp.toPx()

        drawVisualizerBarsAndWave(
            width = width,
            centerY = centerY,
            barHeight = barHeight,
            progress = 0.58f,
            waveOffset = waveOffset,
            waveformData = null,
            amplitudeScale = 1f,
            activeColor = waveColor,
            inactiveColor = waveColor.copy(alpha = 0.35f)
        )

        drawSmoothProgressBar(
            width = width,
            centerY = centerY,
            barHeight = barHeight * 0.8f,
            progress = 0.58f,
            activeColor = waveColor,
            inactiveColor = waveColor.copy(alpha = 0.3f)
        )
    }
}
