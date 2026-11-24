package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshDefaults.LoadPreUpPostDownNestedScrollConnection

/** 下拉刷新提示的 overlay 状态：由 [PullToRefreshLayout] 写入、`MyScaffold` 画在顶栏之上。 */
@Stable
class PullRefreshOverlayState internal constructor() {
    var visible by mutableStateOf(false)
    var refreshing by mutableStateOf(false)
    var willRefresh by mutableStateOf(false)
}

/**
 * 顶栏之上的下拉刷新提示槽位（由 `MyScaffold` 提供）。
 *
 * 提示画在栏的上层：既不会被顶栏遮住（首页就是一例），也不需要把整条栏推开——
 * 推开会让栏停在「非吸顶」的位置，回去时还得再弹一次。
 */
val LocalPullRefreshOverlay = compositionLocalOf<PullRefreshOverlayState?> { null }

/** 下拉刷新提示内容：刷新中转圈，否则「下拉即可刷新 / 松手刷新」。 */
@Composable
fun PullRefreshIndicatorContent(
    isRefreshing: Boolean,
    willLoad: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = ExtendedTheme.colors.primary,
            )
        } else {
            Text(
                text = if (willLoad) stringResource(id = R.string.release_to_refresh)
                else stringResource(id = R.string.pull_down_to_refresh),
                style = MaterialTheme.typography.caption,
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PullToRefreshLayout(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    indicator: @Composable BoxScope.(isRefreshing: Boolean, willLoad: Boolean) -> Unit = { isRefreshing, willLoad ->
        PullRefreshIndicatorContent(
            isRefreshing = isRefreshing,
            willLoad = willLoad,
            modifier = Modifier.align(Alignment.Center),
        )
    },
    refreshDistance: Dp = PullToRefreshDefaults.RefreshDistance,
    refreshingOffset: Dp = PullToRefreshDefaults.RefreshingOffset,
    threshold: Float = 0.75f,
    indicatorTopInset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current

    val curThreshold by rememberUpdatedState(newValue = threshold)

    val refreshDistancePx = remember(refreshDistance) {
        with(density) { refreshDistance.toPx() }
    }
    val refreshingOffsetPx = remember(refreshingOffset) {
        with(density) { refreshingOffset.toPx() }
    }

    val swipeableState = rememberSwipeableState(false) {
        if (it && !refreshing) {
            onRefresh()
        }
        false
    }
    val showIndicator by remember {
        derivedStateOf { swipeableState.offset.value > (-refreshDistancePx + refreshingOffsetPx) }
    }
    val layoutOffsetY by remember {
        derivedStateOf {
            swipeableState.offset.value + refreshDistancePx
        }
    }

    // 提示优先交给页面的 overlay（MyScaffold 画在顶栏之上）：不会被栏遮住，也不推栏。
    // 没有 scaffold 时（极少见）退回画在布局里的老路径。
    // 注意：这几个状态必须在组合期读出来，只在 SideEffect 里读不会订阅变化，overlay 就不会更新。
    val indicatorVisible = showIndicator
    val willRefresh = swipeableState.targetValue
    val overlayState = LocalPullRefreshOverlay.current
    if (overlayState != null) {
        SideEffect {
            overlayState.visible = indicatorVisible
            overlayState.refreshing = refreshing
            overlayState.willRefresh = willRefresh
        }
        DisposableEffect(overlayState) {
            onDispose { overlayState.visible = false }
        }
    }
    Box {
        Box(
            modifier = Modifier
                .nestedScroll(swipeableState.LoadPreUpPostDownNestedScrollConnection)
                .swipeable(
                    state = swipeableState,
                    anchors = mapOf(
                        -refreshDistancePx to false,
                        refreshDistancePx to true,
                    ),
                    thresholds = { _, _ -> FractionalThreshold(curThreshold) },
                    orientation = Orientation.Vertical,
                ),
        ) {
            Box(
                modifier = Modifier.offset {
                    IntOffset(
                        x = 0,
                        y = layoutOffsetY.toInt()
                    )
                }
            ) {
                content()
            }
        }

        if (overlayState == null) {
            AnimatedVisibility(
                visible = showIndicator,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = indicatorTopInset)
            ) {
                indicator(refreshing, swipeableState.targetValue)
            }
        }
    }
}

object PullToRefreshDefaults {
    val RefreshingOffset = 36.dp

    val RefreshDistance = 72.dp

    internal val <T> SwipeableState<T>.LoadPreUpPostDownNestedScrollConnection: NestedScrollConnection
        get() = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.toFloat()
                return if (delta < 0 && source == NestedScrollSource.Drag) {
                    performDrag(delta).toOffset()
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                return if (source == NestedScrollSource.Drag) {
                    performDrag(available.toFloat()).toOffset()
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val toFling = Offset(available.x, available.y).toFloat()
                return if (toFling < 0 && offset.value > minBound) {
                    performFling(velocity = toFling)
                    // since we go to the anchor with tween settling, consume all for the best UX
                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                performFling(velocity = Offset(available.x, available.y).toFloat())
                return available
            }

            private fun Float.toOffset(): Offset = Offset(0f, this)

            private fun Offset.toFloat(): Float = this.y
        }
}