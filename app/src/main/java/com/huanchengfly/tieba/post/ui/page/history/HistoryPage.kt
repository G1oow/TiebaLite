package com.huanchengfly.tieba.post.ui.page.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListPage
import com.huanchengfly.tieba.post.ui.page.history.list.HistoryListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.SegmentedTab
import com.huanchengfly.tieba.post.ui.widgets.compose.SegmentedTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.HistoryUtil
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryPage(
    navigator: NavHostController
) {
    val pagerState = rememberPagerState { 2 }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current

    MyScaffold(
        backgroundColor = Color.Transparent,
        snackbarHostState = snackbarHostState,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_history),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.popBackStack() })
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            HistoryUtil.deleteAll()
                            emitGlobalEvent(HistoryListUiEvent.DeleteAll)
                            launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(
                                        R.string.toast_clear_success
                                    )
                                )
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(id = R.string.title_history_delete),
                            tint = ExtendedTheme.colors.onTopBar
                        )
                    }
                },
                content = {
                    SegmentedTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        pagerState = pagerState,
                        onPrimarySurface = true,
                        contentColor = ExtendedTheme.colors.onTopBar,
                        equalWidth = true,
                        minTabWidth = 96.dp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 6.dp),
                    ) {
                        SegmentedTab(
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            },
                            selectedContentColor = ExtendedTheme.colors.onTopBar,
                            unselectedContentColor = ExtendedTheme.colors.onTopBarSecondary,
                        ) {
                            Text(
                                text = stringResource(id = R.string.title_history_thread),
                                maxLines = 1
                            )
                        }
                        SegmentedTab(
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            selectedContentColor = ExtendedTheme.colors.onTopBar,
                            unselectedContentColor = ExtendedTheme.colors.onTopBarSecondary,
                        ) {
                            Text(
                                text = stringResource(id = R.string.title_history_forum),
                                maxLines = 1
                            )
                        }
                    }
                }
            )
        }
    ) {
        ProvideNavigator(navigator = navigator) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
                verticalAlignment = Alignment.Top,
                userScrollEnabled = true,
            ) {
                if (it == 0) {
                    HistoryListPage(type = HistoryUtil.TYPE_THREAD)
                } else {
                    HistoryListPage(type = HistoryUtil.TYPE_FORUM)
                }
            }
        }
    }
}