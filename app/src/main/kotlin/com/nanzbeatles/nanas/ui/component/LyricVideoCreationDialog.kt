/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import androidx.media3.common.Player
import androidx.media3.datasource.cache.SimpleCache
import coil3.compose.AsyncImage
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.AudioStyleMode
import com.nanzbeatles.nanas.constants.AudioStyleModeKey
import com.nanzbeatles.nanas.constants.VideoLyricsCardStyle
import com.nanzbeatles.nanas.constants.VideoLyricsCardStyleKey
import com.nanzbeatles.nanas.constants.VideoLyricsEffectStyle
import com.nanzbeatles.nanas.constants.VideoLyricsEffectStyleKey
import com.nanzbeatles.nanas.constants.VideoLyricsHideInactiveKey
import com.nanzbeatles.nanas.constants.VideoLyricsJedagJedugKey
import com.nanzbeatles.nanas.constants.VideoLyricsShowUpcomingKey
import com.nanzbeatles.nanas.constants.VideoLyricsSnowKey
import com.nanzbeatles.nanas.constants.VideoLyricsWaveAmplitudePercentKey
import com.nanzbeatles.nanas.constants.VideoLyricsWaveSpeedPercentKey
import androidx.compose.ui.platform.testTag
import com.nanzbeatles.nanas.extensions.toMediaItem
import com.nanzbeatles.nanas.lyrics.LyricsEntry
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.graphics.asImageBitmap
import com.nanzbeatles.nanas.lyricvideo.AudioAmplitudeAnalyzer
import com.nanzbeatles.nanas.lyricvideo.LyricVideoRenderer
import com.nanzbeatles.nanas.lyricvideo.LyricVideoShareUtils
import com.nanzbeatles.nanas.lyricvideo.QrBarcodeGenerator
import com.nanzbeatles.nanas.models.MediaMetadata
import android.os.SystemClock
import com.nanzbeatles.nanas.playback.audio.SilenceDetectorAudioProcessor
import com.nanzbeatles.nanas.playback.queues.ListQueue
import com.nanzbeatles.nanas.utils.ShareUtils
import com.nanzbeatles.nanas.utils.dataStore
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

enum class LyricVideoDuration(val seconds: Int, val label: String, val desc: String) {
    FIFTEEN(15, "15 Detik", "Instagram Story / WA Status"),
    THIRTY(30, "30 Detik", "Reels / Status Panjang"),
    SIXTY(60, "60 Detik", "TikTok / Video Pendek"),
    FULL(-1, "Lagu Penuh", "Seluruh Durasi Lagu")
}

