/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.streak.StreakFlameVisual
import com.nanzbeatles.nanas.streak.StreakManager
import com.nanzbeatles.nanas.streak.StreakState
import com.nanzbeatles.nanas.streak.StreakTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StreakFlameIcon(
    tier: StreakTier,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animated: Boolean = true,
    animationTriggerToken: Int = 0,
    previousTier: StreakTier? = null,
) {
    StreakFlameIcon(
        flame = tier.toVisual(),
        modifier = modifier,
        size = size,
        animated = animated,
        animationTriggerToken = animationTriggerToken,
        previousGlowColor = previousTier?.glowColor,
    )
}

/**
 * 3D Glossy Sculpted Flame Icon that smoothly morphs its colors, aura, and thickness across
 * the automatic [StreakTier] evolution levels, and plays a Level-Up / Color-Change burst
 * animation whenever the user's daily streak increases.
 */
@Composable
fun StreakFlameIcon(
    flame: StreakFlameVisual,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animated: Boolean = true,
    animationTriggerToken: Int = 0,
    previousGlowColor: Color? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streakFlame")
    // Flame scaling 1.0 -> 1.08 -> 1.0 as requested
    val flicker by infiniteTransition.animateFloat(
        initialValue = 1.00f,
        targetValue = if (animated && !flame.isUnlit) 1.08f else 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameFlicker"
    )
    // Glow naik-turun
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.62f,
        targetValue = if (animated && !flame.isUnlit) 1.08f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameGlowPulse"
    )
    // Partikel kecil naik / efek ember (0f -> 1f continuous loop)
    val emberPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated && !flame.isUnlit) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flameEmberPhase"
    )
    // Sedikit rotasi
    val sway by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = if (animated && !flame.isUnlit) 2.2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flameSway"
    )

    val animOuterColor by animateColorAsState(
        targetValue = flame.outerColor,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "flameOuterColor"
    )
    val animMidColor by animateColorAsState(
        targetValue = flame.midColor,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "flameMidColor"
    )
    val animCoreColor by animateColorAsState(
        targetValue = flame.coreColor,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "flameCoreColor"
    )
    val animGlowColor by animateColorAsState(
        targetValue = flame.glowColor,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "flameGlowColor"
    )
    val animThicknessScale by animateFloatAsState(
        targetValue = flame.thicknessScale.coerceIn(0.80f, 1.45f),
        animationSpec = spring(
            dampingRatio = 0.58f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "flameThicknessScale"
    )
    val animThickAuraAlpha by animateFloatAsState(
        targetValue = if (flame.isThickFlame) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "flameThickAuraAlpha"
    )

    val burstProgress = remember { Animatable(1f) }
    LaunchedEffect(animationTriggerToken, flame.id) {
        if (animationTriggerToken > 0) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 950, easing = LinearOutSlowInEasing)
            )
        }
    }

    val bp = burstProgress.value
    val isBursting = bp < 0.99f
    val surgeWave = if (isBursting) {
        sin(bp * PI).toFloat() * (1f - bp * 0.45f)
    } else {
        0f
    }
    val burstWobble = if (isBursting) {
        sin(bp * PI * 5f).toFloat() * (1f - bp) * 8.5f
    } else {
        0f
    }

    // Intensitas glow berdasarkan streak / tier
    val tierGlowIntensity = when {
        flame.isUnlit -> 0.14f
        flame.minLevel >= 365 -> 0.92f
        flame.minLevel >= 100 -> 0.82f
        flame.minLevel >= 30 -> 0.72f
        flame.minLevel >= 7 -> 0.58f
        else -> 0.45f
    }

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.5f
        val cy = h * 0.54f
        val widthFactor = animThicknessScale

        // Pulsing colorful aura glow behind the flame
        val glowRadius = w * (0.58f * glowPulse + 0.22f * surgeWave) * widthFactor
        val activeGlowAlpha = ((tierGlowIntensity * glowPulse) + 0.30f * surgeWave).coerceIn(0.08f, 0.92f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    animGlowColor.copy(alpha = activeGlowAlpha),
                    animOuterColor.copy(alpha = (activeGlowAlpha * 0.45f).coerceAtMost(0.68f)),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = glowRadius.coerceAtLeast(1f)
            ),
            radius = glowRadius.coerceAtLeast(1f),
            center = Offset(cx, cy)
        )

        // Continuous rising ember particles ("partikel kecil naik" & "efek ember")
        if (!flame.isUnlit && animated) {
            val emberCount = when {
                flame.minLevel >= 100 -> 12
                flame.minLevel >= 30 -> 9
                flame.minLevel >= 7 -> 7
                else -> 5
            }
            for (i in 0 until emberCount) {
                val offsetSeed = i.toFloat() / emberCount.toFloat()
                val p = (emberPhase + offsetSeed) % 1f
                // Embers start around mid-flame (y = 0.68h) and float up past the tip (y = 0.02h)
                val emberY = h * (0.68f - p * 0.66f)
                val horizontalSpread = sin((p * 2.2f + i * 1.3f) * PI.toFloat()) * w * 0.14f * widthFactor
                val baseSpread = ((i % 5) - 2) * w * 0.055f * (1f - p * 0.4f)
                val emberX = cx + baseSpread + horizontalSpread
                val emberAlpha = (sin(p * PI).toFloat() * (0.55f + 0.35f * tierGlowIntensity)).coerceIn(0f, 0.95f)
                val emberRadius = (w * (0.034f - 0.020f * p) * widthFactor).coerceAtLeast(0.8f)
                val emberColor = when (i % 3) {
                    0 -> animCoreColor
                    1 -> animMidColor
                    else -> animOuterColor
                }

                // Soft outer ember halo
                drawCircle(
                    color = animGlowColor.copy(alpha = emberAlpha * 0.42f),
                    radius = emberRadius * 2.1f,
                    center = Offset(emberX, emberY)
                )
                // Bright ember core
                drawCircle(
                    color = emberColor.copy(alpha = emberAlpha),
                    radius = emberRadius,
                    center = Offset(emberX, emberY)
                )
            }
        }

        if (isBursting && !flame.isUnlit) {
            val fadeAlpha = (1f - bp).coerceIn(0f, 1f)
            val prevRingColor = previousGlowColor ?: animOuterColor

            val outerRingRadius = w * (0.22f + 0.78f * bp)
            drawCircle(
                color = prevRingColor.copy(alpha = 0.65f * fadeAlpha),
                radius = outerRingRadius,
                center = Offset(cx, cy),
                style = Stroke(width = (w * 0.055f * fadeAlpha).coerceAtLeast(1f))
            )

            val innerBp = ((bp - 0.08f) / 0.92f).coerceIn(0f, 1f)
            if (innerBp > 0f) {
                val innerFade = (1f - innerBp).coerceIn(0f, 1f)
                val innerRingRadius = w * (0.16f + 0.66f * innerBp)
                drawCircle(
                    color = animCoreColor.copy(alpha = 0.85f * innerFade),
                    radius = innerRingRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = (w * 0.04f * innerFade).coerceAtLeast(1f))
                )
            }

            val sparkCount = 16
            for (i in 0 until sparkCount) {
                val seed = i.toFloat() / sparkCount.toFloat()
                val angle = (seed * 2f * PI + bp * 2.4f * (if (i % 2 == 0) 1f else -1f)).toFloat()
                val radialDist = w * (0.14f + 0.56f * bp * (0.65f + 0.35f * sin(i * 1.7f)))
                val riseOffset = h * (0.38f * bp * (0.5f + 0.5f * ((i % 4) / 3f)))
                val sx = cx + cos(angle) * radialDist * 0.85f
                val sy = (cy + h * 0.12f) + sin(angle) * radialDist * 0.45f - riseOffset
                val sparkRadius = w * (0.042f * (1f - bp * 0.7f)) * (if (i % 3 == 0) 1.25f else 0.85f)
                val sparkColor = when (i % 3) {
                    0 -> animCoreColor
                    1 -> animMidColor
                    else -> animOuterColor
                }
                drawCircle(
                    color = sparkColor.copy(alpha = (fadeAlpha * 0.95f).coerceIn(0f, 1f)),
                    radius = sparkRadius.coerceAtLeast(1f),
                    center = Offset(sx, sy)
                )
            }
        }

        val scaleSurgeX = 1f + surgeWave * 0.28f
        val scaleSurgeY = 1f + surgeWave * 0.36f

        withTransform({
            scale(
                scaleX = widthFactor * (1.0f + (flicker - 1.0f) * 0.65f) * scaleSurgeX,
                scaleY = widthFactor * flicker * scaleSurgeY,
                pivot = Offset(cx, h * 0.86f)
            )
            rotate(degrees = sway + burstWobble, pivot = Offset(cx, h * 0.84f))
        }) {
            if (animThickAuraAlpha > 0.01f) {
                val thickAuraPath = Path().apply {
                    moveTo(cx, h * 0.06f)
                    cubicTo(
                        w * 0.88f, h * 0.26f,
                        w * 0.94f, h * 0.64f,
                        cx, h * 0.91f
                    )
                    cubicTo(
                        w * 0.06f, h * 0.64f,
                        w * 0.12f, h * 0.26f,
                        cx, h * 0.06f
                    )
                    close()
                }
                drawPath(
                    path = thickAuraPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            animMidColor.copy(alpha = 0.52f * animThickAuraAlpha),
                            animOuterColor.copy(alpha = 0.88f * animThickAuraAlpha),
                            animOuterColor.copy(alpha = 0.95f * animThickAuraAlpha)
                        ),
                        startY = h * 0.06f,
                        endY = h * 0.91f
                    )
                )
                drawPath(
                    path = thickAuraPath,
                    color = animCoreColor.copy(alpha = 0.36f * animThickAuraAlpha),
                    style = Stroke(width = w * 0.042f, cap = StrokeCap.Round)
                )
            }

            val mainFlamePath = Path().apply {
                moveTo(cx + w * 0.02f, h * 0.10f)
                cubicTo(
                    w * 0.78f, h * 0.30f,
                    w * 0.86f, h * 0.62f,
                    cx, h * 0.89f
                )
                cubicTo(
                    w * 0.14f, h * 0.62f,
                    w * 0.22f, h * 0.26f,
                    cx + w * 0.02f, h * 0.10f
                )
                close()
            }

            drawPath(
                path = mainFlamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animCoreColor,
                        animMidColor,
                        animOuterColor,
                    ),
                    startY = h * 0.10f,
                    endY = h * 0.89f
                )
            )

            val rightTonguePath = Path().apply {
                moveTo(w * 0.64f, h * 0.42f)
                cubicTo(
                    w * 0.72f, h * 0.30f,
                    w * 0.79f, h * 0.24f,
                    w * 0.75f, h * 0.19f
                )
                cubicTo(
                    w * 0.85f, h * 0.32f,
                    w * 0.84f, h * 0.56f,
                    w * 0.66f, h * 0.74f
                )
                close()
            }
            drawPath(
                path = rightTonguePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animCoreColor.copy(alpha = 0.92f),
                        animMidColor,
                        animOuterColor,
                    ),
                    startY = h * 0.19f,
                    endY = h * 0.74f
                )
            )

            val innerCorePath = Path().apply {
                moveTo(cx, h * 0.34f)
                cubicTo(
                    w * 0.67f, h * 0.48f,
                    w * 0.69f, h * 0.72f,
                    cx, h * 0.86f
                )
                cubicTo(
                    w * 0.31f, h * 0.72f,
                    w * 0.33f, h * 0.48f,
                    cx, h * 0.34f
                )
                close()
            }
            drawPath(
                path = innerCorePath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (if (flame.isUnlit) 0.45f else 0.92f + 0.08f * surgeWave).coerceAtMost(1f)),
                        animCoreColor.copy(alpha = 0.90f),
                        animMidColor.copy(alpha = 0.0f)
                    ),
                    center = Offset(cx, h * 0.68f),
                    radius = w * (0.28f + 0.10f * surgeWave)
                )
            )

            val highlightPath = Path().apply {
                moveTo(w * 0.32f, h * 0.42f)
                cubicTo(
                    w * 0.26f, h * 0.54f,
                    w * 0.29f, h * 0.69f,
                    w * 0.38f, h * 0.77f
                )
            }
            drawPath(
                path = highlightPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (flame.isUnlit) 0.25f else 0.65f),
                        Color.Transparent
                    ),
                    startY = h * 0.42f,
                    endY = h * 0.77f
                ),
                style = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Automatic Home Screen Streak Banner Card:
 * - Displays active 3D flame for the user's current automatic streak
 * - Shows "<N> hari berturut-turut" and current evolution tier badge
 */
