/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.wrapped.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanzbeatles.nanas.ui.screens.wrapped.components.AnimatedBackground
import com.nanzbeatles.nanas.ui.screens.wrapped.components.ShapeType
import com.nanzbeatles.nanas.ui.theme.bbh_bartle
import kotlinx.coroutines.delay

@Composable
fun WrappedTimeOfDayScreen(
    listeningByTimeOfDay: Map<String, Long>,
    isVisible: Boolean
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(200)
            visible = true
        }
    }

    val timeEmojis = mapOf(
        "Morning" to "\u2600\uFE0F",
        "Afternoon" to "\u26C5",
        "Evening" to "\uD83C\uDF19",
        "Night" to "\u2B50"
    )

    val timeLabels = mapOf(
        "Morning" to "Pagi",
        "Afternoon" to "Siang",
        "Evening" to "Sore",
        "Night" to "Malam"
    )

    val peakTime = listeningByTimeOfDay.maxByOrNull { it.value }?.key ?: "Night"

    val subtitle = when (peakTime) {
        "Morning" -> "Anda memulai pagi dengan penuh energi lewat musik"
        "Afternoon" -> "Siang hari adalah waktu puncak musik Anda"
        "Evening" -> "Anda bersantai dengan irama sore hari"
        "Night" -> "Pendengar malam — lagu larut malam adalah favorit Anda"
        else -> "Anda punya selera mendengarkan yang unik"
    }

    val totalMinutes = listeningByTimeOfDay.values.sum().coerceAtLeast(1)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Line, ShapeType.Circle))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(1000))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${timeEmojis[peakTime]} Pendengar ${timeLabels[peakTime] ?: peakTime}",
                        fontFamily = bbh_bartle,
                        fontSize = 32.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, delayMillis = 300)) + slideInVertically(tween(1000, delayMillis = 300))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("Morning", "Afternoon", "Evening", "Night").forEach { timeKey ->
                        val minutes = listeningByTimeOfDay[timeKey] ?: 0L
                        val percentage = ((minutes.toFloat() / totalMinutes) * 100).toInt()
                        val isPeak = timeKey == peakTime

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPeak) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${timeEmojis[timeKey]} ${timeLabels[timeKey] ?: timeKey}",
                                color = Color.White,
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "$percentage% (${minutes}m)",
                                color = Color.White.copy(alpha = if (isPeak) 1f else 0.7f),
                                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
