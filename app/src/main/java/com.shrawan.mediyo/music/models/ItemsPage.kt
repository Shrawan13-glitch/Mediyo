package com.shrawan.mediyo.music.models

import com.shrawan.mediyo.innertube.models.YTItem

data class ItemsPage(
    val items: List<YTItem>,
    val continuation: String?,
)
