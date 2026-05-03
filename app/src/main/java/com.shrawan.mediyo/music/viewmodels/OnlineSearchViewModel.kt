package com.shrawan.mediyo.music.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shrawan.mediyo.innertube.YouTube
import com.shrawan.mediyo.innertube.models.filterExplicit
import com.shrawan.mediyo.innertube.pages.SearchSummaryPage
import com.shrawan.mediyo.music.constants.HideExplicitKey
import com.shrawan.mediyo.music.models.ItemsPage
import com.shrawan.mediyo.music.utils.AppLogs
import com.shrawan.mediyo.music.utils.dataStore
import com.shrawan.mediyo.music.utils.get
import com.shrawan.mediyo.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject
import kotlin.collections.set

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
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
        YouTube.searchSummary(query)
            .onSuccess {
                summaryError = null
                summaryPage = it.filterExplicit(context.dataStore.get(HideExplicitKey, false))
            }
            .onFailure {
                summaryError = it
                AppLogs.declare(context, "Search", "All tab summary failed for query=\"$query\"", it)
                reportException(it)
            }
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
