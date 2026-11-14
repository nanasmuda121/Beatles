/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.streak.LevelGifManager
import com.nanzbeatles.nanas.streak.MusicJourneyManager

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
    var replayKey by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF101218),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "🎉 SELAMAT! NAIK LEVEL! 🎉",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Dari Level $fromLevel ➔ Level $toLevel",
                    fontSize = 12.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                if (upGifFile != null) {
                    key(replayKey) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(upGifFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Animasi Naik Level",
                            modifier = Modifier.size(175.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(toSpec.drawableResId),
                        contentDescription = "Level ${toSpec.level}",
                        modifier = Modifier.size(125.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Animasi GIF transparan belum diunduh.\nSilakan unduh paket GIF di menu Level untuk melihat animasi bergerak.",
                        fontSize = 11.5.sp,
                        color = Color(0xFFA0A6B4),
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = "Level $toLevel — ${toSpec.title}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = toSpec.subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (upGifFile != null) {
                        OutlinedButton(
                            onClick = { replayKey++ },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.replay),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Putar Ulang", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Keren, Lanjutkan!", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
