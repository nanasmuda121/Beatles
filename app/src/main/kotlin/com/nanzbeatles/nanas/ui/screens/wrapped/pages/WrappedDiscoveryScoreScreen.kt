/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.wrapped.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanzbeatles.nanas.ui.screens.wrapped.components.AnimatedBackground
import com.nanzbeatles.nanas.ui.screens.wrapped.components.ShapeType
import com.nanzbeatles.nanas.ui.theme.bbh_bartle

@Composable
fun WrappedDiscoveryScoreScreen(
    discoveryScore: Int,
    uniqueArtistCount: Int,
    isVisible: Boolean
) {
    val animatedScore = remember { Animatable(0f) }

    LaunchedEffect(isVisible, discoveryScore) {
        if (isVisible && discoveryScore > 0) {
            animatedScore.animateTo(
                targetValue = discoveryScore.toFloat(),
                animationSpec = tween(1500, easing = FastOutSlowInEasing)
            )
        }
    }

    val verdict = when {
        discoveryScore >= 20 -> "Penjelajah Musik Luar Biasa"
        discoveryScore >= 10 -> "Pendengar Petualang"
        discoveryScore >= 5 -> "Penjelajah Penasaran"
        discoveryScore >= 1 -> "Mulai Mencoba Hal Baru"
        else -> "Setia pada Favorit Anda"
    }

    val subtitle = when {
        discoveryScore >= 20 -> "Anda selalu mencari musik baru!"
        discoveryScore >= 10 -> "Anda suka menemukan suara-suara baru"
        discoveryScore >= 5 -> "Perpaduan sehat antara yang baru dan yang sudah dikenal"
        discoveryScore >= 1 -> "Anda menemukan beberapa artis baru bulan ini"
        else -> "Anda tahu apa yang Anda sukai dan tetap setia"
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
            Text(
                text = "Skor Penemuan",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = animatedScore.value.toInt().toString(),
                style = TextStyle(
                    fontFamily = bbh_bartle,
                    fontSize = 96.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            )

            Text(
                text = "artis baru ditemukan",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = verdict,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Dari total $uniqueArtistCount artis yang didengarkan",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}
