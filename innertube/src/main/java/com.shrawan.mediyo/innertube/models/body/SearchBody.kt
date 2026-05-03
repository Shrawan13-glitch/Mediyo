package com.shrawan.mediyo.innertube.models.body

import com.shrawan.mediyo.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)
