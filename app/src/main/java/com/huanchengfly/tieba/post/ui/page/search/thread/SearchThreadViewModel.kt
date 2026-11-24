package com.huanchengfly.tieba.post.ui.page.search.thread

import androidx.compose.runtime.Stable
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.arch.BaseViewModel
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.PartialChange
import com.huanchengfly.tieba.post.arch.PartialChangeProducer
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiIntent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.wrapImmutable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@Stable
@HiltViewModel
class SearchThreadViewModel @Inject constructor() :
    BaseViewModel<SearchThreadUiIntent, SearchThreadPartialChange, SearchThreadUiState, SearchThreadUiEvent>() {
    override fun createInitialState(): SearchThreadUiState = SearchThreadUiState()

    override fun createPartialChangeProducer(): PartialChangeProducer<SearchThreadUiIntent, SearchThreadPartialChange, SearchThreadUiState> =
        SearchThreadPartialChangeProducer

    private object SearchThreadPartialChangeProducer :
        PartialChangeProducer<SearchThreadUiIntent, SearchThreadPartialChange, SearchThreadUiState> {
        @OptIn(ExperimentalCoroutinesApi::class)
        override fun toPartialChangeFlow(intentFlow: Flow<SearchThreadUiIntent>): Flow<SearchThreadPartialChange> {
            var nextRequestId = 0L
            return intentFlow.filterIsInstance<SearchThreadUiIntent.Refresh>().flatMapLatest { query ->
                val requestId = ++nextRequestId
                flow {
                    emit(SearchThreadPartialChange.Refresh.Start(requestId))
                    var currentPage = 1
                    var hasMore = false
                    query.producePartialChange(requestId).collect { change ->
                        if (change is SearchThreadPartialChange.Refresh.Success) hasMore = change.hasMore
                        emit(change)
                    }
                    // 分页属于本次查询；新查询会取消整条流水线，包括尚未返回的旧分页。
                    emitAll(
                        intentFlow.filterIsInstance<SearchThreadUiIntent.LoadMore>()
                            .filter { it.keyword == query.keyword && it.sortType == query.sortType &&
                                it.page == currentPage && hasMore }
                            .flatMapConcat { request ->
                                flow {
                                    emit(SearchThreadPartialChange.LoadMore.Start(requestId))
                                    request.producePartialChange(requestId).collect { change ->
                                        if (change is SearchThreadPartialChange.LoadMore.Success) {
                                            currentPage = change.page
                                            hasMore = change.hasMore
                                        }
                                        emit(change)
                                    }
                                }
                            }
                    )
                }
            }
        }

        private fun SearchThreadUiIntent.Refresh.producePartialChange(requestId: Long): Flow<SearchThreadPartialChange.Refresh> =
            TiebaApi.getInstance().searchThreadFlow(keyword, 1, sortType)
                .map<SearchThreadBean, SearchThreadPartialChange.Refresh> {
                    val threadList = it.data.postList
                    SearchThreadPartialChange.Refresh.Success(
                        keyword = keyword,
                        data = threadList,
                        hasMore = it.data.hasMore == 1,
                        sortType = sortType,
                        requestId = requestId,
                    )
                }
                .catch { emit(SearchThreadPartialChange.Refresh.Failure(it, requestId)) }

        private fun SearchThreadUiIntent.LoadMore.producePartialChange(requestId: Long): Flow<SearchThreadPartialChange.LoadMore> =
            TiebaApi.getInstance().searchThreadFlow(keyword, page + 1, sortType)
                .map<SearchThreadBean, SearchThreadPartialChange.LoadMore> {
                    val threadList = it.data.postList
                    SearchThreadPartialChange.LoadMore.Success(
                        data = threadList,
                        page = page + 1,
                        hasMore = it.data.hasMore == 1,
                        requestId = requestId,
                    )
                }
                .catch { emit(SearchThreadPartialChange.LoadMore.Failure(it, requestId)) }

    }
}

sealed interface SearchThreadUiIntent : UiIntent {
    data class Refresh(val keyword: String, val sortType: Int) : SearchThreadUiIntent

    data class LoadMore(val keyword: String, val page: Int, val sortType: Int) :
        SearchThreadUiIntent
}

sealed interface SearchThreadPartialChange : PartialChange<SearchThreadUiState> {
    sealed class Refresh : SearchThreadPartialChange {
        override fun reduce(oldState: SearchThreadUiState): SearchThreadUiState = when (this) {
            is Start -> oldState.copy(
                isRefreshing = true, isLoadingMore = false, error = null, requestId = requestId
            )
            is Success -> if (requestId != oldState.requestId) oldState else oldState.copy(
                isRefreshing = false,
                error = null,
                data = data.toImmutableList(),
                currentPage = 1,
                hasMore = hasMore,
                keyword = keyword,
                sortType = sortType
            )
            is Failure -> if (requestId != oldState.requestId) oldState else
                oldState.copy(isRefreshing = false, error = error.wrapImmutable())
        }

        data class Start(val requestId: Long = 0L) : Refresh()
        data class Success(
            val keyword: String,
            val data: List<SearchThreadBean.ThreadInfoBean>,
            val hasMore: Boolean,
            val sortType: Int,
            val requestId: Long = 0L,
        ) : Refresh()
        data class Failure(val error: Throwable, val requestId: Long = 0L) : Refresh()
    }

    sealed class LoadMore : SearchThreadPartialChange {
        override fun reduce(oldState: SearchThreadUiState): SearchThreadUiState = when (this) {
            is Start -> if (requestId != oldState.requestId || oldState.isRefreshing) oldState else
                oldState.copy(isLoadingMore = true, error = null)
            is Success -> if (requestId != oldState.requestId || oldState.isRefreshing ||
                page != oldState.currentPage + 1) oldState else oldState.copy(
                isLoadingMore = false,
                error = null,
                data = (oldState.data + data).toImmutableList(),
                currentPage = page,
                hasMore = hasMore
            )
            is Failure -> if (requestId != oldState.requestId) oldState else
                oldState.copy(isLoadingMore = false, error = error.wrapImmutable())
        }

        data class Start(val requestId: Long = 0L) : LoadMore()
        data class Success(
            val data: List<SearchThreadBean.ThreadInfoBean>,
            val page: Int,
            val hasMore: Boolean,
            val requestId: Long = 0L,
        ) : LoadMore()
        data class Failure(val error: Throwable, val requestId: Long = 0L) : LoadMore()
    }
}

data class SearchThreadUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: ImmutableHolder<Throwable>? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = true,
    val keyword: String = "",
    val data: ImmutableList<SearchThreadBean.ThreadInfoBean> = persistentListOf(),
    val sortType: Int = SearchThreadSortType.SORT_TYPE_NEWEST,
    val requestId: Long = 0L,
) : UiState

sealed interface SearchThreadUiEvent : UiEvent {
    data class SwitchSortType(val sortType: Int) : SearchThreadUiEvent
}

object SearchThreadSortType {
    const val SORT_TYPE_NEWEST = 5
    const val SORT_TYPE_OLDEST = 0
    const val SORT_TYPE_RELATIVE = 2
}