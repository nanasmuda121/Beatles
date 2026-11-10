/**
 * NanzBeatles Project (C) 2026
 */

package com.nanzbeatles.nanas.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nanzbeatles.innertube.YouTube
import com.nanzbeatles.innertube.models.AlbumItem
import com.nanzbeatles.innertube.pages.AlbumPage
import com.nanzbeatles.nanas.db.MusicDatabase
import com.nanzbeatles.nanas.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId").orEmpty()
    val playlistId = MutableStateFlow("")
    val albumPage = MutableStateFlow<AlbumPage?>(null)
    val isLoading = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            if (albumId.isBlank()) {
                isLoading.value = false
                error.value = "ID album tidak valid"
                return@launch
            }
            isLoading.value = true
            error.value = null
            val album = database.album(albumId).first()
            if (album != null && playlistId.value.isEmpty()) {
                playlistId.value = album.album.playlistId.orEmpty()
            }
            YouTube
                .album(albumId)
                .onSuccess { page ->
                    playlistId.value = page.album.playlistId
                    albumPage.value = page
                    otherVersions.value = page.otherVersions
                    isLoading.value = false
                    runCatching {
                        database.transaction {
                            if (album == null) {
                                insert(page)
                            } else {
                                update(album.album, page, album.artists)
                            }
                        }
                    }.onFailure {
                        reportException(it)
                    }
                }.onFailure {
                    isLoading.value = false
                    error.value = it.localizedMessage ?: "Gagal memuat album"
                    reportException(it)
                    if (it.message?.contains("NOT_FOUND") == true) {
                        database.query {
                            album?.album?.let(::delete)
                        }
                    }
                }
        }
    }
}

