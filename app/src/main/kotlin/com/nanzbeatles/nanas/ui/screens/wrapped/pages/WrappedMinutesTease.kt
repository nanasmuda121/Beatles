/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.wrapped.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanzbeatles.nanas.ui.screens.wrapped.WrappedConstants
import com.nanzbeatles.nanas.ui.screens.wrapped.components.AnimatedBackground
import com.nanzbeatles.nanas.ui.screens.wrapped.components.ShapeType
import kotlinx.coroutines.delay

@Composable
fun WrappedMinutesTease(
    isVisible: Boolean,
    onNavigateForward: () -> Unit
) {
    var showText by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            showText = true
            delay(3500)
            onNavigateForward()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle))
        AnimatedVisibility(
            visible = showText,
            enter = fadeIn(tween(1000))
        ) {
            Text(
                text = "Selama Bulan ${WrappedConstants.displayMonthYear}, Anda menghabiskan begitu banyak waktu menikmati musik...",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )
        }
    }
}
