package com.huanchengfly.tieba.post.ui.page.search.thread

import org.junit.Assert.assertSame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchThreadStateRegressionTest {
    @Test
    fun `pagination cannot change a list while it is refreshing`() {
        val state = SearchThreadUiState(
            keyword = "new query",
            isRefreshing = true,
            currentPage = 1,
            hasMore = false,
        )
        val result = SearchThreadPartialChange.LoadMore.Success(
            data = emptyList(),
            page = 2,
            hasMore = true,
        ).reduce(state)
        assertSame(state, result)
    }

    @Test
    fun `new query discards previous query pagination`() {
        val state = SearchThreadUiState(keyword = "B", isRefreshing = false, requestId = 2L)
        val result = SearchThreadPartialChange.LoadMore.Success(emptyList(), 2, true, 1L).reduce(state)
        assertSame(state, result)
    }

    @Test
    fun `previous query success and failure cannot replace active state`() {
        val state = SearchThreadUiState(keyword = "B", isRefreshing = false, requestId = 2L)
        assertSame(state, SearchThreadPartialChange.Refresh.Success("A", emptyList(), true, 0, 1L).reduce(state))
        assertSame(state, SearchThreadPartialChange.Refresh.Failure(IllegalStateException(), 1L).reduce(state))
        assertSame(state, SearchThreadPartialChange.LoadMore.Failure(IllegalStateException(), 1L).reduce(state))
    }

    @Test
    fun `refresh resets pending pagination and advances request identity`() {
        val state = SearchThreadUiState(isLoadingMore = true, requestId = 1L)
        val result = SearchThreadPartialChange.Refresh.Start(2L).reduce(state)
        assertEquals(2L, result.requestId)
        assertFalse(result.isLoadingMore)
    }

    @Test
    fun `active pagination advances only the next page`() {
        val state = SearchThreadUiState(isRefreshing = false, requestId = 2L)
        val result = SearchThreadPartialChange.LoadMore.Success(emptyList(), 2, false, 2L).reduce(state)
        assertEquals(2, result.currentPage)
        assertFalse(result.hasMore)
        assertSame(result, SearchThreadPartialChange.LoadMore.Success(emptyList(), 2, false, 2L).reduce(result))
    }
}
