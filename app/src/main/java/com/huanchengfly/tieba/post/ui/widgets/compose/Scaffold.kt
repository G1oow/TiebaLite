package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.utils.topBarInsetsHeight

/**
 * 当前页顶栏的实测高度（含状态栏底色带）。
 *
 * 吸顶条（`stickyHeader`）用它算出「被顶栏顶下去多少」：顶栏展开时吸顶条落在顶栏下方，
 * 收起后缩回状态栏下沿，两者随同一个 [BarsAutoHideState.hiddenFraction] 一起运动。
 */
val LocalTopBarHeight = compositionLocalOf { 0.dp }

/** 上滑关闭当前提示；新 Snackbar 使用独立的拖动状态。 */
@Composable
private fun SwipeToDismissSnackbarHost(hostState: SnackbarHostState) {
    val data = hostState.currentSnackbarData
    val density = LocalDensity.current
    val threshold = with(density) { 32.dp.toPx() }
    var offset by remember(data) { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { height = it.height }
            .graphicsLayer { translationY = offset }
            .draggable(
                state = rememberDraggableState { offset = (offset + it).coerceAtMost(0f) },
                orientation = Orientation.Vertical,
                enabled = data != null,
                onDragStopped = { velocity ->
                    val dismiss = offset <= -threshold || velocity < -threshold * 10f
                    Animatable(offset).animateTo(
                        if (dismiss) -height.toFloat() else 0f,
                        animationSpec = tween(150),
                    ) { offset = value }
                    if (dismiss) data?.dismiss()
                    offset = 0f
                },
            )
    ) {
        SnackbarHost(hostState = hostState)
    }
}

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> { error("no scaffold here!") }

/**
 * 外层（主界面悬浮底栏）与当前页面栏的占位高度。
 *
 * 栏是独立浮层、页面正文铺满整屏，所以子页面的列表必须同时避让两层栏：
 * 只避让其中一层，内容就会滚到另一层栏底下被永久压住。
 */
val LocalBarsContentPadding = compositionLocalOf { PaddingValues(0.dp) }

/** 把外层与当前层的栏高逐边相加。 */
@Composable
private fun mergedBarsPadding(outer: PaddingValues, inner: PaddingValues): PaddingValues {
    if (outer == PaddingValues(0.dp)) return inner
    val layoutDirection = LocalLayoutDirection.current
    return remember(outer, inner, layoutDirection) {
        PaddingValues(
            start = outer.calculateStartPadding(layoutDirection) + inner.calculateStartPadding(layoutDirection),
            top = outer.calculateTopPadding() + inner.calculateTopPadding(),
            end = outer.calculateEndPadding(layoutDirection) + inner.calculateEndPadding(layoutDirection),
            bottom = outer.calculateBottomPadding() + inner.calculateBottomPadding(),
        )
    }
}

/**
 * 页面骨架（**悬浮栏**布局，AGENTS.md §5.3 / §6）。
 *
 * 与 Material 原生 `Scaffold` 的关键差异：`topBar` / `bottomBar` 是**浮层**，
 * 不占内容布局——正文铺满整屏，只把实测栏高作为 [PaddingValues] 交给内容
 * （列表用它当 `contentPadding`）。这样栏收起滑走时，露出的就是正文本身，
 * 不会留一条空白，也不会出现「内容只到栏顶为止」的截断。
 * 因此这里**不套** M3 `Scaffold`：它会为栏预留布局空间，与 overlay 语义冲突。
 *
 * 栏的实测高度会作为组合局部值向上累加（见 [LocalBarsContentPadding]）：
 * 主界面的胶囊底栏 + 子页面的顶栏会相加后一起交给最内层的列表。
 */
@Composable
fun MyScaffold(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SwipeToDismissSnackbarHost(it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable (PaddingValues) -> Unit
) {
    // 新页面进入时把顶栏/底栏恢复成展开状态：栏状态是 Activity 级共享的，
    // 否则从「已收起」的页面点进下一层，新页面会一进来就没有顶栏。
    val barsAutoHideState = LocalBarsAutoHideState.current
    LaunchedEffect(barsAutoHideState) {
        barsAutoHideState?.show()
    }

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarHeight = with(density) { topBarHeightPx.toDp() }
    val bottomBarHeight = with(density) { bottomBarHeightPx.toDp() }
    val pullRefreshOverlay = remember { PullRefreshOverlayState() }
    val innerPadding = remember(topBarHeight, bottomBarHeight) {
        PaddingValues(top = topBarHeight, bottom = bottomBarHeight)
    }
    val mergedPadding = mergedBarsPadding(
        outer = LocalBarsContentPadding.current,
        inner = innerPadding
    )

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
        ) {
            // 正文铺满整屏：栏收起时露出的就是它。
            CompositionLocalProvider(
                LocalBarsContentPadding provides mergedPadding,
                LocalTopBarHeight provides topBarHeight,
                LocalPullRefreshOverlay provides pullRefreshOverlay,
            ) {
                content(mergedPadding)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { topBarHeightPx = it.height }
            ) {
                topBar()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomBarHeightPx = it.height }
            ) {
                bottomBar()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = mergedPadding.calculateBottomPadding())
            ) {
                snackbarHost(snackbarHostState)
            }
            Box(
                modifier = Modifier
                    .align(
                        when (floatingActionButtonPosition) {
                            FabPosition.Start -> Alignment.BottomStart
                            FabPosition.Center -> Alignment.BottomCenter
                            else -> Alignment.BottomEnd
                        }
                    )
                    .padding(end = 16.dp, bottom = mergedPadding.calculateBottomPadding() + 16.dp),
            ) {
                floatingActionButton()
            }
            if (pullRefreshOverlay.indicator != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(top = topBarInsetsHeight() + pullRefreshOverlay.topInset),
                    contentAlignment = Alignment.Center,
                ) {
                    pullRefreshOverlay.indicator?.invoke(this)
                }
            }
        }
    }
}
