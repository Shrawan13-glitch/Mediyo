package com.shrawan.mediyo.innertube.pages

import com.shrawan.mediyo.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
