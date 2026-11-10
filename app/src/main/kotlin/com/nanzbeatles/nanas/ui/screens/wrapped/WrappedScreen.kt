/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.wrapped

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.nanzbeatles.nanas.LocalPlayerConnection
import com.nanzbeatles.nanas.R
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.ConclusionPage
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.PlaylistPage
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedComparisonScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedDayOfWeekScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedDiscoveryScoreScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedIntro
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedMinutesScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedMinutesTease
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedRepeatOffenderScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedShareCardScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTimeOfDayScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTop5AlbumsScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTop5ArtistsScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTop5SongsScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTopAlbumScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTopArtistAlbumsScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTopArtistScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTopSongScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTotalAlbumsScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTotalArtistsScreen
import com.nanzbeatles.nanas.ui.screens.wrapped.pages.WrappedTotalSongsScreen
import kotlinx.coroutines.launch

sealed class WrappedScreenType {
    object Welcome : WrappedScreenType()
    object MinutesTease : WrappedScreenType()
    object MinutesReveal : WrappedScreenType()
    object TotalSongs : WrappedScreenType()
    object TopSongReveal : WrappedScreenType()
    object Top5Songs : WrappedScreenType()
    object TotalAlbums : WrappedScreenType()
    object TopAlbumReveal : WrappedScreenType()
    object Top5Albums : WrappedScreenType()
    object TotalArtists : WrappedScreenType()
    object TopArtistReveal : WrappedScreenType()
    object Top5Artists : WrappedScreenType()
    object TopArtistAlbums : WrappedScreenType()
    object DayOfWeek : WrappedScreenType()
    object TimeOfDay : WrappedScreenType()
    object RepeatOffender : WrappedScreenType()
    object DiscoveryScore : WrappedScreenType()
    object Comparison : WrappedScreenType()
    object ShareCard : WrappedScreenType()
    object Playlist : WrappedScreenType()
    object Conclusion : WrappedScreenType()
}

internal val LocalWrappedAudioService = staticCompositionLocalOf<WrappedAudioService?> { null }

