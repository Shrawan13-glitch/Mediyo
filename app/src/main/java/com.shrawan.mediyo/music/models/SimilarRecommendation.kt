package com.shrawan.mediyo.music.models

import com.shrawan.mediyo.innertube.models.YTItem
import com.shrawan.mediyo.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
