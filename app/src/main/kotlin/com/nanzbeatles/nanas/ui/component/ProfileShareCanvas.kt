/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.streak.MusicJourneyState
import com.nanzbeatles.nanas.streak.StreakState
import com.nanzbeatles.nanas.streak.StreakTier
import com.nanzbeatles.nanas.streak.TopSongStatItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

data class ProfileShareCanvasData(
    val displayName: String,
    val displayHandle: String,
    val bio: String,
    val avatarUri: String,
    val bannerUri: String,
    val bannerIsVideo: Boolean,
    val streakState: StreakState,
    val journeyState: MusicJourneyState,
    val topSongs: List<TopSongStatItem>,
)

@Composable
fun ProfileShareCanvasDialog(
    data: ProfileShareCanvasData,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRendering by remember { mutableStateOf(true) }

    LaunchedEffect(data) {
        isRendering = true
        renderedBitmap = withContext(Dispatchers.Default) {
            ProfileShareCanvasRenderer.renderBitmap(context, data)
        }
        isRendering = false
    }

    val savePngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val bmp = renderedBitmap
        if (uri != null && bmp != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val ok = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }
                    true
                }.getOrDefault(false)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        if (ok) "Gambar statistik berhasil disimpan!" else "Gagal menyimpan gambar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
                .padding(horizontal = 14.dp, vertical = 18.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0C0E13),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bagikan Canvas Statistik Musik",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Pratinjau gambar resolusi tinggi siap dibagikan",
                            fontSize = 11.5.sp,
                            color = Color(0xFF9EA4B0)
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Tutup", color = Color(0xFFCBD5E1), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Canvas Image Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1080f / 2040f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF050507))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = renderedBitmap
                    if (isRendering || bmp == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "Merender Canvas Statistik...",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.5.sp
                            )
                        }
                    } else {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Canvas Statistik Profil",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
                            savePngLauncher.launch(
                                "Beatles_Profile_Stats_${LocalDateTime.now().format(formatter)}.png"
                            )
                        },
                        enabled = renderedBitmap != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_canvas_download_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan PNG",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val bmp = renderedBitmap ?: return@Button
                            coroutineScope.launch {
                                ProfileShareCanvasRenderer.shareBitmapImage(context, bmp, data)
                            }
                        },
                        enabled = renderedBitmap != null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_canvas_send_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bagikan Gambar",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

object ProfileShareCanvasRenderer {
    private const val CANVAS_W = 1080
    private const val CANVAS_H = 2040

    private fun fmtInt(v: Int): String =
        NumberFormat.getIntegerInstance(Locale("id", "ID")).format(v)

