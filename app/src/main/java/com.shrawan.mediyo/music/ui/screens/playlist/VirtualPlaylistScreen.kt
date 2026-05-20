package com.shrawan.mediyo.music.ui.screens.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shrawan.mediyo.music.LocalPlayerAwareWindowInsets
import com.shrawan.mediyo.music.LocalPlayerConnection
import com.shrawan.mediyo.R
import com.shrawan.mediyo.music.extensions.toMediaItem
import com.shrawan.mediyo.music.extensions.togglePlayPause
import com.shrawan.mediyo.music.playback.queues.ListQueue
import com.shrawan.mediyo.music.ui.component.EmptyPlaceholder
import com.shrawan.mediyo.music.ui.component.LocalMenuState
import com.shrawan.mediyo.music.ui.component.SongListItem
import com.shrawan.mediyo.music.ui.menu.SongMenu
import com.shrawan.mediyo.music.viewmodels.VirtualPlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: VirtualPlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val title = viewModel.title
    val songs by viewModel.songs.collectAsState()
    val lazyListState = rememberLazyListState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            songs?.let { songs ->
                if (songs.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.music_note,
                            text = stringResource(R.string.library_song_empty),
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                itemsIndexed(
                    items = songs,
                    key = { _, item -> item.id },
                    contentType = { _, _ -> "song" }
                ) { index, song ->
                    SongListItem(
                        song = song,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
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
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = title,
                                            items = songs.map { it.toMediaItem() },
                                            startIndex = index
                                        )
                                    )
                                }
                            }
                            .animateItem()
                    )
                }
            }
        }

        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                IconButton(
                    onClick = {
                        navController.navigateUp()
                    }
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                }
            },
            scrollBehavior = scrollBehavior
        )
    }
}
