package com.shrawan.mediyo.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicShelfRenderer(
    val title: Runs?,
    val contents: List<Content>?,
    val bottomEndpoint: NavigationEndpoint?,
    val moreContentButton: Button?,
    val continuations: List<Continuation>?,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val continuationItemRenderer: ContinuationItemRenderer? = null,
    )
}

fun List<MusicShelfRenderer.Content>.getItems(): List<MusicResponsiveListItemRenderer> =
    mapNotNull { it.musicResponsiveListItemRenderer }

fun List<MusicShelfRenderer.Content>.getContinuation(): String? =
    firstOrNull { it.continuationItemRenderer != null }
        ?.continuationItemRenderer
        ?.continuationEndpoint
        ?.continuationCommand
        ?.token

fun List<Continuation>.getContinuation() =
    firstOrNull()?.nextContinuationData?.continuation
