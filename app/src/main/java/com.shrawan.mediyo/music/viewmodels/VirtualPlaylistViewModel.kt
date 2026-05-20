package com.shrawan.mediyo.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.shrawan.mediyo.music.constants.SongSortType
import com.shrawan.mediyo.music.db.MusicDatabase
import com.shrawan.mediyo.music.db.entities.PlaylistEntity
import com.shrawan.mediyo.music.playback.DownloadUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class VirtualPlaylistViewModel @Inject constructor(
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val playlistId = savedStateHandle.get<String>("playlistId")!!

    val title: String = when (playlistId) {
        PlaylistEntity.LIKED_PLAYLIST_ID -> "Liked Songs"
        PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> "Downloaded Songs"
        else -> "Songs"
    }

    val songs = when (playlistId) {
        PlaylistEntity.LIKED_PLAYLIST_ID -> database.likedSongs(SongSortType.CREATE_DATE, true)
        PlaylistEntity.DOWNLOADED_PLAYLIST_ID -> downloadUtil.downloads.flatMapLatest { downloads ->
            database.allSongs()
                .flowOn(Dispatchers.IO)
                .map { songs ->
                    songs.filter {
                        downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                }
                .map { songs ->
                    songs.sortedBy { downloads[it.id]?.updateTimeMs ?: 0L }
                }
        }
        else -> database.likedSongs(SongSortType.CREATE_DATE, true)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)
}
