/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.library

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.LocalPlayerAwareWindowInsets
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.HideExplicitKey
import com.nanzbeatles.nanas.constants.LiquidGlassEffectKey
import com.nanzbeatles.nanas.constants.SongSortType
import com.nanzbeatles.nanas.constants.YtmSyncKey
import com.nanzbeatles.nanas.extensions.toMediaItem
import com.nanzbeatles.nanas.playback.queues.ListQueue
import com.nanzbeatles.nanas.ui.component.CreatePlaylistDialog
import com.nanzbeatles.nanas.ui.component.LibraryArtistGridItem
import com.nanzbeatles.nanas.ui.component.LibraryArtistListItem
import com.nanzbeatles.nanas.ui.component.LibraryPlaylistGridItem
import com.nanzbeatles.nanas.ui.component.LibraryPlaylistListItem
import com.nanzbeatles.nanas.ui.component.LocalMenuState
import com.nanzbeatles.nanas.ui.component.SongListItem
import com.nanzbeatles.nanas.ui.component.SpotifyImportDialog
import com.nanzbeatles.nanas.ui.component.SpotifyLogoIcon
import com.nanzbeatles.nanas.ui.menu.SongMenu
import com.nanzbeatles.nanas.utils.rememberPreference
import com.nanzbeatles.nanas.viewmodels.LibraryArtistsViewModel
import com.nanzbeatles.nanas.viewmodels.LibraryPlaylistsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class PustakaTab(
    val label: String,
    val iconRes: Int,
) {
    PLAYLISTS("Playlists", R.drawable.queue_music),
    LIKED("Disukai", R.drawable.favorite_border),
    OFFLINE("Offline", R.drawable.offline),
    ARTISTS("Artis", R.drawable.artist),
}

/**
 * Custom vector illustration matching the playlist-with-music-note icon in the user's screenshot.
 */
