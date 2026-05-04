package com.shrawan.mediyo.innertube.models.body

import com.shrawan.mediyo.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context,
    val params: String,
)
