package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DismissValue
import androidx.compose.material.DrawerDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FabPosition
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberDismissState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeToDismissSnackbarHost(hostState: SnackbarHostState) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            if (value != DismissValue.Default) {
                hostState.currentSnackbarData?.dismiss()
                true
            } else {
                false
            }
        }
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != DismissValue.Default) {
            dismissState.reset()
        }
    }
    SwipeToDismiss(state = dismissState, background = {}) {
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
 * 页面骨架（**悬浮栏**布局）。
 *
 * 与 Material 原生 `Scaffold` 的关键差异：`topBar` / `bottomBar` 是**浮层**，
 * 不占内容布局——正文铺满整屏，只把实测栏高作为 [PaddingValues] 交给内容
 * （列表用它当 `contentPadding`）。这样栏收起滑走时，露出的就是正文本身，
 * 不会留一条空白，也不会出现「内容只到栏顶为止」的截断。
 *
 * 栏的实测高度会作为组合局部值向上累加（见 [LocalBarsContentPadding]）：
 * 主界面的胶囊底栏 + 子页面的顶栏会相加后一起交给最内层的列表。
 *
 * 仍保留 Material2 `Scaffold` 的抽屉分支：抽屉需要 modal 手势与 scrim，
 * 目前没有页面在用，等该场景迁移到 M3 时一并替换。
 */
@Composable
fun MyScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SwipeToDismissSnackbarHost(it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    drawerScrimColor: Color = DrawerDefaults.scrimColor,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (PaddingValues) -> Unit
) {
    // 新页面进入时把顶栏/底栏恢复成展开状态：栏状态是 Activity 级共享的，
    // 否则从「已收起」的页面点进下一层，新页面会一进来就没有顶栏。
    val barsAutoHideState = LocalBarsAutoHideState.current
    LaunchedEffect(barsAutoHideState) {
        barsAutoHideState?.show()
    }
    CompositionLocalProvider(LocalSnackbarHostState provides scaffoldState.snackbarHostState) {
        if (drawerContent != null) {
            Scaffold(
                modifier = modifier,
                scaffoldState = scaffoldState,
                topBar = topBar,
                bottomBar = bottomBar,
                snackbarHost = { snackbarHost(it) },
                floatingActionButton = floatingActionButton,
                floatingActionButtonPosition = floatingActionButtonPosition,
                isFloatingActionButtonDocked = isFloatingActionButtonDocked,
                drawerContent = drawerContent,
                drawerGesturesEnabled = drawerGesturesEnabled,
                drawerShape = drawerShape,
                drawerElevation = drawerElevation,
                drawerBackgroundColor = drawerBackgroundColor,
                drawerContentColor = drawerContentColor,
                drawerScrimColor = drawerScrimColor,
                backgroundColor = backgroundColor,
                contentColor = contentColor,
                content = content,
            )
            return@CompositionLocalProvider
        }

        val density = LocalDensity.current
        var topBarHeightPx by remember { mutableIntStateOf(0) }
        var bottomBarHeightPx by remember { mutableIntStateOf(0) }
        val topBarHeight = with(density) { topBarHeightPx.toDp() }
        val bottomBarHeight = with(density) { bottomBarHeightPx.toDp() }
        val innerPadding = remember(topBarHeight, bottomBarHeight) {
            PaddingValues(top = topBarHeight, bottom = bottomBarHeight)
        }
        val mergedPadding = mergedBarsPadding(
            outer = LocalBarsContentPadding.current,
            inner = innerPadding
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
        ) {
            // 正文铺满整屏：栏收起时露出的就是它。
            CompositionLocalProvider(LocalBarsContentPadding provides mergedPadding) {
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
                    .padding(bottom = bottomBarHeight)
            ) {
                snackbarHost(scaffoldState.snackbarHostState)
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
                    .padding(end = 16.dp, bottom = bottomBarHeight + 16.dp),
            ) {
                floatingActionButton()
            }
        }
    }
}