@Composable
private fun PlaylistNoteEmptyIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(64.dp),
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barHeight = h * 0.095f
        val corner = CornerRadius(barHeight / 2f, barHeight / 2f)

        // Top horizontal bar
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.18f),
            size = Size(w * 0.56f, barHeight),
            cornerRadius = corner
        )
        // Middle horizontal bar
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.44f),
            size = Size(w * 0.44f, barHeight),
            cornerRadius = corner
        )
        // Bottom horizontal bar
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.70f),
            size = Size(w * 0.44f, barHeight),
            cornerRadius = corner
        )

        // Musical note stem & flag on the right
        val stemX = w * 0.82f
        val stemTopY = h * 0.18f
        val noteCenterY = h * 0.70f
        val noteRadius = w * 0.13f

        drawLine(
            color = tint,
            start = Offset(stemX, stemTopY),
            end = Offset(stemX, noteCenterY),
            strokeWidth = barHeight,
            cap = StrokeCap.Round
        )

        // Note head (hollow circle with thick stroke matching screenshot)
        drawCircle(
            color = tint,
            radius = noteRadius,
            center = Offset(stemX - noteRadius * 0.65f, noteCenterY),
            style = Stroke(width = barHeight * 0.95f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    playlistsViewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    artistsViewModel: LibraryArtistsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by (playerConnection?.isEffectivelyPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
    val mediaMetadata by (playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) })

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val liquidGlassEnabled by rememberPreference(LiquidGlassEffectKey, defaultValue = true)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    var selectedTab by rememberSaveable { mutableStateOf(PustakaTab.PLAYLISTS) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showSpotifyImportDialog by rememberSaveable { mutableStateOf(false) }

    val playlists by playlistsViewModel.allPlaylists.collectAsState()
    val artists by artistsViewModel.allArtists.collectAsState()

    val likedSongsFlow = remember(database) {
        database.likedSongs(SongSortType.CREATE_DATE, true)
    }
    val downloadedSongsFlow = remember(database) {
        database.downloadedSongs(SongSortType.CREATE_DATE, true)
    }
    val likedSongsRaw by likedSongsFlow.collectAsState(initial = emptyList())
    val downloadedSongsRaw by downloadedSongsFlow.collectAsState(initial = emptyList())

    val likedSongs = remember(likedSongsRaw, hideExplicit) {
        if (hideExplicit) likedSongsRaw.filter { !it.song.explicit } else likedSongsRaw
    }
    val downloadedSongs = remember(downloadedSongsRaw, hideExplicit) {
        if (hideExplicit) downloadedSongsRaw.filter { !it.song.explicit } else downloadedSongsRaw
    }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                playlistsViewModel.sync()
                artistsViewModel.sync()
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            allowSyncing = true,
            onOpenSpotifyImport = {
                showCreatePlaylistDialog = false
                showSpotifyImportDialog = true
            }
        )
    }

    if (showSpotifyImportDialog) {
        SpotifyImportDialog(
            onDismiss = { showSpotifyImportDialog = false },
            onPlaylistImported = { importedPlaylistId ->
                navController.navigate("local_playlist/$importedPlaylistId")
            }
        )
    }

    val windowInsetsPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
    ) {
        val isExpandedScreen = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = windowInsetsPadding.calculateTopPadding())
        ) {
            // Segmented Frosted Glass Pill Tab Bar (Playlists | Disukai | Offline | Artis)
            FrostedSegmentedTabBar(
                selectedTab = selectedTab,
                onTabSelect = { selectedTab = it },
                isDark = isDark,
                isGlass = liquidGlassEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.07f),
                modifier = Modifier.padding(top = 6.dp)
            )

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                label = "pustakaTabContent",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    PustakaTab.PLAYLISTS -> {
                        if (isExpandedScreen && playlists.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 20.dp,
                                    bottom = windowInsetsPadding.calculateBottomPadding() + 24.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CreateNewPlaylistGlassButton(
                                            onClick = { showCreatePlaylistDialog = true },
                                            isDark = isDark,
                                            isGlass = liquidGlassEnabled,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ImportSpotifyPlaylistGlassButton(
                                            onClick = { showSpotifyImportDialog = true },
                                            isDark = isDark,
                                            isGlass = liquidGlassEnabled,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                items(
                                    items = playlists.distinctBy { it.id },
                                    key = { it.id }
                                ) { playlist ->
                                    FrostedItemSurface(isDark = isDark, isGlass = liquidGlassEnabled) {
                                        LibraryPlaylistGridItem(
                                            navController = navController,
                                            menuState = menuState,
                                            coroutineScope = coroutineScope,
                                            playlist = playlist
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 20.dp,
                                    bottom = windowInsetsPadding.calculateBottomPadding() + 24.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(key = "create_playlist_btn") {
                                    CreateNewPlaylistGlassButton(
                                        onClick = { showCreatePlaylistDialog = true },
                                        isDark = isDark,
                                        isGlass = liquidGlassEnabled
                                    )
                                }

                                item(key = "import_spotify_playlist_btn") {
                                    ImportSpotifyPlaylistGlassButton(
                                        onClick = { showSpotifyImportDialog = true },
                                        isDark = isDark,
                                        isGlass = liquidGlassEnabled
                                    )
                                }

                                if (playlists.isEmpty()) {
                                    item(key = "empty_playlists_card") {
                                        FrostedEmptyStateCard(
                                            title = "Belum Ada Playlist",
                                            subtitle = "Buat playlist pertamamu dan kumpulkan lagu-lagu favoritmu di satu tempat.",
                                            isDark = isDark,
                                            isGlass = liquidGlassEnabled,
                                            iconContent = {
                                                PlaylistNoteEmptyIcon(
                                                    tint = if (isDark) Color(0xFF6E6E73) else Color(0xFF8E8E93),
                                                    modifier = Modifier.size(64.dp)
                                                )
                                            }
                                        )
                                    }
                                } else {
                                    items(
                                        items = playlists.distinctBy { it.id },
                                        key = { it.id }
                                    ) { playlist ->
                                        FrostedItemSurface(isDark = isDark, isGlass = liquidGlassEnabled) {
                                            LibraryPlaylistListItem(
                                                navController = navController,
                                                menuState = menuState,
                                                coroutineScope = coroutineScope,
                                                playlist = playlist
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    PustakaTab.LIKED -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 20.dp,
                                bottom = windowInsetsPadding.calculateBottomPadding() + 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (likedSongs.isEmpty()) {
                                item(key = "empty_liked_card") {
                                    FrostedEmptyStateCard(
                                        title = "Belum Ada Lagu Disukai",
                                        subtitle = "Ketuk ikon hati pada lagu favoritmu untuk menyimpannya di dalam pustaka ini.",
                                        isDark = isDark,
                                        isGlass = liquidGlassEnabled,
                                        iconContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.favorite_border),
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFF6E6E73) else Color(0xFF8E8E93),
                                                modifier = Modifier.size(64.dp)
                                            )
                                        }
                                    )
                                }
                            } else {
                                item(key = "liked_actions") {
                                    FrostedQueueActionBar(
                                        countText = "${likedSongs.size} lagu disukai",
                                        onPlayAll = {
                                            playerConnection?.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.liked),
                                                    items = likedSongs.map { it.toMediaItem() },
                                                    startIndex = 0
                                                )
                                            )
                                        },
                                        onShuffle = {
                                            playerConnection?.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.liked),
                                                    items = likedSongs.shuffled().map { it.toMediaItem() },
                                                    startIndex = 0
                                                )
                                            )
                                        },
                                        isDark = isDark,
                                        isGlass = liquidGlassEnabled
                                    )
                                }

                                itemsIndexed(
                                    items = likedSongs,
                                    key = { _, song -> song.song.id }
                                ) { index, song ->
                                    FrostedItemSurface(isDark = isDark, isGlass = liquidGlassEnabled) {
                                        SongListItem(
                                            song = song,
                                            showInLibraryIcon = false,
                                            isActive = song.id == mediaMetadata?.id,
                                            isPlaying = isPlaying,
                                            showLikedIcon = true,
                                            showDownloadIcon = true,
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            SongMenu(
                                                                originalSong = song,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = null
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (song.id == mediaMetadata?.id) {
                                                        playerConnection?.togglePlayPause()
                                                    } else {
                                                        playerConnection?.playQueue(
                                                            ListQueue(
                                                                title = context.getString(R.string.liked),
                                                                items = likedSongs.map { it.toMediaItem() },
                                                                startIndex = index
                                                            )
                                                        )
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PustakaTab.OFFLINE -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 20.dp,
                                bottom = windowInsetsPadding.calculateBottomPadding() + 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(key = "cached_songs_shortcut") {
                                CreateNewPlaylistGlassButton(
                                    text = "Buka Lagu Tersimpan di Cache",
                                    onClick = { navController.navigate("cache_playlist/cached") },
                                    isDark = isDark,
                                    isGlass = liquidGlassEnabled
                                )
                            }

                            if (downloadedSongs.isEmpty()) {
                                item(key = "empty_offline_card") {
                                    FrostedEmptyStateCard(
                                        title = "Belum Ada Lagu Offline",
                                        subtitle = "Unduh lagu favoritmu agar tetap bisa didengarkan kapan saja tanpa koneksi internet.",
                                        isDark = isDark,
                                        isGlass = liquidGlassEnabled,
                                        iconContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFF6E6E73) else Color(0xFF8E8E93),
                                                modifier = Modifier.size(64.dp)
                                            )
                                        }
                                    )
                                }
                            } else {
                                item(key = "offline_actions") {
                                    FrostedQueueActionBar(
                                        countText = "${downloadedSongs.size} lagu offline",
                                        onPlayAll = {
                                            playerConnection?.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.offline),
                                                    items = downloadedSongs.map { it.toMediaItem() },
                                                    startIndex = 0
                                                )
                                            )
                                        },
                                        onShuffle = {
                                            playerConnection?.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.offline),
                                                    items = downloadedSongs.shuffled().map { it.toMediaItem() },
                                                    startIndex = 0
                                                )
                                            )
                                        },
                                        isDark = isDark,
                                        isGlass = liquidGlassEnabled
                                    )
                                }

                                itemsIndexed(
                                    items = downloadedSongs,
                                    key = { _, song -> song.song.id }
                                ) { index, song ->
                                    FrostedItemSurface(isDark = isDark, isGlass = liquidGlassEnabled) {
                                        SongListItem(
                                            song = song,
                                            showInLibraryIcon = false,
                                            isActive = song.id == mediaMetadata?.id,
                                            isPlaying = isPlaying,
                                            showLikedIcon = true,
                                            showDownloadIcon = false,
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            SongMenu(
                                                                originalSong = song,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = null
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (song.id == mediaMetadata?.id) {
                                                        playerConnection?.togglePlayPause()
                                                    } else {
                                                        playerConnection?.playQueue(
                                                            ListQueue(
                                                                title = context.getString(R.string.offline),
                                                                items = downloadedSongs.map { it.toMediaItem() },
                                                                startIndex = index
                                                            )
                                                        )
                                                    }
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    PustakaTab.ARTISTS -> {
                        if (artists.isEmpty()) {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 20.dp,
                                    bottom = windowInsetsPadding.calculateBottomPadding() + 24.dp
                                ),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(key = "empty_artists_card") {
                                    FrostedEmptyStateCard(
                                        title = "Belum Ada Artis",
                                        subtitle = "Ikuti artis favoritmu untuk melihat koleksi lagu dan album mereka di satu tempat.",
                                        isDark = isDark,
                                        isGlass = liquidGlassEnabled,
                                        iconContent = {
                                            Icon(
                                                painter = painterResource(R.drawable.artist),
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFF6E6E73) else Color(0xFF8E8E93),
                                                modifier = Modifier.size(64.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        } else if (isExpandedScreen) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 20.dp,
                                    bottom = windowInsetsPadding.calculateBottomPadding() + 24.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = artists.distinctBy { it.id },
                                    key = { it.id }
                                ) { artist ->
                                    FrostedItemSurface(isDark = isDark, isGlass = liquidGlassEnabled) {
                                        LibraryArtistGridItem(
                                            navController = navController,
                                            menuState = menuState,
                                            coroutineScope = coroutineScope,
                                            artist = artist
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 20.dp,
                                    bottom = windowInsetsPadding.calculateBottomPadding() + 24.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = artists.distinctBy { it.id },
                                    key = { it.id }
                                ) { artist ->
                                    FrostedItemSurface(isDark = isDark, isGlass = liquidGlassEnabled) {
                                        LibraryArtistListItem(
                                            navController = navController,
                                            menuState = menuState,
                                            coroutineScope = coroutineScope,
                                            artist = artist
                                        )
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

@Composable
private fun FrostedSegmentedTabBar(
    selectedTab: PustakaTab,
    onTabSelect: (PustakaTab) -> Unit,
    isDark: Boolean,
    isGlass: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerShape = CircleShape
    val containerBg = if (isDark) Color(0xFF141518) else Color(0xFFF0F1F5)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .clip(containerShape)
            .background(containerBg, containerShape)
            .border(1.dp, borderColor, containerShape)
            .padding(5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val activePillColor = if (isDark) Color.White else Color(0xFF0A0C10)
            val activeTextColor = if (isDark) Color.Black else Color.White
            PustakaTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) {
                        activePillColor
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(200),
                    label = "tabPillBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        activeTextColor
                    } else {
                        if (isDark) Color(0xFF9E9E9E) else Color(0xFF5A5A5E)
                    },
                    animationSpec = tween(200),
                    label = "tabContentColor"
                )

                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pillBg, CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = contentColor),
                            onClick = { onTabSelect(tab) }
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("pustaka_tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = tab.label,
                            color = contentColor,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateNewPlaylistGlassButton(
    onClick: () -> Unit,
    isDark: Boolean,
    isGlass: Boolean,
    text: String = "+ Buat Playlist Baru",
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val bgColor = if (isDark) Color(0xFF15161A) else Color(0xFFF2F3F7)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .testTag("create_playlist_glass_button"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isDark) Color.White else Color.Black,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ImportSpotifyPlaylistGlassButton(
    onClick: () -> Unit,
    isDark: Boolean,
    isGlass: Boolean,
    modifier: Modifier = Modifier,
) {
    val spotifyGreen = Color(0xFF1DB954)
    val shape = RoundedCornerShape(16.dp)
    val bgColor = if (isDark) Color(0xFF15161A) else Color(0xFFF2F3F7)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(bgColor, shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .testTag("import_spotify_playlist_glass_button"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            SpotifyLogoIcon(
                tint = spotifyGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Import Playlist dari Spotify",
                color = if (isDark) Color.White else Color(0xFF0A0C10),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FrostedEmptyStateCard(
    title: String,
    subtitle: String,
    isDark: Boolean,
    isGlass: Boolean,
    iconContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    val bgColor = if (isDark) Color(0xFF141518) else Color(0xFFF2F3F7)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor, shape)
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(horizontal = 28.dp, vertical = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            iconContent()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                color = if (isDark) Color.White else Color.Black,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                color = if (isDark) Color(0xFF8E8E93) else Color(0xFF5A5A60),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FrostedQueueActionBar(
    countText: String,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    isDark: Boolean,
    isGlass: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = countText,
            color = if (isDark) Color(0xFF9E9E9E) else Color(0xFF5A5A60),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color(0xFF1A1B1F) else Color(0xFFEAEAEF),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f),
                        CircleShape
                    )
                    .clickable(onClick = onShuffle)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = null,
                        tint = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Acak",
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isDark) Color.White else Color.Black,
                        CircleShape
                    )
                    .clickable(onClick = onPlayAll)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = if (isDark) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Putar",
                        color = if (isDark) Color.Black else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FrostedItemSurface(
    isDark: Boolean,
    isGlass: Boolean,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val bgColor = if (isDark) Color(0xFF141518) else Color(0xFFF2F3F7)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor, shape)
            .border(0.8.dp, borderColor, shape)
    ) {
        content()
    }
}
