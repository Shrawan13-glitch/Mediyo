package com.shrawan.mediyo.music.ui.screens.library

import androidx.compose.foundation.clickable
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
import com.shrawan.mediyo.music.constants.CONTENT_TYPE_ARTIST
import com.shrawan.mediyo.music.constants.CONTENT_TYPE_HEADER
import com.shrawan.mediyo.music.ui.component.ArtistListItem
import com.shrawan.mediyo.music.ui.component.EmptyPlaceholder
import com.shrawan.mediyo.music.ui.component.LocalMenuState
import com.shrawan.mediyo.music.ui.menu.ArtistMenu
import com.shrawan.mediyo.music.viewmodels.LibraryArtistsViewModel

@Composable
fun LibraryArtistsScreen(
    navController: NavController,
    viewModel: LibraryArtistsViewModel = hiltViewModel(),
    filterContent: (@Composable () -> Unit)? = null,
) {
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    val artists by viewModel.allArtists.collectAsState()

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

            artists?.let { artists ->
                if (artists.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.artist,
                            text = stringResource(R.string.library_artist_empty),
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                items(
                    items = artists,
                    key = { it.id },
                    contentType = { CONTENT_TYPE_ARTIST }
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
                                navController.navigate("artist/${artist.id}")
                            }
                            .animateItem()
                    )
                }
            }
        }
    }
}
