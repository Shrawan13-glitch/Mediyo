package com.shrawan.mediyo.music.ui.screens.library

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.shrawan.mediyo.music.LocalPlayerAwareWindowInsets
import com.shrawan.mediyo.R
import com.shrawan.mediyo.music.constants.CONTENT_TYPE_ALBUM
import com.shrawan.mediyo.music.constants.CONTENT_TYPE_HEADER
import com.shrawan.mediyo.music.ui.component.AlbumListItem
import com.shrawan.mediyo.music.ui.component.EmptyPlaceholder
import com.shrawan.mediyo.music.ui.component.LocalMenuState
import com.shrawan.mediyo.music.ui.menu.AlbumMenu
import com.shrawan.mediyo.music.viewmodels.LibraryAlbumsViewModel

@Composable
fun LibraryAlbumsScreen(
    navController: NavController,
    viewModel: LibraryAlbumsViewModel = hiltViewModel(),
    filterContent: (@Composable () -> Unit)? = null,
) {
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    val albums by viewModel.allAlbums.collectAsState()

    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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

            albums?.let { albums ->
                if (albums.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.album,
                            text = stringResource(R.string.library_album_empty),
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                items(
                    items = albums,
                    key = { it.id },
                    contentType = { CONTENT_TYPE_ALBUM }
                ) { album ->
                    AlbumListItem(
                        album = album,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = album,
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
                            .combinedClickable {
                                navController.navigate("album/${album.id}")
                            }
                            .animateItem()
                    )
                }
            }
        }
    }
}
