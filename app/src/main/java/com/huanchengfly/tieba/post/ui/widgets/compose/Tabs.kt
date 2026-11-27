package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha as M2LocalContentAlpha
import androidx.compose.material.LocalContentColor as M2LocalContentColor
import androidx.compose.material.ProvideTextStyle as M2ProvideTextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.HorizontalDivider as M3HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab as M3Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
/**
 * 等宽页签栏：M3 [SecondaryTabRow] 的薄封装。
 *
 * 只多做一件事——迁移期桥接（AGENTS.md §5.2）：页签槽里仍是各页面传入的 M2 `Text` / `Icon`，
 * 它们读的是 M2 的 `LocalContentColor`，M3 `Surface` 不会提供。等页面内容全部换成 M3 后，
 * 删掉 [ProvideM2ContentColor] 这一层即可直接用 M3 的页签栏。
 */
@Composable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TabRowDefaults.secondaryContainerColor,
    contentColor: Color = TabRowDefaults.secondaryContentColor,
    indicator: @Composable TabIndicatorScope.() -> Unit = {
        TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false)
        )
    },
    divider: @Composable () -> Unit = { M3HorizontalDivider() },
    tabs: @Composable () -> Unit,
) {
    SecondaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = contentColor,
        indicator = indicator,
        divider = divider,
    ) {
        ProvideM2ContentColor(color = contentColor, content = tabs)
    }
}

/**
 * 可横向滚动的页签栏：M3 [SecondaryScrollableTabRow] 的薄封装，桥接同 [TabRow]。
 *
 * `minTabWidth` 固定为 0：M3 默认每个页签至少 90dp，本工程的页签是按文字宽度紧排的
 * （吧页「最新 / 精华」、个人主页「贴子 / 回复 / 关注吧」），套默认值会被撑开。
 */
@Composable
fun ScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = TabRowDefaults.secondaryContainerColor,
    contentColor: Color = TabRowDefaults.secondaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable TabIndicatorScope.() -> Unit = {
        TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = false)
        )
    },
    divider: @Composable () -> Unit = { M3HorizontalDivider() },
    tabs: @Composable () -> Unit,
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = contentColor,
        edgePadding = edgePadding,
        indicator = indicator,
        divider = divider,
        minTabWidth = 0.dp,
    ) {
        ProvideM2ContentColor(color = contentColor, content = tabs)
    }
}

/**
 * 文字页签：M3 `Tab` + 迁移期桥接。
 *
 * - 文字样式沿用迁移前的 M2 `button`（= M3 `labelLarge`，见 `Material3Typography`），不用 M3 页签默认的
 *   `titleSmall`，否则字距 / 行高会变；
 * - 未选中色默认仍是「选中色 × M2 `ContentAlpha.medium`」：这个系数取决于主题深浅与内容色亮度
 *   （0.60 / 0.74），M3 没有对位物，迁移期继续读 M2 的值以保持观感，随阶段 2e 清 `ContentAlpha` 时一并处理。
 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium),
) {
    val textStyle = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center)
    M3Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        text = { ProvideM2TabContent(textStyle = textStyle, content = text) },
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        interactionSource = interactionSource,
    )
}

/** 自定义内容的页签：M3 `Tab` + 迁移期桥接，说明同上面的文字版本。 */
@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium),
    content: @Composable ColumnScope.() -> Unit,
) {
    M3Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
        interactionSource = interactionSource,
    ) {
        ProvideM2TabContent(textStyle = null) { content() }
    }
}

/** 迁移期桥接：把页签栏的内容色同时交给 M2 的 `LocalContentColor`（与 M2 `Surface` 的行为一致）。 */
@Composable
private fun ProvideM2ContentColor(color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(M2LocalContentColor provides color, content = content)
}

/**
 * 迁移期桥接：M3 `Tab` 只把按选中态插值后的颜色交给 M3 的 `LocalContentColor`，这里按 M2 `Tab`
 * 的方式（颜色与透明度分开）同步给 M2，页面传入的 M2 `Text` / `Icon` 才能跟着变色。
 */
@Composable
private fun ProvideM2TabContent(textStyle: TextStyle?, content: @Composable () -> Unit) {
    val color = LocalContentColor.current
    CompositionLocalProvider(
        M2LocalContentColor provides color.copy(alpha = 1f),
        M2LocalContentAlpha provides color.alpha,
    ) {
        if (textStyle != null) {
            M2ProvideTextStyle(textStyle) {
                ProvideTextStyle(textStyle, content)
            }
        } else {
            content()
        }
    }
}

