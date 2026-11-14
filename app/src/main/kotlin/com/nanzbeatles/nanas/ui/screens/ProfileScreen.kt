/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.nanzbeatles.innertube.models.WatchEndpoint
import com.nanzbeatles.innertube.utils.parseCookieString
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.LocalPlayerAwareWindowInsets
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.AccountChannelHandleKey
import com.nanzbeatles.nanas.constants.AccountNameKey
import com.nanzbeatles.nanas.constants.InnerTubeCookieKey
import com.nanzbeatles.nanas.constants.ProfileLocalAvatarUriV2Key
import com.nanzbeatles.nanas.constants.ProfileLocalBannerIsVideoKey
import com.nanzbeatles.nanas.constants.ProfileLocalBannerUriKey
import com.nanzbeatles.nanas.constants.ProfileLocalBioV2Key
import com.nanzbeatles.nanas.constants.ProfileLocalHandleV2Key
import com.nanzbeatles.nanas.constants.ProfileLocalNameV2Key
import com.nanzbeatles.nanas.playback.queues.YouTubeQueue
import com.nanzbeatles.nanas.streak.MusicJourneyManager
import com.nanzbeatles.nanas.streak.MusicJourneyState
import com.nanzbeatles.nanas.streak.StreakManager
import com.nanzbeatles.nanas.streak.StreakTier
import com.nanzbeatles.nanas.streak.SummaryPeriodFilter
import com.nanzbeatles.nanas.streak.TopSongStatItem
import com.nanzbeatles.nanas.ui.component.DynamicIslandSettingsDialog
import com.nanzbeatles.nanas.ui.component.ProfileShareCanvasData
import com.nanzbeatles.nanas.ui.component.ProfileShareCanvasDialog
import com.nanzbeatles.nanas.ui.component.StreakDetailModalDialog
import com.nanzbeatles.nanas.ui.component.StreakFlameIcon
import com.nanzbeatles.nanas.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun formatDotNumber(value: Int): String {
    return NumberFormat.getIntegerInstance(Locale("id", "ID")).format(value)
}

/**
 * Copies a picked image or video URI into app-private storage (`context.filesDir`)
 * so that the avatar or banner persists across app restarts and can be backed up.
 */
private suspend fun copyPickedMediaToInternalFile(
    context: Context,
    sourceUri: Uri,
    targetPrefix: String,
): Pair<String, Boolean>? = withContext(Dispatchers.IO) {
    runCatching {
        val mime = context.contentResolver.getType(sourceUri).orEmpty().lowercase(Locale.ROOT)
        val pathLower = sourceUri.toString().lowercase(Locale.ROOT)
        val isVideo = mime.startsWith("video/") ||
            pathLower.endsWith(".mp4") ||
            pathLower.endsWith(".webm") ||
            pathLower.endsWith(".mkv") ||
            pathLower.endsWith(".mov")

        val ext = if (isVideo) "mp4" else "jpg"
        val fixedFile = File(context.filesDir, "${targetPrefix}.$ext")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            fixedFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext null

        // Append version timestamp query to Uri so Coil / MediaPlayer refreshes immediately when updated
        val fileUriString = Uri.fromFile(fixedFile).buildUpon()
            .appendQueryParameter("t", System.currentTimeMillis().toString())
            .build()
            .toString()
        fileUriString to isVideo
    }.getOrNull()
}

