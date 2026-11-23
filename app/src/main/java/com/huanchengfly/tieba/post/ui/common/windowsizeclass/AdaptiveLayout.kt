package com.huanchengfly.tieba.post.ui.common.windowsizeclass

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 窗口尺寸类 -> 布局决策的唯一出口。
 *
 * 所有「宽屏下要不要收窄 / 居中」的判断都应该走这里，避免各页面散落魔法数字。
 * 断点定义见 [WindowWidthSizeClass] / [WindowHeightSizeClass]：
 * Compact < 600dp <= Medium < 840dp <= Expanded。
 */
object AdaptiveLayout {
    /**
     * 单列正文（信息流 / 设置项 / 帖子正文）在宽屏下的最大宽度。
     *
     * 超过该宽度时内容居中显示，两侧留白——避免平板上一条信息流横跨 1200dp
     * 导致阅读行宽过长。
     */
    val ContentMaxWidth: Dp = 840.dp

    /** 居中对话框（[WindowWidthSizeClass] 非 Compact 时）的最大宽度。 */
    val DialogMaxWidth: Dp = 560.dp
}

/** 是否为窄屏（手机竖屏）。 */
val WindowSizeClass.isCompactWidth: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Compact

/** 是否为中等宽度（折叠屏展开态 / 平板竖屏）。 */
val WindowSizeClass.isMediumWidth: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Medium

/** 是否为宽屏（平板横屏 / 桌面窗口）。 */
val WindowSizeClass.isExpandedWidth: Boolean
    get() = widthSizeClass == WindowWidthSizeClass.Expanded

/** 是否为矮屏（手机横屏）。 */
val WindowSizeClass.isCompactHeight: Boolean
    get() = heightSizeClass == WindowHeightSizeClass.Compact

/**
 * 当前窗口下正文允许的最大宽度。
 *
 * 仅 Expanded 需要收窄：Medium 上限 840dp 与 [AdaptiveLayout.ContentMaxWidth] 相同，
 * Compact 本就窄于该值，两者都不做限制（返回 null）。
 */
val WindowSizeClass.contentMaxWidth: Dp?
    get() = if (isExpandedWidth) AdaptiveLayout.ContentMaxWidth else null