private val PagerTabIndicatorWidth = 16.dp
private val PagerTabIndicatorHeight = 3.dp
private val PagerTabIndicatorBottomGap = 8.dp

/**
 * 跟随 [pagerState] 的短横条指示器（16dp × 3dp，贴在页签文字下方）。
 *
 * 水平位置**交给 M3 的 [TabIndicatorScope.tabIndicatorOffset]**，不再自己按 `TabPosition` 算：
 * 可滚动页签栏（`SecondaryScrollableTabRow`）给指示器的容器只有「选中页签的内容区」那么大
 * （实测 `constraints.maxWidth` = 内容宽 96px，而整行 180px），容器原点也在内容左边缘——
 * 拿整行坐标去 `placeRelative` 会整体右移 `(tabWidth − contentWidth) / 2`。
 * `matchContentSize = true` 拿到的正是内容区，在里面居中一条 16dp 短横条即可。
 *
 * 代价：跨页切换是 M3 自带的平滑过渡，不再逐帧跟随手指（M3 页签栏的标准行为）。
 */
@Composable
fun TabIndicatorScope.PagerTabIndicator(pagerState: PagerState) {
    Box(
        modifier = Modifier
            .tabIndicatorOffset(pagerState.currentPage, matchContentSize = true)
            .fillMaxHeight(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = PagerTabIndicatorBottomGap)
                .width(PagerTabIndicatorWidth)
                .height(PagerTabIndicatorHeight)
                .clip(RoundedCornerShape(100))
                .background(color = LocalContentColor.current)
        )
    }
}

@Composable
fun TabClickMenu(
    selected: Boolean,
    onClick: () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium),
    content: @Composable ColumnScope.() -> Unit,
) {
    LaunchedEffect(Unit) {
        launch {
            interactionSource.interactions
                .filterIsInstance<PressInteraction.Press>()
                .collect {
                    menuState.offset = it.pressPosition
                }
        }
    }
    ClickMenu(
        menuContent = menuContent,
        menuState = menuState
    ) {
        Tab(
            selected = selected,
            onClick = {
                if (!selected) {
                    onClick()
                } else {
                    menuState.toggle()
                }
            },
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource,
            selectedContentColor = selectedContentColor,
            unselectedContentColor = unselectedContentColor,
            content = content
        )
    }
}

@Composable
fun TabClickMenu(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = ContentAlpha.medium),
) {
    TabClickMenu(
        selected = selected,
        onClick = onClick,
        menuContent = menuContent,
        modifier = modifier,
        enabled = enabled,
        menuState = menuState,
        interactionSource = interactionSource,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
    ) {
        val rotate by animateFloatAsState(
            targetValue = if (menuState.expanded) 180f else 0f,
            label = "ArrowIndicatorRotate"
        )
        val alpha by animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            label = "ArrowIndicatorAlpha"
        )

        val tabTextStyle =
            MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, letterSpacing = 0.sp)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .padding(start = 16.dp)
        ) {
            M2ProvideTextStyle(value = tabTextStyle) {
                ProvideTextStyle(value = tabTextStyle) {
                    text()
                }
            }
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        rotationZ = rotate
                        this.alpha = alpha
                    }
            )
        }
    }
}

private val SegmentedTrackShape = RoundedCornerShape(percent = 50)
private val SegmentedTrackPadding = 3.dp
private val SegmentedTabHeight = 32.dp
private val SegmentedSliderEasing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
private const val SegmentedSliderDurationMs = 280

/**
 * 胶囊分段页签栏（Motion Primitives `SegmentedTabs` 风格）：
 *
 * - 外层是低对比柔和胶囊轨道，内层唯一一块选中滑块胶囊画在 `drawBehind` 绘制层，
 *   只读取 `pagerState` 实时偏移（拖拽逐帧跟踪、点击平滑滑动）或 `selectedTabIndex` 的减速补间，
 *   动画期间不触发布局重组（AGENTS.md §3.4 / §6）。
 * - 支持等宽模式（`equalWidth = true`，用于 2–3 项短标题页签）和自适应宽度 + 横向滚动模式
 *   （`equalWidth = false, scrollable = true`，用于吧页 / 个人主页等不定长页签）。
 * - 颜色默认按所处背景明暗（浅色 / 深色 / 纯黑 AMOLED / 主色彩色顶栏）自动分层，纯黑下不加亮描边。
 */
