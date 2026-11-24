package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.utils.bottomBarInsetsHeight

/** 胶囊圆角。M3 的「大圆角」观感，配合整块位移不会露出直角边。 */
val FloatingBarShape: Shape = RoundedCornerShape(28.dp)

/** 胶囊与屏幕左右的固定边距。 */
val FloatingBarHorizontalMargin = 16.dp

/** 胶囊与屏幕底部（不含系统手势区）的固定边距。贴得近一些，避免悬空观感。 */
val FloatingBarBottomMargin = 6.dp

/**
 * 悬浮栏底色：跟随主题栏色，深色 / AMOLED 下提亮到与主题「栏面」色（≈#1E1E1E）同一档。
 *
 * 纯黑 / 深邃灰主题下栏色与页面底色几乎相同，不提亮的话栏与页面糊成一片纯黑，
 * 整条读起来像「没渲染出来的黑边」。顶栏与底栏共用本规则，保证是同一视觉族。
 */
@Composable
fun floatingBarColor(color: Color = ExtendedTheme.colors.bottomBar): Color {
    // 0.12 白 ≈ 纯黑底色 (#000000) 叠加后得到 #1F1F1F，与 topBarSurface 同档；
    // 浅色主题只压 3% 黑，让栏在白底上有一条极淡的边界。
    val overlay = if (color.luminance() < 0.2f) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.03f)
    return remember(color, overlay) { color.compositeOver(overlay) }
}


/**
 * `Column(verticalScroll)` 这类没有 `contentPadding` 的滚动容器，
 * 用首尾 Spacer 达到同样效果：内容能滚进栏下面，又不会被栏永久压住。
 */
@Composable
fun BarsTopSpacer() {
    Spacer(Modifier.height(LocalBarsContentPadding.current.calculateTopPadding()))
}

@Composable
fun BarsBottomSpacer() {
    Spacer(Modifier.height(LocalBarsContentPadding.current.calculateBottomPadding()))
}
/**
 * 胶囊式悬浮底栏容器。
 *
 * 约束（对应 AGENTS.md 第 5.3 / 6 节）：
 * 1. 栏**自身高度恒定**（胶囊 + 底边距 + 系统手势区），`MyScaffold` 据此给内容算
 *    `contentPadding`，滚动过程中不会因动画而重排；
 * 2. 隐藏时只做绘制位移，且位移量取**自身实测高度**——胶囊、底边距、手势区一起滑出屏幕，
 *    不会残留一条被系统栏压住的边；
 * 3. 胶囊内容用 M3 [Surface] 承载（圆角 + 阴影），与主界面 / 帖子页保持同一视觉族。
 */
@Composable
fun FloatingBottomBar(
    modifier: Modifier = Modifier,
    color: Color = floatingBarColor(),
    shape: Shape = FloatingBarShape,
    horizontalMargin: Dp = FloatingBarHorizontalMargin,
    bottomMargin: Dp = FloatingBarBottomMargin,
    content: @Composable () -> Unit,
) {
    val barsState = LocalBarsAutoHideState.current
    val hiddenFraction = barsState?.hiddenFraction ?: 0f
    var heightPx by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { heightPx = it.height }
            .graphicsLayer { translationY = heightPx * hiddenFraction },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // 横屏刘海/挖孔在侧边：横向单独避让，纵向由下面的手势区占位负责。
                .windowInsetsPadding(
                    WindowInsets.safeDrawing
                        .only(WindowInsetsSides.Horizontal)
                        .union(WindowInsets.displayCutout)
                )
                .padding(horizontal = horizontalMargin)
        ) {
            Surface(
                shape = shape,
                color = color,
                contentColor = ExtendedTheme.colors.text,
                shadowElevation = 12.dp,
                tonalElevation = 0.dp,
                content = content,
            )
        }
        // 底边距 + 系统手势区：属于被隐藏的整块，跟胶囊一起滑走。
        Spacer(modifier = Modifier.height(bottomMargin + bottomBarInsetsHeight()))
    }
}
