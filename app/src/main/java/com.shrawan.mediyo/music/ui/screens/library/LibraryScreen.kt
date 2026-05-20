@file:OptIn(ExperimentalFoundationApi::class)

package com.shrawan.mediyo.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.shrawan.mediyo.R
import com.shrawan.mediyo.music.LocalDatabase
import com.shrawan.mediyo.music.LocalPlayerAwareWindowInsets
import com.shrawan.mediyo.music.LocalPlayerConnection
import com.shrawan.mediyo.music.db.entities.Album
import com.shrawan.mediyo.music.db.entities.Artist
import com.shrawan.mediyo.music.db.entities.Playlist
import com.shrawan.mediyo.music.db.entities.Song
import com.shrawan.mediyo.music.extensions.toMediaItem
import com.shrawan.mediyo.music.extensions.togglePlayPause
import com.shrawan.mediyo.music.playback.queues.ListQueue
import com.shrawan.mediyo.music.ui.component.AlbumListItem
import com.shrawan.mediyo.music.ui.component.ArtistListItem
import com.shrawan.mediyo.music.ui.component.EmptyPlaceholder
import com.shrawan.mediyo.music.ui.component.LocalMenuState
import com.shrawan.mediyo.music.ui.component.NavigationTitle
import com.shrawan.mediyo.music.ui.component.PlaylistGridItem
import com.shrawan.mediyo.music.ui.component.SongListItem
import com.shrawan.mediyo.music.ui.menu.AlbumMenu
import com.shrawan.mediyo.music.ui.menu.ArtistMenu
import com.shrawan.mediyo.music.ui.menu.SongMenu
import com.shrawan.mediyo.music.viewmodels.LibraryAlbumsViewModel
import com.shrawan.mediyo.music.viewmodels.LibraryArtistsViewModel
import com.shrawan.mediyo.music.viewmodels.LibraryPlaylistsViewModel
import com.shrawan.mediyo.music.viewmodels.LibrarySongsViewModel

enum class LibraryFilter {
    PLAYLISTS, SONGS, ALBUMS, ARTISTS
}

@Composable
fun LibraryScreen(
    navController: NavController,
) {
    var filter by rememberSaveable { mutableStateOf<LibraryFilter?>(null) }

    val songsViewModel: LibrarySongsViewModel = hiltViewModel()
    val artistsViewModel: LibraryArtistsViewModel = hiltViewModel()
    val albumsViewModel: LibraryAlbumsViewModel = hiltViewModel()
    val playlistsViewModel: LibraryPlaylistsViewModel = hiltViewModel()

    val songs by songsViewModel.allSongs.collectAsState()
    val artists by artistsViewModel.allArtists.collectAsState()
    val albums by albumsViewModel.allAlbums.collectAsState()
    val playlists by playlistsViewModel.allPlaylists.collectAsState()

    val database = LocalDatabase.current
    val likedSongsCount by database.likedSongsCount().collectAsState(initial = 0)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    val lazyListState = rememberLazyListState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            filter = null
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val filterContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.width(12.dp))

            listOf(
                LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
            ).forEach { (value, label) ->
                FilterChip(
                    selected = filter == value,
                    onClick = { filter = if (filter == value) null else value },
                    label = { Text(label) },
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }

    when (filter) {
        null -> LibraryOverview(
            songs = songs,
            artists = artists,
            albums = albums,
            playlists = playlists,
            likedSongsCount = likedSongsCount,
            navController = navController,
            onFilterSelected = { filter = it },
            lazyListState = lazyListState,
            filterContent = filterContent,
        )

        LibraryFilter.SONGS -> LibrarySongsScreen(
            navController = navController,
            filterContent = filterContent,
        )
        LibraryFilter.ARTISTS -> LibraryArtistsScreen(
            navController = navController,
            filterContent = filterContent,
        )
        LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
            navController = navController,
            filterContent = filterContent,
        )
        LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(
            navController = navController,
            filterContent = filterContent,
        )
    }
}