    suspend fun shareBitmapImage(
        context: Context,
        bitmap: Bitmap,
        data: ProfileShareCanvasData,
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val shareDir = File(context.cacheDir, "shared_profile_canvas").apply { mkdirs() }
            val outFile = File(shareDir, "Beatles_Statistik_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                outFile
            )

            val levelSpec = data.journeyState.levelSpec
            val caption = buildString {
                appendLine("🎵 Statistik Perjalanan Musikku — ${data.displayName} (${data.displayHandle})")
                appendLine("🏆 Level ${levelSpec.level}: ${levelSpec.title} (${fmtInt(data.journeyState.xp)} / ${fmtInt(data.journeyState.xpTargetForCurrentBanner)} XP)")
                append("🔥 Streak: ${data.streakState.currentStreak} Hari • 🎧 Total: ${fmtInt(data.journeyState.totalHours)} Jam (${fmtInt(data.journeyState.totalSongsPlayed)} Lagu)")
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                clipData = ClipData.newRawUri("Statistik Profil Musik", contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            withContext(Dispatchers.Main) {
                context.startActivity(
                    Intent.createChooser(sendIntent, "Bagikan Gambar Statistik Profil")
                )
            }
        }.onFailure {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gagal membagikan gambar: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun renderBitmap(
        context: Context,
        data: ProfileShareCanvasData,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(CANVAS_W, CANVAS_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val boldTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val mediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val regularTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        // =========================================================================
        // 0. BACKGROUND LAYER (Obsidian Dark + Silk Waves + Vinyl Grooves)
        // =========================================================================
        paint.shader = LinearGradient(
            0f, 0f, 0f, CANVAS_H.toFloat(),
            intArrayOf(
                AndroidColor.parseColor("#07080C"),
                AndroidColor.parseColor("#040507"),
                AndroidColor.parseColor("#08090E")
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, CANVAS_W.toFloat(), CANVAS_H.toFloat(), paint)
        paint.shader = null

        // Decorative silk wave at top
        val wavePath = Path().apply {
            moveTo(0f, 380f)
            cubicTo(CANVAS_W * 0.35f, 240f, CANVAS_W * 0.58f, 50f, CANVAS_W * 0.78f, 0f)
            lineTo(CANVAS_W.toFloat(), 0f)
            lineTo(CANVAS_W.toFloat(), 540f)
            cubicTo(CANVAS_W * 0.65f, 430f, CANVAS_W * 0.32f, 480f, 0f, 380f)
            close()
        }
        paint.shader = LinearGradient(
            0f, 0f, 0f, 540f,
            AndroidColor.argb(14, 255, 255, 255),
            AndroidColor.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(wavePath, paint)
        paint.shader = null

        val padX = 48f
        var curY = 46f

        // =========================================================================
        // 1. TOP HEADER BAR: Waveform Logo + "Profil & Statistik Musik" + Date Stamp
        // =========================================================================
        drawWaveformLogo(canvas, padX, curY + 6f, 54f, 54f)

        textPaint.typeface = boldTypeface
        textPaint.color = AndroidColor.WHITE
        textPaint.textSize = 44f
        canvas.drawText("Profil", padX + 74f, curY + 47f, textPaint)

        // Date & Brand Pill on Top Right
        val dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm", Locale("id", "ID")))
        textPaint.typeface = mediumTypeface
        textPaint.textSize = 23f
        val dateW = textPaint.measureText(dateStr)
        val pillRect = RectF(CANVAS_W - padX - dateW - 44f, curY + 6f, CANVAS_W - padX, curY + 58f)
        paint.style = Paint.Style.FILL
        paint.color = AndroidColor.parseColor("#14161D")
        canvas.drawRoundRect(pillRect, 26f, 26f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = AndroidColor.argb(38, 255, 255, 255)
        canvas.drawRoundRect(pillRect, 26f, 26f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = AndroidColor.parseColor("#CBD5E1")
        canvas.drawText(dateStr, pillRect.left + 22f, curY + 40f, textPaint)

        curY += 86f

        // =========================================================================
        // 2. PROFILE IDENTITY & BANNER CARD (Supports Custom Photo / Video Frame)
        // =========================================================================
        val bannerH = 264f
        val bannerRect = RectF(padX, curY, CANVAS_W - padX, curY + bannerH)
        drawCardContainer(canvas, bannerRect, 40f)

        // Clip inside bannerRect to draw custom photo/video frame or vinyl rings
        val saveBannerCount = canvas.save()
        val bannerClipPath = Path().apply {
            addRoundRect(bannerRect, 40f, 40f, Path.Direction.CW)
        }
        canvas.clipPath(bannerClipPath)

        val bannerBmp = loadBannerBitmap(context, data.bannerUri, data.bannerIsVideo)
        if (bannerBmp != null) {
            drawCenterCropBitmap(canvas, bannerBmp, bannerRect)
            // Dark scrim for legibility
            paint.shader = LinearGradient(
                bannerRect.left, bannerRect.top, bannerRect.left, bannerRect.bottom,
                intArrayOf(
                    AndroidColor.argb(95, 5, 6, 9),
                    AndroidColor.argb(185, 8, 9, 13),
                    AndroidColor.argb(230, 8, 9, 13)
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(bannerRect, paint)
            paint.shader = null
        } else {
            // Subtle vinyl grooves on right side of the banner
            val vx = bannerRect.right - 60f
            val vy = bannerRect.centerY()
            val maxR = 230f
            paint.shader = RadialGradient(
                vx, vy, maxR,
                intArrayOf(
                    AndroidColor.argb(26, 255, 255, 255),
                    AndroidColor.argb(8, 255, 255, 255),
                    AndroidColor.TRANSPARENT
                ),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(vx, vy, maxR, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            for (ring in 1..6) {
                val r = maxR * (0.28f + ring * 0.11f)
                paint.strokeWidth = if (ring == 3) 3.5f else 1.8f
                paint.color = AndroidColor.argb(if (ring % 2 == 0) 24 else 12, 255, 255, 255)
                canvas.drawCircle(vx, vy, r, paint)
            }
            paint.style = Paint.Style.FILL
        }

        // Avatar Circle on Left of Banner
        val avatarCx = bannerRect.left + 122f
        val avatarCy = bannerRect.centerY()
        val avatarR = 84f

        // Avatar outer glowing ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.shader = LinearGradient(
            avatarCx - avatarR, avatarCy - avatarR, avatarCx + avatarR, avatarCy + avatarR,
            intArrayOf(AndroidColor.WHITE, AndroidColor.parseColor("#94A3B8"), AndroidColor.WHITE),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(avatarCx, avatarCy, avatarR, paint)
        paint.shader = null
        paint.style = Paint.Style.FILL

        val innerAvatarR = avatarR - 7f
        paint.color = AndroidColor.parseColor("#14161D")
        canvas.drawCircle(avatarCx, avatarCy, innerAvatarR, paint)

        val avatarBmp = loadImageBitmap(context, data.avatarUri, 300)
        if (avatarBmp != null) {
            drawCircularBitmap(canvas, avatarBmp, avatarCx, avatarCy, innerAvatarR)
        } else {
            // Minimalist empty avatar silhouette
            drawEmptyAvatarSilhouette(canvas, avatarCx, avatarCy, innerAvatarR)
        }

        // Name + Verified Badge + @handle + Bio + Summary Pills
        val infoX = avatarCx + avatarR + 36f
        val maxNameW = bannerRect.right - infoX - 90f

        textPaint.typeface = boldTypeface
        textPaint.textSize = 46f
        textPaint.color = AndroidColor.WHITE
        val fittedName = ellipsizeText(data.displayName, textPaint, maxNameW)
        val nameY = bannerRect.top + 96f
        canvas.drawText(fittedName, infoX, nameY, textPaint)

        val measuredNameW = textPaint.measureText(fittedName)
        drawVerifiedSeal(canvas, infoX + measuredNameW + 24f, nameY - 15f, 18f)

        textPaint.typeface = mediumTypeface
        textPaint.textSize = 29f
        textPaint.color = AndroidColor.parseColor("#D0D5E0")
        val handleY = nameY + 42f
        canvas.drawText("${data.displayHandle}  ✦", infoX, handleY, textPaint)

        textPaint.typeface = regularTypeface
        textPaint.textSize = 26f
        textPaint.color = AndroidColor.parseColor("#98A0B0")
        val bioText = ellipsizeText(data.bio.ifBlank { "Belum ada bio" }, textPaint, bannerRect.right - infoX - 36f)
        canvas.drawText(bioText, infoX, handleY + 40f, textPaint)

        // Small badge pill at bottom of identity column showing Level & Streak quick status
        val quickBadgeText = "Lv.${data.journeyState.levelSpec.level} ${data.journeyState.levelSpec.title}  •  🔥 ${data.streakState.currentStreak} Hari"
        textPaint.typeface = boldTypeface
        textPaint.textSize = 21f
        val qbW = textPaint.measureText(quickBadgeText) + 32f
        val qbRect = RectF(infoX, handleY + 58f, (infoX + qbW).coerceAtMost(bannerRect.right - 28f), handleY + 98f)
        paint.color = AndroidColor.argb(140, 20, 23, 31)
        canvas.drawRoundRect(qbRect, 20f, 20f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = AndroidColor.argb(55, 255, 255, 255)
        canvas.drawRoundRect(qbRect, 20f, 20f, paint)
        paint.style = Paint.Style.FILL
        textPaint.color = AndroidColor.WHITE
        canvas.drawText(
            ellipsizeText(quickBadgeText, textPaint, qbRect.width() - 24f),
            qbRect.left + 16f,
            qbRect.centerY() + 7f,
            textPaint
        )

        canvas.restoreToCount(saveBannerCount)

        curY += bannerH + 26f

        // =========================================================================
        // 3. LEVEL PENDENGAR CARD (Hexagonal Winged Headphone Emblem + Detailed XP)
        // =========================================================================
        val levelSpec = data.journeyState.levelSpec
        val levelCardH = 218f
        val levelRect = RectF(padX, curY, CANVAS_W - padX, curY + levelCardH)
        drawCardContainer(canvas, levelRect, 38f)

        // Soft radial highlight behind emblem
        paint.shader = RadialGradient(
            levelRect.left + 125f, levelRect.centerY(), 170f,
            AndroidColor.argb(30, 255, 255, 255),
            AndroidColor.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(levelRect.left + 125f, levelRect.centerY(), 170f, paint)
        paint.shader = null

        drawWingedHexHeadphoneEmblem(
            canvas = canvas,
            cx = levelRect.left + 118f,
            cy = levelRect.centerY(),
            size = 152f
        )

        val lvTextX = levelRect.left + 226f
        textPaint.typeface = mediumTypeface
        textPaint.textSize = 26f
        textPaint.color = AndroidColor.parseColor("#CBD5E1")
        canvas.drawText("Level ${levelSpec.level}  •  Gelar Pendengar", lvTextX, levelRect.top + 54f, textPaint)

        textPaint.typeface = boldTypeface
        textPaint.textSize = 42f
        textPaint.color = AndroidColor.WHITE
        canvas.drawText(levelSpec.title, lvTextX, levelRect.top + 102f, textPaint)

        textPaint.typeface = regularTypeface
        textPaint.textSize = 24f
        textPaint.color = AndroidColor.parseColor("#989EA9")
        val xpTarget = data.journeyState.xpTargetForCurrentBanner.coerceAtLeast(1)
        val xpRemaining = (xpTarget - data.journeyState.xp).coerceAtLeast(0)
        val subDetail = if (levelSpec.level < 10) {
            "${levelSpec.subtitle} (Kurang ${fmtInt(xpRemaining)} XP ke level berikutnya)"
        } else {
            levelSpec.subtitle
        }
        canvas.drawText(
            ellipsizeText(subDetail, textPaint, levelRect.right - lvTextX - 36f),
            lvTextX,
            levelRect.top + 138f,
            textPaint
        )

        // Progress Bar + XP Counter
        val xpLabel = "${fmtInt(data.journeyState.xp)} / ${fmtInt(xpTarget)} XP"
        textPaint.typeface = boldTypeface
        textPaint.textSize = 25f
        val xpLabelW = textPaint.measureText(xpLabel)
        val barRight = levelRect.right - 36f - xpLabelW - 24f
        val barRect = RectF(lvTextX, levelRect.top + 162f, barRight, levelRect.top + 184f)
        paint.color = AndroidColor.argb(28, 255, 255, 255)
        canvas.drawRoundRect(barRect, 12f, 12f, paint)

        val progressFrac = data.journeyState.xpProgressFraction.coerceIn(0f, 1f)
        if (progressFrac > 0f) {
            val fillW = (barRect.width() * progressFrac).coerceAtLeast(18f)
            val fillRect = RectF(barRect.left, barRect.top, barRect.left + fillW, barRect.bottom)
            paint.shader = LinearGradient(
                fillRect.left, fillRect.top, fillRect.right, fillRect.bottom,
                AndroidColor.parseColor("#CBD5E1"),
                AndroidColor.WHITE,
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(fillRect, 12f, 12f, paint)
            paint.shader = null
        }

        textPaint.color = AndroidColor.WHITE
        canvas.drawText(xpLabel, levelRect.right - 36f - xpLabelW, levelRect.top + 181f, textPaint)

        curY += levelCardH + 26f

        // =========================================================================
        // 4. 3-CARD ROW: 🔥 Streak | ⏱️ Rekor Harian | 📅 Rekor Bulanan (Detailed)
        // =========================================================================
        val colGap = 20f
        val totalColsW = CANVAS_W - padX * 2 - colGap * 2
        val colW = totalColsW / 3f
        val recordCardH = 224f

        val streakDays = data.streakState.currentStreak
        val longestStreak = maxOf(data.streakState.longestStreak, data.journeyState.longestStreak, streakDays)
        val streakTier = StreakTier.fromStreak(streakDays)

        // Card 4A: Streak
        val c1Rect = RectF(padX, curY, padX + colW, curY + recordCardH)
        drawCardContainer(canvas, c1Rect, 34f)
        paint.shader = RadialGradient(
            c1Rect.left + 72f, c1Rect.top + 76f, 140f,
            streakTier.glowColor.copy(alpha = if (streakDays > 0) 0.28f else 0.08f).toArgb(),
            AndroidColor.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(c1Rect.left + 72f, c1Rect.top + 76f, 140f, paint)
        paint.shader = null

        drawColorfulStreakFlame(
            canvas = canvas,
            cx = c1Rect.left + 58f,
            cy = c1Rect.top + 74f,
            size = 78f,
            tier = streakTier
        )

        textPaint.typeface = mediumTypeface
        textPaint.textSize = 23f
        textPaint.color = AndroidColor.parseColor("#B0B6C2")
        canvas.drawText("Streak", c1Rect.left + 106f, c1Rect.top + 60f, textPaint)

        textPaint.typeface = boldTypeface
        textPaint.textSize = 36f
        textPaint.color = AndroidColor.WHITE
        canvas.drawText("$streakDays Hari", c1Rect.left + 106f, c1Rect.top + 102f, textPaint)

        textPaint.typeface = boldTypeface
        textPaint.textSize = 21f
        textPaint.color = streakTier.midColor.toArgb()
        canvas.drawText(
            ellipsizeText(streakTier.title, textPaint, c1Rect.width() - 44f),
            c1Rect.left + 24f,
            c1Rect.top + 156f,
            textPaint
        )

        textPaint.typeface = regularTypeface
        textPaint.textSize = 20f
        textPaint.color = AndroidColor.parseColor("#989EA9")
        canvas.drawText("Rekor Terlama: $longestStreak Hari", c1Rect.left + 24f, c1Rect.top + 190f, textPaint)

        // Card 4B: Rekor Harian
        val c2Left = c1Rect.right + colGap
        val c2Rect = RectF(c2Left, curY, c2Left + colW, curY + recordCardH)
        drawCardContainer(canvas, c2Rect, 34f)
        drawClockIcon(canvas, c2Rect.left + 54f, c2Rect.top + 74f, 28f)

        textPaint.typeface = mediumTypeface
        textPaint.textSize = 23f
        textPaint.color = AndroidColor.parseColor("#B0B6C2")
        canvas.drawText("Rekor Harian", c2Rect.left + 98f, c2Rect.top + 60f, textPaint)

        textPaint.typeface = boldTypeface
        textPaint.textSize = 34f
        textPaint.color = AndroidColor.WHITE
        canvas.drawText(
            ellipsizeText(data.journeyState.dailyRecordCompactText, textPaint, c2Rect.width() - 112f),
            c2Rect.left + 98f,
            c2Rect.top + 102f,
            textPaint
        )

        textPaint.typeface = boldTypeface
        textPaint.textSize = 21f
        textPaint.color = AndroidColor.parseColor("#E2E8F0")
        canvas.drawText(
            ellipsizeText("★ ${data.journeyState.dailyRecordFullText}", textPaint, c2Rect.width() - 44f),
            c2Rect.left + 24f,
            c2Rect.top + 156f,
            textPaint
        )

        val todayListenSec = data.journeyState.dailyHistory.firstOrNull()?.listeningSeconds ?: 0L
        val todayMins = (todayListenSec / 60L).toInt()
        textPaint.typeface = regularTypeface
        textPaint.textSize = 20f
        textPaint.color = AndroidColor.parseColor("#989EA9")
        canvas.drawText("Hari ini: $todayMins Menit", c2Rect.left + 24f, c2Rect.top + 190f, textPaint)

        // Card 4C: Rekor Bulanan
        val c3Left = c2Rect.right + colGap
        val c3Rect = RectF(c3Left, curY, c3Left + colW, curY + recordCardH)
        drawCardContainer(canvas, c3Rect, 34f)
        drawCalendarIcon(canvas, c3Rect.left + 54f, c3Rect.top + 74f, 28f)

        textPaint.typeface = mediumTypeface
        textPaint.textSize = 23f
        textPaint.color = AndroidColor.parseColor("#B0B6C2")
        canvas.drawText("Rekor Bulanan", c3Rect.left + 98f, c3Rect.top + 60f, textPaint)

        textPaint.typeface = boldTypeface
        textPaint.textSize = 34f
        textPaint.color = AndroidColor.WHITE
        canvas.drawText(
            ellipsizeText(data.journeyState.monthlyRecordHoursText, textPaint, c3Rect.width() - 112f),
            c3Rect.left + 98f,
            c3Rect.top + 102f,
            textPaint
        )

        val bestMonthTitle = data.journeyState.monthlyHistory.maxByOrNull { it.listeningSeconds }?.displayTitle
            ?: "Bulan Terbaik"
        textPaint.typeface = boldTypeface
        textPaint.textSize = 21f
        textPaint.color = AndroidColor.parseColor("#E2E8F0")
        canvas.drawText(
            ellipsizeText("👑 $bestMonthTitle", textPaint, c3Rect.width() - 44f),
            c3Rect.left + 24f,
            c3Rect.top + 156f,
            textPaint
        )

        textPaint.typeface = regularTypeface
        textPaint.textSize = 20f
        textPaint.color = AndroidColor.parseColor("#989EA9")
        canvas.drawText(
            "Total: ${data.journeyState.monthlyHistory.size} Bulan tercatat",
            c3Rect.left + 24f,
            c3Rect.top + 190f,
            textPaint
        )

        curY += recordCardH + 26f

        // =========================================================================
        // 5. RINGKASAN MENDENGARKAN + WAVEFORM + POLA 7 HARI (SEN - MIN)
        // =========================================================================
        val summaryCardH = 446f
        val summaryRect = RectF(padX, curY, CANVAS_W - padX, curY + summaryCardH)
        drawCardContainer(canvas, summaryRect, 38f)

        drawBarChartMiniIcon(canvas, summaryRect.left + 36f, summaryRect.top + 34f, 34f)
        textPaint.typeface = boldTypeface
        textPaint.textSize = 32f
        textPaint.color = AndroidColor.WHITE
        canvas.drawText("Ringkasan & Pola Mendengarkan", summaryRect.left + 84f, summaryRect.top + 62f, textPaint)

        // Left big stat: Jam / Menit
        val totalSec = data.journeyState.totalListeningSeconds
        val totalHrs = (totalSec / 3600L).toInt()
        val totalMins = ((totalSec % 3600L) / 60L).toInt()
        val primaryDurationNum = if (totalHrs == 0 && totalSec > 0L) fmtInt(totalMins.coerceAtLeast(1)) else fmtInt(totalHrs)
        val primaryDurationUnit = if (totalHrs == 0 && totalSec > 0L) "Menit" else "Jam"

        textPaint.typeface = boldTypeface
        textPaint.textSize = 72f
        textPaint.color = AndroidColor.WHITE
        val numY = summaryRect.top + 154f
        canvas.drawText(primaryDurationNum, summaryRect.left + 38f, numY, textPaint)
        val numW = textPaint.measureText(primaryDurationNum)

        textPaint.textSize = 40f
        canvas.drawText(primaryDurationUnit, summaryRect.left + 52f + numW, numY, textPaint)

        textPaint.typeface = regularTypeface
        textPaint.textSize = 23f
        textPaint.color = AndroidColor.parseColor("#9EA4B0")
        val subDurationText = if (totalHrs > 0) {
            "Total Waktu Mendengarkan (${fmtInt(totalHrs)}j ${totalMins}m)"
        } else {
            "Total Waktu Mendengarkan"
        }
        canvas.drawText(subDurationText, summaryRect.left + 38f, numY + 38f, textPaint)

        // Vertical Divider
        val divX = summaryRect.centerX() + 20f
        paint.color = AndroidColor.argb(38, 255, 255, 255)
        canvas.drawRect(divX, summaryRect.top + 92f, divX + 2f, summaryRect.top + 200f, paint)

        // Right big stat: Total Lagu
        textPaint.typeface = boldTypeface
        textPaint.textSize = 58f
        textPaint.color = AndroidColor.WHITE
        canvas.drawText("♪ ${fmtInt(data.journeyState.totalSongsPlayed)}", divX + 38f, numY - 6f, textPaint)

        textPaint.typeface = regularTypeface
        textPaint.textSize = 23f
        textPaint.color = AndroidColor.parseColor("#9EA4B0")
        canvas.drawText("Total Lagu Diputar", divX + 38f, numY + 38f, textPaint)

        // Waveform across middle of card
        drawDenseWaveform(
            canvas = canvas,
            left = summaryRect.left + 38f,
            top = summaryRect.top + 220f,
            width = summaryRect.width() - 76f,
            height = 68f,
            isActive = totalSec > 0L
        )

        // 7-Day Weekly Pattern Mini Bar Graph (Sen .. Min) at bottom of Summary Card
        val dayLabels = arrayOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
        val maxDaySec = (data.journeyState.weekdaySeconds.maxOrNull() ?: 0L).coerceAtLeast(1L)
        val chartTop = summaryRect.top + 314f
        val chartBottom = summaryRect.bottom - 28f
        val slotW = (summaryRect.width() - 76f) / 7f

        for (i in 0 until 7) {
            val sec = data.journeyState.weekdaySeconds.getOrElse(i) { 0L }
            val frac = if (sec <= 0L) 0.08f else (sec.toFloat() / maxDaySec.toFloat()).coerceIn(0.15f, 1f)
            val slotLeft = summaryRect.left + 38f + i * slotW
            val slotCx = slotLeft + slotW * 0.5f

            // Track bar
            val barMaxH = 58f
            val barW = 36f
            val trackRect = RectF(slotCx - barW / 2f, chartTop, slotCx + barW / 2f, chartTop + barMaxH)
            paint.color = AndroidColor.argb(22, 255, 255, 255)
            canvas.drawRoundRect(trackRect, 10f, 10f, paint)

            val fillH = (barMaxH * frac).coerceAtLeast(6f)
            val fillRect = RectF(
                slotCx - barW / 2f,
                chartTop + barMaxH - fillH,
                slotCx + barW / 2f,
                chartTop + barMaxH
            )
            paint.color = if (sec > 0L) AndroidColor.WHITE else AndroidColor.argb(55, 255, 255, 255)
            canvas.drawRoundRect(fillRect, 10f, 10f, paint)

            // Day label + duration below
            textPaint.typeface = boldTypeface
            textPaint.textSize = 20f
            textPaint.color = AndroidColor.parseColor("#D0D5E0")
            val dlW = textPaint.measureText(dayLabels[i])
            canvas.drawText(dayLabels[i], slotCx - dlW / 2f, chartBottom - 18f, textPaint)

            val durLabel = when {
                sec >= 3600L -> String.format(Locale.US, "%.1fj", sec / 3600.0)
                sec > 0L -> "${(sec / 60L).coerceAtLeast(1L)}m"
                else -> "0m"
            }
            textPaint.typeface = regularTypeface
            textPaint.textSize = 18f
            textPaint.color = AndroidColor.parseColor("#9096A4")
            val durW = textPaint.measureText(durLabel)
            canvas.drawText(durLabel, slotCx - durW / 2f, chartBottom + 6f, textPaint)
        }

        curY += summaryCardH + 26f

        // =========================================================================
        // 6. LAGU PALING SERING DIDENGAR (Detailed Top Songs List)
        // =========================================================================
        val topSongsCardH = 440f
        val topSongsRect = RectF(padX, curY, CANVAS_W - padX, curY + topSongsCardH)
        drawCardContainer(canvas, topSongsRect, 38f)

        drawStarPolygon(
            canvas = canvas,
            cx = topSongsRect.left + 52f,
            cy = topSongsRect.top + 50f,
            outerR = 18f,
            innerR = 8f,
            color = AndroidColor.parseColor("#E2E8F0")
        )
        textPaint.typeface = boldTypeface
        textPaint.textSize = 32f
        textPaint.color = AndroidColor.WHITE
        canvas.drawText("Lagu Paling Sering Didengar", topSongsRect.left + 84f, topSongsRect.top + 62f, textPaint)

        val songsToDisplay = data.topSongs.take(4)
        if (songsToDisplay.isEmpty()) {
            // Empty state box matching 0 initial state
            val emptyRect = RectF(
                topSongsRect.left + 36f,
                topSongsRect.top + 96f,
                topSongsRect.right - 36f,
                topSongsRect.bottom - 36f
            )
            paint.color = AndroidColor.parseColor("#141720")
            canvas.drawRoundRect(emptyRect, 28f, 28f, paint)

            drawGeometricPrismArt(
                canvas = canvas,
                left = emptyRect.left + 36f,
                top = emptyRect.centerY() - 56f,
                size = 112f
            )

            textPaint.typeface = boldTypeface
            textPaint.textSize = 34f
            textPaint.color = AndroidColor.WHITE
            canvas.drawText("Belum Ada Lagu Diputar", emptyRect.left + 176f, emptyRect.centerY() - 10f, textPaint)

            textPaint.typeface = regularTypeface
            textPaint.textSize = 24f
            textPaint.color = AndroidColor.parseColor("#9EA4B0")
            canvas.drawText(
                "Putar musik untuk mulai mencatat lagu favoritmu • 0x diputar",
                emptyRect.left + 176f,
                emptyRect.centerY() + 34f,
                textPaint
            )
        } else {
            var rowY = topSongsRect.top + 88f
            val rowH = 76f
            songsToDisplay.forEachIndexed { idx, song ->
                val rowRect = RectF(
                    topSongsRect.left + 32f,
                    rowY,
                    topSongsRect.right - 32f,
                    rowY + rowH
                )
                paint.color = if (idx == 0) AndroidColor.argb(32, 255, 255, 255) else AndroidColor.parseColor("#14161E")
                canvas.drawRoundRect(rowRect, 22f, 22f, paint)
                if (idx == 0) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 2f
                    paint.color = AndroidColor.argb(80, 255, 255, 255)
                    canvas.drawRoundRect(rowRect, 22f, 22f, paint)
                    paint.style = Paint.Style.FILL
                }

                val rankLabel = when (idx) {
                    0 -> "#1"
                    1 -> "#2"
                    2 -> "#3"
                    else -> "#${idx + 1}"
                }
                textPaint.typeface = boldTypeface
                textPaint.textSize = 26f
                textPaint.color = if (idx == 0) AndroidColor.WHITE else AndroidColor.parseColor("#CBD5E1")
                canvas.drawText(rankLabel, rowRect.left + 22f, rowRect.centerY() + 9f, textPaint)

                val songTitleMaxW = rowRect.width() - 310f
                textPaint.typeface = boldTypeface
                textPaint.textSize = 26f
                textPaint.color = AndroidColor.WHITE
                canvas.drawText(
                    ellipsizeText(song.title, textPaint, songTitleMaxW),
                    rowRect.left + 84f,
                    rowRect.top + 33f,
                    textPaint
                )

                textPaint.typeface = regularTypeface
                textPaint.textSize = 21f
                textPaint.color = AndroidColor.parseColor("#9EA4B0")
                canvas.drawText(
                    ellipsizeText(song.artist, textPaint, songTitleMaxW),
                    rowRect.left + 84f,
                    rowRect.top + 61f,
                    textPaint
                )

                val playBadge = "${song.playCount}x diputar"
                textPaint.typeface = boldTypeface
                textPaint.textSize = 23f
                textPaint.color = AndroidColor.WHITE
                val pbW = textPaint.measureText(playBadge)
                canvas.drawText(playBadge, rowRect.right - 26f - pbW, rowRect.centerY() + 8f, textPaint)

                rowY += rowH + 10f
            }
        }

        curY += topSongsCardH + 26f

        // =========================================================================
        // 7. ACHIEVEMENT TROPHY BANNER + FOOTER WATERMARK
        // =========================================================================
        val achCardH = 162f
        val achRect = RectF(padX, curY, CANVAS_W - padX, curY + achCardH)
        drawCardContainer(canvas, achRect, 36f)

        drawTrophyEmblem(canvas, achRect.left + 72f, achRect.centerY(), 86f)

        paint.color = AndroidColor.argb(38, 255, 255, 255)
        canvas.drawRect(achRect.left + 138f, achRect.top + 32f, achRect.left + 140f, achRect.bottom - 32f, paint)

        val achTextX = achRect.left + 168f
        textPaint.typeface = regularTypeface
        textPaint.textSize = 22f
        textPaint.color = AndroidColor.parseColor("#A0A6B4")
        canvas.drawText("Pencapaian Perjalanan Musik", achTextX, achRect.top + 50f, textPaint)

        textPaint.typeface = boldTypeface
        textPaint.textSize = 33f
        textPaint.color = AndroidColor.WHITE
        canvas.drawText(
            ellipsizeText("Level ${levelSpec.level} – ${levelSpec.title}!", textPaint, achRect.width() - 300f),
            achTextX,
            achRect.top + 92f,
            textPaint
        )

        textPaint.typeface = regularTypeface
        textPaint.textSize = 22f
        textPaint.color = AndroidColor.parseColor("#9096A4")
        canvas.drawText("@BeatlesMusic • Statistik Perjalanan Musik Resmi", achTextX, achRect.top + 128f, textPaint)

        drawRisingBarsIcon(canvas, achRect.right - 108f, achRect.centerY() - 40f, 76f)

        return bmp
    }

    private fun drawCardContainer(canvas: Canvas, rect: RectF, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom,
            AndroidColor.parseColor("#14151A"),
            AndroidColor.parseColor("#0B0C10"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f
        paint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(
                AndroidColor.argb(46, 255, 255, 255),
                AndroidColor.argb(14, 255, 255, 255),
                AndroidColor.argb(36, 255, 255, 255)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.shader = null
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f) return ""
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "..."
        val ellipsisW = paint.measureText(ellipsis)
        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + ellipsisW > maxWidth) {
            end--
        }
        return if (end > 0) text.substring(0, end) + ellipsis else ""
    }

    private fun loadImageBitmap(context: Context, uriString: String, maxDim: Int): Bitmap? {
        if (uriString.isBlank()) return null
        return runCatching {
            val cleanUri = Uri.parse(uriString.substringBefore("?t="))
            val bytes = when (cleanUri.scheme) {
                "file" -> cleanUri.path?.let { File(it).readBytes() }
                else -> context.contentResolver.openInputStream(cleanUri)?.use { it.readBytes() }
            } ?: return null

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
            var sampleSize = 1
            while (boundsOpts.outWidth / sampleSize > maxDim * 2 || boundsOpts.outHeight / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
        }.getOrNull()
    }

    private fun loadBannerBitmap(context: Context, uriString: String, isVideo: Boolean): Bitmap? {
        if (uriString.isBlank()) return null
        if (!isVideo) {
            return loadImageBitmap(context, uriString, 1080)
        }
        return runCatching {
            val cleanUri = Uri.parse(uriString.substringBefore("?t="))
            val retriever = MediaMetadataRetriever()
            try {
                if (cleanUri.scheme == "file" && cleanUri.path != null) {
                    retriever.setDataSource(cleanUri.path)
                } else {
                    retriever.setDataSource(context, cleanUri)
                }
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.frameAtTime
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()
    }

    private fun drawCenterCropBitmap(canvas: Canvas, bmp: Bitmap, targetRect: RectF) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val scale = maxOf(
            targetRect.width() / bmp.width.toFloat(),
            targetRect.height() / bmp.height.toFloat()
        )
        val dx = targetRect.centerX() - (bmp.width * scale) / 2f
        val dy = targetRect.centerY() - (bmp.height * scale) / 2f
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dx, dy)
        }
        canvas.drawBitmap(bmp, matrix, paint)
    }

    private fun drawCircularBitmap(canvas: Canvas, bmp: Bitmap, cx: Float, cy: Float, radius: Float) {
        val diameter = radius * 2f
        val scale = maxOf(diameter / bmp.width.toFloat(), diameter / bmp.height.toFloat())
        val shaderMatrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(cx - (bmp.width * scale) / 2f, cy - (bmp.height * scale) / 2f)
        }
        val shader = BitmapShader(bmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(shaderMatrix)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
        }
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawEmptyAvatarSilhouette(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
            color = AndroidColor.argb(170, 255, 255, 255)
        }
        canvas.drawCircle(cx, cy - radius * 0.22f, radius * 0.28f, paint)
        val path = Path().apply {
            moveTo(cx - radius * 0.48f, cy + radius * 0.52f)
            cubicTo(
                cx - radius * 0.42f, cy + radius * 0.12f,
                cx + radius * 0.42f, cy + radius * 0.12f,
                cx + radius * 0.48f, cy + radius * 0.52f
            )
        }
        canvas.drawPath(path, paint)
    }

    private fun drawWaveformLogo(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            strokeWidth = w * 0.13f
            strokeCap = Paint.Cap.ROUND
        }
        val heights = floatArrayOf(0.32f, 0.64f, 0.94f, 0.58f, 0.30f)
        val spacing = w * 0.19f
        val startX = x + w * 0.10f
        heights.forEachIndexed { idx, frac ->
            val bx = startX + idx * spacing
            val bh = h * frac
            val top = y + (h - bh) / 2f
            canvas.drawLine(bx, top, bx, top + bh, paint)
        }
    }

    private fun drawVerifiedSeal(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = AndroidColor.parseColor("#E5E9F0")
        }
        val points = 16
        val outerR = radius
        val innerR = radius * 0.80f
        val path = Path()
        for (i in 0 until points) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = (i * (360.0 / points)) * PI / 180.0
            val px = cx + cos(angle).toFloat() * r
            val py = cy + sin(angle).toFloat() * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.6f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = AndroidColor.parseColor("#0A0B0E")
        val check = Path().apply {
            moveTo(cx - radius * 0.34f, cy + radius * 0.04f)
            lineTo(cx - radius * 0.08f, cy + radius * 0.30f)
            lineTo(cx + radius * 0.38f, cy - radius * 0.24f)
        }
        canvas.drawPath(check, paint)
    }

    private fun drawWingedHexHeadphoneEmblem(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val w = size
        val h = size

        // Left & Right 3-tier metallic wings
        paint.style = Paint.Style.FILL
        paint.color = AndroidColor.parseColor("#E2E8F0")
        for (side in intArrayOf(-1, 1)) {
            for (tier in 0..2) {
                val wingTopY = cy - h * (0.16f - tier * 0.10f)
                val wingTipX = cx + side * w * (0.48f - tier * 0.04f)
                val wingBaseX = cx + side * w * (0.28f)
                val wingPath = Path().apply {
                    moveTo(wingBaseX, wingTopY)
                    lineTo(wingTipX, wingTopY - h * 0.06f)
                    lineTo(wingTipX - side * w * 0.04f, wingTopY + h * 0.05f)
                    lineTo(wingBaseX, wingTopY + h * 0.08f)
                    close()
                }
                canvas.drawPath(wingPath, paint)
            }
        }

        // Hexagon Shield
        val hexR = w * 0.34f
        val hexPath = Path()
        for (i in 0 until 6) {
            val angle = (i * 60.0 - 90.0) * PI / 180.0
            val px = cx + cos(angle).toFloat() * hexR
            val py = cy + sin(angle).toFloat() * hexR
            if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
        }
        hexPath.close()

        paint.shader = LinearGradient(
            cx, cy - hexR, cx, cy + hexR,
            AndroidColor.parseColor("#282C35"),
            AndroidColor.parseColor("#12141A"),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(hexPath, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = AndroidColor.WHITE
        canvas.drawPath(hexPath, paint)

        // Headphones inside hexagon
        val hpR = w * 0.14f
        val arcRect = RectF(cx - hpR, cy - hpR * 0.95f, cx + hpR, cy + hpR * 0.95f)
        paint.strokeWidth = 4.8f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(arcRect, 180f, 180f, false, paint)

        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            RectF(cx - hpR - 5f, cy - hpR * 0.1f, cx - hpR + 5f, cy + hpR * 0.55f),
            4f, 4f, paint
        )
        canvas.drawRoundRect(
            RectF(cx + hpR - 5f, cy - hpR * 0.1f, cx + hpR + 5f, cy + hpR * 0.55f),
            4f, 4f, paint
        )

        drawStarPolygon(canvas, cx, cy + hexR * 0.92f, w * 0.085f, w * 0.04f, AndroidColor.WHITE)
    }

    private fun drawColorfulStreakFlame(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        tier: StreakTier,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val topY = cy - size * 0.48f
        val bottomY = cy + size * 0.46f
        val halfW = size * 0.38f * tier.thicknessScale.coerceIn(0.85f, 1.25f)

        // Outer Flame
        val outerPath = Path().apply {
            moveTo(cx, topY)
            cubicTo(cx + halfW * 1.05f, cy - size * 0.14f, cx + halfW * 1.12f, cy + size * 0.25f, cx, bottomY)
            cubicTo(cx - halfW * 1.12f, cy + size * 0.25f, cx - halfW * 1.05f, cy - size * 0.14f, cx, topY)
            close()
        }
        paint.shader = LinearGradient(
            cx, topY, cx, bottomY,
            tier.midColor.toArgb(),
            tier.outerColor.toArgb(),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(outerPath, paint)

        // Inner Core Flame
        val corePath = Path().apply {
            moveTo(cx, topY + size * 0.22f)
            cubicTo(cx + halfW * 0.56f, cy + size * 0.02f, cx + halfW * 0.60f, cy + size * 0.28f, cx, bottomY - size * 0.06f)
            cubicTo(cx - halfW * 0.60f, cy + size * 0.28f, cx - halfW * 0.56f, cy + size * 0.02f, cx, topY + size * 0.22f)
            close()
        }
        paint.shader = LinearGradient(
            cx, topY + size * 0.22f, cx, bottomY,
            tier.coreColor.toArgb(),
            tier.midColor.toArgb(),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(corePath, paint)
        paint.shader = null

        // Rising embers if lit
        if (tier != StreakTier.UNLIT) {
            paint.color = tier.coreColor.toArgb()
            canvas.drawCircle(cx - halfW * 0.45f, topY + size * 0.12f, 3.8f, paint)
            paint.color = tier.midColor.toArgb()
            canvas.drawCircle(cx + halfW * 0.42f, topY + size * 0.05f, 3.2f, paint)
            canvas.drawCircle(cx + halfW * 0.15f, topY - size * 0.06f, 2.8f, paint)
        }
    }

    private fun drawClockIcon(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.2f
            strokeCap = Paint.Cap.ROUND
            color = AndroidColor.parseColor("#E2E8F0")
        }
        canvas.drawCircle(cx, cy, r, paint)
        paint.color = AndroidColor.WHITE
        canvas.drawLine(cx, cy, cx, cy - r * 0.55f, paint)
        canvas.drawLine(cx, cy, cx + r * 0.45f, cy, paint)
    }

    private fun drawCalendarIcon(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            color = AndroidColor.parseColor("#E2E8F0")
        }
        val rect = RectF(cx - r, cy - r * 0.82f, cx + r, cy + r * 0.92f)
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        canvas.drawLine(cx - r * 0.45f, cy - r, cx - r * 0.45f, cy - r * 0.62f, paint)
        canvas.drawLine(cx + r * 0.45f, cy - r, cx + r * 0.45f, cy - r * 0.62f, paint)
        canvas.drawLine(rect.left, cy - r * 0.25f, rect.right, cy - r * 0.25f, paint)
    }

    private fun drawBarChartMiniIcon(canvas: Canvas, left: Float, top: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = AndroidColor.WHITE
        }
        val bw = size * 0.22f
        canvas.drawRoundRect(RectF(left, top + size * 0.4f, left + bw, top + size), bw / 2f, bw / 2f, paint)
        canvas.drawRoundRect(RectF(left + size * 0.38f, top + size * 0.1f, left + size * 0.38f + bw, top + size), bw / 2f, bw / 2f, paint)
        canvas.drawRoundRect(RectF(left + size * 0.76f, top + size * 0.55f, left + size * 0.76f + bw, top + size), bw / 2f, bw / 2f, paint)
    }

    private fun drawDenseWaveform(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        isActive: Boolean,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val barCount = 72
        val step = width / barCount
        paint.strokeWidth = (step * 0.46f).coerceAtLeast(3f)

        for (i in 0 until barCount) {
            val norm = i.toFloat() / (barCount - 1).toFloat()
            val frac = if (isActive) {
                val env1 = exp(-((norm - 0.18f) * (norm - 0.18f)) / 0.012f)
                val env2 = exp(-((norm - 0.50f) * (norm - 0.50f)) / 0.016f)
                val env3 = exp(-((norm - 0.82f) * (norm - 0.82f)) / 0.014f)
                val ripple = 0.5f + 0.5f * sin(i * 1.35f)
                (0.18f + (env1 * 0.55f + env2 * 0.82f + env3 * 0.65f) * (0.45f + 0.55f * ripple))
                    .coerceIn(0.16f, 0.98f)
            } else {
                (0.16f + 0.07f * sin(i * 0.45f)).coerceIn(0.12f, 0.25f)
            }
            val bh = height * frac
            val x = left + i * step + step / 2f
            val alpha = if (isActive) ((0.38f + frac * 0.62f) * 255).toInt().coerceIn(85, 255) else 65
            paint.color = AndroidColor.argb(alpha, 255, 255, 255)
            canvas.drawLine(x, top + height - bh, x, top + height, paint)
        }
    }

    private fun drawGeometricPrismArt(canvas: Canvas, left: Float, top: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = AndroidColor.WHITE
        }
        val tri = Path().apply {
            moveTo(left + size * 0.55f, top + size * 0.10f)
            lineTo(left + size * 0.90f, top + size * 0.76f)
            lineTo(left + size * 0.12f, top + size * 0.64f)
            close()
        }
        canvas.drawPath(tri, paint)
        canvas.drawLine(
            left + size * 0.55f, top + size * 0.10f,
            left + size * 0.28f, top + size * 0.90f,
            paint
        )
    }

    private fun drawTrophyEmblem(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val half = size / 2f

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4.2f
        paint.color = AndroidColor.parseColor("#CBD5E1")
        canvas.drawArc(
            RectF(cx - half * 0.82f, cy - half * 0.62f, cx - half * 0.28f, cy - half * 0.02f),
            90f, 180f, false, paint
        )
        canvas.drawArc(
            RectF(cx + half * 0.28f, cy - half * 0.62f, cx + half * 0.82f, cy - half * 0.02f),
            -90f, 180f, false, paint
        )

        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            cx - half * 0.55f, cy, cx + half * 0.55f, cy,
            intArrayOf(
                AndroidColor.parseColor("#94A3B8"),
                AndroidColor.WHITE,
                AndroidColor.parseColor("#CBD5E1")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        val cup = Path().apply {
            moveTo(cx - half * 0.52f, cy - half * 0.72f)
            lineTo(cx + half * 0.52f, cy - half * 0.72f)
            cubicTo(cx + half * 0.48f, cy - half * 0.04f, cx + half * 0.24f, cy + half * 0.16f, cx, cy + half * 0.20f)
            cubicTo(cx - half * 0.24f, cy + half * 0.16f, cx - half * 0.48f, cy - half * 0.04f, cx - half * 0.52f, cy - half * 0.72f)
            close()
        }
        canvas.drawPath(cup, paint)
        paint.shader = null

        paint.color = AndroidColor.parseColor("#CBD5E1")
        canvas.drawRoundRect(
            RectF(cx - half * 0.12f, cy + half * 0.16f, cx + half * 0.12f, cy + half * 0.52f),
            4f, 4f, paint
        )
        canvas.drawRoundRect(
            RectF(cx - half * 0.44f, cy + half * 0.50f, cx + half * 0.44f, cy + half * 0.72f),
            8f, 8f, paint
        )
    }

    private fun drawRisingBarsIcon(canvas: Canvas, left: Float, top: Float, size: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val bw = size * 0.18f
        paint.color = AndroidColor.parseColor("#94A3B8")
        canvas.drawRoundRect(RectF(left + size * 0.12f, top + size * 0.62f, left + size * 0.12f + bw, top + size * 0.90f), 6f, 6f, paint)
        paint.color = AndroidColor.parseColor("#CBD5E1")
        canvas.drawRoundRect(RectF(left + size * 0.42f, top + size * 0.44f, left + size * 0.42f + bw, top + size * 0.90f), 6f, 6f, paint)
        paint.color = AndroidColor.WHITE
        canvas.drawRoundRect(RectF(left + size * 0.72f, top + size * 0.24f, left + size * 0.72f + bw, top + size * 0.90f), 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4.5f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(left + size * 0.14f, top + size * 0.48f, left + size * 0.76f, top + size * 0.10f, paint)
        canvas.drawLine(left + size * 0.54f, top + size * 0.10f, left + size * 0.76f, top + size * 0.10f, paint)
        canvas.drawLine(left + size * 0.76f, top + size * 0.10f, left + size * 0.76f, top + size * 0.30f, paint)
    }

    private fun drawStarPolygon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        outerR: Float,
        innerR: Float,
        color: Int,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        val path = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = (i * 36.0 - 90.0) * PI / 180.0
            val px = cx + cos(angle).toFloat() * r
            val py = cy + sin(angle).toFloat() * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, paint)
    }
}
