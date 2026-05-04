package com.shrawan.mediyo.innertube.pages

import com.shrawan.mediyo.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
