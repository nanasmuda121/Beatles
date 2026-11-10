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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanzbeatles.nanas.ui.screens.wrapped.WrappedConstants
import com.nanzbeatles.nanas.ui.screens.wrapped.components.AnimatedBackground
import com.nanzbeatles.nanas.ui.screens.wrapped.components.ShapeType
import com.nanzbeatles.nanas.ui.theme.bbh_bartle
import kotlinx.coroutines.delay

@Composable
fun WrappedComparisonScreen(
    currentMinutes: Long,
    previousMonthMinutes: Long,
    currentUniqueSongs: Int,
    previousMonthUniqueSongs: Int,
    currentUniqueArtists: Int,
    previousMonthUniqueArtists: Int,
    monthOverMonthChange: Float,
    isVisible: Boolean
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(200)
            visible = true
        }
    }

    val changeText = if (monthOverMonthChange >= 0) "+${String.format("%.0f", monthOverMonthChange)}%" else "${String.format("%.0f", monthOverMonthChange)}%"
    val verdict = when {
        monthOverMonthChange >= 50 -> "Lonjakan mendengarkan yang luar biasa!"
        monthOverMonthChange >= 20 -> "Anda mendengarkan lebih banyak bulan ini"
        monthOverMonthChange >= 5 -> "Pertumbuhan stabil"
        monthOverMonthChange >= -5 -> "Konsisten dengan bulan lalu"
        monthOverMonthChange >= -20 -> "Sedikit lebih sedikit mendengarkan bulan ini"
        else -> "Bulan yang lebih tenang untuk musik"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle))
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
                        text = "${WrappedConstants.displayMonthYear} vs Bulan Lalu",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = changeText,
                        style = TextStyle(
                            fontFamily = bbh_bartle,
                            fontSize = 64.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    )
                    Text(
                        text = verdict,
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
                    ComparisonStatCard(
                        label = "Menit Didengarkan",
                        current = "${currentMinutes}m",
                        previous = "${previousMonthMinutes}m"
                    )
                    ComparisonStatCard(
                        label = "Lagu Unik",
                        current = "$currentUniqueSongs",
                        previous = "$previousMonthUniqueSongs"
                    )
                    ComparisonStatCard(
                        label = "Artis Unik",
                        current = "$currentUniqueArtists",
                        previous = "$previousMonthUniqueArtists"
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonStatCard(
    label: String,
    current: String,
    previous: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = "Bulan lalu: $previous", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Text(text = current, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}
