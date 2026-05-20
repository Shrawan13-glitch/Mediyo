package com.shrawan.mediyo.music.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.shrawan.mediyo.music.LocalDatabase
import com.shrawan.mediyo.music.LocalPlayerAwareWindowInsets
import com.shrawan.mediyo.R
import com.shrawan.mediyo.music.constants.CONTENT_TYPE_HEADER
import com.shrawan.mediyo.music.constants.CONTENT_TYPE_PLAYLIST
import com.shrawan.mediyo.music.constants.PlaylistSortDescendingKey
import com.shrawan.mediyo.music.constants.PlaylistSortType
import com.shrawan.mediyo.music.constants.PlaylistSortTypeKey
import com.shrawan.mediyo.music.db.entities.PlaylistEntity
import com.shrawan.mediyo.music.ui.component.EmptyPlaceholder
import com.shrawan.mediyo.music.ui.component.LocalMenuState
import com.shrawan.mediyo.music.ui.component.PlaylistListItem
import com.shrawan.mediyo.music.ui.component.SortHeader
import com.shrawan.mediyo.music.ui.component.TextFieldDialog
import com.shrawan.mediyo.music.ui.menu.PlaylistMenu
import com.shrawan.mediyo.music.utils.rememberEnumPreference
import com.shrawan.mediyo.music.utils.rememberPreference
import com.shrawan.mediyo.music.viewmodels.LibraryPlaylistsViewModel

@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    filterContent: (@Composable () -> Unit)? = null,
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true)

    val playlists by viewModel.allPlaylists.collectAsState()

    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    var showAddPlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showAddPlaylistDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
            title = { Text(text = stringResource(R.string.create_playlist)) },
            onDismiss = { showAddPlaylistDialog = false },
            onDone = { playlistName ->
                database.query {
                    insert(
                        PlaylistEntity(
                            name = playlistName
                        )
                    )
                }
            }
        )
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        PlaylistSortType.NAME -> R.string.sort_by_name
                        PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            playlists?.let { playlists ->
                Text(
                    text = pluralStringResource(R.plurals.n_playlist, playlists.size, playlists.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(
                onClick = { showAddPlaylistDialog = true }
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null
                )
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) {
        if (filterContent != null) {
            item(
                key = "library_filter",
                contentType = CONTENT_TYPE_HEADER
            ) {
                filterContent()
            }
        }

        item(
            key = "header",
            contentType = CONTENT_TYPE_HEADER
        ) {
            headerContent()
        }

        item(
            key = "liked_songs",
            contentType = CONTENT_TYPE_PLAYLIST
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.liked_songs)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.favorite),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
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
            contentType = CONTENT_TYPE_PLAYLIST
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.filter_downloaded)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.queue_music,
                        text = stringResource(R.string.library_playlist_empty),
                        modifier = Modifier.animateItem()
                    )
                }
            }

            items(
                items = playlists,
                key = { it.id },
                contentType = { CONTENT_TYPE_PLAYLIST }
            ) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    PlaylistMenu(
                                        playlist = playlist,
                                        coroutineScope = coroutineScope,
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
                            navController.navigate("local_playlist/${playlist.id}")
                        }
                        .animateItem()
                )
            }
        }
    }
}


