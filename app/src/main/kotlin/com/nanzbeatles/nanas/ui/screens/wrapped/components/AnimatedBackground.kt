/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.wrapped.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class ShapeType {
    Rect, Circle, Line, Ring, Plus, Sparkle, Dot
}

private data class FloatingElementState(
    val xFactor: Float,
    val yFactor: Float,
    val size: Float,
    val rotationSpeed: Float,
    val initialRotation: Float,
    val xSpeed: Float,
    val ySpeed: Float,
    val xPhase: Float,
    val yPhase: Float,
    val pulseSpeed: Float,
    val pulsePhase: Float,
    val shapeType: ShapeType,
    val baseAlpha: Float,
    val isOutlined: Boolean
)

@Composable
fun AnimatedBackground(
    modifier: Modifier = Modifier,
    elementCount: Int = 48,
    shapeTypes: List<ShapeType> = listOf(
        ShapeType.Circle,
        ShapeType.Line,
        ShapeType.Ring,
        ShapeType.Sparkle,
        ShapeType.Plus,
        ShapeType.Dot
    )
) {
    // Expand any single-shape caller list with complimentary festive accents while keeping the primary shape dominant
    val enrichedShapes = remember(shapeTypes) {
        buildList {
            repeat(3) { addAll(shapeTypes) }
            add(ShapeType.Ring)
            add(ShapeType.Sparkle)
            add(ShapeType.Plus)
            add(ShapeType.Dot)
        }
    }

    val effectiveCount = remember(elementCount) {
        elementCount.coerceAtLeast(44)
    }

    val elements = remember(effectiveCount, enrichedShapes) {
        val random = Random(987654321)
        (1..effectiveCount).map { index ->
            val chosenShape = enrichedShapes.random(random)
            val baseSize = when (chosenShape) {
                ShapeType.Dot -> random.nextFloat() * 10f + 4f
                ShapeType.Sparkle -> random.nextFloat() * 28f + 12f
                ShapeType.Plus -> random.nextFloat() * 26f + 12f
                ShapeType.Ring -> random.nextFloat() * 110f + 32f
                else -> random.nextFloat() * 120f + 28f
            }
            FloatingElementState(
                xFactor = random.nextFloat(),
                yFactor = random.nextFloat(),
                size = baseSize,
                rotationSpeed = (random.nextFloat() - 0.5f) * 2.4f,
                initialRotation = random.nextFloat() * 360f,
                xSpeed = random.nextFloat() * 1.6f + 0.4f,
                ySpeed = random.nextFloat() * 1.6f + 0.4f,
                xPhase = random.nextFloat() * 2 * PI.toFloat(),
                yPhase = random.nextFloat() * 2 * PI.toFloat(),
                pulseSpeed = random.nextFloat() * 2f + 1f,
                pulsePhase = random.nextFloat() * 2 * PI.toFloat(),
                shapeType = chosenShape,
                baseAlpha = when (chosenShape) {
                    ShapeType.Dot -> random.nextFloat() * 0.22f + 0.08f
                    ShapeType.Sparkle -> random.nextFloat() * 0.18f + 0.07f
                    else -> random.nextFloat() * 0.12f + 0.04f
                },
                isOutlined = index % 3 == 0
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "WrappedBackgroundTransition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Progress"
    )

    val sparklePath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = Color.Black)

        // Subtle ambient corner rings in monochrome white
        val maxDim = size.maxDimension
        drawCircle(
            color = Color.White.copy(alpha = 0.035f),
            radius = maxDim * 0.42f,
            center = Offset(size.width * 0.1f, size.height * 0.12f),
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.035f),
            radius = maxDim * 0.48f,
            center = Offset(size.width * 0.9f, size.height * 0.88f),
            style = Stroke(width = 1.5f)
        )

        elements.forEach { el ->
            val currentRotation = (el.initialRotation + progress * 360f * el.rotationSpeed)
            val currentX = (size.width * el.xFactor) + sin(progress * 2 * PI.toFloat() * el.xSpeed + el.xPhase) * 36f
            val currentY = (size.height * el.yFactor) + cos(progress * 2 * PI.toFloat() * el.ySpeed + el.yPhase) * 36f
            val pulse = 0.75f + 0.25f * sin(progress * 2 * PI.toFloat() * el.pulseSpeed + el.pulsePhase)
            val color = Color.White.copy(alpha = (el.baseAlpha * pulse).coerceIn(0.02f, 0.35f))

            rotate(degrees = currentRotation, pivot = Offset(currentX, currentY)) {
                when (el.shapeType) {
                    ShapeType.Rect -> {
                        if (el.isOutlined) {
                            drawRect(
                                color = color,
                                topLeft = Offset(currentX - el.size / 2, currentY - el.size / 2),
                                size = Size(el.size, el.size),
                                style = Stroke(width = 2.5f)
                            )
                        } else {
                            drawRect(
                                color = color,
                                topLeft = Offset(currentX - el.size / 2, currentY - el.size / 2),
                                size = Size(el.size, el.size)
                            )
                        }
                    }

                    ShapeType.Circle -> {
                        if (el.isOutlined) {
                            drawCircle(
                                color = color,
                                radius = el.size / 2,
                                center = Offset(currentX, currentY),
                                style = Stroke(width = 2.5f)
                            )
                        } else {
                            drawCircle(
                                color = color,
                                radius = el.size / 2,
                                center = Offset(currentX, currentY)
                            )
                        }
                    }

                    ShapeType.Ring -> {
                        drawCircle(
                            color = color,
                            radius = el.size / 2,
                            center = Offset(currentX, currentY),
                            style = Stroke(width = 2.5f)
                        )
                        drawCircle(
                            color = color.copy(alpha = color.alpha * 0.5f),
                            radius = el.size * 0.32f,
                            center = Offset(currentX, currentY),
                            style = Stroke(width = 1.2f)
                        )
                    }

                    ShapeType.Line -> {
                        drawLine(
                            color = color,
                            start = Offset(currentX - el.size / 2, currentY),
                            end = Offset(currentX + el.size / 2, currentY),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }

                    ShapeType.Plus -> {
                        val half = el.size / 2f
                        drawLine(
                            color = color,
                            start = Offset(currentX - half, currentY),
                            end = Offset(currentX + half, currentY),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = color,
                            start = Offset(currentX, currentY - half),
                            end = Offset(currentX, currentY + half),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }

                    ShapeType.Sparkle -> {
                        val r = (el.size / 2f) * pulse
                        val inner = r * 0.26f
                        sparklePath.reset()
                        sparklePath.moveTo(currentX, currentY - r)
                        sparklePath.quadraticTo(currentX + inner, currentY - inner, currentX + r, currentY)
                        sparklePath.quadraticTo(currentX + inner, currentY + inner, currentX, currentY + r)
                        sparklePath.quadraticTo(currentX - inner, currentY + inner, currentX - r, currentY)
                        sparklePath.quadraticTo(currentX - inner, currentY - inner, currentX, currentY - r)
                        sparklePath.close()
                        drawPath(path = sparklePath, color = color)
                    }

                    ShapeType.Dot -> {
                        drawCircle(
                            color = color,
                            radius = (el.size / 2f) * pulse,
                            center = Offset(currentX, currentY)
                        )
                    }
                }
            }
        }
    }
}
