package com.huanchengfly.tieba.post.ui.page.history.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.fromJson
import com.huanchengfly.tieba.post.models.ThreadHistoryInfoBean
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import com.huanchengfly.tieba.post.ui.widgets.compose.stickyHeaderTopInset
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LoadMoreLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalSnackbarHostState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.UserHeader
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.HistoryUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryListPage(
    type: Int,
    viewModel: HistoryListViewModel = if (type == HistoryUtil.TYPE_THREAD) pageViewModel<ThreadHistoryListViewModel>() else pageViewModel<ForumHistoryListViewModel>()
) {
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(HistoryListUiIntent.Refresh)
        viewModel.initialized = true
    }
    onGlobalEvent<HistoryListUiEvent.DeleteAll> {
        viewModel.send(HistoryListUiIntent.DeleteAll)
    }
    val isLoadingMore by viewModel.uiState.collectPartialAsState(
        prop1 = HistoryListUiState::isLoadingMore,
        initial = false
    )
    val hasMore by viewModel.uiState.collectPartialAsState(
        prop1 = HistoryListUiState::hasMore,
        initial = true
    )
    val currentPage by viewModel.uiState.collectPartialAsState(
        prop1 = HistoryListUiState::currentPage,
        initial = 0
    )
    val todayHistoryData by viewModel.uiState.collectPartialAsState(
        prop1 = HistoryListUiState::todayHistoryData,
        initial = emptyList()
    )
    val beforeHistoryData by viewModel.uiState.collectPartialAsState(
        prop1 = HistoryListUiState::beforeHistoryData,
        initial = emptyList()
    )

    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    viewModel.onEvent<HistoryListUiEvent.Delete.Failure> {
        snackbarHostState.showSnackbar(
            context.getString(
                R.string.delete_history_failure,
                it.errorMsg
            )
        )
    }
    viewModel.onEvent<HistoryListUiEvent.Delete.Success> {
        snackbarHostState.showSnackbar(context.getString(R.string.delete_history_success))
    }
    val lazyListState = rememberLazyListState()
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LoadMoreLayout(
            isLoading = isLoadingMore,
            onLoadMore = { viewModel.send(HistoryListUiIntent.LoadMore(currentPage + 1)) },
            loadEnd = !hasMore,
            lazyListState = lazyListState,
            isEmpty = todayHistoryData.isEmpty() && beforeHistoryData.isEmpty()
        ) {
            MyLazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                state = lazyListState
            ) {
                if (todayHistoryData.isNotEmpty()) {
                    stickyHeader(key = "TodayHistoryHeader") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExtendedTheme.colors.background)
                                .padding(top = stickyHeaderTopInset())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Chip(
                                text = stringResource(id = R.string.title_history_today),
                                invertColor = true
                            )
                        }
                    }
                    items(
                        items = todayHistoryData,
                        key = { it.id }
                    ) { info ->
                        HistoryItem(
                            info,
                            onDelete = {
                                viewModel.send(HistoryListUiIntent.Delete(it.id))
                            },
                            onClick = {
                                when (it.type) {
                                    HistoryUtil.TYPE_FORUM -> {
                                        navigator.navigate("forum/${it.data}")
                                    }

                                    HistoryUtil.TYPE_THREAD -> {
                                        val extra =
                                            if (it.extras != null) it.extras.fromJson<ThreadHistoryInfoBean>() else null
                                        navigator.navigate("thread/${it.data.toLong()}")
                                    }
                                }
                            }
                        )
                    }
                }
                if (beforeHistoryData.isNotEmpty()) {
                    stickyHeader(key = "BeforeHistoryHeader") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ExtendedTheme.colors.background)
                                .padding(top = stickyHeaderTopInset())
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Chip(text = stringResource(id = R.string.title_history_before))
                        }
                    }
                    items(
                        items = beforeHistoryData,
                        key = { it.id }
                    ) { info ->
                        HistoryItem(
                            info,
                            onDelete = {
                                viewModel.send(HistoryListUiIntent.Delete(it.id))
                            },
                            onClick = {
                                when (it.type) {
                                    HistoryUtil.TYPE_FORUM -> {
                                        navigator.navigate("forum/${it.data}")
                                    }

                                    HistoryUtil.TYPE_THREAD -> {
                                        val extra =
                                            if (it.extras != null) it.extras.fromJson<ThreadHistoryInfoBean>() else null
                                        navigator.navigate("thread/${it.data.toLong()}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        // 空态兜底：数据层返回 0 条时列表什么都不渲染，整页看起来就是空白（台账「已知问题」）。
        // 用 !hasMore 当「Refresh 已落过至少一次」的判据——初始 hasMore = true，首次 Refresh 会按
        // 实际条数写回，所以加载过程中不会闪一下占位。
        if (todayHistoryData.isEmpty() && beforeHistoryData.isEmpty() && !hasMore && !isLoadingMore) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                TipScreen(
                    title = { Text(text = stringResource(id = R.string.title_no_history)) },
                    message = { Text(text = stringResource(id = R.string.summary_no_history)) },
                    image = {
                        val composition by rememberLottieComposition(
                            LottieCompositionSpec.RawRes(R.raw.lottie_empty_box)
                        )
                        LottieAnimation(
                            composition = composition,
                            iterations = LottieConstants.IterateForever,
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .fillMaxWidth()
                                .aspectRatio(2.5f)
                        )
                    },
                    scrollable = false,
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(
    info: History,
    modifier: Modifier = Modifier,
    onClick: (History) -> Unit = {},
    onDelete: (History) -> Unit = {},
) {
    val menuState = rememberMenuState()
    LongClickMenu(
        menuContent = {
            DropdownMenuItem(onClick = {
                onDelete(info)
                menuState.expanded = false
            }) {
                Text(text = stringResource(id = R.string.title_delete))
            }
        },
        menuState = menuState,
        onClick = { onClick(info) }
    ) {
        Column(
            modifier = modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UserHeader(
                avatar = {
                    Avatar(
                        data = info.avatar,
                        size = Sizes.Small,
                        contentDescription = null
                    )
                },
                name = {
                    Text(
                        text = (if (info.type == HistoryUtil.TYPE_THREAD) info.username else info.title)
                            ?: ""
                    )
                },
            ) {
                Text(
                    text = DateTimeUtils.getRelativeTimeString(
                        LocalContext.current,
                        info.timestamp
                    ),
                    fontSize = 15.sp,
                    color = ExtendedTheme.colors.text,
                )
            }
            if (info.type == HistoryUtil.TYPE_THREAD) {
                Text(
                    text = info.title,
                    fontSize = 15.sp,
                    color = ExtendedTheme.colors.text,
                )
            }
        }
    }
}