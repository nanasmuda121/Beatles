/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SamsungSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    enabled: Boolean = true,
    isPlaying: Boolean = true,
    trackHeight: Dp = 4.dp,
    expandedTrackHeight: Dp = 6.dp,
) {
    val rangeSpan = (valueRange.endInclusive - valueRange.start).let { if (it <= 0f) 1f else it }
    val normalizedValue = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val currentValueRange by rememberUpdatedState(valueRange)

    val displayValue = if (isDragging) dragValue else normalizedValue

    val infiniteTransition = rememberInfiniteTransition(label = "liquidWaveTransition")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveOffset"
    )

    val amplitudeFactor by animateFloatAsState(
        targetValue = if (isPlaying && !isDragging) 1f else 0.25f,
        animationSpec = tween(durationMillis = 300),
        label = "liquidAmplitude"
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor
    val thumbColor = colors.thumbColor

    val baseModifier = modifier
        .fillMaxWidth()
        .height(36.dp)

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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = if (isDragging) expandedTrackHeight.toPx() else trackHeight.toPx()
            val centerY = size.height / 2f
            val totalWidth = size.width
            val progressWidth = (totalWidth * displayValue).coerceIn(0f, totalWidth)
            val maxAmp = 4.5.dp.toPx() * amplitudeFactor

            drawLiquidDualTrack(
                centerY = centerY,
                strokeWidth = strokePx,
                waveOffset = waveOffset,
                amplitude = maxAmp,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
                progressWidth = progressWidth
            )

            // Liquid droplet thumb
            val thumbRadius = if (isDragging) 8.5.dp.toPx() else 7.dp.toPx()
            val thumbX = progressWidth.coerceIn(thumbRadius, (totalWidth - thumbRadius).coerceAtLeast(thumbRadius))

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        thumbColor.copy(alpha = 0.45f),
                        thumbColor.copy(alpha = 0f)
                    ),
                    center = Offset(thumbX, centerY),
                    radius = thumbRadius * 2.1f
                ),
                radius = thumbRadius * 2.1f,
                center = Offset(thumbX, centerY)
            )

            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = thumbRadius * 0.42f,
                center = Offset(thumbX - thumbRadius * 0.2f, centerY - thumbRadius * 0.2f)
            )
        }
    }
}

private fun DrawScope.drawLiquidDualTrack(
    centerY: Float,
    strokeWidth: Float,
    waveOffset: Float,
    amplitude: Float,
    activeColor: Color,
    inactiveColor: Color,
    progressWidth: Float,
) {
    val totalWidth = size.width
    if (totalWidth <= 0f) return

    val steps = 84
    val stepX = totalWidth / steps
    val waveCycles = 3.5f

    // 1. Inactive baseline track (from progressWidth to totalWidth)
    if (progressWidth < totalWidth) {
        drawLine(
            color = inactiveColor,
            start = Offset(progressWidth, centerY),
            end = Offset(totalWidth, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }

    // 2. Active dual-harmonic liquid waves (from 0 to progressWidth)
    if (progressWidth > 1f) {
        val primaryWavePath = Path()
        val secondaryWavePath = Path()

        val activeSteps = ((progressWidth / stepX).toInt()).coerceIn(2, steps)
        val actualStep = progressWidth / activeSteps

        for (i in 0..activeSteps) {
            val x = (i * actualStep).coerceAtMost(progressWidth)
            val normalizedX = x / totalWidth
            val edgeTaper = sin(normalizedX * Math.PI.toFloat()).coerceIn(0.25f, 1f)
            val phase1 = normalizedX * waveCycles * 2f * Math.PI.toFloat() - waveOffset
            val phase2 = normalizedX * (waveCycles * 1.4f) * 2f * Math.PI.toFloat() + waveOffset * 1.2f

            val y1 = centerY + sin(phase1) * amplitude * edgeTaper
            val y2 = centerY + cos(phase2) * (amplitude * 0.65f) * edgeTaper

            if (i == 0) {
                primaryWavePath.moveTo(x, y1)
                secondaryWavePath.moveTo(x, y2)
            } else {
                primaryWavePath.lineTo(x, y1)
                secondaryWavePath.lineTo(x, y2)
            }
        }

        drawPath(
            path = secondaryWavePath,
            color = activeColor.copy(alpha = 0.42f),
            style = Stroke(width = strokeWidth * 0.85f, cap = StrokeCap.Round)
        )

        drawPath(
            path = primaryWavePath,
            color = activeColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