@Composable
fun SegmentedTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    pagerState: PagerState? = null,
    onPrimarySurface: Boolean = false,
    trackColor: Color = defaultSegmentedTrackColor(onPrimarySurface),
    indicatorColor: Color = defaultSegmentedIndicatorColor(onPrimarySurface),
    contentColor: Color = ExtendedTheme.colors.onTopBar,
    equalWidth: Boolean = true,
    scrollable: Boolean = false,
    minTabWidth: Dp = 64.dp,
    tabs: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val minTabWidthPx = with(density) { minTabWidth.roundToPx() }

    // 无 PagerState 时退回到索引减速补间；有 PagerState 时直接在 drawBehind 读取
    // currentPage + currentPageOffsetFraction，拖拽跟手、点击动画同步，零重组开销。
    val fallbackAnimatedIndex = if (pagerState == null) {
        animateFloatAsState(
            targetValue = selectedTabIndex.toFloat(),
            animationSpec = tween(
                durationMillis = SegmentedSliderDurationMs,
                easing = SegmentedSliderEasing,
            ),
            label = "SegmentedTabIndex",
        )
    } else {
        null
    }

    // 仅记录各页签布局几何供绘制层插值，不走 MutableState 以免测量后二次重组。
    val tabBounds = remember { SegmentedTabBounds() }
    val scrollState = if (scrollable) rememberScrollState() else null

    // 可横向滚动模式下，选中项超出可视区时自动平滑滚入。
    if (scrollState != null) {
        LaunchedEffect(selectedTabIndex, tabBounds.count) {
            if (selectedTabIndex in 0 until tabBounds.count) {
                val left = tabBounds.lefts[selectedTabIndex]
                val right = left + tabBounds.widths[selectedTabIndex]
                val viewportWidth = scrollState.viewportSize
                if (viewportWidth > 0) {
                    val target = ((left + right) / 2 - viewportWidth / 2)
                        .coerceIn(0, scrollState.maxValue)
                    scrollState.animateScrollTo(target)
                }
            }
        }
    }

    val scrollModifier = if (scrollState != null) {
        Modifier.horizontalScroll(scrollState)
    } else {
        Modifier
    }

    Box(
        propagateMinConstraints = !scrollable,
        modifier = modifier
            .clip(SegmentedTrackShape)
            .background(trackColor)
            .then(scrollModifier)
            .padding(SegmentedTrackPadding)
    ) {
        Layout(
            content = {
                ProvideM2ContentColor(color = contentColor) {
                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                        tabs()
                    }
                }
            },
            modifier = Modifier.drawBehind {
                val count = tabBounds.count
                if (count <= 0) return@drawBehind

                val rawPosition = if (pagerState != null) {
                    (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, (count - 1).toFloat())
                } else {
                    (fallbackAnimatedIndex?.value ?: selectedTabIndex.toFloat())
                        .coerceIn(0f, (count - 1).toFloat())
                }

                val lower = floor(rawPosition).toInt().coerceIn(0, count - 1)
                val upper = ceil(rawPosition).toInt().coerceIn(0, count - 1)
                val fraction = rawPosition - lower

                val left = lerp(tabBounds.lefts[lower].toFloat(), tabBounds.lefts[upper].toFloat(), fraction)
                val width = lerp(tabBounds.widths[lower].toFloat(), tabBounds.widths[upper].toFloat(), fraction)
                if (width <= 0f || size.height <= 0f) return@drawBehind

                val radius = size.height / 2f
                drawRoundRect(
                    color = indicatorColor,
                    topLeft = Offset(left, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(radius, radius),
                )
            }
        ) { measurables, constraints ->
            val count = measurables.size
            if (count == 0) {
                tabBounds.update(0, IntArray(0), IntArray(0))
                return@Layout layout(0, 0) {}
            }

            val tabHeightPx = with(density) { SegmentedTabHeight.roundToPx() }
                .coerceIn(constraints.minHeight, constraints.maxHeight)

            val placeables = if (equalWidth) {
                val cellWidth = if (constraints.hasBoundedWidth && constraints.maxWidth != Constraints.Infinity) {
                    val bounded = (constraints.maxWidth / count).coerceAtLeast(1)
                    // 若父级指定了固定宽度（如 fillMaxWidth），直接按容器等分；否则取最大自然宽与最小宽。
                    if (constraints.minWidth == constraints.maxWidth) {
                        bounded
                    } else {
                        val maxIntrinsic = measurables.maxOf { it.maxIntrinsicWidth(tabHeightPx) }
                        max(maxIntrinsic, minTabWidthPx).coerceAtMost(bounded)
                    }
                } else {
                    val maxIntrinsic = measurables.maxOf { it.maxIntrinsicWidth(tabHeightPx) }
                    max(maxIntrinsic, minTabWidthPx)
                }
                val childConstraints = Constraints.fixed(width = cellWidth, height = tabHeightPx)
                measurables.map { it.measure(childConstraints) }
            } else {
                val childConstraints = Constraints(
                    minWidth = minTabWidthPx,
                    maxWidth = Constraints.Infinity,
                    minHeight = tabHeightPx,
                    maxHeight = tabHeightPx,
                )
                measurables.map { it.measure(childConstraints) }
            }

            val lefts = IntArray(count)
            val widths = IntArray(count)
            var x = 0
            placeables.forEachIndexed { index, placeable ->
                lefts[index] = x
                widths[index] = placeable.width
                x += placeable.width
            }
            tabBounds.update(count, lefts, widths)

            val totalWidth = x.coerceIn(constraints.minWidth, constraints.maxWidth)
            layout(totalWidth, tabHeightPx) {
                placeables.forEachIndexed { index, placeable ->
                    placeable.placeRelative(x = lefts[index], y = 0)
                }
            }
        }
    }
}

