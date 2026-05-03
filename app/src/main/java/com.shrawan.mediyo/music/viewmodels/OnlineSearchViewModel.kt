package com.shrawan.mediyo.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shrawan.mediyo.R
import com.shrawan.mediyo.innertube.YouTube
import com.shrawan.mediyo.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.shrawan.mediyo.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.shrawan.mediyo.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.shrawan.mediyo.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.shrawan.mediyo.innertube.YouTube.SearchFilter.Companion.FILTER_PODCAST
import com.shrawan.mediyo.innertube.YouTube.SearchFilter.Companion.FILTER_PROFILE
import com.shrawan.mediyo.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.shrawan.mediyo.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.shrawan.mediyo.innertube.models.filterExplicit
import com.shrawan.mediyo.innertube.pages.SearchSummaryPage
import com.shrawan.mediyo.innertube.pages.SearchSummary
import com.shrawan.mediyo.music.constants.HideExplicitKey
import com.shrawan.mediyo.music.models.ItemsPage
import com.shrawan.mediyo.music.utils.AppLogs
import com.shrawan.mediyo.music.utils.dataStore
import com.shrawan.mediyo.music.utils.get
import com.shrawan.mediyo.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.net.URLDecoder
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private data class SectionSpec(
        val title: String,
        val filter: YouTube.SearchFilter,
    )

    private data class SectionLoadResult(
        val summary: SearchSummary?,
        val error: Throwable? = null,
    )

    val query = try {
        URLDecoder.decode(savedStateHandle.get<String>("query")!!, "UTF-8")
    } catch (e: IllegalArgumentException) {
        savedStateHandle.get<String>("query")!!
    }
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
    var summaryError by mutableStateOf<Throwable?>(null)
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    private suspend fun loadSummaryPage() {
        if (summaryPage != null) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val specs = listOf(
            SectionSpec(context.getString(R.string.filter_songs), FILTER_SONG),
            SectionSpec(context.getString(R.string.filter_videos), FILTER_VIDEO),
            SectionSpec(context.getString(R.string.filter_albums), FILTER_ALBUM),
            SectionSpec(context.getString(R.string.filter_artists), FILTER_ARTIST),
            SectionSpec(context.getString(R.string.filter_featured_playlists), FILTER_FEATURED_PLAYLIST),
            SectionSpec(context.getString(R.string.filter_community_playlists), FILTER_COMMUNITY_PLAYLIST),
            SectionSpec(context.getString(R.string.filter_podcasts), FILTER_PODCAST),
            SectionSpec(context.getString(R.string.filter_profiles), FILTER_PROFILE),
        )
        val results = supervisorScope {
            specs.map { spec ->
                async {
                    runCatching {
                        val items = YouTube.search(query, spec.filter)
                            .getOrThrow()
                            .items
                            .distinctBy { it.id }
                            .filterExplicit(hideExplicit)
                            .take(5)
                        SectionLoadResult(
                            summary = items.takeIf { it.isNotEmpty() }?.let {
                                SearchSummary(title = spec.title, items = it)
                            }
                        )
                    }.getOrElse { error ->
                        AppLogs.declare(
                            context,
                            "Search",
                            "All tab section failed for query=\"$query\" filter=${spec.filter.value}",
                            error
                        )
                        reportException(error)
                        SectionLoadResult(summary = null, error = error)
                    }
                }
            }.map { it.await() }
        }
        val summaries = results.mapNotNull { it.summary }
        val firstError = results.firstOrNull { it.error != null }?.error
        val failedCompletely = summaries.isEmpty() && firstError != null
        summaryPage = if (failedCompletely) null else SearchSummaryPage(summaries)
        summaryError = if (failedCompletely) firstError else null
    }

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    loadSummaryPage()
                } else {
                    if (viewStateMap[filter.value] == null) {
                        YouTube.search(query, filter)
                            .onSuccess { result ->
                                summaryError = null
                                viewStateMap[filter.value] = ItemsPage(
                                    result.items
                                        .distinctBy { it.id }
                                        .filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                                    result.continuation
                                )
                            }
                            .onFailure {
                                summaryError = it
                                AppLogs.declare(context, "Search", "Filtered search failed for query=\"$query\" filter=${filter.value}", it)
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    fun reloadSummaryPage() {
        summaryPage = null
        summaryError = null
        viewModelScope.launch {
            loadSummaryPage()
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val viewState = viewStateMap[filter] ?: return@launch
            val continuation = viewState.continuation
            if (continuation != null) {
                val searchResult = YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                viewStateMap[filter] = ItemsPage((viewState.items + searchResult.items).distinctBy { it.id }, searchResult.continuation)
            }
        }
    }
}
