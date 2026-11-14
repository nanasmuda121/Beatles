/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nanzbeatles.nanas.streak.LevelGifManager
import com.nanzbeatles.nanas.streak.MusicJourneyManager
import kotlinx.coroutines.delay

@Composable
fun LevelEmblemIcon(
    level: Int,
    staticResId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val isGifEnabled by LevelGifManager.isGifEnabled.collectAsState()
    val gifFile = remember(isGifEnabled, level) {
        if (isGifEnabled) LevelGifManager.getLoopGifFile(context, level) else null
    }

    if (gifFile != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(gifFile)
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Image(
            painter = painterResource(staticResId),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    }
}

/**
 * Immersive Fullscreen Level-Up Celebration.
 * Plays the transition up_*.gif (e.g. up_6-7.gif), and automatically switches to the
 * continuous loop_*.gif (e.g. loop_7.gif) when the transition duration completes.
 * Dismissable by tapping anywhere on the screen.
 */
@Composable
fun LevelUpCelebrationDialog(
    fromLevel: Int,
    toLevel: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val toSpec = MusicJourneyManager.allLevels.getOrElse(toLevel - 1) { MusicJourneyManager.allLevels.last() }

    val upGifFile = remember(fromLevel, toLevel) {
        LevelGifManager.getUpGifFile(context, fromLevel, toLevel)
    }
    val loopGifFile = remember(toLevel) {
        LevelGifManager.getLoopGifFile(context, toLevel, ignoreEnabled = true)
    }

    var isShowingLoop by remember(fromLevel, toLevel) { mutableStateOf(false) }

    LaunchedEffect(fromLevel, toLevel, upGifFile) {
        if (upGifFile != null) {
            val durationMs = LevelGifManager.getGifDurationMs(upGifFile)
            delay(durationMs)
            isShowingLoop = true
        } else {
            isShowingLoop = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF06070A),
                            Color(0xFF0F111A),
                            Color(0xFF06070A)
                        )
                    )
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.6f
                        ),
                        radius = size.minDimension * 0.6f,
                        center = center
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = 24.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "🎉 SELAMAT! ANDA NAIK LEVEL! 🎉",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Level $fromLevel ➔ Level $toLevel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF67E8F9),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Box(
                    modifier = Modifier.size(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = isShowingLoop,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "levelUpAnimationTransition"
                    ) { showingLoop ->
                        val currentGif = if (showingLoop) loopGifFile else upGifFile
                        if (currentGif != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(currentGif)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = if (showingLoop) "Animasi Loop Level $toLevel" else "Animasi Kenaikan Level",
                                modifier = Modifier.size(240.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Image(
                                painter = painterResource(toSpec.drawableResId),
                                contentDescription = "Level ${toSpec.level}",
                                modifier = Modifier.size(160.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Level $toLevel — ${toSpec.title}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = toSpec.subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Klik di mana saja untuk menutup",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