@Composable
fun HomeStreakCard(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        StreakManager.ensureInitialized(context)
    }

    val streakState by StreakManager.state.collectAsState()
    var showModal by rememberSaveable { mutableStateOf(false) }

    val activeFlame = streakState.activeFlame
    val animatedStreakCount by animateIntAsState(
        targetValue = streakState.currentStreak.coerceAtLeast(1),
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "homeStreakCount"
    )
    val animatedGlowColor by animateColorAsState(
        targetValue = activeFlame.glowColor,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "homeStreakGlow"
    )
    val animatedOuterColor by animateColorAsState(
        targetValue = activeFlame.outerColor,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "homeStreakOuter"
    )
    val animatedCoreColor by animateColorAsState(
        targetValue = activeFlame.coreColor,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "homeStreakCore"
    )

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF141518) else Color(0xFFF2F3F7)
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val primaryTextColor = if (isDark) Color.White else Color(0xFF0E1015)
    val secondaryTextColor = if (isDark) Color(0xFF9EA2AE) else Color(0xFF5A6070)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg, RoundedCornerShape(18.dp))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            animatedGlowColor.copy(alpha = if (!activeFlame.isUnlit) 0.28f else 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.height * 0.62f, size.height * 0.5f),
                        radius = size.height * 1.15f
                    ),
                    radius = size.height * 1.15f,
                    center = Offset(size.height * 0.62f, size.height * 0.5f)
                )
            }
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .clickable {
                showModal = true
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("home_streak_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StreakFlameIcon(
                flame = activeFlame,
                size = 48.dp,
                animationTriggerToken = streakState.animationTriggerToken,
                previousGlowColor = streakState.previousTier.glowColor,
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryTextColor
                            )
                        ) {
                            append("$animatedStreakCount")
                        }
                        withStyle(
                            SpanStyle(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        ) {
                            append(" hari berturut-turut")
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${activeFlame.title} • ${activeFlame.badgeLabel}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = animatedOuterColor.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, animatedGlowColor.copy(alpha = 0.40f)),
            ) {
                Text(
                    text = activeFlame.levelRangeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) animatedCoreColor else animatedOuterColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (showModal) {
        StreakDetailModalDialog(
            state = streakState,
            onDismiss = { showModal = false }
        )
    }
}

