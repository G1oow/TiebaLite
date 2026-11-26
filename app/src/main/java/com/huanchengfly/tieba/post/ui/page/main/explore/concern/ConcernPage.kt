package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.hasAgree
import com.huanchengfly.tieba.post.arch.CommonUiEvent.ScrollToTop.bindScrollToTopEvent
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedListDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.PullRefresh
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ConcernPage(
    viewModel: ConcernViewModel = pageViewModel()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ConcernUiIntent.Refresh)
        viewModel.initialized = true
    }
    val navigator = LocalNavigator.current
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isRefreshing,
        initial = false
    )
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::hasMore,
        initial = true
    )
    val nextPageTag by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::nextPageTag,
        initial = ""
    )
    val data by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::data,
        initial = persistentListOf()
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::error,
        initial = null
    )

    val isEmpty by remember { derivedStateOf { data.isEmpty() } }
    val isError by remember { derivedStateOf { error != null } }

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "concern" }
    ) {
        viewModel.send(ConcernUiIntent.Refresh)
    }

    val lazyListState = rememberLazyListState()
    viewModel.bindScrollToTopEvent(lazyListState = lazyListState)

    StateScreen(
        modifier = Modifier.fillMaxSize(),
        isEmpty = isEmpty,
        isError = isError,
        isLoading = isRefreshing,
        onReload = { viewModel.send(ConcernUiIntent.Refresh) },
        emptyScreen = {
            TipScreen(
                title = { Text(text = stringResource(id = R.string.title_concern_empty)) },
                message = { Text(text = stringResource(id = R.string.summary_concern_empty)) },
                image = {
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.lottie_empty_box)
                    )
                    LottieAnimation(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f)
                    )
                },
                actions = {
                    Button(onClick = { reload() }) {
                        Text(text = stringResource(id = R.string.btn_refresh))
                    }
                },
                scrollable = false,
            )
        },
        errorScreen = {
            error?.let { ErrorScreen(error = it) }
        },
    ) {
        PullRefresh(
            refreshing = isRefreshing,
            onRefresh = { viewModel.send(ConcernUiIntent.Refresh) },
        ) {
            LoadMoreLayout(
                isLoading = isLoadingMore,
                onLoadMore = { viewModel.send(ConcernUiIntent.LoadMore(nextPageTag)) },
                loadEnd = !hasMore,
                lazyListState = lazyListState,
                isEmpty = isEmpty,
            ) {
                MyLazyColumn(
                    state = lazyListState,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    itemsIndexed(
                        items = data,
                        key = { _, item -> "${item.recommendType}_${item.postData?.id}_${item.threadList?.id}" },
                        contentType = { _, item -> item.recommendType }
                    ) { index, item ->
                        Container {
                            // 原来只渲染 recommendType==1，其余类型静默丢弃（且 threadList!!
                            // 遇到空值会直接崩）。现改为有 threadList 就按帖子卡片渲染：
                            // 回复型动态（postData）也落到其所属帖子卡片上，不再丢内容。
                            val thread = item.threadList
                            if (thread != null) {
                                Column {
                                    FeedCard(
                                        item = wrapImmutable(thread),
                                        onClick = {
                                            navigator.navigate("thread/${it.threadId}")
                                        },
                                        onClickReply = {
                                            navigator.navigate("thread/${it.threadId}")
                                        },
                                        onAgree = {
                                            viewModel.send(
                                                ConcernUiIntent.Agree(
                                                    it.threadId,
                                                    it.firstPostId,
                                                    it.hasAgree
                                                )
                                            )
                                        },
                                        onClickForum = { navigator.navigate("forum/${it.name}") },
                                        onClickUser = { navigator.navigate("user/${it.id}") },
                                    )
                                    if (index < data.size - 1) {
                                        FeedListDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 2.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}