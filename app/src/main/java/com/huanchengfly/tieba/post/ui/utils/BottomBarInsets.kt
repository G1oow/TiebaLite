package com.huanchengfly.tieba.post.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 手势导航热区兜底高度（24-32dp 手势条热区的中间值）。
 *
 * 部分国产 ROM（如 ColorOS 16）在手势导航模式下，
 * navigationBars 与 safeGestures 两个 inset 可能同时上报 0，
 * 导致底栏内容被手势热区穿透。此时用该值兜底。
 */
private val FallbackGestureHotspotHeight = 28.dp

/**
 * 取底部安全区高度：navigationBars ∪ safeGestures，
 * 两者均为 0 时兜底 [FallbackGestureHotspotHeight]。
 */
@Composable
fun bottomBarInsetsHeight(): Dp {
    // 并入 displayCutout：横屏或屏下摄像头机型中挖孔可能落在底部/侧边，
    // 只避让 navigationBars ∪ safeGestures 会让底栏内容被挖孔切掉。
    val insets = WindowInsets.navigationBars
        .union(WindowInsets.safeGestures)
        .union(WindowInsets.displayCutout)
    val density = LocalDensity.current
    val bottomPx = insets.getBottom(density)
    return if (bottomPx > 0) {
        with(density) { bottomPx.toDp() }
    } else {
        FallbackGestureHotspotHeight
    }
}

/**
 * 取顶部安全区高度：`safeDrawing` 的 Top（= statusBars ∪ captionBar ∪ displayCutout），
 * 与 AGENTS.md §4-6 规定的顶栏 inset 同源。
 *
 * 栏是 overlay、正文铺满整屏（见 AGENTS.md §5.3），所以吸顶（`stickyHeader`）页签粘的是
 * 列表视口顶边 y=0——既不吃 `contentPadding` 的栏高占位，也不吃系统 inset。
 * 这类「贴在视口顶边但仍需留在系统栏下方」的内容统一用本值避让。
 */
@Composable
fun topBarInsetsHeight(): Dp {
    val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    val density = LocalDensity.current
    return with(density) { insets.getTop(density).toDp() }
}

/**
 * 顶部安全区避让的 Modifier 版：[topBarInsetsHeight] 的等值实现，同时把这段 inset 消费掉。
 *
 * 页面根部 / 侧栏头部这类「自己就是最顶层容器」的位置用它，取代裸的 `statusBarsPadding()`
 * ——后者只算状态栏，刘海 / 挖孔高于状态栏时会压住内容（AGENTS.md §4-6）。
 */
fun Modifier.topBarInsetsPadding(): Modifier = composed {
    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
}

/**
 * 底栏手势热区避让：高度取 (navigationBars ∪ safeGestures) 底部值，
 * 值为 0 时兜底 [FallbackGestureHotspotHeight]；
 * 并在该区域内消费点击，避免触摸穿透到手势热区之下的内容。
 */
fun Modifier.bottomBarInsetsPadding(): Modifier = composed {
    val height = bottomBarInsetsHeight()
    this
        .height(height)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {}
        )
}

/**
 * 底栏手势热区占位 Spacer：高度逻辑同 [bottomBarInsetsPadding]，
 * 并消费点击防止触摸穿透。用于替换裸的 windowInsetsBottomHeight Spacer。
 */
@Composable
fun BottomInsetsSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier = modifier.bottomBarInsetsPadding())
}
