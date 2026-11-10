/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens
import com.nanzbeatles.nanas.ui.screens.wrapped.WrappedConstants

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import com.nanzbeatles.nanas.ui.theme.AuraAnimatedVisibility
import com.nanzbeatles.nanas.ui.theme.AuraTransitions
import com.nanzbeatles.nanas.ui.theme.StaggeredAnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation

import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nanzbeatles.innertube.models.AlbumItem
import com.nanzbeatles.innertube.models.ArtistItem
import com.nanzbeatles.innertube.models.EpisodeItem
import com.nanzbeatles.innertube.models.PlaylistItem
import com.nanzbeatles.innertube.models.PodcastItem
import com.nanzbeatles.innertube.models.SongItem
import com.nanzbeatles.innertube.models.YTItem
import com.nanzbeatles.innertube.utils.parseCookieString
import com.nanzbeatles.nanas.LocalDatabase
import com.nanzbeatles.nanas.LocalListenTogetherManager
import com.nanzbeatles.nanas.LocalPlayerAwareWindowInsets
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.constants.GridItemSize
import com.nanzbeatles.nanas.constants.GridItemsSizeKey
import com.nanzbeatles.nanas.constants.GridThumbnailHeight
import com.nanzbeatles.nanas.constants.HomeLayoutMode
import com.nanzbeatles.nanas.constants.HomeLayoutModeKey
import com.nanzbeatles.nanas.constants.InnerTubeCookieKey
import com.nanzbeatles.nanas.constants.ListItemHeight
import com.nanzbeatles.nanas.constants.ListThumbnailSize
import com.nanzbeatles.nanas.constants.SmallGridThumbnailHeight
import com.nanzbeatles.nanas.constants.ThumbnailCornerRadius
import com.nanzbeatles.nanas.ui.component.PlayingIndicatorBox
import com.nanzbeatles.nanas.db.entities.Album
import com.nanzbeatles.nanas.db.entities.Artist
import com.nanzbeatles.nanas.db.entities.LocalItem
import com.nanzbeatles.nanas.db.entities.Playlist
import com.nanzbeatles.nanas.db.entities.Song
import com.nanzbeatles.nanas.extensions.toMediaItem
import com.nanzbeatles.nanas.models.toMediaMetadata
import com.nanzbeatles.nanas.playback.queues.LocalAlbumRadio
import com.nanzbeatles.nanas.playback.queues.YouTubeAlbumRadio
import com.nanzbeatles.nanas.playback.queues.YouTubeQueue
import com.nanzbeatles.nanas.playback.queues.ListQueue
import com.nanzbeatles.nanas.ui.component.YouTubeListItem
import com.nanzbeatles.nanas.ui.component.AlbumGridItem
import com.nanzbeatles.nanas.ui.component.ArtistGridItem
import com.nanzbeatles.nanas.ui.component.ChipsRow
import com.nanzbeatles.nanas.ui.component.HeroCarousel
import com.nanzbeatles.nanas.ui.component.HideOnScrollFAB
import com.nanzbeatles.nanas.ui.component.HomeStreakCard
import com.nanzbeatles.nanas.ui.component.LocalBottomSheetPageState
import com.nanzbeatles.nanas.ui.component.LocalMenuState
import com.nanzbeatles.nanas.ui.component.NavigationTitle
import com.nanzbeatles.nanas.ui.component.PlaylistGridItem
import com.nanzbeatles.nanas.ui.component.SongGridItem
import com.nanzbeatles.nanas.ui.component.SongListItem
import com.nanzbeatles.nanas.ui.component.SpeedDialGridItem
import com.nanzbeatles.nanas.ui.component.YouTubeGridItem
import com.nanzbeatles.nanas.ui.component.shimmer.GridItemPlaceHolder
import com.nanzbeatles.nanas.ui.component.shimmer.ShimmerHost
import com.nanzbeatles.nanas.ui.component.shimmer.TextPlaceholder
import com.nanzbeatles.nanas.ui.menu.AlbumMenu
import com.nanzbeatles.nanas.ui.menu.ArtistMenu
import com.nanzbeatles.nanas.ui.menu.PlaylistMenu
import com.nanzbeatles.nanas.ui.menu.SongMenu
import com.nanzbeatles.nanas.ui.menu.YouTubeAlbumMenu
import com.nanzbeatles.nanas.ui.menu.YouTubeArtistMenu
import com.nanzbeatles.nanas.ui.menu.YouTubePlaylistMenu
import com.nanzbeatles.nanas.ui.menu.YouTubeSongMenu
import com.nanzbeatles.nanas.ui.utils.SnapLayoutInfoProvider
import com.nanzbeatles.nanas.ui.utils.liquidGlassFromPrefs
import com.nanzbeatles.nanas.constants.LiquidGlassApplyHomeCardsKey
import com.nanzbeatles.nanas.utils.makeTimeString
import com.nanzbeatles.nanas.utils.rememberEnumPreference
import com.nanzbeatles.nanas.utils.rememberPreference
import com.nanzbeatles.nanas.viewmodels.HomeViewModel
import com.nanzbeatles.nanas.viewmodels.CommunityPlaylistItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