@Composable
fun StreakDetailModalDialog(
    state: StreakState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeFlame = state.activeFlame

    val animatedCount by animateIntAsState(
        targetValue = state.currentStreak,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "modalStreakCount"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = state.tierProgress,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "tierProgress"
    )
    val animatedGlow by animateColorAsState(
        targetValue = activeFlame.glowColor,
        animationSpec = tween(durationMillis = 600),
        label = "modalGlow"
    )
    val animatedOuter by animateColorAsState(
        targetValue = activeFlame.outerColor,
        animationSpec = tween(durationMillis = 600),
        label = "modalOuter"
    )
    val animatedCore by animateColorAsState(
        targetValue = activeFlame.coreColor,
        animationSpec = tween(durationMillis = 600),
        label = "modalCore"
    )

    // Live countdown to 00:00 midnight reset
    var countdownText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
            val dur = Duration.between(now, midnight)
            val hours = dur.toHours()
            val minutes = dur.toMinutesPart()
            val seconds = dur.toSecondsPart()
            countdownText = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            delay(1_000L)
        }
    }

    val exportStreakLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = StreakManager.exportStreakToUri(context, uri)
                Toast.makeText(
                    context,
                    if (ok) "Data Streak berhasil dicadangkan ke file!" else "Gagal mencadangkan streak",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val importStreakLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = StreakManager.importStreakFromUri(context, uri)
                Toast.makeText(
                    context,
                    if (ok) "Data Streak berhasil dipulihkan dari file!" else "File cadangan streak tidak valid",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF12141B),
            border = BorderStroke(1.2.dp, animatedGlow.copy(alpha = 0.48f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF171A24))
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        animatedGlow.copy(alpha = if (activeFlame.isUnlit) 0.12f else 0.34f),
                                        animatedOuter.copy(alpha = if (activeFlame.isUnlit) 0.04f else 0.12f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.5f, size.height * 0.42f),
                                    radius = size.width * 0.55f
                                ),
                                radius = size.width * 0.55f,
                                center = Offset(size.width * 0.5f, size.height * 0.42f)
                            )
                        }
                        .border(1.dp, animatedGlow.copy(alpha = 0.28f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StreakFlameIcon(
                            flame = activeFlame,
                            size = 100.dp,
                            animationTriggerToken = state.animationTriggerToken,
                            previousGlowColor = state.previousTier.glowColor,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "$animatedCount hari berturut-turut",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            shape = CircleShape,
                            color = animatedOuter.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, animatedGlow.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "${activeFlame.levelRangeText} • ${activeFlame.badgeLabel}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = animatedCore,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = activeFlame.description,
                            fontSize = 12.sp,
                            color = Color(0xFFB4B9C6),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val nextTier = state.nextTier
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (nextTier != null) {
                                        "Menuju ${nextTier.badgeLabel} (Level ${nextTier.minLevel})"
                                    } else {
                                        "Warna Api Tertinggi Tercapai (Level 201+)"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB4B9C6)
                                )
                                Text(
                                    text = if (nextTier != null) {
                                        "${(nextTier.minLevel - state.currentStreak).coerceAtLeast(0)} hari lagi"
                                    } else {
                                        "MAX"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = animatedCore
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape),
                                color = animatedCore,
                                trackColor = Color.White.copy(alpha = 0.10f),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StreakStatBox(
                        label = "Hari Ini",
                        value = if (state.listenedToday) "Selesai ✓" else "Belum",
                        valueColor = if (state.listenedToday) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                        modifier = Modifier.weight(1f)
                    )
                    StreakStatBox(
                        label = "Rekor Terbaik",
                        value = "${state.longestStreak} Hari",
                        valueColor = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    StreakStatBox(
                        label = "Reset 00:00",
                        value = countdownText,
                        valueColor = animatedCore,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Read-only automatic evolution tiers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1A1D26))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Level Evolusi Warna Api",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Warna api berevolusi otomatis seiring bertambahnya hari mendengarkan musik",
                        fontSize = 11.sp,
                        color = Color(0xFF9EA4B3)
                    )

                    StreakTier.evolutionTiers.forEach { itemTier ->
                        val isCurrentActive = state.tier == itemTier
                        val isUnlocked = state.currentStreak >= itemTier.minLevel
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrentActive) itemTier.outerColor.copy(alpha = 0.22f)
                                    else Color(0xFF222633)
                                )
                                .border(
                                    1.dp,
                                    if (isCurrentActive) itemTier.glowColor.copy(alpha = 0.65f)
                                    else Color.White.copy(alpha = 0.06f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StreakFlameIcon(
                                tier = itemTier,
                                size = 32.dp,
                                animated = isCurrentActive
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${itemTier.levelRangeText} • ${itemTier.badgeLabel}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentActive) itemTier.coreColor else Color.White
                                )
                                Text(
                                    text = itemTier.title,
                                    fontSize = 11.sp,
                                    color = Color(0xFF9EA4B3)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isCurrentActive -> itemTier.outerColor
                                    isUnlocked -> Color(0xFF4ADE80).copy(alpha = 0.16f)
                                    else -> Color.White.copy(alpha = 0.06f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        isCurrentActive -> "Aktif ✓"
                                        isUnlocked -> "Terbuka"
                                        else -> "Lv.${itemTier.minLevel}"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isCurrentActive -> Color.White
                                        isUnlocked -> Color(0xFF4ADE80)
                                        else -> Color(0xFF9EA4B3)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val stamp = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
                            exportStreakLauncher.launch("Streak_${state.currentStreak}hari_$stamp.streak.json")
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, animatedGlow.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.backup),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan File", color = Color.White, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            importStreakLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.restore),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pulihkan File", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = animatedOuter,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StreakStatBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A1D26))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF9EA4B3),
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            maxLines = 1
        )
    }
}