@Composable
fun ProfileScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current
    val database = LocalDatabase.current

    LaunchedEffect(Unit) {
        StreakManager.ensureInitialized(context)
        MusicJourneyManager.ensureInitialized(context)
    }

    val streakState by StreakManager.state.collectAsState()
    val journeyState by MusicJourneyManager.state.collectAsState()
    val dbSongs by database.mostPlayedSongs(fromTimeStamp = 0L, limit = 10).collectAsState(initial = emptyList())

    // Local Profile preferences (starts empty / @user as requested, with optional YouTube account sync)
    var localName by rememberPreference(ProfileLocalNameV2Key, defaultValue = "User")
    var localHandle by rememberPreference(ProfileLocalHandleV2Key, defaultValue = "@user")
    var localBio by rememberPreference(ProfileLocalBioV2Key, defaultValue = "Belum ada bio")
    var localAvatarUri by rememberPreference(ProfileLocalAvatarUriV2Key, defaultValue = "")
    var localBannerUri by rememberPreference(ProfileLocalBannerUriKey, defaultValue = "")
    var localBannerIsVideo by rememberPreference(ProfileLocalBannerIsVideoKey, defaultValue = false)

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, defaultValue = "")
    val accountName by rememberPreference(AccountNameKey, defaultValue = "")
    val accountHandle by rememberPreference(AccountChannelHandleKey, defaultValue = "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val displayName = remember(localName, accountName, isLoggedIn) {
        when {
            localName.isNotBlank() && localName != "User" -> localName
            isLoggedIn && accountName.isNotBlank() -> accountName
            localName.isNotBlank() -> localName
            else -> "User"
        }
    }
    val displayHandle = remember(localHandle, accountHandle, isLoggedIn) {
        val raw = when {
            localHandle.isNotBlank() && localHandle != "@user" -> localHandle
            isLoggedIn && accountHandle.isNotBlank() -> accountHandle
            localHandle.isNotBlank() -> localHandle
            else -> "@user"
        }
        if (raw.startsWith("@")) raw else "@$raw"
    }

    // Direct Banner Picker (supports both Photo and Video)
    val bannerMediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val saved = copyPickedMediaToInternalFile(context, uri, "profile_banner_media")
                if (saved != null) {
                    localBannerUri = saved.first
                    localBannerIsVideo = saved.second
                    Toast.makeText(
                        context,
                        if (saved.second) "Banner video berhasil dipasang!" else "Banner foto berhasil dipasang!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    localBannerUri = uri.toString()
                    localBannerIsVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
                }
            }
        }
    }

    // Backup & Restore All Statistics Launchers
    val exportAllStatsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val ok = MusicJourneyManager.exportAllStatsToUri(context, uri)
                Toast.makeText(
                    context,
                    if (ok) "Semua statistik berhasil dicadangkan ke file!" else "Gagal mencadangkan statistik",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val importAllStatsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val ok = MusicJourneyManager.importAllStatsFromUri(context, uri)
                Toast.makeText(
                    context,
                    if (ok) "Semua statistik berhasil dipulihkan dari file!" else "File cadangan statistik tidak valid",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Merge real played songs from Room DB with journeyState.topSongs (no fake Linkin Park fallback)
    val mergedTopSongs = remember(journeyState.topSongs, dbSongs) {
        val list = journeyState.topSongs.toMutableList()
        dbSongs.forEach { song ->
            if (song.song.totalPlayTime > 0L) {
                val exists = list.any {
                    it.songId == song.id || it.title.equals(song.song.title, ignoreCase = true)
                }
                if (!exists) {
                    list.add(
                        TopSongStatItem(
                            songId = song.id,
                            title = song.song.title,
                            artist = song.artists.joinToString(", ") { it.name }.ifBlank { "Artis" },
                            playCount = (song.song.totalPlayTime / 180_000L).toInt().coerceAtLeast(1),
                            listeningSeconds = (song.song.totalPlayTime / 1000L).coerceAtLeast(30L),
                            thumbnailUrl = song.song.thumbnailUrl,
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.playCount }.take(10)
    }

    val topSong = mergedTopSongs.firstOrNull()

    // Dialog states
    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showLevelModal by rememberSaveable { mutableStateOf(false) }
    var showStreakModal by rememberSaveable { mutableStateOf(false) }
    var showDailyRecordModal by rememberSaveable { mutableStateOf(false) }
    var showMonthlyRecordModal by rememberSaveable { mutableStateOf(false) }
    var showListeningDetailModal by rememberSaveable { mutableStateOf(false) }
    var showTopSongsModal by rememberSaveable { mutableStateOf(false) }
    var showQuickToolsSheet by rememberSaveable { mutableStateOf(false) }
    var showDynamicIslandDialog by rememberSaveable { mutableStateOf(false) }
    var showShareCanvasDialog by rememberSaveable { mutableStateOf(false) }
    var selectedPeriod by rememberSaveable { mutableStateOf(SummaryPeriodFilter.ALL) }

    val effectiveStreakDays = streakState.currentStreak
    val levelSpec = journeyState.levelSpec
    val (periodSeconds, periodSongs) = remember(journeyState, selectedPeriod) {
        journeyState.summaryForPeriod(selectedPeriod)
    }
    val periodHours = (periodSeconds / 3600L).toInt()
    val periodMinutes = ((periodSeconds % 3600L) / 60L).toInt()

    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    val cardBgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF141519),
            Color(0xFF0C0D10),
        )
    )
    val cardBorderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.12f),
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050507))
            .drawBehind {
                val w = size.width
                val h = size.height

                // Diagonal dark silk wave at top
                val wavePath = Path().apply {
                    moveTo(0f, h * 0.18f)
                    cubicTo(
                        w * 0.35f, h * 0.12f,
                        w * 0.55f, h * 0.02f,
                        w * 0.72f, 0f
                    )
                    lineTo(w, 0f)
                    lineTo(w, h * 0.28f)
                    cubicTo(
                        w * 0.65f, h * 0.22f,
                        w * 0.30f, h * 0.26f,
                        0f, h * 0.18f
                    )
                    close()
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.035f),
                            Color.Transparent,
                        )
                    )
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .testTag("profile_lazy_column"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = bottomPadding + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // =========================================================================
            // 1. TOP BAR: Waveform Icon + "Profil" | Quick Tools, Settings (⚙) & Share (🔗)
            // =========================================================================
            item(key = "profile_top_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileWaveformLogoIcon(modifier = Modifier.size(32.dp))
                        Text(
                            text = "Profil",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = (-0.3).sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Quick Tools button (Riwayat, Dynamic Island, Tema, Cadangkan)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14151A), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                .clickable { showQuickToolsSheet = true }
                                .testTag("profile_quick_tools_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.history),
                                contentDescription = "Riwayat & Fitur Cepat",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Settings button (⚙)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14151A), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                .clickable { navController.navigate("settings") }
                                .testTag("profile_settings_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileGearIcon(modifier = Modifier.size(20.dp))
                        }

                        // Share button (🔗) — opens detailed Canvas Image Share & Download dialog
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14151A), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                                .clickable { showShareCanvasDialog = true }
                                .testTag("profile_share_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = "Bagikan Gambar Statistik Profil",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 2. PROFILE HEADER WITH UPLOADABLE PHOTO / VIDEO BANNER BEHIND AVATAR & @user
            // =========================================================================
            item(key = "profile_identity_header") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF151821), Color(0xFF0B0C10))
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(22.dp))
                        .testTag("profile_banner_container")
                ) {
                    // Background Layer: Custom Photo/Video Banner OR Decorative Vinyl Grooves
                    if (localBannerUri.isNotBlank()) {
                        if (localBannerIsVideo) {
                            LoopingVideoBanner(
                                videoUriString = localBannerUri,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(22.dp))
                            )
                        } else {
                            AsyncImage(
                                model = localBannerUri,
                                contentDescription = "Banner Profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(22.dp))
                            )
                        }
                        // Dark gradient overlay so avatar, name, @user, and bio stay crisp
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.30f),
                                            Color(0xB808090D),
                                            Color(0xE608090D)
                                        )
                                    )
                                )
                        )
                    } else {
                        // Default decorative vinyl grooves inside the banner when no banner is uploaded yet
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            val vinylCenter = Offset(w * 0.92f, h * 0.50f)
                            val maxVinylRadius = w * 0.42f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.02f),
                                        Color.Transparent
                                    ),
                                    center = vinylCenter,
                                    radius = maxVinylRadius
                                ),
                                radius = maxVinylRadius,
                                center = vinylCenter
                            )
                            for (ring in 1..6) {
                                val r = maxVinylRadius * (0.30f + ring * 0.11f)
                                drawCircle(
                                    color = Color.White.copy(alpha = if (ring % 2 == 0) 0.08f else 0.04f),
                                    radius = r,
                                    center = vinylCenter,
                                    style = Stroke(width = if (ring == 3) 2.dp.toPx() else 1.dp.toPx())
                                )
                            }
                        }
                    }

                    // Top-Right Banner Upload Button (Photo / Video)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.58f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                                .clickable {
                                    bannerMediaPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("profile_upload_banner_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.edit),
                                    contentDescription = "Upload Banner Foto atau Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (localBannerUri.isBlank()) "Banner Foto/Video" else if (localBannerIsVideo) "Ganti Video" else "Ganti Banner",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Foreground Content: Empty/Custom Avatar + Name ("User") + Handle ("@user") + Bio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar with glowing white ring & bottom-right pencil button
                        Box(
                            modifier = Modifier
                                .size(94.dp)
                                .clickable { showEditProfileDialog = true }
                                .testTag("profile_avatar_box"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White,
                                                Color(0xFFB8C0CC),
                                                Color.White
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF14161C)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (localAvatarUri.isNotBlank()) {
                                    AsyncImage(
                                        model = localAvatarUri,
                                        contentDescription = "Avatar Profil",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    // Empty Profile Placeholder (no pre-filled anime image)
                                    EmptyAvatarSilhouetteIcon(
                                        modifier = Modifier.size(46.dp)
                                    )
                                }
                            }

                            // Pencil Edit Button at bottom-right of avatar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 2.dp, bottom = 2.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1A1C22), CircleShape)
                                    .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                    .clickable { showEditProfileDialog = true }
                                    .testTag("profile_edit_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.edit),
                                    contentDescription = "Edit Profil",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEditProfileDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                VerifiedSealBadgeIcon(modifier = Modifier.size(19.dp))
                                Image(
                                    painter = painterResource(journeyState.levelSpec.drawableResId),
                                    contentDescription = "Badge Level ${journeyState.levelSpec.level}",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { showLevelModal = true },
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = displayHandle,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD0D5E0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "✦",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            Text(
                                text = localBio.ifBlank { "Belum ada bio" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFA0A6B4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. LEVEL PENDENGAR CARD (Hexagonal Winged Headphone Emblem + XP Bar)
            // =========================================================================
            item(key = "profile_level_card") {
                val animatedXpProgress by animateFloatAsState(
                    targetValue = journeyState.xpProgressFraction,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
                    label = "profileXpProgress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBgBrush, RoundedCornerShape(20.dp))
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.11f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.16f, size.height * 0.5f),
                                    radius = size.height * 0.95f
                                ),
                                radius = size.height * 0.95f,
                                center = Offset(size.width * 0.16f, size.height * 0.5f)
                            )
                        }
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                        .clickable { showLevelModal = true }
                        .padding(horizontal = 16.dp, vertical = 15.dp)
                        .testTag("profile_level_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(levelSpec.drawableResId),
                            contentDescription = "Level ${levelSpec.level}",
                            modifier = Modifier.size(68.dp),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Level ${levelSpec.level}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD0D5E0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = levelSpec.title,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    painter = painterResource(R.drawable.navigate_next),
                                    contentDescription = "Lihat Level",
                                    tint = Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = levelSpec.subtitle,
                                fontSize = 12.sp,
                                color = Color(0xFF989EA9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(9.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.10f))
                                ) {
                                    if (animatedXpProgress > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(animatedXpProgress)
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFFD8DEE9),
                                                            Color.White
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }

                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            )
                                        ) {
                                            append(formatDotNumber(journeyState.xp))
                                        }
                                        withStyle(
                                            SpanStyle(
                                                color = Color(0xFF989EA9),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.5.sp
                                            )
                                        ) {
                                            append(" / ${formatDotNumber(journeyState.xpTargetForCurrentBanner)} XP")
                                        }
                                    },
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. 3-CARD ROW: 🔥 Streak (Original Colorful Flame) | ⏱️ Rekor Harian | 📅 Rekor Bulanan
            // =========================================================================
            item(key = "profile_records_row") {
                val activeTier = StreakTier.fromStreak(effectiveStreakDays)
                val activeFlameVisual = activeTier.toVisual()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card 1: Streak (with original colorful animated flame)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBgBrush, RoundedCornerShape(18.dp))
                            .drawBehind {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            activeFlameVisual.glowColor.copy(
                                                alpha = if (effectiveStreakDays > 0) 0.22f else 0.06f
                                            ),
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width * 0.28f, size.height * 0.35f),
                                        radius = size.width * 0.72f
                                    ),
                                    radius = size.width * 0.72f,
                                    center = Offset(size.width * 0.28f, size.height * 0.35f)
                                )
                            }
                            .border(1.dp, cardBorderBrush, RoundedCornerShape(18.dp))
                            .clickable { showStreakModal = true }
                            .padding(horizontal = 9.dp, vertical = 11.dp)
                            .testTag("profile_streak_card")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                StreakFlameIcon(
                                    flame = activeFlameVisual,
                                    size = 30.dp,
                                    animated = true,
                                    animationTriggerToken = streakState.animationTriggerToken
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Streak",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFB0B6C2),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$effectiveStreakDays Hari",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MiniColorfulFlameIcon(
                                    flameColor = if (effectiveStreakDays > 0) activeFlameVisual.midColor else Color(0xFF94A3B8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (effectiveStreakDays == 0) "Mulai dengar!" else "Terus dengar!",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFA0A6B4),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Card 2: Rekor Harian
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBgBrush, RoundedCornerShape(18.dp))
                            .border(1.dp, cardBorderBrush, RoundedCornerShape(18.dp))
                            .clickable { showDailyRecordModal = true }
                            .padding(horizontal = 9.dp, vertical = 11.dp)
                            .testTag("profile_daily_record_card")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                MonochromeClockIcon(modifier = Modifier.size(26.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Rekor Harian",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFB0B6C2),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = journeyState.dailyRecordCompactText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MonochromeStarIcon(modifier = Modifier.size(12.dp))
                                Text(
                                    text = "Hari terbaik!",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFA0A6B4),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Card 3: Rekor Bulanan
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBgBrush, RoundedCornerShape(18.dp))
                            .border(1.dp, cardBorderBrush, RoundedCornerShape(18.dp))
                            .clickable { showMonthlyRecordModal = true }
                            .padding(horizontal = 9.dp, vertical = 11.dp)
                            .testTag("profile_monthly_record_card")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                MonochromeCalendarIcon(modifier = Modifier.size(26.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Rekor Bulanan",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFB0B6C2),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = journeyState.monthlyRecordHoursText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                MonochromeCrownIcon(modifier = Modifier.size(12.dp))
                                Text(
                                    text = "Bulan terbaik!",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFA0A6B4),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 5. RINGKASAN MENDENGARKAN CARD (Starts at 0 Jam | 0 Total Lagu + Waveform)
            // =========================================================================
            item(key = "profile_listening_summary_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBgBrush, RoundedCornerShape(20.dp))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                        .clickable { showListeningDetailModal = true }
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .testTag("profile_listening_summary_card")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MonochromeBarChartIcon(modifier = Modifier.size(19.dp))
                                Text(
                                    text = "Ringkasan Mendengarkan",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showListeningDetailModal = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Lihat Detail",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFA0A6B4)
                                )
                                Icon(
                                    painter = painterResource(R.drawable.navigate_next),
                                    contentDescription = null,
                                    tint = Color(0xFFA0A6B4),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Duration
                            Column(
                                modifier = Modifier.weight(1.1f)
                            ) {
                                val showMinutesInstead = periodHours == 0 && periodSeconds > 0L
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                fontSize = 35.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        ) {
                                            if (showMinutesInstead) {
                                                append("${formatDotNumber(periodMinutes.coerceAtLeast(1))} ")
                                            } else {
                                                append("${formatDotNumber(periodHours)} ")
                                            }
                                        }
                                        withStyle(
                                            SpanStyle(
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        ) {
                                            append(if (showMinutesInstead) "Menit" else "Jam")
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    MonochromeClockIcon(modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "Total Waktu Mendengarkan",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9EA4B0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Vertical Divider
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(Color.White.copy(alpha = 0.14f))
                            )

                            // Right: Total Lagu
                            Row(
                                modifier = Modifier.weight(0.9f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.music_note),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                                Column {
                                    Text(
                                        text = formatDotNumber(periodSongs),
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Total Lagu",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9EA4B0)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Filter Chips: Semua | Hari | Minggu | Bulan | Tahun
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SummaryPeriodFilter.entries.forEach { period ->
                                val selected = selectedPeriod == period
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) Color.White else Color.White.copy(alpha = 0.06f)
                                        )
                                        .clickable { selectedPeriod = period }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = period.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (selected) Color.Black else Color(0xFFB4BAC6)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        MonochromeWaveformGraph(
                            periodSeed = selectedPeriod.ordinal,
                            isActive = periodSeconds > 0L,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        )
                    }
                }
            }

            // =========================================================================
            // 6. LAGU PALING SERING DIDENGAR CARD (Starts Empty at 0 until songs are played)
            // =========================================================================
            item(key = "profile_top_song_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBgBrush, RoundedCornerShape(20.dp))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                        .clickable { showTopSongsModal = true }
                        .padding(horizontal = 16.dp, vertical = 15.dp)
                        .testTag("profile_top_song_card")
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MonochromeStarIcon(modifier = Modifier.size(19.dp))
                                Text(
                                    text = "Lagu Paling Sering Didengar",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showTopSongsModal = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Lihat Semua",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFA0A6B4)
                                )
                                Icon(
                                    painter = painterResource(R.drawable.navigate_next),
                                    contentDescription = null,
                                    tint = Color(0xFFA0A6B4),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0A0B0E))
                                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (topSong?.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = topSong.thumbnailUrl,
                                        contentDescription = topSong.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    GeometricLinkinPrismArt(modifier = Modifier.size(44.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topSong?.title ?: "Belum Ada Lagu",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = topSong?.artist ?: "Putar musik untuk mulai mencatat",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF9EA4B0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    MonochromeHeadphoneMiniIcon(modifier = Modifier.size(14.dp))
                                    Text(
                                        text = "${topSong?.playCount ?: 0}x diputar",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB8BECB)
                                    )
                                }
                            }

                            if (topSong != null) {
                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF14161B), CircleShape)
                                        .border(
                                            width = 1.4.dp,
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.55f),
                                                    Color.White.copy(alpha = 0.18f),
                                                    Color.White.copy(alpha = 0.45f)
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            playerConnection?.playQueue(
                                                YouTubeQueue(
                                                    WatchEndpoint(videoId = topSong.songId)
                                                )
                                            )
                                        }
                                        .testTag("profile_play_top_song_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = "Putar ${topSong.title}",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 7. ACHIEVEMENT / LEVEL BANNER CARD
            // =========================================================================
            item(key = "profile_achievement_banner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBgBrush, RoundedCornerShape(20.dp))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                        .clickable { showLevelModal = true }
                        .padding(horizontal = 16.dp, vertical = 15.dp)
                        .testTag("profile_achievement_banner")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MonochromeTrophyIcon(modifier = Modifier.size(48.dp))

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 14.dp)
                                .width(1.dp)
                                .height(44.dp)
                                .background(Color.White.copy(alpha = 0.14f))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kamu berada di",
                                fontSize = 12.sp,
                                color = Color(0xFFA0A6B4)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Level ${levelSpec.level} – ${levelSpec.title}!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Terus dengarkan musik kesukaanmu!",
                                fontSize = 12.sp,
                                color = Color(0xFF9096A4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        RisingBarsArrowIcon(modifier = Modifier.size(44.dp))
                    }
                }
            }

            // =========================================================================
            // 8. CADANGKAN & PULIHKAN SEMUA STATISTIK KE FILE
            // =========================================================================
            item(key = "profile_backup_restore_stats_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBgBrush, RoundedCornerShape(20.dp))
                        .border(1.dp, cardBorderBrush, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("profile_backup_stats_card")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Cadangkan & Pulihkan Statistik",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Simpan semua statistik (Streak, Level XP, Rekor, & Lagu) ke file",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF989EA9)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
                                    exportAllStatsLauncher.launch(
                                        "Statistik_Musik_${LocalDateTime.now().format(formatter)}.json"
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_export_stats_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.backup),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cadangkan File",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    importAllStatsLauncher.launch(
                                        arrayOf("application/json", "application/octet-stream", "*/*")
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_import_stats_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.restore),
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pulihkan File",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODALS & INTERACTIVE SHEETS FOR ALL SYSTEMS
    // =========================================================================

    if (showShareCanvasDialog) {
        ProfileShareCanvasDialog(
            data = ProfileShareCanvasData(
                displayName = displayName,
                displayHandle = displayHandle,
                bio = localBio,
                avatarUri = localAvatarUri,
                bannerUri = localBannerUri,
                bannerIsVideo = localBannerIsVideo,
                streakState = streakState,
                journeyState = journeyState,
                topSongs = mergedTopSongs,
            ),
            onDismiss = { showShareCanvasDialog = false }
        )
    }

    if (showStreakModal) {
        StreakDetailModalDialog(
            state = streakState,
            onDismiss = { showStreakModal = false }
        )
    }

    if (showEditProfileDialog) {
        EditLocalProfileDialog(
            initialName = displayName,
            initialHandle = displayHandle,
            initialBio = localBio,
            currentAvatarUri = localAvatarUri,
            currentBannerUri = localBannerUri,
            currentBannerIsVideo = localBannerIsVideo,
            isLoggedIn = isLoggedIn,
            onSave = { newName, newHandle, newBio, newAvatar, newBannerUri, newBannerIsVid ->
                localName = newName.trim().ifBlank { "User" }
                localHandle = newHandle.trim().let {
                    if (it.isBlank()) "@user"
                    else if (it.startsWith("@")) it else "@$it"
                }
                localBio = newBio.trim().ifBlank { "Belum ada bio" }
                localAvatarUri = newAvatar
                localBannerUri = newBannerUri
                localBannerIsVideo = newBannerIsVid
                showEditProfileDialog = false
            },
            onOpenYouTubeLogin = {
                showEditProfileDialog = false
                navController.navigate("login")
            },
            onDismiss = { showEditProfileDialog = false }
        )
    }

    if (showLevelModal) {
        ListenerLevelsModalDialog(
            journeyState = journeyState,
            effectiveStreakDays = effectiveStreakDays,
            onExportStatsFile = {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
                exportAllStatsLauncher.launch(
                    "Statistik_Musik_${LocalDateTime.now().format(formatter)}.json"
                )
            },
            onImportStatsFile = {
                importAllStatsLauncher.launch(
                    arrayOf("application/json", "application/octet-stream", "*/*")
                )
            },
            onDismiss = { showLevelModal = false }
        )
    }

    if (showDailyRecordModal) {
        DailyRecordDetailDialog(
            journeyState = journeyState,
            onDismiss = { showDailyRecordModal = false }
        )
    }

    if (showMonthlyRecordModal) {
        MonthlyRecordDetailDialog(
            journeyState = journeyState,
            onDismiss = { showMonthlyRecordModal = false }
        )
    }

    if (showListeningDetailModal) {
        ListeningSummaryDetailDialog(
            journeyState = journeyState,
            onOpenFullStats = {
                showListeningDetailModal = false
                navController.navigate("stats")
            },
            onDismiss = { showListeningDetailModal = false }
        )
    }

    if (showTopSongsModal) {
        Top10SongsModalDialog(
            songs = mergedTopSongs,
            onPlaySong = { item ->
                playerConnection?.playQueue(
                    YouTubeQueue(WatchEndpoint(videoId = item.songId))
                )
            },
            onDismiss = { showTopSongsModal = false }
        )
    }

    if (showQuickToolsSheet) {
        QuickProfileToolsDialog(
            onNavigateHistory = {
                showQuickToolsSheet = false
                navController.navigate("history")
            },
            onNavigateStats = {
                showQuickToolsSheet = false
                navController.navigate("stats")
            },
            onOpenDynamicIsland = {
                showQuickToolsSheet = false
                showDynamicIslandDialog = true
            },
            onNavigateTheme = {
                showQuickToolsSheet = false
                navController.navigate("settings/appearance/theme")
            },
            onNavigateBackupRestore = {
                showQuickToolsSheet = false
                navController.navigate("settings/backup_restore")
            },
            onNavigateSettings = {
                showQuickToolsSheet = false
                navController.navigate("settings")
            },
            onDismiss = { showQuickToolsSheet = false }
        )
    }

    if (showDynamicIslandDialog) {
        DynamicIslandSettingsDialog(
            onDismiss = { showDynamicIslandDialog = false }
        )
    }
}

// =============================================================================
// LOOPING MUTED VIDEO BANNER PLAYER (TextureView + MediaPlayer)
// =============================================================================

@Composable
private fun LoopingVideoBanner(
    videoUriString: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(videoUriString) {
        onDispose {
            runCatching {
                mediaPlayerRef.value?.stop()
            }
            runCatching {
                mediaPlayerRef.value?.release()
            }
            mediaPlayerRef.value = null
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
            }
        },
        update = { textureView ->
            if (textureView.tag != videoUriString) {
                textureView.tag = videoUriString
                runCatching {
                    mediaPlayerRef.value?.stop()
                    mediaPlayerRef.value?.release()
                }
                mediaPlayerRef.value = null

                fun startVideoOnSurface(surfaceTexture: SurfaceTexture, viewW: Int, viewH: Int) {
                    runCatching {
                        val mp = MediaPlayer()
                        mediaPlayerRef.value = mp
                        val cleanUri = Uri.parse(videoUriString.substringBefore("?t="))
                        mp.setDataSource(context, cleanUri)
                        mp.setSurface(Surface(surfaceTexture))
                        mp.isLooping = true
                        mp.setVolume(0f, 0f)
                        mp.setOnVideoSizeChangedListener { _, videoW, videoH ->
                            if (videoW > 0 && videoH > 0 && textureView.width > 0 && textureView.height > 0) {
                                val vw = textureView.width.toFloat()
                                val vh = textureView.height.toFloat()
                                val scaleX = vw / videoW.toFloat()
                                val scaleY = vh / videoH.toFloat()
                                val maxScale = maxOf(scaleX, scaleY)
                                val scaledW = videoW * maxScale
                                val scaledH = videoH * maxScale
                                val matrix = Matrix()
                                matrix.setScale(scaledW / vw, scaledH / vh, vw / 2f, vh / 2f)
                                textureView.setTransform(matrix)
                            }
                        }
                        mp.setOnPreparedListener { player ->
                            player.setVolume(0f, 0f)
                            player.isLooping = true
                            player.start()
                        }
                        mp.prepareAsync()
                    }
                }

                if (textureView.isAvailable) {
                    textureView.surfaceTexture?.let { st ->
                        startVideoOnSurface(st, textureView.width, textureView.height)
                    }
                }
                textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        startVideoOnSurface(surface, width, height)
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        runCatching {
                            mediaPlayerRef.value?.stop()
                            mediaPlayerRef.value?.release()
                        }
                        mediaPlayerRef.value = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        }
    )
}

// =============================================================================
// CUSTOM CANVAS ICONS & EMBLEMS
// =============================================================================

@Composable
private fun EmptyAvatarSilhouetteIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Head circle
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = w * 0.21f,
            center = Offset(cx, h * 0.32f),
            style = Stroke(width = 2.2.dp.toPx())
        )

        // Shoulders arc
        val shouldersPath = Path().apply {
            moveTo(w * 0.16f, h * 0.86f)
            cubicTo(
                w * 0.18f, h * 0.58f,
                w * 0.82f, h * 0.58f,
                w * 0.84f, h * 0.86f
            )
        }
        drawPath(
            path = shouldersPath,
            color = Color.White.copy(alpha = 0.65f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun ProfileWaveformLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.12f
        val heights = floatArrayOf(0.32f, 0.62f, 0.92f, 0.56f, 0.30f)
        val spacing = w * 0.18f
        val startX = w * 0.12f
        heights.forEachIndexed { idx, frac ->
            val x = startX + idx * spacing
            val barH = h * frac
            val top = (h - barH) / 2f
            drawLine(
                color = Color.White,
                start = Offset(x, top),
                end = Offset(x, top + barH),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
        val sx = w * 0.88f
        val sy = h * 0.12f
        val sr = w * 0.09f
        drawLine(Color.White.copy(alpha = 0.85f), Offset(sx - sr, sy), Offset(sx + sr, sy), strokeWidth = 1.2.dp.toPx())
        drawLine(Color.White.copy(alpha = 0.85f), Offset(sx, sy - sr), Offset(sx, sy + sr), strokeWidth = 1.2.dp.toPx())
    }
}

@Composable
private fun ProfileGearIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerR = w * 0.34f
        val innerR = w * 0.16f
        val strokeW = 1.7.dp.toPx()

        for (i in 0 until 8) {
            val angle = (i * 45.0) * PI / 180.0
            val x1 = cx + cos(angle).toFloat() * (outerR * 0.82f)
            val y1 = cy + sin(angle).toFloat() * (outerR * 0.82f)
            val x2 = cx + cos(angle).toFloat() * (outerR * 1.18f)
            val y2 = cy + sin(angle).toFloat() * (outerR * 1.18f)
            drawLine(
                color = Color.White,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = strokeW * 1.25f,
                cap = StrokeCap.Round
            )
        }
        drawCircle(
            color = Color.White,
            radius = outerR * 0.82f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeW)
        )
        drawCircle(
            color = Color.White,
            radius = innerR,
            center = Offset(cx, cy),
            style = Stroke(width = strokeW * 0.9f)
        )
    }
}

@Composable
private fun VerifiedSealBadgeIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerR = w * 0.46f
        val innerR = w * 0.37f
        val points = 16
        val sealPath = Path()
        for (i in 0 until points) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = (i * (360.0 / points)) * PI / 180.0
            val px = cx + cos(angle).toFloat() * r
            val py = cy + sin(angle).toFloat() * r
            if (i == 0) sealPath.moveTo(px, py) else sealPath.lineTo(px, py)
        }
        sealPath.close()
        drawPath(
            path = sealPath,
            color = Color(0xFFE5E9F0)
        )
        val checkPath = Path().apply {
            moveTo(w * 0.33f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.64f)
            lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(
            path = checkPath,
            color = Color(0xFF0A0B0E),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun WingedHexHeadphoneBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.48f

        for (side in intArrayOf(-1, 1)) {
            for (tier in 0..2) {
                val wingTopY = cy - h * (0.16f - tier * 0.10f)
                val wingTipX = cx + side * w * (0.48f - tier * 0.04f)
                val wingBaseX = cx + side * w * 0.28f
                val wingPath = Path().apply {
                    moveTo(wingBaseX, wingTopY)
                    lineTo(wingTipX, wingTopY - h * 0.06f)
                    lineTo(wingTipX - side * w * 0.04f, wingTopY + h * 0.05f)
                    lineTo(wingBaseX, wingTopY + h * 0.08f)
                    close()
                }
                drawPath(
                    path = wingPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFCBD5E1), Color.White)
                    )
                )
            }
        }

        val hexR = w * 0.34f
        val hexPath = Path()
        for (i in 0 until 6) {
            val angle = (i * 60.0 - 90.0) * PI / 180.0
            val px = cx + cos(angle).toFloat() * hexR
            val py = cy + sin(angle).toFloat() * hexR
            if (i == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
        }
        hexPath.close()

        drawPath(
            path = hexPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF282C35), Color(0xFF12141A))
            )
        )
        drawPath(
            path = hexPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White, Color(0xFF94A3B8), Color(0xFFE2E8F0))
            ),
            style = Stroke(width = 2.6.dp.toPx(), join = StrokeJoin.Round)
        )

        val innerHexR = hexR * 0.80f
        val innerHexPath = Path()
        for (i in 0 until 6) {
            val angle = (i * 60.0 - 90.0) * PI / 180.0
            val px = cx + cos(angle).toFloat() * innerHexR
            val py = cy + sin(angle).toFloat() * innerHexR
            if (i == 0) innerHexPath.moveTo(px, py) else innerHexPath.lineTo(px, py)
        }
        innerHexPath.close()
        drawPath(
            path = innerHexPath,
            color = Color.White.copy(alpha = 0.25f),
            style = Stroke(width = 1.dp.toPx())
        )

        val hpR = w * 0.14f
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(cx - hpR, cy - hpR * 0.95f),
            size = Size(hpR * 2f, hpR * 1.9f),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx - hpR - 2.dp.toPx(), cy - hpR * 0.1f),
            size = Size(4.8.dp.toPx(), 9.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(cx + hpR - 2.8.dp.toPx(), cy - hpR * 0.1f),
            size = Size(4.8.dp.toPx(), 9.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        drawStarShape(
            center = Offset(cx, cy + hexR * 0.92f),
            outerRadius = w * 0.085f,
            innerRadius = w * 0.04f,
            color = Color.White
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarShape(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
) {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (i * 36.0 - 90.0) * PI / 180.0
        val px = center.x + cos(angle).toFloat() * r
        val py = center.y + sin(angle).toFloat() * r
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path = path, color = color)
}

@Composable
private fun MiniColorfulFlameIcon(
    flameColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val path = Path().apply {
            moveTo(cx, h * 0.08f)
            cubicTo(w * 0.85f, h * 0.35f, w * 0.88f, h * 0.72f, cx, h * 0.92f)
            cubicTo(w * 0.12f, h * 0.72f, w * 0.15f, h * 0.35f, cx, h * 0.08f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(Color.White, flameColor)
            )
        )
    }
}

@Composable
private fun MonochromeClockIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = w * 0.42f
        val strokeW = (w * 0.09f).coerceAtLeast(1.4.dp.toPx())
        drawCircle(
            color = Color(0xFFE2E8F0),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = strokeW)
        )
        drawLine(
            color = Color.White,
            start = Offset(cx, cy),
            end = Offset(cx, cy - r * 0.55f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(cx, cy),
            end = Offset(cx + r * 0.45f, cy),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MonochromeCalendarIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = (w * 0.085f).coerceAtLeast(1.4.dp.toPx())
        val left = w * 0.14f
        val top = h * 0.20f
        val boxW = w * 0.72f
        val boxH = h * 0.66f

        drawRoundRect(
            color = Color(0xFFE2E8F0),
            topLeft = Offset(left, top),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(width = strokeW)
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.32f, h * 0.11f),
            end = Offset(w * 0.32f, h * 0.26f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.68f, h * 0.11f),
            end = Offset(w * 0.68f, h * 0.26f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(left, h * 0.38f),
            end = Offset(left + boxW, h * 0.38f),
            strokeWidth = strokeW * 0.85f
        )
        val dotR = w * 0.045f
        for (row in 0..1) {
            for (col in 0..2) {
                drawCircle(
                    color = Color.White,
                    radius = dotR,
                    center = Offset(w * (0.31f + col * 0.19f), h * (0.52f + row * 0.17f))
                )
            }
        }
    }
}

