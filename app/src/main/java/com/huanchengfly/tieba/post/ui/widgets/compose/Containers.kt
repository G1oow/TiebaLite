package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.AdaptiveLayout
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isExpandedWidth
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.isMediumWidth

/**
 * 单列内容容器：在宽屏上居中并限制最大宽度。
 *
 * 之前的实现是「按窗口宽度取百分比」（Medium 87% / Expanded 75%），
 * 窗口越宽内容越宽，1280dp 的平板上会得到 960dp 宽的一列，阅读行宽过长。
 * 现在改为**按绝对宽度**限制：Expanded 下最多 [AdaptiveLayout.ContentMaxWidth]，
 * 且不超过原百分比宽度（避免小窗口上反而比之前更宽）。
 *
 * @param fluid 为 true 时不做任何限制，直接铺满可用宽度（横向轮播等场景）
 */
@Composable
fun Container(
    modifier: Modifier = Modifier,
    fluid: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (fluid) {
        content()
        return
    }
    val windowSizeClass = LocalWindowSizeClass.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val fractionWidth = when {
            windowSizeClass.isMediumWidth -> maxWidth * 0.87f
            windowSizeClass.isExpandedWidth -> maxWidth * 0.75f
            else -> maxWidth
        }
        val contentWidth = when (windowSizeClass.widthSizeClass) {
            WindowWidthSizeClass.Expanded -> minOf(fractionWidth, AdaptiveLayout.ContentMaxWidth)
            else -> fractionWidth
        }
        Box(modifier = modifier.width(contentWidth)) {
            content()
        }
    }
}