private const val SpeedDialLoadingMinDurationMillis = 650L

private data class CompactQuickPlayCardItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val isActive: Boolean,
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    val speedDialItems by viewModel.speedDialItems.collectAsState()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsState()
    val communityPlaylists by viewModel.communityPlaylists.collectAsState()
    val resumeAudiobook by viewModel.resumeAudiobook.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isMoodAndGenresLoading by remember(isLoading, explorePage) {
        derivedStateOf { isLoading && explorePage?.moodAndGenres == null }
    }
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")

    val glassApplyHomeCards by rememberPreference(LiquidGlassApplyHomeCardsKey, defaultValue = true)

    val shouldShowWrappedCard by viewModel.showWrappedCard.collectAsState()
    val wrappedState by viewModel.wrappedManager.state.collectAsState()
    val isWrappedDataReady = wrappedState.isDataReady

    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val homeLayoutMode by rememberEnumPreference(HomeLayoutModeKey, defaultValue = HomeLayoutMode.GRID)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val wrappedDismissed by backStackEntry?.savedStateHandle?.getStateFlow("wrapped_seen", false)
        ?.collectAsState() ?: remember { mutableStateOf(false) }

    LaunchedEffect(wrappedDismissed) {
        if (wrappedDismissed) {
            viewModel.markWrappedAsSeen()
            scope.launch {
                snackbarHostState.showSnackbar("Ditemukan di Pengaturan > Konten")
            }
            backStackEntry?.savedStateHandle?.set("wrapped_seen", false) // Reset the value
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {
            // if a chip is selected, go back to the normal homepage first
            viewModel.toggleChip(selectedChip)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )

            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("album/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )

            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            navController.navigate("artist/${it.id}")
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress,
                            )
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )

            is Playlist -> PlaylistGridItem(
                playlist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            val browseId = it.playlist.browseId
                            if (it.songCount == 0 && !browseId.isNullOrBlank()) {
                                navController.navigate("online_playlist/${browseId.removePrefix("VL")}")
                            } else {
                                navController.navigate("local_playlist/${it.id}")
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                PlaylistMenu(
                                    playlist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
            )
        }
    }

    val localListItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongListItem(
                song = it,
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
                isSwipeable = false,
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
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
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        }
                    )
            )
            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { navController.navigate("album/${it.id}") },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                AlbumMenu(
                                    originalAlbum = it,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )
            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { navController.navigate("artist/${it.id}") },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                ArtistMenu(
                                    originalArtist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )
            is Playlist -> PlaylistGridItem(
                playlist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            val browseId = it.playlist.browseId
                            if (it.songCount == 0 && !browseId.isNullOrBlank()) {
                                navController.navigate("online_playlist/${browseId.removePrefix("VL")}")
                            } else {
                                navController.navigate("local_playlist/${it.id}")
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                PlaylistMenu(
                                    playlist = it,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    )
            )
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(
                                YouTubeQueue.radio(item.toMediaMetadata())
                            )

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                            is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                            is EpisodeItem -> playerConnection.playQueue(
                                YouTubeQueue.radio(item.toMediaMetadata())
                            )
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is AlbumItem -> YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )

                                is ArtistItem -> YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = menuState::dismiss
                                )

                                is PlaylistItem -> YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )

                                is PodcastItem -> YouTubePlaylistMenu(
                                    playlist = item.asPlaylistItem(),
                                    coroutineScope = scope,
                                    onDismiss = menuState::dismiss
                                )

                                is EpisodeItem -> YouTubeSongMenu(
                                    song = item.asSongItem(),
                                    navController = navController,
                                    onDismiss = menuState::dismiss
                                )
                            }
                        }
                    }
                )
        )
    }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
        contentAlignment = Alignment.TopStart
    ) {
        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
        val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = forgottenFavoritesLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }

        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val isEditorial = com.nanzbeatles.nanas.ui.theme.LocalAppVisualStyle.current == "EDITORIAL"
        val greetingText = remember {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when (hour) {
                in 4..10 -> "Selamat pagi"
                in 11..14 -> "Selamat siang"
                in 15..17 -> "Selamat sore"
                else -> "Selamat malam"
            }
        }
        var fallbackMoodChip by remember { mutableStateOf<String?>(null) }

        val compactGridItems = remember(
            keepListening,
            quickPicks,
            speedDialItems,
            homePage,
            mediaMetadata?.id,
            mediaMetadata?.album?.id
        ) {
            val result = mutableListOf<CompactQuickPlayCardItem>()
            val seenIds = mutableSetOf<String>()

            fun addLocalItem(localItem: LocalItem) {
                if (result.size >= 6 || !seenIds.add(localItem.id)) return
                when (localItem) {
                    is Song -> result.add(
                        CompactQuickPlayCardItem(
                            id = localItem.id,
                            title = localItem.title,
                            thumbnailUrl = localItem.thumbnailUrl,
                            isActive = localItem.id == mediaMetadata?.id,
                            onClick = {
                                if (localItem.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(YouTubeQueue.radio(localItem.toMediaMetadata()))
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    SongMenu(
                                        originalSong = localItem,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                    )
                    is Album -> result.add(
                        CompactQuickPlayCardItem(
                            id = localItem.id,
                            title = localItem.title,
                            thumbnailUrl = localItem.thumbnailUrl,
                            isActive = localItem.id == mediaMetadata?.album?.id,
                            onClick = { navController.navigate("album/${localItem.id}") },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = localItem,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                    )
                    is Artist -> result.add(
                        CompactQuickPlayCardItem(
                            id = localItem.id,
                            title = localItem.title,
                            thumbnailUrl = localItem.thumbnailUrl,
                            isActive = false,
                            onClick = { navController.navigate("artist/${localItem.id}") },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    ArtistMenu(
                                        originalArtist = localItem,
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                    )
                    is Playlist -> result.add(
                        CompactQuickPlayCardItem(
                            id = localItem.id,
                            title = localItem.title,
                            thumbnailUrl = localItem.thumbnailUrl,
                            isActive = false,
                            onClick = {
                                val browseId = localItem.playlist.browseId
                                if (localItem.songCount == 0 && !browseId.isNullOrBlank()) {
                                    navController.navigate("online_playlist/${browseId.removePrefix("VL")}")
                                } else {
                                    navController.navigate("local_playlist/${localItem.id}")
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    PlaylistMenu(
                                        playlist = localItem,
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        )
                    )
                }
            }

            fun addYtItem(ytItem: YTItem) {
                if (result.size >= 6 || !seenIds.add(ytItem.id)) return
                result.add(
                    CompactQuickPlayCardItem(
                        id = ytItem.id,
                        title = ytItem.title,
                        thumbnailUrl = ytItem.thumbnail,
                        isActive = ytItem.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                        onClick = {
                            when (ytItem) {
                                is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(ytItem.toMediaMetadata()))
                                is AlbumItem -> navController.navigate("album/${ytItem.id}")
                                is ArtistItem -> navController.navigate("artist/${ytItem.id}")
                                is PlaylistItem -> navController.navigate("online_playlist/${ytItem.id}")
                                is PodcastItem -> navController.navigate("online_podcast/${ytItem.id}")
                                is EpisodeItem -> playerConnection.playQueue(YouTubeQueue.radio(ytItem.toMediaMetadata()))
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (ytItem) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = ytItem,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = ytItem,
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = ytItem,
                                        onDismiss = menuState::dismiss
                                    )
                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = ytItem,
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss
                                    )
                                    is PodcastItem -> YouTubePlaylistMenu(
                                        playlist = ytItem.asPlaylistItem(),
                                        coroutineScope = scope,
                                        onDismiss = menuState::dismiss
                                    )
                                    is EpisodeItem -> YouTubeSongMenu(
                                        song = ytItem.asSongItem(),
                                        navController = navController,
                                        onDismiss = menuState::dismiss
                                    )
                                }
                            }
                        }
                    )
                )
            }

            keepListening?.forEach { addLocalItem(it) }
            quickPicks?.forEach { addLocalItem(it) }
            speedDialItems.forEach { addYtItem(it) }
            homePage?.sections?.flatMap { it.items }?.forEach { addYtItem(it) }
            result
        }

        LazyColumn(
            state = lazylistState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            // Refresh indicator at top
            item(key = "refresh_indicator") {
                AnimatedVisibility(
                    visible = isRefreshing,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
            }

            if (selectedChip != null) {
                item(key = "home_active_chip_bar") {
                    ChipsRow(
                        chips = homePage?.chips?.map { it to it.title } ?: emptyList(),
                        currentValue = selectedChip,
                        onValueUpdate = { viewModel.toggleChip(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp)
                    )
                }
            }

            if (selectedChip == null) {
                // 1. Time-based Greeting ("Selamat malam" / "Selamat pagi" / "Selamat siang" / "Selamat sore")
                item(key = "home_greeting_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 12.dp)
                            .animateItem()
                    ) {
                        if (isEditorial) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "EDITORIAL EDITION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.6.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(
                                            if (isDark) Color(0xFFF5F2EB).copy(alpha = 0.22f)
                                            else Color(0xFF111111).copy(alpha = 0.20f)
                                        )
                                )
                                Text(
                                    text = "VOL. 26",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = greetingText,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = if (isEditorial) 25.sp else 22.sp,
                                fontWeight = if (isEditorial) FontWeight.ExtraBold else FontWeight.Bold,
                                letterSpacing = if (isEditorial) (-0.6).sp else (-0.3).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 2. 2-Column Compact Quick-Play Grid (6 items = 3 rows x 2 columns)
                if (compactGridItems.isNotEmpty()) {
                    item(key = "home_compact_quick_grid") {
                        val rows = remember(compactGridItems) { compactGridItems.chunked(2) }
                        val cardBg = if (isEditorial) {
                            if (isDark) Color(0xFF121216) else Color(0xFFF3EFE7)
                        } else {
                            if (isDark) Color(0xFF15161A) else Color(0xFFF2F3F7)
                        }
                        val cardBorder = if (isEditorial) {
                            if (isDark) Color(0xFFF5F2EB).copy(alpha = 0.18f) else Color(0xFF111111).copy(alpha = 0.18f)
                        } else {
                            if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
                        }
                        val textColor = MaterialTheme.colorScheme.onSurface
                        val cornerDp = if (isEditorial) 6.dp else 10.dp

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 8.dp)
                                .animateItem(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rows.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { entry ->
                                        val cardShape = RoundedCornerShape(cornerDp)
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(56.dp)
                                                .clip(cardShape)
                                                .background(cardBg, cardShape)
                                                .border(
                                                    width = if (entry.isActive) 1.4.dp else if (isEditorial) 1.dp else 0.8.dp,
                                                    color = if (entry.isActive) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                    } else {
                                                        cardBorder
                                                    },
                                                    shape = cardShape
                                                )
                                                .combinedClickable(
                                                    onClick = entry.onClick,
                                                    onLongClick = entry.onLongClick
                                                ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(topStart = cornerDp, bottomStart = cornerDp))
                                                    .background(
                                                        if (isDark) Color(0xFF202228) else Color(0xFFE2E4EC)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!entry.thumbnailUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = entry.thumbnailUrl,
                                                        contentDescription = entry.title,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(R.drawable.music_note),
                                                        contentDescription = null,
                                                        tint = textColor.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = entry.title,
                                                color = textColor,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 15.5.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(horizontal = 10.dp)
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Daily Music Listening Flame Streak Card ("1 hari berturut-turut" / "Percikan")
                item(key = "home_streak_card") {
                    HomeStreakCard(
                        modifier = Modifier.animateItem()
                    )
                }

                // 4. Filter Chips Row ("Bersantai", "Senang", "Mengisi energi", "Perjalanan") below Streak Card
                item(key = "home_chips_row") {
                    val ytChips = homePage?.chips?.map { it to it.title }.orEmpty()
                    if (ytChips.isNotEmpty()) {
                        ChipsRow(
                            chips = ytChips,
                            currentValue = selectedChip,
                            onValueUpdate = { viewModel.toggleChip(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 4.dp)
                                .animateItem()
                        )
                    } else {
                        val defaultMoodChips = remember {
                            listOf(
                                "Bersantai" to "Bersantai",
                                "Senang" to "Senang",
                                "Mengisi energi" to "Mengisi energi",
                                "Perjalanan" to "Perjalanan",
                                "Fokus" to "Fokus"
                            )
                        }
                        ChipsRow(
                            chips = defaultMoodChips,
                            currentValue = fallbackMoodChip,
                            onValueUpdate = { clicked ->
                                fallbackMoodChip = if (fallbackMoodChip == clicked) null else clicked
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 4.dp)
                                .animateItem()
                        )
                    }
                }

                // 5. "Lanjutkan" (Keep Listening) directly below ChipsRow matching the reference video
                keepListening?.takeIf { it.isNotEmpty() }?.let { keepListeningList ->
                    item(key = "keep_listening_top_title") {
                        NavigationTitle(
                            title = stringResource(R.string.keep_listening),
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "keep_listening_top_list") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(keepListeningList, key = { "top_keep_${it.id}" }) {
                                localGridItem(it)
                            }
                        }
                    }
                }

                resumeAudiobook?.let { audiobook ->
                    item(key = "resume_audiobook") {
                        val song = audiobook.song
                        val durationMs = (song.song.duration * 1000L).coerceAtLeast(0L)
                        val resumePositionMs = audiobook.resumePositionMs.coerceIn(0L, durationMs)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .let { mod ->
                                    if (glassApplyHomeCards) mod.liquidGlassFromPrefs() else mod
                                }
                                .combinedClickable(
                                    onClick = {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = context.getString(R.string.audiobooks),
                                                items = listOf(song.toMediaItem()),
                                                startIndex = 0,
                                                position = resumePositionMs,
                                            )
                                        )
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (glassApplyHomeCards) Color.Transparent else MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            val contentColor = if (glassApplyHomeCards) {
                                if (isDark) Color.White else Color.Black
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.library_music),
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.resume_playback),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = contentColor.copy(alpha = 0.8f),
                                        )
                                        Text(
                                            text = song.song.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = contentColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.resume_at,
                                                makeTimeString(resumePositionMs),
                                                makeTimeString(durationMs),
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = contentColor.copy(alpha = 0.7f),
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null,
                                        tint = contentColor,
                                    )
                                }
                                if (durationMs > 0L) {
                                    LinearProgressIndicator(
                                        progress = { (resumePositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "wrapped_card") {
                    AnimatedVisibility(visible = shouldShowWrappedCard) {
                        val accentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (isWrappedDataReady) {
                                            navController.navigate("wrapped")
                                        }
                                    }
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isWrappedDataReady) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.wrapped_ready_title, WrappedConstants.displayMonthYear),
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontFamily = com.nanzbeatles.nanas.ui.theme.bbh_bartle,
                                                textAlign = TextAlign.Center
                                            ),
                                            color = accentColor,
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(R.string.wrapped_ready_subtitle),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                textAlign = TextAlign.Center
                                            ),
                                            color = accentColor.copy(alpha = 0.8f),
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(onClick = {
                                            navController.navigate("wrapped")
                                        }) {
                                            Icon(
                                                painter = painterResource(R.drawable.play),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Buka Ringkasan")
                                        }
                                    }
                                } else {
                                    ContainedLoadingIndicator()
                                }
                            }
                        }
                    }
                }


                // Speed Dial Section
                speedDialItems.takeIf { it.isNotEmpty() }?.let { speedDialItemsList ->
                    item(key = "speed_dial_title") {
                        NavigationTitle(
                            title = stringResource(R.string.speed_dial),
                            modifier = Modifier.animateItem(),
                        )
                    }

                    item(key = "speed_dial_grid") {
                        val itemSize = 160.dp
                        val pinnedSpeedDialIds = remember(pinnedSpeedDialItems) {
                            pinnedSpeedDialItems.map { it.id }.toSet()
                        }
                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(2),
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemSize * 2 + 16.dp)
                                .animateItem()
                        ) {
item(key = "speed_dial_shuffle") {
                                  var isLoading by remember { mutableStateOf(false) }
                                  var shuffledPositions by remember { mutableStateOf(listOf(0, 1, 2, 3)) }
                                  var lastLoadedSongId by remember { mutableStateOf<String?>(null) }
                                  var loadingStartedAt by remember { mutableStateOf<Long?>(null) }

                                  val animatedPos0 by animateFloatAsState(
                                      targetValue = shuffledPositions.indexOf(0).toFloat(),
                                      animationSpec = tween(300),
                                      label = "pos0"
                                  )
                                  val animatedPos1 by animateFloatAsState(
                                      targetValue = shuffledPositions.indexOf(1).toFloat(),
                                      animationSpec = tween(300),
                                      label = "pos1"
                                  )
                                  val animatedPos2 by animateFloatAsState(
                                      targetValue = shuffledPositions.indexOf(2).toFloat(),
                                      animationSpec = tween(300),
                                      label = "pos2"
                                  )
                                  val animatedPos3 by animateFloatAsState(
                                      targetValue = shuffledPositions.indexOf(3).toFloat(),
                                      animationSpec = tween(300),
                                      label = "pos3"
                                  )

                                  Box(
                                      modifier = Modifier
                                          .size(itemSize)
                                          .padding(4.dp),
                                  ) {
                                      Card(
                                          modifier = Modifier
                                              .fillMaxSize()
                                              .combinedClickable(
                                                  onClick = {
                                                      val playableItems = speedDialItemsList.filterIsInstance<SongItem>()
                                                      if (playableItems.isNotEmpty()) {
                                                          isLoading = true
                                                          val randomItem = playableItems.random()
                                                          lastLoadedSongId = randomItem.id
                                                          loadingStartedAt = System.currentTimeMillis()
                                                          playerConnection.playQueue(
                                                              YouTubeQueue.radio(randomItem.toMediaMetadata())
                                                          )
                                                      }
                                                  },
                                                  onLongClick = {
                                                      shuffledPositions = shuffledPositions.shuffled()
                                                  }
                                              ),
                                          shape = RoundedCornerShape(16.dp),
                                          colors = CardDefaults.cardColors(
                                              containerColor = MaterialTheme.colorScheme.primaryContainer
                                          ),
                                      ) {
                                          Box(
                                              modifier = Modifier.fillMaxSize(),
                                              contentAlignment = Alignment.Center
                                          ) {
                                               if (isLoading) {
                                                   ContainedLoadingIndicator(
                                                       modifier = Modifier.size(48.dp)
                                                   )
                                               } else {
                                                   Column(
                                                       horizontalAlignment = Alignment.CenterHorizontally,
                                                       verticalArrangement = Arrangement.spacedBy(16.dp)
                                                   ) {
                                                       Row(
                                                           horizontalArrangement = Arrangement.spacedBy(24.dp)
                                                       ) {
                                                           listOf(0, 1).forEach { dot ->
                                                               val animatedPos = when (dot) {
                                                                   0 -> animatedPos0
                                                                   1 -> animatedPos1
                                                                   else -> 0f
                                                               }
                                                               val targetCol = (animatedPos % 2).toInt()
                                                               val xOffset = (targetCol - dot) * 24
                                                               Box(
                                                                   modifier = Modifier
                                                                       .graphicsLayer { translationX = xOffset.toDp().toPx() }
                                                                       .size(14.dp)
                                                                       .background(
                                                                           color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                           shape = CircleShape
                                                                       )
                                                               )
                                                           }
                                                       }
                                                       Row(
                                                           horizontalArrangement = Arrangement.spacedBy(24.dp)
                                                       ) {
                                                           listOf(2, 3).forEach { dot ->
                                                               val animatedPos = when (dot) {
                                                                   2 -> animatedPos2
                                                                   3 -> animatedPos3
                                                                   else -> 0f
                                                               }
                                                               val targetCol = (animatedPos % 2).toInt()
                                                               val xOffset = (targetCol - (dot - 2)) * 24
                                                               Box(
                                                                   modifier = Modifier
                                                                       .graphicsLayer { translationX = xOffset.toDp().toPx() }
                                                                       .size(14.dp)
                                                                       .background(
                                                                           color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                           shape = CircleShape
                                                                       )
                                                               )
                                                           }
                                                       }
                                                   }
                                               }
                                          }
                                      }
                                  }
                                  LaunchedEffect(isLoading, lastLoadedSongId, mediaMetadata?.id) {
                                      if (isLoading && lastLoadedSongId != null && mediaMetadata?.id != null) {
                                          val elapsed = System.currentTimeMillis() - (loadingStartedAt ?: 0L)
                                          if (elapsed < SpeedDialLoadingMinDurationMillis) {
                                              delay(SpeedDialLoadingMinDurationMillis - elapsed)
                                          }
                                          isLoading = false
                                          lastLoadedSongId = null
                                          loadingStartedAt = null
                                      }
                                  }
                              }
                            items(
                                items = speedDialItemsList,
                                key = { it.id }
                            ) { item ->
                                val isPinned = item.id in pinnedSpeedDialIds

                                Box(
                                    modifier = Modifier
                                        .size(itemSize)
                                        .padding(4.dp),
                                ) {
                                    SpeedDialGridItem(
                                        item = item,
                                        isPinned = isPinned,
                                        isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                                        isPlaying = isPlaying,
                                        modifier = Modifier.fillMaxSize(),
                                        onClick = {
                                            when (item) {
                                                is SongItem -> playerConnection.playQueue(
                                                    YouTubeQueue.radio(item.toMediaMetadata())
                                                )
                                                is AlbumItem -> navController.navigate("album/${item.id}")
                                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                                                is EpisodeItem -> playerConnection.playQueue(ListQueue(title = item.title, items = listOf(item.toMediaMetadata().toMediaItem())))
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.togglePin(item)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                    item(key = "quick_picks_title") {
                        val quickPicksTitle = stringResource(R.string.quick_picks)
                        NavigationTitle(
                            title = quickPicksTitle,
                            modifier = Modifier.animateItem(),
                            onPlayAllClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = quickPicksTitle,
                                        items = quickPicks.distinctBy { it.id }.map { it.toMediaItem() }
                                    )
                                )
                            }
                        )
                    }

                    if (homeLayoutMode == HomeLayoutMode.LIST) {
                        item(key = "quick_picks_list") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                quickPicks.distinctBy { it.id }.take(10).forEach { song ->
                                    SongListItem(
                                        song = song,
                                        showInLibraryIcon = true,
                                        isActive = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        isSwipeable = false,
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
                                            .combinedClickable(
                                                onClick = {
                                                    if (song.id == mediaMetadata?.id) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue.radio(song.toMediaMetadata())
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        item(key = "quick_picks_list") {
                            val distinctQuickPicks = remember(quickPicks) { quickPicks.distinctBy { it.id } }
                            LazyHorizontalGrid(
                                state = quickPicksLazyGridState,
                                rows = GridCells.Fixed(4),
                                flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight * 4)
                            ) {
                                items(
                                    items = distinctQuickPicks,
                                    key = { it.id }
                                ) { song ->
                                    SongListItem(
                                        song = song,
                                        showInLibraryIcon = true,
                                        isActive = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        isSwipeable = false,
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
                                            .width(horizontalLazyGridItemWidth)
                                            .combinedClickable(
                                                onClick = {
                                                    if (song.id == mediaMetadata?.id) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue.radio(
                                                                song.toMediaMetadata()
                                                            )
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                // From the Community Section
                communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                    item(key = "community_playlists_title") {
                        NavigationTitle(
                            title = stringResource(R.string.from_the_community),
                            modifier = Modifier.animateItem(),
                        )
                    }

                    item(key = "community_playlists_content") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.animateItem(),
                        ) {
                            items(playlists, key = { it.playlist.id }) { item ->
                                CommunityPlaylistCard(
                                    item = item,
                                    onClick = {
                                        navController.navigate("online_playlist/${item.playlist.id.removePrefix("VL")}")
                                    },
                                    onSongClick = { song ->
                                        playerConnection.playQueue(
                                            YouTubeQueue.radio(song.toMediaMetadata()),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                    item(key = "account_playlists_title") {
                        NavigationTitle(
                            label = stringResource(R.string.your_youtube_playlists),
                            title = accountName,
                            thumbnail = {
                                if (url != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(url)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .diskCacheKey(url)
                                            .crossfade(false)
                                            .build(),
                                        placeholder = painterResource(id = R.drawable.person),
                                        error = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.person),
                                        contentDescription = null,
                                        modifier = Modifier.size(ListThumbnailSize)
                                    )
                                }
                            },
                            onClick = {
                                navController.navigate("account")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "account_playlists_list") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(
                                items = accountPlaylists.distinctBy { it.id },
                                key = { it.id },
                            ) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }

                forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                    item(key = "forgotten_favorites_title") {
                        val forgottenFavoritesTitle = stringResource(R.string.forgotten_favorites)
                        NavigationTitle(
                            title = forgottenFavoritesTitle,
                            modifier = Modifier.animateItem(),
                            onPlayAllClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = forgottenFavoritesTitle,
                                        items = forgottenFavorites.distinctBy { it.id }.map { it.toMediaItem() }
                                    )
                                )
                            }
                        )
                    }

                    item(key = "forgotten_favorites_list") {
                        // take min in case list size is less than 4
                        val distinctForgotten = remember(forgottenFavorites) { forgottenFavorites.distinctBy { it.id } }
                        val rows = min(4, distinctForgotten.size)
                        LazyHorizontalGrid(
                            state = forgottenFavoritesLazyGridState,
                            rows = GridCells.Fixed(rows),
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            flingBehavior = rememberSnapFlingBehavior(
                                forgottenFavoritesSnapLayoutInfoProvider
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * rows)
                        ) {
                            items(
                                items = distinctForgotten,
                                key = { it.id }
                            ) { song ->
                                SongListItem(
                                    song = song,
                                    showInLibraryIcon = true,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSwipeable = false,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                                        .width(horizontalLazyGridItemWidth)
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(
                                                            song.toMediaMetadata()
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }

                similarRecommendations?.forEachIndexed { index, recommendation ->
                    item(key = "similar_to_title_$index") {
                        NavigationTitle(
                            label = stringResource(R.string.similar_to),
                            title = recommendation.title.title,
                            thumbnail = recommendation.title.thumbnailUrl?.let { thumbnailUrl ->
                                {
                                    val shape =
                                        if (recommendation.title is Artist) CircleShape else RoundedCornerShape(
                                            ThumbnailCornerRadius
                                        )
                                    AsyncImage(
                                        model = thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(ListThumbnailSize)
                                            .clip(shape)
                                    )
                                }
                            },
                            onClick = {
                                when (recommendation.title) {
                                    is Song -> recommendation.title.album?.id?.let { navController.navigate("album/$it") }
                                    is Album -> navController.navigate("album/${recommendation.title.id}")
                                    is Artist -> navController.navigate("artist/${recommendation.title.id}")
                                    is Playlist -> {
                                        val browseId = recommendation.title.playlist.browseId
                                        if (recommendation.title.songCount == 0 && !browseId.isNullOrBlank()) {
                                            navController.navigate("online_playlist/${browseId.removePrefix("VL")}")
                                        } else {
                                            navController.navigate("local_playlist/${recommendation.title.id}")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "similar_to_list_$index") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(recommendation.items, key = { it.id }) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

            homePage?.sections?.forEachIndexed { index, section ->
                // Check if section contains songs for Play All functionality
                val sectionSongs = section.items.filterIsInstance<SongItem>()
                val hasPlayableSongs = sectionSongs.isNotEmpty()
                // Check if this section contains ONLY songs (like Quick picks, Trending songs)
                val isSongsOnlySection = section.items.isNotEmpty() && 
                    section.items.all { it is SongItem }

                item(key = "home_section_title_$index") {
                    NavigationTitle(
                        title = section.title,
                        label = section.label,
                        thumbnail = section.thumbnail?.let { thumbnailUrl ->
                            {
                                val shape =
                                    if (section.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                        ThumbnailCornerRadius
                                    )
                                AsyncImage(
                                    model = thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(ListThumbnailSize)
                                        .clip(shape)
                                )
                            }
                        },
                        onClick = section.endpoint?.let { endpoint ->
                            {
                                when {
                                    endpoint.browseId == "FEmusic_moods_and_genres" -> 
                                        navController.navigate("mood_and_genres")
                                    endpoint.params != null -> 
                                        navController.navigate("youtube_browse/${endpoint.browseId}?params=${endpoint.params}")
                                    else -> 
                                        navController.navigate("browse/${endpoint.browseId}")
                                }
                            }
                        },
                        onPlayAllClick = if (hasPlayableSongs) {
                            {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = section.title,
                                        items = sectionSongs.map { it.toMediaMetadata().toMediaItem() }
                                    )
                                )
                            }
                        } else null,
                        modifier = Modifier.animateItem()
                    )
                }

                if (isSongsOnlySection) {
                    // Render songs as a horizontal scrollable list (like Quick picks in YouTube Music)
                    item(key = "home_section_list_$index") {
                        LazyHorizontalGrid(
                            state = rememberLazyGridState(),
                            rows = GridCells.Fixed(4),
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight * 4)
                                .animateItem()
                        ) {
                            items(
                                items = sectionSongs.distinctBy { it.id },
                                key = { it.id }
                            ) { song ->
                                YouTubeListItem(
                                    item = song,
                                    isActive = song.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    isSwipeable = false,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
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
                                        .width(horizontalLazyGridItemWidth)
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue.radio(song.toMediaMetadata())
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                } else {
                    // Render mixed content as horizontal grid items (albums, playlists, artists, etc.)
                    item(key = "home_section_list_$index") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(section.items, key = { it.id }) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }
            }

            if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                item(key = "loading_shimmer") {
                    ShimmerHost(
                        modifier = Modifier.animateItem()
                    ) {
                        TextPlaceholder(
                            height = 36.dp,
                            modifier = Modifier
                                .padding(12.dp)
                                .width(250.dp),
                        )
                        LazyRow(
                            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                        ) {
                            items(4) {
                                GridItemPlaceHolder()
                            }
                        }
                    }
                }
            }

            if (selectedChip == null) {
                explorePage?.podcasts?.takeIf { it.isNotEmpty() }?.let { podcasts ->
                    item(key = "podcasts_title") {
                        NavigationTitle(
                            title = stringResource(R.string.podcasts),
                            onClick = {
                                navController.navigate("podcasts")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "podcasts_list") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(podcasts.flatMap { it.items }.take(10)) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }

                explorePage?.mixes?.takeIf { it.isNotEmpty() }?.let { mixes ->
                    item(key = "mixes_title") {
                        NavigationTitle(
                            title = stringResource(R.string.mixes),
                            onClick = {
                                navController.navigate("mixes")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    item(key = "mixes_list") {
                        LazyRow(
                            contentPadding = WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .asPaddingValues(),
                            modifier = Modifier.animateItem()
                        ) {
                            items(mixes.flatMap { it.items }.take(10)) { item ->
                                ytGridItem(item)
                            }
                        }
                    }
                }

                explorePage?.moodAndGenres?.let { moodAndGenres ->
                    item(key = "mood_and_genres_title") {
                        NavigationTitle(
                            title = stringResource(R.string.mood_and_genres),
                            onClick = {
                                navController.navigate("mood_and_genres")
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                    item(key = "mood_and_genres_list") {
                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(4),
                            contentPadding = PaddingValues(6.dp),
                            modifier = Modifier
                                .height((MoodAndGenresButtonHeight + 12.dp) * 4 + 12.dp)
                                .animateItem()
                        ) {
                            items(moodAndGenres, key = { "${it.endpoint.browseId}_${it.title}" }) {
                                MoodAndGenresButton(
                                    title = it.title,
                                    onClick = {
                                        navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                                    },
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .width(180.dp)
                                )
                            }
                        }
                    }
                }

                if (isMoodAndGenresLoading) {
                    item(key = "mood_and_genres_shimmer") {
                        ShimmerHost(
                            modifier = Modifier.animateItem()
                        ) {
                            TextPlaceholder(
                                height = 36.dp,
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 12.dp)
                                    .width(250.dp),
                            )

                            repeat(4) {
                                Row {
                                    repeat(2) {
                                        TextPlaceholder(
                                            height = MoodAndGenresButtonHeight,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp)
                                                .width(200.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
            lazyListState = lazylistState,
            icon = R.drawable.shuffle,
            onClick = {
                val local = when {
                    allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                    allLocalItems.isNotEmpty() -> true
                    else -> false
                }
                scope.launch(Dispatchers.Main) {
                    if (local) {
                        when (val luckyItem = allLocalItems.random()) {
                            is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                            is Album -> {
                                val albumWithSongs = withContext(Dispatchers.IO) {
                                    database.albumWithSongs(luckyItem.id).first()
                                }
                                albumWithSongs?.let {
                                    playerConnection.playQueue(LocalAlbumRadio(it))
                                }
                            }
                            is Artist -> {}
                            is Playlist -> {}
                        }
                    } else {
                        when (val luckyItem = allYtItems.random()) {
                            is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                            is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                            is ArtistItem -> luckyItem.radioEndpoint?.let {
                                playerConnection.playQueue(YouTubeQueue(it))
                            }
                            is PlaylistItem -> luckyItem.playEndpoint?.let {
                                playerConnection.playQueue(YouTubeQueue(it))
                            }
                            is PodcastItem -> luckyItem.playEndpoint?.let {
                                playerConnection.playQueue(YouTubeQueue(it))
                            }
                            is EpisodeItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                        }
                    }
                }
            },
            onRecognitionClick = {
                navController.navigate("recognition")
            }
        )


    }
}

@Composable
fun CommunityPlaylistCard(
    item: CommunityPlaylistItem,
    onClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val dbPlaylist by database.playlistByBrowseId(item.playlist.id).collectAsState(initial = null)
    val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null

    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    val isCurrentSongPlaying = item.songs.any { it.id == mediaMetadata?.id }
    val isPlaying by playerConnection?.isEffectivelyPlaying?.collectAsState(initial = false) ?: remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .width(320.dp)
            .height(420.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        shape = RoundedCornerShape(28.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 2x2 Grid of thumbnails
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp)),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = item.songs.getOrNull(0)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                            )
                            AsyncImage(
                                model = item.songs.getOrNull(1)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                            )
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = item.songs.getOrNull(2)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                            )
                            AsyncImage(
                                model = item.songs.getOrNull(3)?.thumbnail?.replace(Regex("w\\d+-h\\d+"), "w120-h120"),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize(),
                            )
                        }
                    }

                    if (isCurrentSongPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            PlayingIndicatorBox(
                                isActive = true,
                                playWhenReady = isPlaying,
                                color = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = item.playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.playlist.author?.name ?: "Tidak Diketahui",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.playlist.songCountText ?: "${item.songs.size} lagu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            // Song list
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                item.songs.take(4).forEach { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSongClick(song) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = song.thumbnail.replace(Regex("w\\d+-h\\d+"), "w60-h60"),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.firstOrNull()?.name ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = { onSongClick(song) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.play),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