@Composable
private fun MonochromeStarIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawStarShape(
            center = Offset(size.width / 2f, size.height / 2f),
            outerRadius = size.width * 0.46f,
            innerRadius = size.width * 0.20f,
            color = Color(0xFFE2E8F0)
        )
    }
}

@Composable
private fun MonochromeCrownIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.78f)
            lineTo(w * 0.18f, h * 0.28f)
            lineTo(w * 0.38f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.20f)
            lineTo(w * 0.62f, h * 0.52f)
            lineTo(w * 0.82f, h * 0.28f)
            lineTo(w * 0.88f, h * 0.78f)
            close()
        }
        drawPath(path = path, color = Color(0xFFE2E8F0))
    }
}

@Composable
private fun MonochromeBarChartIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.20f
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(w * 0.10f, h * 0.42f),
            size = Size(barW, h * 0.48f),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.40f, h * 0.15f),
            size = Size(barW, h * 0.75f),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(w * 0.70f, h * 0.55f),
            size = Size(barW, h * 0.35f),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f)
        )
    }
}

@Composable
private fun MonochromeWaveformGraph(
    periodSeed: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "profileWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barCount = 68
        val step = w / barCount
        val strokeW = (step * 0.46f).coerceAtLeast(1.4.dp.toPx())

        for (i in 0 until barCount) {
            val norm = i.toFloat() / (barCount - 1).toFloat()
            val baseFrac = if (isActive) {
                val env1 = kotlin.math.exp(-((norm - 0.18f) * (norm - 0.18f)) / 0.012f)
                val env2 = kotlin.math.exp(-((norm - 0.48f) * (norm - 0.48f)) / 0.015f)
                val env3 = kotlin.math.exp(-((norm - 0.80f) * (norm - 0.80f)) / 0.014f)
                val ripple = 0.5f + 0.5f * sin(i * 1.35f + periodSeed * 1.9f + wavePhase * 0.35f)
                (0.16f + (env1 * 0.55f + env2 * 0.82f + env3 * 0.65f) * (0.45f + 0.55f * ripple))
                    .coerceIn(0.14f, 0.98f)
            } else {
                // Subtle baseline wave when 0 Jam
                (0.14f + 0.08f * sin(i * 0.45f + wavePhase * 0.25f)).coerceIn(0.10f, 0.26f)
            }

            val barH = h * baseFrac
            val x = i * step + step / 2f
            val topY = h - barH
            val alpha = if (isActive) (0.35f + baseFrac * 0.65f).coerceIn(0.35f, 1f) else 0.24f
            drawLine(
                color = Color.White.copy(alpha = alpha),
                start = Offset(x, topY),
                end = Offset(x, h),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun GeometricLinkinPrismArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.8.dp.toPx()

        val outerTriangle = Path().apply {
            moveTo(w * 0.55f, h * 0.08f)
            lineTo(w * 0.90f, h * 0.72f)
            lineTo(w * 0.12f, h * 0.62f)
            close()
        }
        drawPath(
            path = outerTriangle,
            color = Color.White,
            style = Stroke(width = strokeW, join = StrokeJoin.Round)
        )
        drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = Offset(w * 0.55f, h * 0.08f),
            end = Offset(w * 0.28f, h * 0.92f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.75f),
            start = Offset(w * 0.34f, h * 0.45f),
            end = Offset(w * 0.72f, h * 0.56f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MonochromeHeadphoneMiniIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()
        drawArc(
            color = Color(0xFFB8BECB),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.70f, h * 0.70f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawRoundRect(
            color = Color(0xFFB8BECB),
            topLeft = Offset(w * 0.10f, h * 0.48f),
            size = Size(w * 0.20f, h * 0.36f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFFB8BECB),
            topLeft = Offset(w * 0.70f, h * 0.48f),
            size = Size(w * 0.20f, h * 0.36f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

@Composable
private fun MonochromeTrophyIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        drawArc(
            color = Color(0xFFCBD5E1),
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.10f, h * 0.18f),
            size = Size(w * 0.26f, h * 0.30f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFFCBD5E1),
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.64f, h * 0.18f),
            size = Size(w * 0.26f, h * 0.30f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )

        val cupPath = Path().apply {
            moveTo(w * 0.24f, h * 0.14f)
            lineTo(w * 0.76f, h * 0.14f)
            cubicTo(w * 0.74f, h * 0.48f, w * 0.62f, h * 0.58f, cx, h * 0.60f)
            cubicTo(w * 0.38f, h * 0.58f, w * 0.26f, h * 0.48f, w * 0.24f, h * 0.14f)
            close()
        }
        drawPath(
            path = cupPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF94A3B8), Color.White, Color(0xFFCBD5E1))
            )
        )

        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(cx - w * 0.06f, h * 0.58f),
            size = Size(w * 0.12f, h * 0.18f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF94A3B8), Color.White, Color(0xFF94A3B8))
            ),
            topLeft = Offset(w * 0.28f, h * 0.75f),
            size = Size(w * 0.44f, h * 0.11f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        drawStarShape(
            center = Offset(cx, h * 0.34f),
            outerRadius = w * 0.10f,
            innerRadius = w * 0.045f,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun RisingBarsArrowIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.17f

        drawRoundRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(w * 0.14f, h * 0.62f),
            size = Size(barW, h * 0.28f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFFCBD5E1),
            topLeft = Offset(w * 0.42f, h * 0.44f),
            size = Size(barW, h * 0.46f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.70f, h * 0.26f),
            size = Size(barW, h * 0.64f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
        )

        val strokeW = 2.2.dp.toPx()
        drawLine(
            color = Color.White,
            start = Offset(w * 0.16f, h * 0.48f),
            end = Offset(w * 0.74f, h * 0.10f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.54f, h * 0.10f),
            end = Offset(w * 0.74f, h * 0.10f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.74f, h * 0.10f),
            end = Offset(w * 0.74f, h * 0.28f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

// =============================================================================
// DIALOGS FOR EDIT PROFILE (+ BANNER PHOTO/VIDEO), 10 LEVELS, RECORDS & TOP 10
// =============================================================================

@Composable
private fun EditLocalProfileDialog(
    initialName: String,
    initialHandle: String,
    initialBio: String,
    currentAvatarUri: String,
    currentBannerUri: String,
    currentBannerIsVideo: Boolean,
    isLoggedIn: Boolean,
    onSave: (String, String, String, String, String, Boolean) -> Unit,
    onOpenYouTubeLogin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(initialName) }
    var handle by rememberSaveable { mutableStateOf(initialHandle) }
    var bio by rememberSaveable { mutableStateOf(initialBio) }
    var avatarUri by rememberSaveable { mutableStateOf(currentAvatarUri) }
    var bannerUri by rememberSaveable { mutableStateOf(currentBannerUri) }
    var bannerIsVideo by rememberSaveable { mutableStateOf(currentBannerIsVideo) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val saved = copyPickedMediaToInternalFile(context, uri, "profile_avatar_media")
                avatarUri = saved?.first ?: uri.toString()
            }
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val saved = copyPickedMediaToInternalFile(context, uri, "profile_banner_media")
                if (saved != null) {
                    bannerUri = saved.first
                    bannerIsVideo = saved.second
                } else {
                    bannerUri = uri.toString()
                    bannerIsVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
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
                .padding(horizontal = 18.dp, vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101217),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Edit Profil & Banner",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Atur foto profil, banner foto/video, nama, dan @username",
                    fontSize = 12.sp,
                    color = Color(0xFF9EA4B0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Banner + Avatar Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF181B24))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (bannerUri.isNotBlank()) {
                        if (bannerIsVideo) {
                            LoopingVideoBanner(
                                videoUriString = bannerUri,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = bannerUri,
                                contentDescription = "Preview Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                    }

                    // Avatar inside Banner Preview
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14161C))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable {
                                avatarPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            EmptyAvatarSilhouetteIcon(modifier = Modifier.size(38.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Avatar & Banner Picker Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            avatarPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Text("Upload Foto Profil", color = Color.White, fontSize = 11.5.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            bannerPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Text("Banner Foto/Video", color = Color.White, fontSize = 11.5.sp)
                    }
                }

                if (avatarUri.isNotBlank() || bannerUri.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (avatarUri.isNotBlank()) {
                            TextButton(onClick = { avatarUri = "" }) {
                                Text("Kosongkan Foto Profil", color = Color(0xFFB0B6C2), fontSize = 11.5.sp)
                            }
                        }
                        if (bannerUri.isNotBlank()) {
                            TextButton(
                                onClick = {
                                    bannerUri = ""
                                    bannerIsVideo = false
                                }
                            ) {
                                Text("Hapus Banner", color = Color(0xFFB0B6C2), fontSize = 11.5.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Profil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color(0xFF9EA4B0)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Username (@user)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color(0xFF9EA4B0)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio Musik") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color(0xFF9EA4B0)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onOpenYouTubeLogin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.account),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLoggedIn) "Akun YouTube Music Terhubung (Kelola)" else "Login YouTube Music (Opsional)",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Batal", color = Color.White)
                    }
                    Button(
                        onClick = { onSave(name, handle, bio, avatarUri, bannerUri, bannerIsVideo) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Simpan", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListenerLevelsModalDialog(
    journeyState: MusicJourneyState,
    effectiveStreakDays: Int,
    onExportStatsFile: () -> Unit,
    onImportStatsFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showStatEditor by rememberSaveable { mutableStateOf(false) }
    var customXpText by rememberSaveable(journeyState.xp) { mutableStateOf(journeyState.xp.toString()) }
    var customHoursText by rememberSaveable(journeyState.totalHours) { mutableStateOf(journeyState.totalHours.toString()) }
    var customSongsText by rememberSaveable(journeyState.totalSongsPlayed) { mutableStateOf(journeyState.totalSongsPlayed.toString()) }
    var customStreakText by rememberSaveable(effectiveStreakDays) { mutableStateOf(effectiveStreakDays.toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0E1015),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(journeyState.levelSpec.drawableResId),
                        contentDescription = "Level ${journeyState.levelSpec.level}",
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Fit
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Level ${journeyState.levelSpec.level} – ${journeyState.levelSpec.title}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${formatDotNumber(journeyState.xp)} / ${formatDotNumber(journeyState.xpTargetForCurrentBanner)} XP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Daftar 10 Level & Gelar Pendengar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                MusicJourneyManager.allLevels.forEach { spec ->
                    val isCurrent = spec.level == journeyState.levelSpec.level
                    val isUnlocked = journeyState.xp >= spec.xpThreshold && spec.level <= journeyState.levelSpec.level
                    val xpLabel = if (spec.level == 10) "15.000+ XP" else "${formatDotNumber(spec.xpThreshold)} XP"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCurrent) Color.White.copy(alpha = 0.14f)
                                else Color(0xFF161820)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isCurrent) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(spec.drawableResId),
                            contentDescription = "Level ${spec.level}",
                            modifier = Modifier.size(38.dp),
                            contentScale = ContentScale.Fit
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lv.${spec.level} ${spec.title}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = spec.subtitle,
                                fontSize = 11.sp,
                                color = Color(0xFF989EA9),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = if (isCurrent) "Aktif • $xpLabel" else if (isUnlocked) "✓ $xpLabel" else xpLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) Color.White else Color(0xFFB0B6C2),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Backup & Restore buttons inside Level Modal as well
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onExportStatsFile,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Cadangkan Statistik",
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    OutlinedButton(
                        onClick = onImportStatsFile,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Pulihkan Statistik",
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TextButton(
                    onClick = { showStatEditor = !showStatEditor },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (showStatEditor) "Sembunyikan Kustomisasi Statistik" else "Kustomisasi / Reset Statistik (Mulai dari 0)",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                }

                AnimatedVisibility(visible = showStatEditor) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF161820))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Uji Coba Kondisi Api Streak:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        val presets = listOf(
                            0 to "0 Hari (Mati)",
                            5 to "5 Hari (Kuning)",
                            15 to "15 Hari (Oranye)",
                            40 to "40 Hari (Tebal)",
                            80 to "80 Hari (Ungu)",
                            150 to "150 Hari (Biru)",
                            250 to "250+ (Kosmik)"
                        )
                        presets.chunked(3).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { (days, label) ->
                                    OutlinedButton(
                                        onClick = {
                                            customStreakText = days.toString()
                                            StreakManager.setCustomStreakCount(context, days = days)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                                    ) {
                                        Text(label, fontSize = 10.sp, color = Color.White, maxLines = 1)
                                    }
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customXpText,
                                onValueChange = { customXpText = it },
                                label = { Text("XP") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customHoursText,
                                onValueChange = { customHoursText = it },
                                label = { Text("Total Jam") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customSongsText,
                                onValueChange = { customSongsText = it },
                                label = { Text("Total Lagu") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    MusicJourneyManager.updateCustomStats(
                                        context = context,
                                        totalHours = customHoursText.toIntOrNull() ?: journeyState.totalHours,
                                        totalSongs = customSongsText.toIntOrNull() ?: journeyState.totalSongsPlayed,
                                        dailyRecordMinutes = (journeyState.dailyRecordSeconds / 60L).toInt(),
                                        monthlyRecordHours = (journeyState.monthlyRecordSeconds / 3600L).toInt(),
                                        xp = customXpText.toIntOrNull() ?: journeyState.xp,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                            ) {
                                Text("Terapkan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    MusicJourneyManager.resetAllToZero(context)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reset ke 0", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Tutup", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun DailyRecordDetailDialog(
    journeyState: MusicJourneyState,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101217),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonochromeStarIcon(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Rekor Harian",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF171A22))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "⭐ Rekor Harian Terlama",
                            fontSize = 12.sp,
                            color = Color(0xFFA0A6B4)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = journeyState.dailyRecordFullText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Mencatat durasi mendengarkan terlama dalam satu hari. Rekor otomatis diperbarui saat kamu mendengarkan lebih lama.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF989EA9)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Riwayat Durasi Harian",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (journeyState.dailyHistory.isEmpty()) {
                    Text(
                        text = "Belum ada riwayat harian — putar musik untuk mulai mencatat!",
                        color = Color(0xFF989EA9),
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    journeyState.dailyHistory.forEach { item ->
                        val hrs = item.listeningSeconds / 3600L
                        val mins = ((item.listeningSeconds % 3600L) / 60L).let {
                            if (hrs == 0L && it == 0L && item.listeningSeconds > 0L) 1L else it
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF161820))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.displayDate, color = Color(0xFFD0D5E0), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(
                                text = when {
                                    hrs > 0L && mins > 0L -> "$hrs Jam $mins Menit"
                                    hrs > 0L -> "$hrs Jam"
                                    else -> "$mins Menit"
                                },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Tutup", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun MonthlyRecordDetailDialog(
    journeyState: MusicJourneyState,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101217),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonochromeCrownIcon(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Rekor Bulanan",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF171A22))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "👑 Rekor Bulanan Tertinggi",
                            fontSize = 12.sp,
                            color = Color(0xFFA0A6B4)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = journeyState.monthlyRecordHoursText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Statistik Berdasarkan Bulan",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (journeyState.monthlyHistory.isEmpty()) {
                    Text(
                        text = "Belum ada riwayat bulanan — putar musik untuk mulai mencatat!",
                        color = Color(0xFF989EA9),
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    journeyState.monthlyHistory.forEachIndexed { idx, entry ->
                        val mins = (entry.listeningSeconds / 60L).coerceAtLeast(1L)
                        val label = if (entry.listeningSeconds < 3600L) "$mins Menit" else "${entry.hoursInt} Jam"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (idx == 0) Color.White.copy(alpha = 0.12f) else Color(0xFF161820))
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.displayTitle,
                                color = Color.White,
                                fontSize = 13.5.sp,
                                fontWeight = if (idx == 0) FontWeight.ExtraBold else FontWeight.Medium
                            )
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Tutup", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun ListeningSummaryDetailDialog(
    journeyState: MusicJourneyState,
    onOpenFullStats: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dayNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val maxDaySec = (journeyState.weekdaySeconds.maxOrNull() ?: 0L).coerceAtLeast(1L)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101217),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonochromeBarChartIcon(modifier = Modifier.size(22.dp))
                    Text(
                        text = "Pola & Ringkasan Mendengarkan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161820))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pola Mendengarkan Mingguan (Sen – Min)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    dayNames.forEachIndexed { idx, dayLabel ->
                        val sec = journeyState.weekdaySeconds.getOrElse(idx) { 0L }
                        val fraction = if (sec <= 0L) 0f else (sec.toFloat() / maxDaySec.toFloat()).coerceIn(0.06f, 1f)
                        val hrs = sec / 3600.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = dayLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD0D5E0),
                                modifier = Modifier.width(30.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                            ) {
                                if (fraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(Color(0xFFCBD5E1), Color.White)
                                                )
                                            )
                                    )
                                }
                            }
                            Text(
                                text = String.format(Locale.US, "%.1fj", hrs),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.width(38.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenFullStats,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Text("Statistik Lengkap", color = Color.White, fontSize = 12.5.sp)
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                    ) {
                        Text("Tutup", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Top10SongsModalDialog(
    songs: List<TopSongStatItem>,
    onPlaySong: (TopSongStatItem) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0E1015),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MonochromeStarIcon(modifier = Modifier.size(22.dp))
                    Column {
                        Text(
                            text = "Top 10 Lagu Paling Sering Didengar",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Berdasarkan jumlah pemutaran (play count)",
                            fontSize = 12.sp,
                            color = Color(0xFF9EA4B0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (songs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada lagu yang diputar.\nMulai dengarkan musik untuk melihat Top 10 lagumu!",
                            color = Color(0xFF9EA4B0),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(songs.take(10)) { index, item ->
                            val rankBadge = when (index) {
                                0 -> "🥇"
                                1 -> "🥈"
                                2 -> "🥉"
                                else -> "#${index + 1}"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (index < 3) Color.White.copy(alpha = 0.10f) else Color(0xFF15171F)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (index == 0) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onPlaySong(item) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rankBadge,
                                    fontSize = if (index < 3) 18.sp else 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.width(32.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.artist,
                                        fontSize = 12.sp,
                                        color = Color(0xFFA0A6B4),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = "${item.playCount}x diputar",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(end = 10.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                        .clickable { onPlaySong(item) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = "Putar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Tutup", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun QuickProfileToolsDialog(
    onNavigateHistory: () -> Unit,
    onNavigateStats: () -> Unit,
    onOpenDynamicIsland: () -> Unit,
    onNavigateTheme: () -> Unit,
    onNavigateBackupRestore: () -> Unit,
    onNavigateSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF101217),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pintasan & Fitur Musik",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))

                QuickToolItemRow(
                    iconRes = R.drawable.history,
                    title = "Riwayat Mendengarkan",
                    subtitle = "Lihat daftar lagu yang baru saja diputar",
                    onClick = onNavigateHistory
                )
                QuickToolItemRow(
                    iconRes = R.drawable.stats,
                    title = "Statistik Detail",
                    subtitle = "Grafik artis, album, dan lagu teratas",
                    onClick = onNavigateStats
                )
                QuickToolItemRow(
                    iconRes = R.drawable.backup,
                    title = "Cadangkan & Pulihkan (.backup / .json)",
                    subtitle = "Simpan atau muat ulang database & seluruh statistik",
                    onClick = onNavigateBackupRestore
                )
                QuickToolItemRow(
                    iconRes = R.drawable.equalizer,
                    title = "Dynamic Island",
                    subtitle = "Atur kapsul visualisasi musik di atas layar",
                    onClick = onOpenDynamicIsland
                )
                QuickToolItemRow(
                    iconRes = R.drawable.contrast,
                    title = "Tema Monokrom & Tampilan",
                    subtitle = "Kustomisasi warna hitam/putih dan gaya visual",
                    onClick = onNavigateTheme
                )
                QuickToolItemRow(
                    iconRes = R.drawable.tune,
                    title = "Pengaturan Aplikasi",
                    subtitle = "Pengaturan audio, lirik, akun, dan cadangan",
                    onClick = onNavigateSettings
                )

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Text("Tutup", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun QuickToolItemRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF161820))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, fontSize = 11.5.sp, color = Color(0xFF989EA9))
        }
        Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = null,
            tint = Color(0xFF989EA9),
            modifier = Modifier.size(18.dp)
        )
    }
}
