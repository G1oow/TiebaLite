package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    AdaptiveListWidthContainer(modifier = modifier) { listModifier ->
        LazyColumn(
            modifier = listModifier,
            state = state,
            contentPadding = contentPadding,
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

    AdaptiveListWidthContainer(modifier = modifier) { gridModifier ->
        LazyVerticalGrid(
            columns = columns,
            modifier = gridModifier,
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
            content = content
        )
    }
}
