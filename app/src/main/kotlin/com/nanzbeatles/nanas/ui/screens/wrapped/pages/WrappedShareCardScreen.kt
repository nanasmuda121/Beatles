/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.wrapped.pages

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Environment
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanzbeatles.nanas.ui.screens.wrapped.WrappedConstants
import com.nanzbeatles.nanas.ui.screens.wrapped.WrappedState
import com.nanzbeatles.nanas.ui.screens.wrapped.components.AnimatedBackground
import com.nanzbeatles.nanas.ui.screens.wrapped.components.ShapeType
import com.nanzbeatles.nanas.ui.theme.bbh_bartle
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

@Composable
fun WrappedShareCardScreen(
    state: WrappedState,
    isVisible: Boolean
) {
    var visible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(200)
            visible = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle, ShapeType.Line))
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ringkasan ${WrappedConstants.displayMonthYear}",
                        style = TextStyle(
                            fontFamily = bbh_bartle,
                            fontSize = 28.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${state.totalMinutes} menit didengarkan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${state.uniqueSongCount} lagu • ${state.uniqueArtistCount} artis",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    state.topSongs.firstOrNull()?.let { topSong ->
                        Text(
                            text = "Lagu Teratas: ${topSong.title}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                    state.topArtists.firstOrNull()?.let { topArtist ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Artis Teratas: ${topArtist.artist.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val bitmap = generateShareCardBitmap(context, state)
                            saveAndShareBitmap(context, bitmap)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = "Bagikan Kartu Ringkasan",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun generateShareCardBitmap(context: Context, state: WrappedState): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background
    canvas.drawColor(android.graphics.Color.parseColor("#121212"))

    val titlePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 72f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Ringkasan ${WrappedConstants.displayMonthYear}", width / 2f, 240f, titlePaint)

    val subtitlePaint = Paint().apply {
        color = android.graphics.Color.parseColor("#B3B3B3")
        textSize = 42f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("NanzBeatles", width / 2f, 310f, subtitlePaint)

    val statValuePaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 96f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val statLabelPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#999999")
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    var yPos = 500f

    // Minutes
    canvas.drawText("${state.totalMinutes}", width / 2f, yPos, statValuePaint)
    canvas.drawText("menit didengarkan", width / 2f, yPos + 50f, statLabelPaint)
    yPos += 200f

    // Songs
    canvas.drawText("${state.uniqueSongCount}", width / 2f, yPos, statValuePaint)
    canvas.drawText("lagu unik", width / 2f, yPos + 50f, statLabelPaint)
    yPos += 200f

    // Top Song
    state.topSongs.firstOrNull()?.let { topSong ->
        canvas.drawText("Lagu Teratas", width / 2f, yPos, subtitlePaint)
        yPos += 60f
        val songTitlePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 54f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val songTitle = if (topSong.title.length > 28) topSong.title.take(25) + "..." else topSong.title
        canvas.drawText(songTitle, width / 2f, yPos, songTitlePaint)
        yPos += 50f
        canvas.drawText(topSong.artistName ?: "", width / 2f, yPos, statLabelPaint)
        yPos += 120f
    }

    // Top Artist
    state.topArtists.firstOrNull()?.let { topArtist ->
        canvas.drawText("Artis Teratas", width / 2f, yPos, subtitlePaint)
        yPos += 60f
        val artistNamePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 54f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val artistName = topArtist.artist.name
        canvas.drawText(artistName, width / 2f, yPos, artistNamePaint)
    }

    // Footer
    val footerPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#666666")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText("Dibuat dengan Beatles", width / 2f, height - 100f, footerPaint)

    return bitmap
}

private fun saveAndShareBitmap(context: Context, bitmap: Bitmap) {
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Beatles_Wrapped_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }

    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.FileProvider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_TEXT, "Lihat Ringkasan ${WrappedConstants.displayMonthYear} saya di Beatles!")
    }
    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Ringkasan Anda"))
}