private class SegmentedTabBounds {
    var count: Int = 0
        private set
    var lefts: IntArray = IntArray(0)
        private set
    var widths: IntArray = IntArray(0)
        private set

    fun update(count: Int, lefts: IntArray, widths: IntArray) {
        this.count = count
        this.lefts = lefts
        this.widths = widths
    }
}

/** 轨道默认底色：浅色用柔灰、深色/纯黑用低对比微亮层、彩色顶栏用半透白/黑叠层。 */
@Composable
fun defaultSegmentedTrackColor(onPrimarySurface: Boolean = false): Color {
    val colors = ExtendedTheme.colors
    val pureBlack = rememberPureBlackTheme()
    return when {
        pureBlack -> Color(0xFF141414)
        colors.isNightMode -> Color.White.copy(alpha = 0.08f)
        onPrimarySurface && colors.topBar.luminance() < 0.35f -> Color.White.copy(alpha = 0.16f)
        else -> Color.Black.copy(alpha = 0.055f)
    }
}

/** 选中滑块胶囊默认底色：相对轨道略提一档，柔和分层、无强对比或亮描边。 */
@Composable
fun defaultSegmentedIndicatorColor(onPrimarySurface: Boolean = false): Color {
    val colors = ExtendedTheme.colors
    val pureBlack = rememberPureBlackTheme()
    return when {
        pureBlack -> Color(0xFF282828)
        colors.isNightMode -> Color.White.copy(alpha = 0.16f)
        onPrimarySurface && colors.topBar.luminance() < 0.35f -> Color.White.copy(alpha = 0.28f)
        else -> Color.White
    }
}

/**
 * 胶囊分段页签单项：文字颜色随选中态平滑过渡，点击波纹裁剪到胶囊圆角内。
 */
@Composable
fun SegmentedTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = 0.65f),
    horizontalPadding: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor,
        animationSpec = tween(durationMillis = 200, easing = SegmentedSliderEasing),
        label = "SegmentedTabColor",
    )
    val textStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center,
    )
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(SegmentedTrackShape)
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideM2TabContent(textStyle = textStyle, content = content)
        }
    }
}

/**
 * 带排序下拉菜单的胶囊分段页签（如吧内「最新▾」、搜索「贴子▾」）：
 * 未选中时点击切页，已选中时点击弹出下拉菜单并旋转箭头。
 */
@Composable
fun SegmentedTabClickMenu(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor.copy(alpha = 0.65f),
) {
    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Press>()
            .collect {
                menuState.offset = it.pressPosition
            }
    }
    val rotate = animateFloatAsState(
        targetValue = if (menuState.expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200, easing = SegmentedSliderEasing),
        label = "SegmentedArrowRotate",
    )
    val arrowAlpha = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = SegmentedSliderEasing),
        label = "SegmentedArrowAlpha",
    )
    ClickMenu(
        menuContent = menuContent,
        menuState = menuState,
        indication = null,
        triggerShape = SegmentedTrackShape,
        propagateMinConstraints = true,
    ) {
        SegmentedTab(
            selected = selected,
            onClick = {
                if (!selected) {
                    onClick()
                } else {
                    menuState.toggle()
                }
            },
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource,
            selectedContentColor = selectedContentColor,
            unselectedContentColor = unselectedContentColor,
            horizontalPadding = 0.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 6.dp),
            ) {
                // 左侧与右侧下拉箭头等宽对称占位，保证文字本体在胶囊中严格几何居中，
                // 未选中时与普通页签同轴对齐，选中时箭头淡入不推动文字。
                Spacer(modifier = Modifier.width(16.dp))
                text()
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer {
                            rotationZ = rotate.value
                            alpha = arrowAlpha.value
                        }
                )
            }
        }
    }
}
