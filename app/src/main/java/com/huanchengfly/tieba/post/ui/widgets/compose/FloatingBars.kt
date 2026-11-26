package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.border
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.utils.bottomBarInsetsHeight
import com.huanchengfly.tieba.post.utils.ThemeUtil

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
    // 注意合成方向：这里是不透明栏色合在 overlay 之上，所以深色 / 纯黑主题下 12% 白**不会**提亮
    // 栏色（用户 2026-11 明确不接受栏变亮），只有浅色主题的 3% 黑会在栏上留一条极淡的边界。
    val overlay = if (color.luminance() < 0.2f) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.03f)
    return remember(color, overlay) { color.compositeOver(overlay) }
}

/**
 * 悬浮栏视觉样式（设置内可切换，见「设置 → 个性化 → 悬浮底栏样式」）。
 *
 * - [SHADOW]：双层投影 + 发丝描边，选中项带主色淡胶囊底——层次来自光与影；
 * - [TONAL]：去掉大投影只留贴地投影，靠 1dp 描边与中性色调块分层——层次来自面与边。
 *
 * 两者只改「画法」，栏的高度、位移语义完全一致（AGENTS.md §6），
 * 因此 `MyScaffold` 算给内容的 `contentPadding` 不随样式变化。
 */
enum class FloatingBarStyle(val prefValue: String) {
    SHADOW("shadow"),
    TONAL("tonal");

    companion object {
        fun of(value: String?): FloatingBarStyle =
            entries.firstOrNull { it.prefValue == value } ?: SHADOW
    }
}

/** [FloatingBarStyle] 对应的 DataStore 键；设置页的 `ListPref` 必须用同一字符串。 */
const val FloatingBarStylePrefKey = "floating_bar_style"

/** Activity 级共享的悬浮底栏样式（由 `BaseComposeActivity` 统一订阅 DataStore 后提供）。 */
val LocalFloatingBarStyle = compositionLocalOf<FloatingBarStyle?> { null }

/** 读当前样式：优先从 [LocalFloatingBarStyle] 读取，避免各处重复订阅 DataStore。 */
@Composable
fun rememberFloatingBarStyle(): FloatingBarStyle {
    LocalFloatingBarStyle.current?.let { return it }
    val value by rememberPreferenceAsState(
        key = stringPreferencesKey(FloatingBarStylePrefKey),
        defaultValue = FloatingBarStyle.SHADOW.prefValue
    )
    return FloatingBarStyle.of(value)
}

/**
 * 纯黑（AMOLED）主题判定。
 *
 * 两种取色路径都要覆盖：写死主题时 `theme` 是 `amoled_dark`；开启动态取色后
 * `theme` 变成 `dynamic`，但纯黑档的 `background` 仍是纯黑，所以再兜一层底色判断。
 */
@Composable
fun rememberPureBlackTheme(): Boolean {
    val colors = ExtendedTheme.colors
    return colors.theme == ThemeUtil.THEME_AMOLED_DARK || colors.background == Color.Black
}

/**
 * 胶囊描边色：深色主题用极淡白、浅色主题用极淡黑（与 [floatingBarColor] 同一明暗判据）。
 *
 * [pureBlack] 时返回透明：纯黑主题下页面底色是 #000000，任何 1dp 亮描边都会读成
 * 「栏外面镶了一圈亮线」，用户 2026-11 明确要求纯黑模式不要描边。
 */
fun floatingBarBorderColor(
    color: Color,
    style: FloatingBarStyle,
    pureBlack: Boolean = false
): Color {
    if (pureBlack) return Color.Transparent
    val dark = color.luminance() < 0.2f
    return when (style) {
        FloatingBarStyle.SHADOW ->
            if (dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.07f)
        FloatingBarStyle.TONAL ->
            if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    }
}

/**
 * 选中项胶囊底色：样式 A 用主色淡底（强调「当前在哪」），样式 C 用中性色调块（安静一档）。
 * 深色下 alpha 更高，否则淡底在深底上几乎看不见。
 */
fun floatingBarIndicatorColor(style: FloatingBarStyle, accent: Color, isNight: Boolean): Color {
    return when (style) {
        FloatingBarStyle.SHADOW -> accent.copy(alpha = if (isNight) 0.20f else 0.12f)
        FloatingBarStyle.TONAL ->
            if (isNight) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f)
    }
}

/** 选中项胶囊的描边：只有样式 A 画（样式 C 的中性块靠颜色本身分层）。 */
fun floatingBarIndicatorBorderColor(
    style: FloatingBarStyle,
    accent: Color,
    isNight: Boolean
): Color? = when (style) {
    FloatingBarStyle.SHADOW -> accent.copy(alpha = if (isNight) 0.24f else 0.18f)
    FloatingBarStyle.TONAL -> null
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
    val style = rememberFloatingBarStyle()
    val pureBlack = rememberPureBlackTheme()
    val borderColor = remember(color, style, pureBlack) {
        floatingBarBorderColor(color, style, pureBlack)
    }
    val barsState = LocalBarsAutoHideState.current
    var heightPx by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { heightPx = it.height }
            // 在绘制阶段读取 hiddenFraction，栏显隐动画期间不触发底栏子树逐帧重组。
            .graphicsLayer { translationY = heightPx * (barsState?.hiddenFraction ?: 0f) },
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
            // 两种样式共用同一个 Surface，只换「画法」：
            // A 叠两层投影（2dp 定轮廓 + 14dp 铺氛围）；C 只留 1dp 贴地投影，靠描边分层。
            val shadowModifier = if (style == FloatingBarStyle.SHADOW) {
                Modifier
                    .shadow(2.dp, shape, clip = false)
                    .shadow(14.dp, shape, clip = false)
            } else {
                Modifier.shadow(1.dp, shape, clip = false)
            }
            Surface(
                modifier = shadowModifier.then(
                    // 纯黑主题下 borderColor 是透明：不叠 border，避免多画一层无用的描边。
                    if (borderColor.alpha > 0f) Modifier.border(1.dp, borderColor, shape) else Modifier
                ),
                shape = shape,
                color = color,
                contentColor = ExtendedTheme.colors.text,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
                content = content,
            )
        }
        // 底边距 + 系统手势区：属于被隐藏的整块，跟胶囊一起滑走。
        Spacer(modifier = Modifier.height(bottomMargin + bottomBarInsetsHeight()))
    }
}
