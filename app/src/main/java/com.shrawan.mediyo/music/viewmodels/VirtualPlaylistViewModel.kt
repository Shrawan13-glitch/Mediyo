package com.shrawan.mediyo.music.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.shrawan.mediyo.innertube.YouTube
import com.shrawan.mediyo.music.constants.SongSortType
import com.shrawan.mediyo.music.db.MusicDatabase
import com.shrawan.mediyo.music.db.entities.PlaylistEntity
import com.shrawan.mediyo.music.models.toMediaMetadata
import com.shrawan.mediyo.innertube.utils.completed
import com.shrawan.mediyo.music.playback.DownloadUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
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

    init {
        if (playlistId == PlaylistEntity.LIKED_PLAYLIST_ID) {
            syncLikedSongs()
        }
    }

    private fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            if (YouTube.cookie == null) return@launch
            val page = YouTube.playlist("LM").completed().getOrNull() ?: return@launch
            val remoteSongs = page.songs
            val remoteIds = remoteSongs.mapTo(mutableSetOf()) { it.id }
            val now = LocalDateTime.now()
            database.likedSongEntities().forEach { localSong ->
                if (localSong.id !in remoteIds) {
                    database.update(localSong.copy(liked = false))
                }
            }
            remoteSongs.forEachIndexed { index, song ->
                val timestamp = now.minusSeconds(index.toLong())
                val existing = database.songEntity(song.id)
                if (existing == null) {
                    database.insert(song.toMediaMetadata()) {
                        it.copy(liked = true, inLibrary = timestamp)
                    }
                } else {
                    database.update(existing.copy(liked = true, inLibrary = timestamp))
                }
            }
        }
    }
}
