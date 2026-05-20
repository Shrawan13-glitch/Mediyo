package com.shrawan.mediyo.music.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.shrawan.mediyo.R
import com.shrawan.mediyo.music.LocalPlayerAwareWindowInsets
import com.shrawan.mediyo.music.constants.ListThumbnailSize
import com.shrawan.mediyo.music.db.entities.Album
import com.shrawan.mediyo.music.db.entities.Artist
import com.shrawan.mediyo.music.db.entities.Playlist
import com.shrawan.mediyo.music.db.entities.PlaylistEntity
import com.shrawan.mediyo.music.ui.component.EmptyPlaceholder
import com.shrawan.mediyo.music.ui.component.LocalMenuState
import com.shrawan.mediyo.music.ui.menu.AlbumMenu
import com.shrawan.mediyo.music.ui.menu.ArtistMenu
import com.shrawan.mediyo.music.ui.menu.PlaylistMenu
import com.shrawan.mediyo.music.viewmodels.LibraryAlbumsViewModel
import com.shrawan.mediyo.music.viewmodels.LibraryArtistsViewModel
import com.shrawan.mediyo.music.viewmodels.LibraryPlaylistsViewModel

enum class LibraryFilter {
    PLAYLISTS, SONGS, ALBUMS, ARTISTS
}

@Composable
fun LibraryScreen(
    navController: NavController,
) {
    var filter by rememberSaveable { mutableStateOf<LibraryFilter?>(null) }

    val artistsViewModel: LibraryArtistsViewModel = hiltViewModel()
    val albumsViewModel: LibraryAlbumsViewModel = hiltViewModel()
    val playlistsViewModel: LibraryPlaylistsViewModel = hiltViewModel()

    val artists by artistsViewModel.allArtists.collectAsState()
    val albums by albumsViewModel.allAlbums.collectAsState()
    val playlists by playlistsViewModel.allPlaylists.collectAsState()

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
            artists = artists,
            albums = albums,
            playlists = playlists,
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
    artists: List<Artist>?,
    albums: List<Album>?,
    playlists: List<Playlist>?,
    navController: NavController,
    onFilterSelected: (LibraryFilter) -> Unit,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    filterContent: @Composable () -> Unit,
) {
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

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
            key = "liked_songs",
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.liked_songs)) },
                supportingContent = { Text(stringResource(R.string.filter_playlists)) },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.favorite),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("local_playlist/${PlaylistEntity.LIKED_PLAYLIST_ID}")
                    }
                    .animateItem()
            )
        }

        item(
            key = "downloaded_songs",
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.filter_downloaded)) },
                supportingContent = { Text(stringResource(R.string.filter_playlists)) },
                leadingContent = {
                    Box(
                        modifier = Modifier
                            .size(ListThumbnailSize)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("local_playlist/${PlaylistEntity.DOWNLOADED_PLAYLIST_ID}")
                    }
                    .animateItem()
            )
        }

        playlists?.let { playlists ->
            if (playlists.isEmpty()) {
                item(
                    key = "playlists_empty"
                ) {
                    EmptyPlaceholder(
                        icon = R.drawable.queue_music,
                        text = stringResource(R.string.library_playlist_empty),
                    )
                }
            }

            items(
                items = playlists,
                key = { "playlist_${it.id}" },
            ) { playlist ->
                ListItem(
                    headlineContent = { Text(playlist.playlist.name) },
                    supportingContent = { Text(stringResource(R.string.filter_playlists)) },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    PlaylistMenu(
                                        playlist = playlist,
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
                            navController.navigate("local_playlist/${playlist.id}")
                        }
                        .animateItem()
                )
            }
        }

        albums?.let { albums ->
            if (albums.isEmpty()) {
                item(
                    key = "albums_empty"
                ) {
                    EmptyPlaceholder(
                        icon = R.drawable.album,
                        text = stringResource(R.string.library_album_empty),
                    )
                }
            }

            items(
                items = albums,
                key = { "album_${it.id}" },
            ) { album ->
                ListItem(
                    headlineContent = { Text(album.album.title) },
                    supportingContent = { Text(stringResource(R.string.filter_albums)) },
                    leadingContent = {
                        AsyncImage(
                            model = album.album.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    },
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
                        .clickable {
                            navController.navigate("album/${album.id}")
                        }
                        .animateItem()
                )
            }
        }

        artists?.let { artists ->
            if (artists.isEmpty()) {
                item(
                    key = "artists_empty"
                ) {
                    EmptyPlaceholder(
                        icon = R.drawable.artist,
                        text = stringResource(R.string.library_artist_empty),
                    )
                }
            }

            items(
                items = artists,
                key = { "artist_${it.id}" },
            ) { artist ->
                ListItem(
                    headlineContent = { Text(artist.artist.name) },
                    supportingContent = { Text(stringResource(R.string.filter_artists)) },
                    leadingContent = {
                        AsyncImage(
                            model = artist.artist.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(ListThumbnailSize)
                                .clip(CircleShape)
                        )
                    },
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
                        .animateItem()
                )
            }
        }
    }
}
