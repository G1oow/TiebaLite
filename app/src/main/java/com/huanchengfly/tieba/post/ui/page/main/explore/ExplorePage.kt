package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.FilterAltOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.HotPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.Tab
import com.huanchengfly.tieba.post.ui.widgets.compose.TabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch


@Immutable
data class ExplorePageItem(
    val id: String,
    val name: @Composable (selected: Boolean) -> Unit,
    val content: @Composable () -> Unit,
)

/**
 * 顶栏的次级槽：居中的页签 + 行尾的「只看已关注的吧」开关。
 *
 * 开关只对「推荐」生效，所以只在推荐页签时出现；它改的是 `personalized_followed_only`
 * 这个偏好（推荐流刷新时按它过滤），点按后广播一次该页签的刷新事件。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ColumnScope.ExplorePageTab(
    pagerState: PagerState,
    pages: ImmutableList<ExplorePageItem>,
    followedOnly: Boolean,
    onToggleFollowedOnly: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val showFollowedOnlyToggle = pages[pagerState.currentPage].id == "personalized"

    Box(modifier = Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { PagerTabIndicator(pagerState) },
            divider = {},
            backgroundColor = Color.Transparent,
            contentColor = ExtendedTheme.colors.onTopBar,
            modifier = Modifier
                .align(Alignment.Center)
                .width(76.dp * pages.size),
        ) {
            pages.fastForEachIndexed { index, item ->
                Tab(
                    text = { item.name(pagerState.currentPage == index) },
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage == index) {
                                emitGlobalEvent(GlobalEvent.Refresh(item.id))
                            } else {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    },
                )
            }
        }
        if (showFollowedOnlyToggle) {
            IconButton(
                onClick = onToggleFollowedOnly,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
            ) {
                Icon(
                    imageVector = if (followedOnly) Icons.Rounded.FilterAlt else Icons.Rounded.FilterAltOff,
                    contentDescription = stringResource(id = R.string.personalized_followed_only),
                    tint = if (followedOnly) ExtendedTheme.colors.primary else ExtendedTheme.colors.onTopBar,
                )
            }
        }
    }
}

@Composable
private fun TabText(
    text: String,
    selected: Boolean
) {
    val style = MaterialTheme.typography.button.copy(
        letterSpacing = 0.75.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center
    )
    Text(text = text, style = style)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExplorePage() {
    val account = LocalAccount.current
    val navigator = LocalNavigator.current

    val loggedIn = remember(account) { account != null }

    val pages = remember {
        listOfNotNull(
            if (loggedIn) ExplorePageItem(
                "concern",
                { TabText(text = stringResource(id = R.string.title_concern), selected = it) },
                { ConcernPage() }
            ) else null,
            ExplorePageItem(
                "personalized",
                { TabText(text = stringResource(id = R.string.title_personalized), selected = it) },
                { PersonalizedPage() }
            ),
            ExplorePageItem(
                "hot",
                { TabText(text = stringResource(id = R.string.title_hot), selected = it) },
                { HotPage(navigator = navigator) }
            ),
        ).toImmutableList()
    }
    val pagerState = rememberPagerState(initialPage = if (account != null) 1 else 0) { pages.size }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    // 开关与推荐流共用同一个偏好：推荐流刷新时按它过滤，这里是它的顶栏入口。
    val followedOnly by rememberPreferenceAsState(
        key = booleanPreferencesKey("personalized_followed_only"),
        defaultValue = context.appPreferences.personalizedFollowedOnly,
    )
    val onToggleFollowedOnly: () -> Unit = {
        context.appPreferences.personalizedFollowedOnly = !followedOnly
        coroutineScope.emitGlobalEvent(GlobalEvent.Refresh("personalized"))
    }

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "explore" }
    ) {
        coroutineScope.emitGlobalEvent(GlobalEvent.Refresh(pages[pagerState.currentPage].id))
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.title_explore),
                navigationIcon = accountNavIconIfCompact(),
                actions = {
                    ActionItem(
                        icon = Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.title_search)
                    ) {
                        navigator.navigate("search")
                    }
                },
            ) {
                ExplorePageTab(
                    pagerState = pagerState,
                    pages = pages,
                    followedOnly = followedOnly,
                    onToggleFollowedOnly = onToggleFollowedOnly,
                )
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) { _ ->
        // 正文铺满整屏：栏占位由各页自己的 MyLazyColumn 依 LocalBarsContentPadding 合并，
        // 不能再塞进 pager 的 contentPadding，否则整页会被内缩、栏所在区域永远空白。
        LazyLoadHorizontalPager(
            contentPadding = PaddingValues(0.dp),
            state = pagerState,
            key = { pages[it].id },
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = true,
        ) {
            pages[it].content()
        }
    }
}