@Composable
private fun LibraryOverview(
    songs: List<Song>?,
    artists: List<Artist>?,
    albums: List<Album>?,
    playlists: List<Playlist>?,
    likedSongsCount: Int,
    navController: NavController,
    onFilterSelected: (LibraryFilter) -> Unit,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    filterContent: @Composable () -> Unit,
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val queueAllSongs = stringResource(R.string.queue_all_songs)

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        item(
            key = "filter_content"
        ) {
            filterContent()
        }

        item(
            key = "playlists_carousel"
        ) {
            Column {
                NavigationTitle(
                    title = stringResource(R.string.filter_playlists),
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "liked_songs") {
                        Card(
                            onClick = { onFilterSelected(LibraryFilter.SONGS) },
                            modifier = Modifier.width(140.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.favorite),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = stringResource(R.string.liked_songs),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                )
                                Text(
                                    text = pluralStringResource(R.plurals.n_song, likedSongsCount, likedSongsCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }

                    items(
                        items = playlists ?: emptyList(),
                        key = { it.id },
                    ) { playlist ->
                        PlaylistGridItem(
                            playlist = playlist,
                            modifier = Modifier.clickable {
                                navController.navigate("local_playlist/${playlist.id}")
                            },
                        )
                    }
                }
            }
        }

        item(
            key = "songs_header"
        ) {
            NavigationTitle(
                title = stringResource(R.string.filter_songs),
                onClick = { onFilterSelected(LibraryFilter.SONGS) },
            )
        }

        val previewSongs = songs?.take(5) ?: emptyList()
        if (previewSongs.isEmpty()) {
            item(key = "songs_empty") {
                EmptyPlaceholder(
                    icon = R.drawable.music_note,
                    text = stringResource(R.string.library_song_empty),
                )
            }
        } else {
            items(
                items = previewSongs,
                key = { it.id },
                contentType = { "song" },
            ) { song ->
                SongListItem(
                    song = song,
                    showLikedIcon = true,
                    showDownloadIcon = true,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (playerConnection != null) {
                                    if (song.id == playerConnection.mediaMetadata.value?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = queueAllSongs,
                                                items = (songs ?: emptyList()).map { it.toMediaItem() },
                                                startIndex = (songs ?: emptyList()).indexOf(song).coerceAtLeast(0),
                                            )
                                        )
                                    }
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    SongMenu(
                                        originalSong = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                        .animateItem(),
                )
            }
        }

        item(
            key = "albums_header"
        ) {
            NavigationTitle(
                title = stringResource(R.string.filter_albums),
                onClick = { onFilterSelected(LibraryFilter.ALBUMS) },
            )
        }

        val previewAlbums = albums?.take(5) ?: emptyList()
        if (previewAlbums.isEmpty()) {
            item(key = "albums_empty") {
                EmptyPlaceholder(
                    icon = R.drawable.album,
                    text = stringResource(R.string.library_album_empty),
                )
            }
        } else {
            items(
                items = previewAlbums,
                key = { it.id },
                contentType = { "album" },
            ) { album ->
                AlbumListItem(
                    album = album,
                    showLikedIcon = true,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = album,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                navController.navigate("album/${album.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum = album,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                        .animateItem(),
                )
            }
        }

        item(
            key = "artists_header"
        ) {
            NavigationTitle(
                title = stringResource(R.string.filter_artists),
                onClick = { onFilterSelected(LibraryFilter.ARTISTS) },
            )
        }

        val previewArtists = artists?.take(5) ?: emptyList()
        if (previewArtists.isEmpty()) {
            item(key = "artists_empty") {
                EmptyPlaceholder(
                    icon = R.drawable.artist,
                    text = stringResource(R.string.library_artist_empty),
                )
            }
        } else {
            items(
                items = previewArtists,
                key = { it.id },
                contentType = { "artist" },
            ) { artist ->
                ArtistListItem(
                    artist = artist,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    ArtistMenu(
                                        originalArtist = artist,
                                        coroutineScope = coroutineScope,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                        }
                        .animateItem(),
                )
            }
        }
    }
}