@Composable
fun WrappedScreen(navController: NavController) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current

    val manager = remember {
        provideWrappedManager(context)
    }
    val audioService = remember {
        WrappedAudioService(context)
    }

    DisposableEffect(Unit) {
        val wasPlaying = playerConnection?.isPlaying?.value == true
        if (wasPlaying) {
            playerConnection?.player?.pause()
        }
        onDispose {
            audioService.release()
        }
    }

    LaunchedEffect(Unit) {
        manager.prepare()
    }

    CompositionLocalProvider(
        LocalWrappedManager provides manager,
        LocalWrappedAudioService provides audioService
    ) {
        WrappedScreenContent(navController = navController)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WrappedScreenContent(navController: NavController) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val manager = LocalWrappedManager.current
    val audioService = LocalWrappedAudioService.current
    val state by manager.state.collectAsState()
    val isMuted = audioService?.isMuted?.collectAsState()?.value ?: false
    val messagePair = remember(state.totalMinutes) {
        WrappedRepository.getMessage(state.totalMinutes)
    }

    val screens = remember {
        listOf(
            WrappedScreenType.Welcome,
            WrappedScreenType.MinutesTease,
            WrappedScreenType.MinutesReveal,
            WrappedScreenType.TotalSongs,
            WrappedScreenType.TopSongReveal,
            WrappedScreenType.Top5Songs,
            WrappedScreenType.TotalAlbums,
            WrappedScreenType.TopAlbumReveal,
            WrappedScreenType.Top5Albums,
            WrappedScreenType.TotalArtists,
            WrappedScreenType.TopArtistReveal,
            WrappedScreenType.Top5Artists,
            WrappedScreenType.TopArtistAlbums,
            WrappedScreenType.DayOfWeek,
            WrappedScreenType.TimeOfDay,
            WrappedScreenType.RepeatOffender,
            WrappedScreenType.DiscoveryScore,
            WrappedScreenType.Comparison,
            WrappedScreenType.ShareCard,
            WrappedScreenType.Playlist,
            WrappedScreenType.Conclusion
        )
    }
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> audioService?.pause()
                Lifecycle.Event.ON_RESUME -> audioService?.resume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(pagerState.currentPage, state.trackMap) {
        if (state.trackMap.isNotEmpty()) {
            val currentScreen = screens[pagerState.currentPage]
            audioService?.playTrack(state.trackMap[currentScreen])
        }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Scaffold(
        containerColor = Color.Black
    ) { internalPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (screens[page]) {
                    is WrappedScreenType.Welcome -> WrappedIntro {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page = 1)
                        }
                    }

                    is WrappedScreenType.MinutesTease -> WrappedMinutesTease(
                        isVisible = pagerState.currentPage == page,
                        onNavigateForward = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page = 2)
                            }
                        }
                    )

                    is WrappedScreenType.MinutesReveal -> WrappedMinutesScreen(
                        range = Pair(messagePair.tease, messagePair.reveal),
                        totalMinutes = state.totalMinutes,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.TotalSongs -> WrappedTotalSongsScreen(
                        uniqueSongCount = state.uniqueSongCount,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.TopSongReveal -> WrappedTopSongScreen(
                        topSong = state.topSongs.firstOrNull(),
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.Top5Songs -> WrappedTop5SongsScreen(
                        topSongs = state.topSongs,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.TotalAlbums -> WrappedTotalAlbumsScreen(
                        uniqueAlbumCount = state.totalAlbums,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.TopAlbumReveal -> WrappedTopAlbumScreen(
                        topAlbum = state.topAlbum,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.Top5Albums -> WrappedTop5AlbumsScreen(
                        topAlbums = state.top5Albums,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.TotalArtists -> WrappedTotalArtistsScreen(
                        uniqueArtistCount = state.uniqueArtistCount,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.TopArtistReveal -> WrappedTopArtistScreen(
                        topArtist = state.topArtists.firstOrNull(),
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.Top5Artists -> WrappedTop5ArtistsScreen(
                        topArtists = state.topArtists,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.TopArtistAlbums -> WrappedTopArtistAlbumsScreen(
                        topArtistName = state.topArtists.firstOrNull()?.artist?.name,
                        topArtistAlbums = state.topArtistAlbums,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.DayOfWeek -> WrappedDayOfWeekScreen(
                        listeningByDayOfWeek = state.listeningByDayOfWeek,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.TimeOfDay -> WrappedTimeOfDayScreen(
                        listeningByTimeOfDay = state.listeningByTimeOfDay,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.RepeatOffender -> WrappedRepeatOffenderScreen(
                        repeatOffenderSong = state.repeatOffenderSong,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.DiscoveryScore -> WrappedDiscoveryScoreScreen(
                        discoveryScore = state.discoveryScore,
                        uniqueArtistCount = state.uniqueArtistCount,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.Comparison -> WrappedComparisonScreen(
                        currentMinutes = state.totalMinutes,
                        previousMonthMinutes = state.previousMonthMinutes,
                        currentUniqueSongs = state.uniqueSongCount,
                        previousMonthUniqueSongs = state.previousMonthUniqueSongs,
                        currentUniqueArtists = state.uniqueArtistCount,
                        previousMonthUniqueArtists = state.previousMonthUniqueArtists,
                        monthOverMonthChange = state.monthOverMonthChange,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.ShareCard -> WrappedShareCardScreen(
                        state = state,
                        isVisible = pagerState.currentPage == page
                    )

                    is WrappedScreenType.Playlist -> PlaylistPage()
                    is WrappedScreenType.Conclusion -> ConclusionPage(onClose = { navController.popBackStack() })
                }
            }

            // Top Story Progress & Controls Overlay (pure monochrome white)
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = topPadding.coerceAtLeast(14.dp),
                        start = 16.dp,
                        end = 16.dp
                    )
            ) {
                // Segmented story progress bars
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)
                ) {
                    screens.indices.forEach { idx ->
                        val isCompleted = idx < pagerState.currentPage
                        val isCurrent = idx == pagerState.currentPage
                        androidx.compose.material3.Surface(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 2.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = when {
                                isCurrent -> Color.White
                                isCompleted -> Color.White.copy(alpha = 0.65f)
                                else -> Color.White.copy(alpha = 0.22f)
                            }
                        ) {
                            androidx.compose.foundation.layout.Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.5.dp)
                            )
                        }
                    }
                }

                // Header Row: Close button, Page counter pill, Mute/Unmute button
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White
                        )
                    }

                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        androidx.compose.material3.Text(
                            text = "✦ RINGKASAN • ${pagerState.currentPage + 1} / ${screens.size} ✦",
                            color = Color.White,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = { audioService?.toggleMute() }
                    ) {
                        val iconRes = if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        val contentDesc = if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = contentDesc,
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom subtle swipe indicator on non-final pages
            if (pagerState.currentPage in 1 until screens.lastIndex) {
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    androidx.compose.material3.Text(
                        text = "↑ Geser ke atas untuk lanjut",
                        color = Color.White.copy(alpha = 0.7f),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
