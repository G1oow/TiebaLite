package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator

/**
 * 全站统一的下拉刷新容器（台账 D6 / 阶段 3）。
 *
 * 栏是悬浮 overlay、正文铺满整屏（AGENTS.md §5.3），所以 M3 默认「贴内容区顶边」的
 * 指示器会被不透明顶栏整块盖住——下拉只剩「等一会儿内容变了」，中间没有任何反馈。
 * 这里把指示器整体下移到栏的下沿：偏移取 [LocalBarsContentPadding] 的 top，
 * 即 [MyScaffold] 实测的顶栏完整高度（状态栏底色带 + 标题行 + 次级槽）。
 *
 * 指示器观感按 D6 使用 M3 预设（下拉时圆形指示器从栏下沿滑出），不再逐页自绘。
 * `LocalBarsContentPadding` 未提供时（页面在 MyScaffold 之外）退化为贴顶，与 M3 默认一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    // 顶栏高度随次级槽（页签 / 搜索框）变化，取组合局部值才能跟着更新。
    val barsTopPadding = LocalBarsContentPadding.current.calculateTopPadding()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        enabled = enabled,
        indicator = {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = barsTopPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                PullToRefreshDefaults.Indicator(
                    state = state,
                    isRefreshing = refreshing,
                    containerColor = ExtendedTheme.colors.pullRefreshIndicator,
                    color = ExtendedTheme.colors.primary,
                    // 停在槽位顶端（栏下沿）就够：M3 默认会让指示器在栏下再空出 40dp。
                    // 40dp = M3 指示器容器尺寸（SpinnerContainerSize，未公开），按视觉常量固定。
                    maxDistance = RefreshIndicatorContainerSize,
                )
            }
        },
        content = content,
    )
}

/** M3 下拉指示器的容器直径：使指示器停在槽位顶端（顶栏下沿），不多空一截。 */
private val RefreshIndicatorContainerSize = 40.dp