@Composable
fun LyricVideoCreationDialog(
    mediaMetadata: MediaMetadata,
    lyrics: List<LyricsEntry>?,
    currentPlaybackPositionMs: Long = 0L,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current
    val downloadCache: SimpleCache? = playerConnection?.service?.downloadCache
    val playerCache: SimpleCache? = playerConnection?.service?.playerCache

    val songDurationSec = mediaMetadata.duration.takeIf { it > 0 } ?: 180
    val artistNames = remember(mediaMetadata.artists) {
        mediaMetadata.artists.joinToString { it.name }.ifBlank { "Beatles" }
    }

    val currentSongEntity by (playerConnection?.currentSong ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState(initial = null)
    val activeAudioStyle by (playerConnection?.service?.audioStyleMode ?: kotlinx.coroutines.flow.MutableStateFlow(AudioStyleMode.NORMAL)).collectAsState()
    val songLyricsOffsetMs = (currentSongEntity?.song?.lyricsOffset ?: 0).toLong()

    // Strictly exclude any translations or secondary lines from VideoLyrics, and apply song's lyricsOffset
    val videoLyricsOnly = remember(lyrics, songLyricsOffsetMs) {
        lyrics?.map { entry ->
            val primaryLine = entry.text.lineSequence().firstOrNull()?.trim() ?: entry.text
            val shiftedTime = (entry.time - songLyricsOffsetMs).coerceAtLeast(0L)
            val offsetSec = songLyricsOffsetMs / 1000.0
            val shiftedWords = entry.words?.map { w ->
                w.copy(
                    startTime = (w.startTime - offsetSec).coerceAtLeast(0.0),
                    endTime = (w.endTime - offsetSec).coerceAtLeast(0.05)
                )
            }
            entry.copy(
                time = shiftedTime,
                text = primaryLine,
                words = shiftedWords,
                translatedTextFlow = kotlinx.coroutines.flow.MutableStateFlow(null),
                romanizedTextFlow = kotlinx.coroutines.flow.MutableStateFlow(null)
            )
        }
    }

    val initialStartSec = (currentPlaybackPositionMs / 1000f)
        .coerceIn(0f, maxOf(0f, songDurationSec.toFloat() - 30f))
    val initialEndSec = minOf(songDurationSec.toFloat(), initialStartSec + 30f)

    var startTimeSec by remember { mutableFloatStateOf(initialStartSec) }
    var endTimeSec by remember { mutableFloatStateOf(initialEndSec) }

    var isGenerating by remember { mutableStateOf(false) }
    var currentStage by remember { mutableStateOf("Menyiapkan...") }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    var generatedVideoFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isAudioPreviewPlaying by remember { mutableStateOf(false) }
    var cardStyle by rememberEnumPreference(VideoLyricsCardStyleKey, VideoLyricsCardStyle.KASET)
    var effectStyle by rememberEnumPreference(VideoLyricsEffectStyleKey, VideoLyricsEffectStyle.KINETIC_SHAKE)
    var hideInactiveLyrics by rememberPreference(VideoLyricsHideInactiveKey, defaultValue = false)
    var jedagJedugEnabled by rememberPreference(VideoLyricsJedagJedugKey, defaultValue = true)
    var snowEnabled by rememberPreference(VideoLyricsSnowKey, defaultValue = false)
    var waveAmplitudePercent by rememberPreference(VideoLyricsWaveAmplitudePercentKey, defaultValue = 100)
    var waveSpeedPercent by rememberPreference(VideoLyricsWaveSpeedPercentKey, defaultValue = 100)

    // Live 16:9 canvas preview state
    var coverBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val caseBitmap = remember {
        try {
            BitmapFactory.decodeResource(context.resources, R.drawable.lyric_video_cd_case)
        } catch (_: Exception) {
            null
        }
    }

    LaunchedEffect(mediaMetadata.thumbnailUrl) {
        coverBitmap = LyricVideoShareUtils.loadCoverBitmap(context, mediaMetadata.thumbnailUrl)
    }

    val previewRenderer = remember(caseBitmap, artistNames) {
        val handle = if (artistNames.startsWith("@")) artistNames else "@$artistNames"
        LyricVideoRenderer(
            width = 1280,
            height = 720,
            caseBitmap = caseBitmap,
            brandText = "@BeatlesMusic",
            artistHandle = handle,
        )
    }

    LaunchedEffect(coverBitmap) {
        previewRenderer.setCoverBitmap(coverBitmap)
    }

    // Real-audio sub-bass amplitudes pre-analyzed for 100% bass-accurate live preview
    var previewAmplitudes by remember { mutableStateOf<FloatArray?>(null) }

    LaunchedEffect(mediaMetadata.id, startTimeSec, endTimeSec, activeAudioStyle) {
        val startMs = (startTimeSec * 1000L).toLong()
        val durMs = ((endTimeSec - startTimeSec) * 1000L).toLong().coerceAtLeast(1000L)
        previewRenderer.clipStartTimeMs = startMs
        // Debounce slider drag slightly before analyzing audio segment
        delay(220L)
        withContext(Dispatchers.Default) {
            try {
                val clipFile = LyricVideoShareUtils.resolveAudioClip(
                    context = context,
                    mediaId = mediaMetadata.id,
                    startTimeMs = startMs,
                    durationMs = durMs,
                    downloadCache = downloadCache,
                    playerCache = playerCache,
                    audioStyleMode = activeAudioStyle,
                    onProgress = null
                )
                val analyzed = AudioAmplitudeAnalyzer.analyzeAmplitudes(
                    audioFile = clipFile,
                    fps = 30,
                    totalDurationMs = durMs
                )
                if (analyzed.isNotEmpty()) {
                    previewAmplitudes = analyzed
                }
            } catch (_: Exception) {
                // Keep procedural fallback if offline/stream not yet cached
            }
        }
    }

    var previewClockMs by remember { mutableLongStateOf((initialStartSec * 1000L).toLong()) }

    LaunchedEffect(startTimeSec, endTimeSec, isAudioPreviewPlaying, isGenerating, activeAudioStyle) {
        if (!isAudioPreviewPlaying && !isGenerating) {
            val startMs = (startTimeSec * 1000L).toLong()
            val endMs = (endTimeSec * 1000L).toLong().coerceAtLeast(startMs + 3000L)
            previewClockMs = startMs
            val stepMs = (33f * activeAudioStyle.speed).toLong().coerceAtLeast(10L)
            while (!isAudioPreviewPlaying && !isGenerating) {
                delay(33L)
                val next = previewClockMs + stepMs
                previewClockMs = if (next >= endMs) startMs else next
            }
        }
    }

    LaunchedEffect(isAudioPreviewPlaying, startTimeSec, endTimeSec) {
        if (isAudioPreviewPlaying) {
            val startMs = (startTimeSec * 1000L).toLong()
            val endMs = (endTimeSec * 1000L).toLong()
            delay(100L)
            while (isAudioPreviewPlaying) {
                val player = playerConnection?.player ?: break
                val current = player.currentPosition
                previewClockMs = current.coerceAtLeast(startMs)

                if ((player.playbackState == Player.STATE_READY && current >= endMs && current >= startMs) ||
                    player.playbackState == Player.STATE_ENDED
                ) {
                    player.pause()
                    isAudioPreviewPlaying = false
                    previewClockMs = startMs
                    break
                }

                if (!player.playWhenReady && player.playbackState != Player.STATE_BUFFERING) {
                    isAudioPreviewPlaying = false
                    break
                }
                delay(33L)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isAudioPreviewPlaying) {
                playerConnection?.player?.pause()
            }
        }
    }

    fun formatTime(sec: Float): String {
        val totalSec = sec.toInt().coerceAtLeast(0)
        return "%02d:%02d".format(totalSec / 60, totalSec % 60)
    }

    Dialog(
        onDismissRequest = {
            if (!isGenerating) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isGenerating,
            dismissOnClickOutside = !isGenerating,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 14.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .heightIn(max = 760.dp)
                    .clip(RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // --- STICKY HEADER ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!mediaMetadata.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = mediaMetadata.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.movie),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Buat Video Lirik",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (activeAudioStyle != AudioStyleMode.NORMAL) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                ) {
                                    Text(
                                        text = activeAudioStyle.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeAudioStyle != AudioStyleMode.NORMAL) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${mediaMetadata.title} • $artistNames",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!isGenerating) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "Tutup",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    if (generatedVideoFile != null) {
                        // --- STATE 3: COMPLETED ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Video Lirik Siap!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Resolusi 720p HD • ${(endTimeSec - startTimeSec).toInt().coerceAtLeast(1)} detik",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            var isSavedToGallery by remember { mutableStateOf(false) }
                            var isUploadingTempLink by remember { mutableStateOf(false) }
                            var uploadedTempUrl by remember { mutableStateOf<String?>(null) }
                            var tempUploadError by remember { mutableStateOf<String?>(null) }
                            var isTempLinkCopied by remember { mutableStateOf(false) }

                            fun copyTempUrlToClipboard(url: String) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Link Video Lirik", url))
                                isTempLinkCopied = true
                                Toast.makeText(
                                    context,
                                    "Link Temp Files berhasil disalin!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            LaunchedEffect(generatedVideoFile) {
                                val fileToUpload = generatedVideoFile ?: return@LaunchedEffect
                                if (uploadedTempUrl == null && !isUploadingTempLink) {
                                    isUploadingTempLink = true
                                    tempUploadError = null
                                    val uploadRes = LyricVideoShareUtils.uploadVideoToTempFiles(fileToUpload)
                                    isUploadingTempLink = false
                                    if (uploadRes.isSuccess) {
                                        uploadedTempUrl = uploadRes.getOrNull()
                                    } else {
                                        tempUploadError = uploadRes.exceptionOrNull()?.message ?: "Gagal mengunggah link sementara"
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val uri = LyricVideoShareUtils.saveVideoToGallery(
                                            context = context,
                                            videoFile = generatedVideoFile!!,
                                            songTitle = mediaMetadata.title,
                                            songArtist = artistNames
                                        )
                                        if (uri != null) {
                                            isSavedToGallery = true
                                            Toast.makeText(
                                                context,
                                                "Tersimpan di Galeri (Movies/Beatles)",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Gagal menyimpan video ke Galeri",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSavedToGallery) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(if (isSavedToGallery) R.drawable.check else R.drawable.download),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isSavedToGallery) "Tersimpan" else "Simpan Galeri",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Button(
                                    onClick = {
                                        val currentUrl = uploadedTempUrl
                                        if (currentUrl != null) {
                                            copyTempUrlToClipboard(currentUrl)
                                        } else if (!isUploadingTempLink) {
                                            val fileToUpload = generatedVideoFile ?: return@Button
                                            isUploadingTempLink = true
                                            tempUploadError = null
                                            scope.launch {
                                                val uploadRes = LyricVideoShareUtils.uploadVideoToTempFiles(fileToUpload)
                                                isUploadingTempLink = false
                                                val newUrl = uploadRes.getOrNull()
                                                if (newUrl != null) {
                                                    uploadedTempUrl = newUrl
                                                    copyTempUrlToClipboard(newUrl)
                                                } else {
                                                    val msg = uploadRes.exceptionOrNull()?.message ?: "Gagal mengunggah ke Temp Files"
                                                    tempUploadError = msg
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Sedang mengunggah ke Temp Files, mohon tunggu...",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("copy_temp_link_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isTempLinkCopied) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        contentColor = if (isTempLinkCopied) {
                                            MaterialTheme.colorScheme.onSecondary
                                        } else {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    )
                                ) {
                                    Icon(
                                        painter = painterResource(if (isTempLinkCopied) R.drawable.check else R.drawable.link),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = when {
                                            isUploadingTempLink -> "Upload Link..."
                                            isTempLinkCopied -> "Link Disalin"
                                            else -> "Salin Link"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // --- TEMP FILES LINK & BARCODE / QR CODE SECTION ---
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("temp_link_barcode_card"),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.link),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Barcode & Link Video (filebin.net)",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    when {
                                        isUploadingTempLink -> {
                                            LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.75f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            )
                                            Text(
                                                text = "Mengunggah video ke filebin.net untuk membuat link & barcode...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        uploadedTempUrl != null -> {
                                            val activeUrl = uploadedTempUrl!!
                                            val qrBitmap = remember(activeUrl) {
                                                QrBarcodeGenerator.generateQrCodeBitmap(activeUrl, pixelSize = 480)
                                            }
                                            val barcode1dBitmap = remember(activeUrl) {
                                                QrBarcodeGenerator.generateCode128Bitmap(activeUrl, widthPx = 640, heightPx = 120)
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Image(
                                                        bitmap = qrBitmap.asImageBitmap(),
                                                        contentDescription = "QR Barcode Link Video",
                                                        modifier = Modifier.size(148.dp)
                                                    )
                                                    Image(
                                                        bitmap = barcode1dBitmap.asImageBitmap(),
                                                        contentDescription = "Barcode 1D Link Video",
                                                        modifier = Modifier
                                                            .width(210.dp)
                                                            .height(42.dp),
                                                        contentScale = ContentScale.FillBounds
                                                    )
                                                }
                                            }

                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { copyTempUrlToClipboard(activeUrl) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surface
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = activeUrl,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isTempLinkCopied) "Tersalin" else "Salin",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            Text(
                                                text = tempUploadError ?: "Ketuk tombol Salin Link di atas untuk mengunggah & membuat barcode.",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = "  Bagikan Langsung  ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            val platforms = listOf(
                                SharePlatformItem(ShareUtils.SharePlatform.WHATSAPP, R.drawable.whatsapp, MaterialTheme.colorScheme.onSurface),
                                SharePlatformItem(ShareUtils.SharePlatform.INSTAGRAM, R.drawable.instagram, MaterialTheme.colorScheme.onSurface),
                                SharePlatformItem(ShareUtils.SharePlatform.TIKTOK, R.drawable.tiktok, MaterialTheme.colorScheme.onSurface),
                                SharePlatformItem(ShareUtils.SharePlatform.X, R.drawable.x_logo, MaterialTheme.colorScheme.onSurface),
                                SharePlatformItem(ShareUtils.SharePlatform.TELEGRAM, R.drawable.telegram, MaterialTheme.colorScheme.onSurface),
                                SharePlatformItem(ShareUtils.SharePlatform.GENERIC, R.drawable.share, MaterialTheme.colorScheme.primary)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                platforms.chunked(3).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowItems.forEach { p ->
                                            PlatformGridTile(
                                                item = p,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    LyricVideoShareUtils.shareLyricVideo(
                                                        context,
                                                        generatedVideoFile!!,
                                                        mediaMetadata.title,
                                                        artistNames,
                                                        p.platform
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        generatedVideoFile = null
                                        isGenerating = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Edit Ulang")
                                }
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Selesai", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (isGenerating) {
                        // --- STATE 2: GENERATING PROGRESS ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LyricVideoLivePreview(
                                renderer = previewRenderer,
                                cardStyle = cardStyle,
                                effectStyle = effectStyle,
                                hideInactiveLyrics = hideInactiveLyrics,
                                jedagJedugEnabled = jedagJedugEnabled,
                                snowEnabled = snowEnabled,
                                waveAmplitudeScale = waveAmplitudePercent / 100f,
                                waveSpeedScale = waveSpeedPercent / 100f,
                                clipStartMs = (startTimeSec * 1000f).toLong(),
                                previewTimeMs = (startTimeSec * 1000f).toLong() + ((endTimeSec - startTimeSec) * 1000f * currentProgress).toLong(),
                                previewAmplitudes = previewAmplitudes,
                                isPlaying = true,
                                lyrics = lyrics,
                                songTitle = mediaMetadata.title,
                                songArtist = artistNames,
                                onTogglePlay = null
                            )

                            Spacer(Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentStage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${(currentProgress * 100).toInt().coerceIn(0, 100)}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { currentProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "Merender video 720p HD secara lokal di perangkat Anda. Mohon tunggu sebentar...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // --- STATE 1: STUDIO EDITOR & OPTIONS ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. Live 16:9 Interactive Canvas Preview
                            LyricVideoLivePreview(
                                renderer = previewRenderer,
                                cardStyle = cardStyle,
                                effectStyle = effectStyle,
                                hideInactiveLyrics = hideInactiveLyrics,
                                jedagJedugEnabled = jedagJedugEnabled,
                                snowEnabled = snowEnabled,
                                waveAmplitudeScale = waveAmplitudePercent / 100f,
                                waveSpeedScale = waveSpeedPercent / 100f,
                                clipStartMs = (startTimeSec * 1000L).toLong(),
                                previewTimeMs = previewClockMs,
                                previewAmplitudes = previewAmplitudes,
                                isPlaying = isAudioPreviewPlaying,
                                lyrics = videoLyricsOnly,
                                songTitle = mediaMetadata.title,
                                songArtist = artistNames,
                                onTogglePlay = {
                                    val player = playerConnection?.player
                                    if (player != null) {
                                        if (isAudioPreviewPlaying) {
                                            player.pause()
                                            isAudioPreviewPlaying = false
                                        } else {
                                            if (player.currentMediaItem?.mediaId != mediaMetadata.id) {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = mediaMetadata.title,
                                                        items = listOf(mediaMetadata.toMediaItem())
                                                    )
                                                )
                                            }
                                            player.seekTo((startTimeSec * 1000L).toLong())
                                            player.play()
                                            isAudioPreviewPlaying = true
                                        }
                                    }
                                }
                            )

                            // 2. Gaya Tampilan & Efek Animasi Video
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Gaya Tampilan",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StyleOptionCard(
                                        title = "Kaset CD",
                                        subtitle = "Piringan Berputar",
                                        iconRes = R.drawable.album,
                                        selected = cardStyle == VideoLyricsCardStyle.KASET,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            cardStyle = VideoLyricsCardStyle.KASET
                                            scope.launch {
                                                context.dataStore.edit {
                                                    it[VideoLyricsCardStyleKey] = VideoLyricsCardStyle.KASET.name
                                                }
                                            }
                                        }
                                    )
                                    StyleOptionCard(
                                        title = "Normal",
                                        subtitle = "Sampul Persegi",
                                        iconRes = R.drawable.music_note,
                                        selected = cardStyle == VideoLyricsCardStyle.NORMAL,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            cardStyle = VideoLyricsCardStyle.NORMAL
                                            scope.launch {
                                                context.dataStore.edit {
                                                    it[VideoLyricsCardStyleKey] = VideoLyricsCardStyle.NORMAL.name
                                                }
                                            }
                                        }
                                    )
                                }

                                Spacer(Modifier.height(2.dp))

                                Text(
                                    text = "Efek Gerak & Animasi Lirik",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                val effectOptions = listOf(
                                    Triple(
                                        VideoLyricsEffectStyle.KINETIC_SHAKE,
                                        "Kinetic Shake & Blur",
                                        "Ayunan gelombang 3D jelas + teks pop-in dengan motion blur vertikal & glow putih"
                                    ),
                                    Triple(
                                        VideoLyricsEffectStyle.SMOOTH_FLOAT,
                                        "3D Smooth Float",
                                        "Ayunan gelombang 3D melayang jelas & seimbang dengan transisi lirik halus"
                                    ),
                                    Triple(
                                        VideoLyricsEffectStyle.STATIC_CLASSIC,
                                        "Klasik Statis",
                                        "Kamera diam dengan lirik gulir standar"
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    effectOptions.forEach { (styleItem, label, _) ->
                                        val isSelected = effectStyle == styleItem
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    effectStyle = styleItem
                                                    scope.launch {
                                                        context.dataStore.edit {
                                                            it[VideoLyricsEffectStyleKey] = styleItem.name
                                                        }
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                            },
                                            border = if (isSelected) null else BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                val activeEffectDesc = effectOptions.firstOrNull { it.first == effectStyle }?.third ?: ""
                                Text(
                                    text = activeEffectDesc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                // Pengaturan Ayunan Gelombang + Dummy Preview / Ilustrasi Gelombang (untuk Kinetic Shake & 3D Smooth Float)
                                if (effectStyle != VideoLyricsEffectStyle.STATIC_CLASSIC) {
                                    WaveSettingsWithDummyPreviewCard(
                                        waveAmplitudePercent = waveAmplitudePercent,
                                        waveSpeedPercent = waveSpeedPercent,
                                        previewTimeMs = previewClockMs,
                                        onAmplitudeChange = { newAmp ->
                                            waveAmplitudePercent = newAmp.coerceIn(20, 250)
                                        },
                                        onSpeedChange = { newSpd ->
                                            waveSpeedPercent = newSpd.coerceIn(30, 250)
                                        },
                                        onReset = {
                                            waveAmplitudePercent = 100
                                            waveSpeedPercent = 100
                                        }
                                    )
                                }

                                // Tombol Tuas 1: Getar Bass (Jedag-Jedug)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { jedagJedugEnabled = !jedagJedugEnabled },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(
                                                text = "Getar Bass (Jedag-Jedug)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (jedagJedugEnabled) {
                                                    "Aktif • Getaran bas jedag-jedug + ayunan gelombang aktif"
                                                } else {
                                                    "Nonaktif • Hanya ayunan gelombang (tanpa getaran bas)"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Switch(
                                            checked = jedagJedugEnabled,
                                            onCheckedChange = { jedagJedugEnabled = it },
                                            modifier = Modifier.testTag("jedag_jedug_switch")
                                        )
                                    }
                                }

                                // Tombol Tuas 2: Efek Salju / Tanpa Salju
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { snowEnabled = !snowEnabled },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                            Text(
                                                text = "Efek Salju",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (snowEnabled) {
                                                    "Dengan Salju • Partikel salju melayang di latar belakang"
                                                } else {
                                                    "Tanpa Salju • Latar hitam bersih seperti di video"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Switch(
                                            checked = snowEnabled,
                                            onCheckedChange = { snowEnabled = it },
                                            modifier = Modifier.testTag("snow_effect_switch")
                                        )
                                    }
                                }

                                if (effectStyle == VideoLyricsEffectStyle.STATIC_CLASSIC) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { hideInactiveLyrics = !hideInactiveLyrics },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                                Text(
                                                    text = "Fokus 1 Baris Lirik",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (hideInactiveLyrics) {
                                                        "Hanya menampilkan baris lirik yang sedang dinyanyikan"
                                                    } else {
                                                        "Menampilkan baris aktif beserta lirik sebelum & berikutnya"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Switch(
                                                checked = hideInactiveLyrics,
                                                onCheckedChange = { hideInactiveLyrics = it }
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Durasi & Rentang Waktu Klip
                            val selDurationSec = (endTimeSec - startTimeSec).toInt().coerceAtLeast(1)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Durasi & Rentang Klip",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "${formatTime(startTimeSec)} – ${formatTime(endTimeSec)} • ${selDurationSec}d",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // 4 Segmented Duration Pills
                                    val presets = listOf(
                                        15 to "15 Detik",
                                        30 to "30 Detik",
                                        60 to "60 Detik",
                                        -1 to "Penuh"
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        presets.forEach { (dur, label) ->
                                            val isSelected = if (dur == -1) {
                                                startTimeSec <= 1f && endTimeSec >= songDurationSec.toFloat() - 1f
                                            } else {
                                                selDurationSec == dur
                                            }
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        if (dur == -1) {
                                                            startTimeSec = 0f
                                                            endTimeSec = songDurationSec.toFloat()
                                                        } else {
                                                            val targetEnd = startTimeSec + dur.toFloat()
                                                            if (targetEnd <= songDurationSec.toFloat()) {
                                                                endTimeSec = targetEnd
                                                            } else {
                                                                startTimeSec = maxOf(0f, songDurationSec.toFloat() - dur.toFloat())
                                                                endTimeSec = songDurationSec.toFloat()
                                                            }
                                                        }
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                border = if (isSelected) null else BorderStroke(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface
                                                        },
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Range Slider
                                    RangeSlider(
                                        value = startTimeSec..endTimeSec,
                                        onValueChange = { range ->
                                            if (range.endInclusive - range.start >= 3f) {
                                                startTimeSec = range.start
                                                endTimeSec = range.endInclusive
                                            }
                                        },
                                        valueRange = 0f..songDurationSec.toFloat(),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (currentPlaybackPositionMs > 3000L) {
                                        val curSec = currentPlaybackPositionMs / 1000f
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    val selDur = (endTimeSec - startTimeSec).coerceAtLeast(10f)
                                                    startTimeSec = curSec.coerceIn(0f, maxOf(0f, songDurationSec.toFloat() - 3f))
                                                    endTimeSec = minOf(songDurationSec.toFloat(), startTimeSec + selDur)
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.ic_play),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "Gunakan posisi lagu saat ini (${formatTime(curSec)})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }

                                    // Compact Lyric Start/End Preview (original lyrics only, no translation)
                                    val startLyricText = remember(videoLyricsOnly, startTimeSec) {
                                        val startMs = (startTimeSec * 1000L).toLong()
                                        videoLyricsOnly?.findLast { it.time <= startMs }?.text
                                            ?: videoLyricsOnly?.firstOrNull()?.text
                                            ?: "(Lirik instrumental / belum tersedia)"
                                    }
                                    val endLyricText = remember(videoLyricsOnly, endTimeSec) {
                                        val endMs = (endTimeSec * 1000L).toLong()
                                        videoLyricsOnly?.findLast { it.time <= endMs }?.text
                                            ?: videoLyricsOnly?.lastOrNull()?.text
                                            ?: "(Lirik instrumental / belum tersedia)"
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        LyricEndpointRow(
                                            badgeText = "AWAL ${formatTime(startTimeSec)}",
                                            badgeColor = MaterialTheme.colorScheme.primaryContainer,
                                            badgeTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            lyricText = startLyricText
                                        )
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )
                                        LyricEndpointRow(
                                            badgeText = "AKHIR ${formatTime(endTimeSec)}",
                                            badgeColor = MaterialTheme.colorScheme.secondaryContainer,
                                            badgeTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            lyricText = endLyricText
                                        )
                                    }
                                }
                            }

                            // 4. Error message (if any)
                            if (errorMessage != null) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.close),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = errorMessage ?: "",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }

                        // --- STICKY FOOTER ACTIONS ---
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Batal", fontWeight = FontWeight.Medium)
                            }

                            val renderDurationSec = (endTimeSec - startTimeSec).toInt().coerceAtLeast(1)
                            Button(
                                onClick = {
                                    if (isAudioPreviewPlaying) {
                                        playerConnection?.player?.pause()
                                        isAudioPreviewPlaying = false
                                    }
                                    isGenerating = true
                                    errorMessage = null

                                    val startPosMs = (startTimeSec * 1000L).toLong()
                                    val durationMs = ((endTimeSec - startTimeSec) * 1000L).toLong().coerceAtLeast(1000L)

                                    val currentCardStyle = cardStyle
                                    val currentEffectStyle = effectStyle
                                    val currentHideInactive = hideInactiveLyrics
                                    val currentJedagJedug = jedagJedugEnabled
                                    val currentSnow = snowEnabled
                                    val currentWaveAmpPercent = waveAmplitudePercent
                                    val currentWaveSpeedPercent = waveSpeedPercent
                                    val currentAudioStyle = activeAudioStyle
                                    scope.launch {
                                        context.dataStore.edit { prefs ->
                                            prefs[VideoLyricsCardStyleKey] = currentCardStyle.name
                                            prefs[VideoLyricsEffectStyleKey] = currentEffectStyle.name
                                            prefs[VideoLyricsHideInactiveKey] = currentHideInactive
                                            prefs[VideoLyricsShowUpcomingKey] = !currentHideInactive
                                            prefs[VideoLyricsJedagJedugKey] = currentJedagJedug
                                            prefs[VideoLyricsSnowKey] = currentSnow
                                            prefs[VideoLyricsWaveAmplitudePercentKey] = currentWaveAmpPercent
                                            prefs[VideoLyricsWaveSpeedPercentKey] = currentWaveSpeedPercent
                                        }
                                        val result = LyricVideoShareUtils.generateLyricVideo(
                                            context = context,
                                            mediaMetadata = mediaMetadata,
                                            lyrics = videoLyricsOnly,
                                            startTimeMs = startPosMs,
                                            durationMs = durationMs,
                                            cardStyle = currentCardStyle,
                                            effectStyle = currentEffectStyle,
                                            jedagJedugEnabled = currentJedagJedug,
                                            snowEnabled = currentSnow,
                                            waveAmplitudeScale = currentWaveAmpPercent / 100f,
                                            waveSpeedScale = currentWaveSpeedPercent / 100f,
                                            audioStyleMode = currentAudioStyle,
                                            downloadCache = downloadCache,
                                            playerCache = playerCache,
                                            onProgress = { stage, prog ->
                                                currentStage = stage
                                                currentProgress = prog
                                            }
                                        )

                                        if (result.isSuccess) {
                                            generatedVideoFile = result.getOrNull()
                                        } else {
                                            isGenerating = false
                                            errorMessage = "Gagal membuat video: ${result.exceptionOrNull()?.message}"
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.movie),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Mulai Render (${renderDurationSec}d)",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricVideoLivePreview(
    renderer: LyricVideoRenderer,
    cardStyle: VideoLyricsCardStyle,
    effectStyle: VideoLyricsEffectStyle,
    hideInactiveLyrics: Boolean,
    jedagJedugEnabled: Boolean,
    snowEnabled: Boolean,
    waveAmplitudeScale: Float,
    waveSpeedScale: Float,
    clipStartMs: Long,
    previewTimeMs: Long,
    previewAmplitudes: FloatArray?,
    isPlaying: Boolean,
    lyrics: List<LyricsEntry>?,
    songTitle: String,
    songArtist: String,
    onTogglePlay: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            renderer.cardStyle = cardStyle
            renderer.effectStyle = effectStyle
            renderer.clipStartTimeMs = clipStartMs
            renderer.hideInactiveLyrics = hideInactiveLyrics
            renderer.showUpcomingLyrics = !hideInactiveLyrics
            renderer.jedagJedugEnabled = jedagJedugEnabled
            renderer.snowEnabled = snowEnabled
            renderer.waveAmplitudeScale = waveAmplitudeScale
            renderer.waveSpeedScale = waveSpeedScale

            val scaleX = size.width / renderer.width.toFloat()
            val scaleY = size.height / renderer.height.toFloat()

            // Look up real-audio sub-bass amplitude:
            // 1) During live audio playback, combine real-time ExoPlayer PCM sub-bass with offline analyzed frame
            // 2) When paused with analyzed clip, use the exact 30fps peak-picked sub-bass amplitude
            // 3) If not yet analyzed, use a smooth zoom-only wave (< 0.42f) so it NEVER forces fake shakes
            val frameIdx = (((previewTimeMs - clipStartMs).coerceAtLeast(0L) * 30L) / 1000L).toInt()
            val analyzedAmp = previewAmplitudes?.getOrNull(frameIdx)
            val realtimeActive = isPlaying &&
                (SystemClock.elapsedRealtime() - SilenceDetectorAudioProcessor.lastRealtimeUpdateMs) < 220L
            val livePcmAmp = if (realtimeActive) SilenceDetectorAudioProcessor.realtimeBassAmplitude else null

            val amp = when {
                livePcmAmp != null && analyzedAmp != null -> maxOf(livePcmAmp, analyzedAmp)
                livePcmAmp != null -> livePcmAmp
                analyzedAmp != null -> analyzedAmp
                else -> {
                    val tSec = previewTimeMs / 1000.0
                    val smoothZoomWave = (0.5 + 0.5 * sin(tSec * 2.2)).toFloat()
                    (0.14f + 0.14f * smoothZoomWave).coerceIn(0.14f, 0.28f)
                }
            }

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                val save = native.save()
                native.scale(scaleX, scaleY)
                renderer.renderFrame(
                    canvas = native,
                    currentTimeMs = previewTimeMs,
                    amplitude = amp,
                    lyrics = lyrics,
                    songTitle = songTitle,
                    songArtist = songArtist
                )
                native.restoreToCount(save)
            }
        }

        // Top-left Live Preview badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.65f),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.22f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "PRATINJAU AKTIF" else "PRATINJAU 16:9 HD",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom-right Play/Pause Audio Preview pill
        if (onTogglePlay != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onTogglePlay),
                shape = RoundedCornerShape(20.dp),
                color = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Black.copy(alpha = 0.72f)
                },
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.ic_play),
                        contentDescription = null,
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "Hentikan" else "Coba Putar",
                        color = if (isPlaying) MaterialTheme.colorScheme.onPrimary else Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StyleOptionCard(
    title: String,
    subtitle: String,
    iconRes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LyricEndpointRow(
    badgeText: String,
    badgeColor: Color,
    badgeTextColor: Color,
    lyricText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = badgeColor,
            modifier = Modifier.width(86.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = lyricText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlatformGridTile(
    item: SharePlatformItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.iconRes),
                    contentDescription = item.platform.displayName,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(item.color)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.platform.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Interactive Wave Settings card with a real-time animated Dummy Preview / Wave Illustration
 * so users can customize the 3D wave amplitude and speed and immediately visualize the motion.
 */
@Composable
private fun WaveSettingsWithDummyPreviewCard(
    waveAmplitudePercent: Int,
    waveSpeedPercent: Int,
    previewTimeMs: Long,
    onAmplitudeChange: (Int) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onReset: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("wave_settings_card"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.50f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pengaturan Ayunan Gelombang",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Atur kekuatan & kecepatan gelombang beserta dummy ilustrasi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (waveAmplitudePercent != 100 || waveSpeedPercent != 100) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onReset),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Reset 100%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // --- DUMMY PREVIEW / ILUSTRASI GELOMBANG ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF07090E))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                    .testTag("wave_dummy_preview_box")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val ampScale = (waveAmplitudePercent / 100f).coerceIn(0.2f, 2.5f)
                    val spdScale = (waveSpeedPercent / 100f).coerceIn(0.3f, 2.5f)
                    val tSec = (previewTimeMs / 1000f) * spdScale

                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas

                        // Divider between Left (Wave Curve Graph) and Right (Dummy Scene Preview)
                        val splitX = w * 0.52f
                        val dividerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(45, 255, 255, 255)
                            strokeWidth = 2f
                        }
                        native.drawLine(splitX, h * 0.12f, splitX, h * 0.88f, dividerPaint)

                        // --- LEFT PANEL: LIVE WAVE CURVE ILLUSTRATION ---
                        val graphLeft = 24f
                        val graphRight = splitX - 22f
                        val graphW = (graphRight - graphLeft).coerceAtLeast(40f)
                        val graphCy = h * 0.54f
                        val maxWaveH = h * 0.34f
                        val curAmpH = (maxWaveH * (ampScale / 2.2f)).coerceIn(h * 0.05f, maxWaveH)

                        // Center baseline & upper/lower envelope guides
                        val guidePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(40, 200, 215, 255)
                            strokeWidth = 1.5f
                            style = android.graphics.Paint.Style.STROKE
                        }
                        native.drawLine(graphLeft, graphCy, graphRight, graphCy, guidePaint)
                        guidePaint.color = android.graphics.Color.argb(26, 255, 255, 255)
                        native.drawLine(graphLeft, graphCy - curAmpH, graphRight, graphCy - curAmpH, guidePaint)
                        native.drawLine(graphLeft, graphCy + curAmpH, graphRight, graphCy + curAmpH, guidePaint)

                        // Animated Dual-Harmonic Wave Path matching LyricVideoRenderer formula
                        val wavePath = android.graphics.Path()
                        val steps = 48
                        var headX = graphRight
                        var headY = graphCy
                        for (i in 0..steps) {
                            val frac = i.toFloat() / steps
                            val px = graphLeft + frac * graphW
                            val phase = (tSec * 2.15f) - ((1f - frac) * 4.2f * spdScale.coerceIn(0.5f, 1.8f))
                            val harmonic = (sin(phase) * 0.68f) + (cos(phase * 0.58f) * 0.32f)
                            val py = (graphCy - harmonic * curAmpH).coerceIn(10f, h - 10f)
                            if (i == 0) {
                                wavePath.moveTo(px, py)
                            } else {
                                wavePath.lineTo(px, py)
                            }
                            if (i == steps) {
                                headX = px
                                headY = py
                            }
                        }

                        // Glow pass on wave curve
                        val waveGlowPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(95, 255, 255, 255)
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 7f
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            strokeJoin = android.graphics.Paint.Join.ROUND
                        }
                        native.drawPath(wavePath, waveGlowPaint)

                        // Core pass on wave curve
                        val waveCorePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 3.2f
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            strokeJoin = android.graphics.Paint.Join.ROUND
                        }
                        native.drawPath(wavePath, waveCorePaint)

                        // Glowing tracking dot at the live wave head
                        val dotPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            style = android.graphics.Paint.Style.FILL
                            setShadowLayer(10f, 0f, 0f, android.graphics.Color.WHITE)
                        }
                        native.drawCircle(headX, headY, 5.5f, dotPaint)

                        val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(185, 255, 255, 255)
                            textSize = h * 0.115f
                            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
                        }
                        native.drawText("ILUSTRASI KURVA", graphLeft, h * 0.18f, labelPaint)

                        // --- RIGHT PANEL: DUMMY SCENE SWAY PREVIEW ---
                        val dummyCx = splitX + (w - splitX) * 0.50f
                        val dummyCy = h * 0.54f
                        val panelW = (w - splitX)

                        native.drawText("DUMMY AYUNAN 3D", splitX + 18f, h * 0.18f, labelPaint)

                        val dummyWaveX = ((sin(tSec * 2.15f) * (panelW * 0.055f)) + (cos(tSec * 1.25f) * (panelW * 0.025f))) * ampScale
                        val dummyWaveY = ((cos(tSec * 1.90f) * (h * 0.075f)) + (sin(tSec * 2.60f) * (h * 0.030f))) * ampScale
                        val dummyRot = ((sin(tSec * 1.70f) * 3.2f) + (cos(tSec * 2.35f) * 1.2f)) * ampScale
                        val dummyScale = (1.0f + ((0.045f * sin(tSec * 2.05f)) * ampScale)).coerceIn(0.85f, 1.22f)

                        val saveDummy = native.save()
                        native.translate(dummyCx + dummyWaveX, dummyCy + dummyWaveY)
                        native.rotate(dummyRot)
                        native.scale(dummyScale, dummyScale)

                        // Dummy Lyric Lines on the left of dummy scene
                        val dummyLyricPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            textSize = h * 0.145f
                            typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
                            setShadowLayer(8f, 0f, 0f, android.graphics.Color.argb(140, 255, 255, 255))
                        }
                        native.drawText("Lirik", -panelW * 0.34f, -2f, dummyLyricPaint)
                        dummyLyricPaint.textSize = h * 0.115f
                        dummyLyricPaint.alpha = 185
                        native.drawText("Gelombang", -panelW * 0.34f, h * 0.14f, dummyLyricPaint)

                        // Dummy CD Jewel Case on the right of dummy scene
                        val caseSize = h * 0.42f
                        val caseCx = panelW * 0.16f
                        val caseCy = 2f
                        val dummyCaseRect = android.graphics.RectF(
                            caseCx - caseSize * 0.54f,
                            caseCy - caseSize * 0.50f,
                            caseCx + caseSize * 0.54f,
                            caseCy + caseSize * 0.50f
                        )
                        val dummyCasePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(210, 255, 255, 255)
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 2.2f
                        }
                        native.drawRoundRect(dummyCaseRect, 5f, 5f, dummyCasePaint)

                        // Mini spinning disc inside dummy case
                        val discR = caseSize * 0.40f
                        val discPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.argb(90, 255, 255, 255)
                            style = android.graphics.Paint.Style.FILL
                        }
                        native.drawCircle(caseCx, caseCy, discR, discPaint)
                        val hubPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.parseColor("#F59E0B")
                            style = android.graphics.Paint.Style.FILL
                        }
                        native.drawCircle(caseCx, caseCy, discR * 0.32f, hubPaint)

                        // Mini @BeatlesMusic above dummy case
                        val miniTagPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            textSize = h * 0.085f
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                        }
                        native.drawText("@BeatlesMusic", caseCx, dummyCaseRect.top - 4f, miniTagPaint)

                        // Mini bass bars below dummy case
                        val miniBarPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.WHITE
                            strokeWidth = 2.0f
                            strokeCap = android.graphics.Paint.Cap.ROUND
                        }
                        val barY = dummyCaseRect.bottom + 7f
                        for (b in -4..4) {
                            val bx = caseCx + b * 4.5f
                            val bh = 2.5f + 4.5f * kotlin.math.abs(sin(tSec * 5f + b * 0.6f))
                            native.drawLine(bx, barY - bh / 2f, bx, barY + bh / 2f, miniBarPaint)
                        }

                        native.restoreToCount(saveDummy)
                    }
                }
            }

            // Preset Chips for Wave
            val wavePresets = listOf(
                Triple("Halus", 65, 80),
                Triple("Seimbang", 100, 100),
                Triple("Berayun", 145, 115),
                Triple("Kuat", 190, 135)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                wavePresets.forEach { (label, presetAmp, presetSpd) ->
                    val isSelected = kotlin.math.abs(waveAmplitudePercent - presetAmp) <= 5 &&
                        kotlin.math.abs(waveSpeedPercent - presetSpd) <= 5
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onAmplitudeChange(presetAmp)
                                onSpeedChange(presetSpd)
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            primaryColor
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = if (isSelected) null else BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            // Slider 1: Amplitudo / Lebar Ayunan Gelombang
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kekuatan / Lebar Ayunan Gelombang",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${waveAmplitudePercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
                Slider(
                    value = waveAmplitudePercent.toFloat(),
                    onValueChange = { onAmplitudeChange(it.roundToInt()) },
                    valueRange = 20f..250f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wave_amplitude_slider")
                )
            }

            // Slider 2: Kecepatan / Frekuensi Ayunan Gelombang
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kecepatan Ayunan Gelombang",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${waveSpeedPercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
                Slider(
                    value = waveSpeedPercent.toFloat(),
                    onValueChange = { onSpeedChange(it.roundToInt()) },
                    valueRange = 30f..250f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wave_speed_slider")
                )
            }
        }
    }
}
