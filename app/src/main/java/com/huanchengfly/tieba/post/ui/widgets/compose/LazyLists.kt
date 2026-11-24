package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.contentMaxWidth

/**
 * 列表在宽屏下的居中收窄容器。
 *
 * [maxContentWidth] 由窗口尺寸类推导（见 [contentMaxWidth]）：Compact / Medium 返回 null，
 * 列表保持原样铺满；Expanded（平板横屏、桌面窗口）收窄到固定宽度并居中，
 * 避免单列信息流横跨整个窗口。
 */
@Composable
private fun AdaptiveListWidthContainer(
    modifier: Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val maxWidth: Dp? = LocalWindowSizeClass.current.contentMaxWidth
    if (maxWidth == null) {
        content(modifier)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
            content(
                Modifier
                    .fillMaxHeight()
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
            )
        }
    }
}

/**
 * 列表统一避让悬浮栏。
 *
 * 页面正文铺满整屏（见 [MyScaffold]），所以「不让最后一条被胶囊压住」只能靠
 * [LocalBarsContentPadding] 换算成 `contentPadding`——**不能**再用 `Modifier.padding`
 * 缩视口，那会让栏所在的那几条永远不显示滚动内容（看起来就是一圈黑边）。
 * 这里默认把两层栏的占位合并进来，页面只需传自己的装饰性 padding。
 */
@Composable
private fun barsAwareContentPadding(contentPadding: PaddingValues): PaddingValues {
    val bars = LocalBarsContentPadding.current
    if (bars == PaddingValues(0.dp)) return contentPadding
    val layoutDirection = LocalLayoutDirection.current
    return remember(contentPadding, bars, layoutDirection) {
        PaddingValues(
            start = contentPadding.calculateStartPadding(layoutDirection) +
                    bars.calculateStartPadding(layoutDirection),
            top = contentPadding.calculateTopPadding() + bars.calculateTopPadding(),
            end = contentPadding.calculateEndPadding(layoutDirection) +
                    bars.calculateEndPadding(layoutDirection),
            bottom = contentPadding.calculateBottomPadding() + bars.calculateBottomPadding(),
        )
    }
}

@Composable
fun MyLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    onGlobalEvent<GlobalEvent.ScrollToTop> {
        state.animateScrollToItem(0)
    }

    val resolvedContentPadding = barsAwareContentPadding(contentPadding)
    AdaptiveListWidthContainer(modifier = modifier) { listModifier ->
        LazyColumn(
            modifier = listModifier,
            state = state,
            contentPadding = resolvedContentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
    }
}

@Composable
fun MyLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyGridScope.() -> Unit,
) {
    onGlobalEvent<GlobalEvent.ScrollToTop> {
        state.animateScrollToItem(0)
    }

    val resolvedContentPadding = barsAwareContentPadding(contentPadding)
    AdaptiveListWidthContainer(modifier = modifier) { gridModifier ->
        LazyVerticalGrid(
            columns = columns,
            modifier = gridModifier,
            state = state,
            contentPadding = resolvedContentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
    }
}
