/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.component

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nanzbeatles.innertube.models.SongItem
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.LiquidGlassEffectKey
import com.nanzbeatles.nanas.utils.SpotifyPlaylistImporter
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val SpotifyGreen = Color(0xFF1DB954)
private val YtMusicRed = Color(0xFFFF0033)

/**
 * Custom vector emblem for Spotify (circle with 3 curved acoustic waves).
 */
@Composable
fun SpotifyLogoIcon(
    tint: Color = SpotifyGreen,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = minOf(w, h) / 2f
        val cx = w / 2f
        val cy = h / 2f

        // Outer circle filled with tint
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(cx, cy)
        )

        val waveColor = Color(0xFF0B0E11)

        // Top wave (widest)
        val pathTop = Path().apply {
            moveTo(w * 0.24f, h * 0.36f)
            quadraticTo(w * 0.50f, h * 0.26f, w * 0.77f, h * 0.40f)
        }
        drawPath(
            path = pathTop,
            color = waveColor,
            style = Stroke(width = w * 0.088f, cap = StrokeCap.Round)
        )

        // Middle wave
        val pathMid = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            quadraticTo(w * 0.50f, h * 0.44f, w * 0.72f, h * 0.55f)
        }
        drawPath(
            path = pathMid,
            color = waveColor,
            style = Stroke(width = w * 0.076f, cap = StrokeCap.Round)
        )

        // Bottom wave
        val pathBot = Path().apply {
            moveTo(w * 0.32f, h * 0.67f)
            quadraticTo(w * 0.49f, h * 0.60f, w * 0.67f, h * 0.70f)
        }
        drawPath(
            path = pathBot,
            color = waveColor,
            style = Stroke(width = w * 0.066f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Custom vector emblem for YouTube Music (red circle with white ring and play triangle).
 */
@Composable
fun YouTubeMusicLogoIcon(
    tint: Color = YtMusicRed,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = minOf(w, h) / 2f
        val center = Offset(w / 2f, h / 2f)

        drawCircle(
            color = tint,
            radius = r,
            center = center
        )

        drawCircle(
            color = Color.White,
            radius = r * 0.56f,
            center = center,
            style = Stroke(width = w * 0.07f)
        )

        val playPath = Path().apply {
            moveTo(w * 0.42f, h * 0.36f)
            lineTo(w * 0.64f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.64f)
            close()
        }
        drawPath(playPath, color = Color.White)
    }
}

@Composable
fun SpotifyImportDialog(
    initialUrl: String = "",
    onDismiss: () -> Unit,
    onPlaylistImported: ((playlistId: String) -> Unit)? = null,
    onPlaySong: ((song: SongItem) -> Unit)? = null,
    onOpenArtist: ((artistBrowseId: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    @Suppress("UNUSED_VARIABLE")
    val liquidGlassEnabled by rememberPreference(LiquidGlassEffectKey, defaultValue = true)

    var urlInput by remember { mutableStateOf(initialUrl) }
    var isFetchingPlaylist by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }

    var playlistData by remember { mutableStateOf<SpotifyPlaylistImporter.SpotifyPlaylistData?>(null) }
    var customPlaylistName by remember { mutableStateOf("") }
    val selectedTrackIndices = remember { mutableStateListOf<Int>() }

    var isImporting by remember { mutableStateOf(false) }
    var isQuickActionLoading by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<SpotifyPlaylistImporter.ImportProgress?>(null) }
    var importSummary by remember { mutableStateOf<SpotifyPlaylistImporter.ImportSummary?>(null) }
    var activeJob by remember { mutableStateOf<Job?>(null) }

    val isYtMusicMode = remember(urlInput, playlistData) {
        playlistData?.platform == SpotifyPlaylistImporter.ImportSourcePlatform.YOUTUBE_MUSIC ||
            (playlistData == null && SpotifyPlaylistImporter.isYouTubeMusicInput(urlInput))
    }
    val activeAccent = if (isYtMusicMode) YtMusicRed else SpotifyGreen
    val activeButtonContentColor = if (isYtMusicMode) Color.White else Color.Black

    fun triggerFetch(targetUrl: String) {
        if (targetUrl.isBlank()) {
            fetchError = "Masukkan link Spotify atau YouTube Music terlebih dahulu"
            return
        }
        isFetchingPlaylist = true
        fetchError = null
        activeJob?.cancel()
        activeJob = scope.launch {
            val result = SpotifyPlaylistImporter.fetchSpotifyPlaylist(targetUrl)
            isFetchingPlaylist = false
            result.onSuccess { data ->
                playlistData = data
                customPlaylistName = data.name
                selectedTrackIndices.clear()
                selectedTrackIndices.addAll(data.tracks.indices)
            }.onFailure { err ->
                fetchError = err.message ?: "Gagal mengambil data playlist"
            }
        }
    }

    // Auto-fetch if initialUrl was passed via external share / deep link, or auto-detect clipboard
    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) {
            urlInput = initialUrl
            playlistData = null
            importSummary = null
            triggerFetch(initialUrl)
        } else if (urlInput.isBlank()) {
            val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
            if (SpotifyPlaylistImporter.isSupportedImportUrl(clipText)) {
                urlInput = clipText
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isImporting) {
                activeJob?.cancel()
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = !isImporting
        )
    ) {
        val dialogShape = RoundedCornerShape(28.dp)
        val containerBg = if (isDark) Color(0xFF12151B).copy(alpha = 0.94f) else Color(0xFFF8FAFC).copy(alpha = 0.96f)
        val borderBrush = Brush.linearGradient(
            listOf(
                SpotifyGreen.copy(alpha = 0.45f),
                if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f),
                YtMusicRed.copy(alpha = 0.40f)
            )
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .heightIn(max = 680.dp)
                .clip(dialogShape)
                .border(BorderStroke(1.2.dp, borderBrush), dialogShape),
            shape = dialogShape,
            color = containerBg,
            tonalElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Subtle top ambient glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    activeAccent.copy(alpha = if (isDark) 0.16f else 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // --- HEADER ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy((-8).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SpotifyGreen.copy(alpha = 0.16f))
                                    .border(
                                        1.dp,
                                        SpotifyGreen.copy(alpha = 0.38f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                SpotifyLogoIcon(
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(YtMusicRed.copy(alpha = 0.16f))
                                    .border(
                                        1.dp,
                                        YtMusicRed.copy(alpha = 0.38f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                YouTubeMusicLogoIcon(
                                    tint = YtMusicRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Import via Link",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Text(
                                text = "Playlist, Album, Lagu & Artis (Spotify / YT Music)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }

                        if (!isImporting) {
                            IconButton(
                                onClick = {
                                    activeJob?.cancel()
                                    onDismiss()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "Tutup",
                                    tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val currentStage = when {
                        importSummary != null -> "SUMMARY"
                        isImporting -> "IMPORTING"
                        playlistData != null -> "PREVIEW"
                        else -> "INPUT"
                    }

                    AnimatedContent(
                        targetState = currentStage,
                        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                        label = "spotifyImportStage"
                    ) { stage ->
                        when (stage) {
                            "INPUT" -> {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = urlInput,
                                        onValueChange = {
                                            urlInput = it
                                            fetchError = null
                                        },
                                        label = { Text("Link Spotify / YouTube Music") },
                                        placeholder = { Text("https://open.spotify.com/... atau music.youtube.com/...") },
                                        singleLine = true,
                                        enabled = !isFetchingPlaylist,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = activeAccent,
                                            focusedLabelColor = activeAccent,
                                            cursorColor = activeAccent
                                        ),
                                        trailingIcon = {
                                            if (urlInput.isNotEmpty() && !isFetchingPlaylist) {
                                                IconButton(onClick = { urlInput = "" }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.close),
                                                        contentDescription = "Hapus",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("spotify_url_input")
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    // Quick Paste from Clipboard + Supported platform badges
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = activeAccent.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.32f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(enabled = !isFetchingPlaylist) {
                                                    val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                                                    if (clip.isNotBlank()) {
                                                        urlInput = clip
                                                        fetchError = null
                                                    } else {
                                                        Toast.makeText(
                                                            context,
                                                            "Clipboard kosong",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                                .testTag("spotify_paste_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.link),
                                                    contentDescription = null,
                                                    tint = activeAccent,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "Tempel dari Clipboard",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = activeAccent
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Spotify & YT Music",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }

                                    if (fetchError != null) {
                                        Spacer(Modifier.height(12.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = fetchError ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(20.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                activeJob?.cancel()
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text("Batal", fontWeight = FontWeight.SemiBold)
                                        }

                                        Button(
                                            onClick = { triggerFetch(urlInput) },
                                            enabled = !isFetchingPlaylist && urlInput.isNotBlank(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = activeAccent,
                                                contentColor = activeButtonContentColor
                                            ),
                                            modifier = Modifier
                                                .weight(1.5f)
                                                .height(48.dp)
                                                .testTag("spotify_fetch_button")
                                        ) {
                                            if (isFetchingPlaylist) {
                                                CircularProgressIndicator(
                                                    color = activeButtonContentColor,
                                                    strokeWidth = 2.2.dp,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text("Memuat...", fontWeight = FontWeight.Bold)
                                            } else {
                                                Text("Cari Playlist", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            "PREVIEW" -> {
                                val data = playlistData ?: return@AnimatedContent
                                val isYtSource = data.platform == SpotifyPlaylistImporter.ImportSourcePlatform.YOUTUBE_MUSIC
                                val previewAccent = if (isYtSource) YtMusicRed else SpotifyGreen
                                val previewBtnContent = if (isYtSource) Color.White else Color.Black

                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Playlist Info Card
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(previewAccent.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!data.coverArtUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = data.coverArtUrl,
                                                        contentDescription = data.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else if (isYtSource) {
                                                    YouTubeMusicLogoIcon(
                                                        tint = YtMusicRed,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                } else {
                                                    SpotifyLogoIcon(
                                                        tint = SpotifyGreen,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = data.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (data.subtitle.isNotBlank()) {
                                                    Text(
                                                        text = "${data.platform.label} • ${data.subtitle}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Spacer(Modifier.height(4.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = previewAccent.copy(alpha = 0.18f)
                                                ) {
                                                    Text(
                                                        text = "${data.type.label} • ${data.tracks.size} Lagu ditemukan",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = previewAccent,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            TextButton(
                                                onClick = {
                                                    playlistData = null
                                                    fetchError = null
                                                }
                                            ) {
                                                Text("Ganti", fontSize = 12.sp, color = previewAccent)
                                            }
                                        }
                                    }

                                    // Quick Action Buttons for Single Track (Putar Lagu) or Artist (Buka Halaman Artis)
                                    val firstTrack = data.tracks.firstOrNull()
                                    val showPlayTrackQuickButton = onPlaySong != null &&
                                        firstTrack != null &&
                                        (data.type == SpotifyPlaylistImporter.SpotifyResourceType.TRACK || data.tracks.size == 1)
                                    val showOpenArtistQuickButton = onOpenArtist != null &&
                                        data.type == SpotifyPlaylistImporter.SpotifyResourceType.ARTIST

                                    if (showPlayTrackQuickButton || showOpenArtistQuickButton) {
                                        Spacer(Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                if (isQuickActionLoading) return@Button
                                                isQuickActionLoading = true
                                                activeJob?.cancel()
                                                activeJob = scope.launch {
                                                    if (showPlayTrackQuickButton && firstTrack != null) {
                                                        val matchedSong = SpotifyPlaylistImporter.resolveSingleTrack(firstTrack)
                                                        isQuickActionLoading = false
                                                        if (matchedSong != null) {
                                                            onPlaySong?.invoke(matchedSong)
                                                            onDismiss()
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Gagal menemukan lagu yang cocok untuk diputar",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    } else if (showOpenArtistQuickButton) {
                                                        val artistId = SpotifyPlaylistImporter.resolveArtistBrowseId(data)
                                                        isQuickActionLoading = false
                                                        if (artistId != null) {
                                                            onOpenArtist?.invoke(artistId)
                                                            onDismiss()
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                "Halaman artis tidak ditemukan, silakan import daftar lagunya di bawah",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            },
                                            enabled = !isQuickActionLoading,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = previewAccent,
                                                contentColor = previewBtnContent
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(46.dp)
                                        ) {
                                            if (isQuickActionLoading) {
                                                CircularProgressIndicator(
                                                    color = previewBtnContent,
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text("Membuka...", fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(
                                                    painter = painterResource(
                                                        if (showPlayTrackQuickButton) R.drawable.play else R.drawable.artist
                                                    ),
                                                    contentDescription = null,
                                                    tint = previewBtnContent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = if (showPlayTrackQuickButton) {
                                                        "Putar Lagu Sekarang"
                                                    } else {
                                                        "Buka Halaman Artis (${data.name})"
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    // Editable Playlist Name
                                    OutlinedTextField(
                                        value = customPlaylistName,
                                        onValueChange = { customPlaylistName = it },
                                        label = { Text("Nama Playlist di Pustaka") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = previewAccent,
                                            focusedLabelColor = previewAccent,
                                            cursorColor = previewAccent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    // Select All / Deselect All Header
                                    val allSelected = selectedTrackIndices.size == data.tracks.size
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Daftar Lagu (${selectedTrackIndices.size}/${data.tracks.size} dipilih)",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                        )
                                        TextButton(
                                            onClick = {
                                                if (allSelected) {
                                                    selectedTrackIndices.clear()
                                                } else {
                                                    selectedTrackIndices.clear()
                                                    selectedTrackIndices.addAll(data.tracks.indices)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (allSelected) "Hapus Pilihan" else "Pilih Semua",
                                                fontSize = 12.sp,
                                                color = previewAccent,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    // Scrollable Track List
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isDark) Color.Black.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.03f),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 140.dp, max = 230.dp)
                                    ) {
                                        LazyColumn(
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            itemsIndexed(
                                                items = data.tracks,
                                                key = { idx, item -> "${idx}_${item.id}" }
                                            ) { idx, track ->
                                                val isChecked = selectedTrackIndices.contains(idx)
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            if (isChecked) {
                                                                selectedTrackIndices.remove(idx)
                                                            } else {
                                                                selectedTrackIndices.add(idx)
                                                            }
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {
                                                                if (!selectedTrackIndices.contains(idx)) {
                                                                    selectedTrackIndices.add(idx)
                                                                }
                                                            } else {
                                                                selectedTrackIndices.remove(idx)
                                                            }
                                                        },
                                                        colors = CheckboxDefaults.colors(
                                                            checkedColor = previewAccent,
                                                            checkmarkColor = previewBtnContent
                                                        ),
                                                        modifier = Modifier.size(32.dp)
                                                    )

                                                    Spacer(Modifier.width(6.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = track.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = track.artist.ifBlank { "Artis tidak diketahui" },
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    if (track.formattedDuration.isNotBlank()) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = track.formattedDuration,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onDismiss,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text("Batal", fontWeight = FontWeight.SemiBold)
                                        }

                                        Button(
                                            onClick = {
                                                val chosenTracks = selectedTrackIndices
                                                    .sorted()
                                                    .mapNotNull { data.tracks.getOrNull(it) }
                                                if (chosenTracks.isEmpty()) return@Button

                                                isImporting = true
                                                importProgress = SpotifyPlaylistImporter.ImportProgress(
                                                    processedCount = 0,
                                                    totalCount = chosenTracks.size,
                                                    matchedCount = 0,
                                                    failedCount = 0,
                                                    currentTrackTitle = "Menyiapkan import lagu..."
                                                )

                                                activeJob = scope.launch {
                                                    val res = SpotifyPlaylistImporter.importTracksToDatabase(
                                                        database = database,
                                                        playlistName = customPlaylistName.ifBlank { data.name },
                                                        coverArtUrl = data.coverArtUrl,
                                                        tracks = chosenTracks,
                                                        onProgress = { prog ->
                                                            importProgress = prog
                                                        }
                                                    )
                                                    isImporting = false
                                                    res.onSuccess { summary ->
                                                        importSummary = summary
                                                    }.onFailure { err ->
                                                        fetchError = err.message ?: "Gagal mengimport lagu"
                                                        playlistData = null
                                                    }
                                                }
                                            },
                                            enabled = selectedTrackIndices.isNotEmpty(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = previewAccent,
                                                contentColor = previewBtnContent
                                            ),
                                            modifier = Modifier
                                                .weight(1.6f)
                                                .height(48.dp)
                                                .testTag("spotify_start_import_button")
                                        ) {
                                            Text(
                                                text = "Import ${selectedTrackIndices.size} Lagu",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            "IMPORTING" -> {
                                val prog = importProgress
                                val animatedFrac by animateFloatAsState(
                                    targetValue = prog?.progressFraction ?: 0f,
                                    animationSpec = tween(220),
                                    label = "spotifyImportProgress"
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = activeAccent,
                                        strokeWidth = 3.5.dp,
                                        modifier = Modifier.size(54.dp)
                                    )

                                    Spacer(Modifier.height(18.dp))

                                    Text(
                                        text = "Mengimport Lagu ke Pustaka...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = prog?.currentTrackTitle ?: "Memproses...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    Spacer(Modifier.height(16.dp))

                                    LinearProgressIndicator(
                                        progress = { animatedFrac },
                                        color = activeAccent,
                                        trackColor = activeAccent.copy(alpha = 0.18f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape)
                                    )

                                    Spacer(Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${prog?.processedCount ?: 0} / ${prog?.totalCount ?: 0} lagu",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Berhasil: ${prog?.matchedCount ?: 0}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = activeAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            "SUMMARY" -> {
                                val summary = importSummary ?: return@AnimatedContent
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(activeAccent.copy(alpha = 0.18f))
                                            .border(1.5.dp, activeAccent, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.check),
                                            contentDescription = null,
                                            tint = activeAccent,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    Spacer(Modifier.height(14.dp))

                                    Text(
                                        text = "Playlist Berhasil Diimport!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = "\"${summary.playlistName}\"\n${summary.matchedCount} dari ${summary.totalRequested} lagu berhasil ditambahkan ke Pustaka.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                                        textAlign = TextAlign.Center
                                    )

                                    if (summary.failedTracks.isNotEmpty()) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = "${summary.failedTracks.size} lagu tidak ditemukan kecocokannya.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        )
                                    }

                                    Spacer(Modifier.height(22.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = onDismiss,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                        ) {
                                            Text("Tutup", fontWeight = FontWeight.SemiBold)
                                        }

                                        if (onPlaylistImported != null && summary.matchedCount > 0) {
                                            Button(
                                                onClick = {
                                                    onDismiss()
                                                    onPlaylistImported(summary.playlistId)
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = activeAccent,
                                                    contentColor = activeButtonContentColor
                                                ),
                                                modifier = Modifier
                                                    .weight(1.4f)
                                                    .height(48.dp)
                                                    .testTag("spotify_open_playlist_button")
                                            ) {
                                                Text("Buka Playlist", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
