package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

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
 * 位置在**布局的放置阶段**算：`currentPageOffsetFraction` 每帧都在变，放在组合里读会让指示器
 * 每帧重组；这里只触发重新放置。位移直接取 pager 的偏移，不再叠一层弹簧动画——那会让指示器
 * 滞后于手指。
 */
@Composable
fun TabIndicatorScope.PagerTabIndicator(pagerState: PagerState) {
    Box(
        Modifier
            .tabIndicatorLayout { measurable, constraints, tabPositions ->
                val placeable = measurable.measure(
                    Constraints.fixed(
                        PagerTabIndicatorWidth.roundToPx(),
                        PagerTabIndicatorHeight.roundToPx()
                    )
                )
                layout(constraints.maxWidth, constraints.maxHeight) {
                    if (tabPositions.isEmpty()) return@layout
                    val currentPage = minOf(tabPositions.lastIndex, pagerState.currentPage)
                    val fraction = pagerState.currentPageOffsetFraction
                    val current = tabPositions[currentPage]
                    val currentLeft = current.left + (current.width - PagerTabIndicatorWidth) / 2
                    val neighbour = when {
                        fraction > 0 -> tabPositions.getOrNull(currentPage + 1)
                        fraction < 0 -> tabPositions.getOrNull(currentPage - 1)
                        else -> null
                    }
                    val left = if (neighbour != null) {
                        val neighbourLeft =
                            neighbour.left + (neighbour.width - PagerTabIndicatorWidth) / 2
                        lerp(currentLeft, neighbourLeft, kotlin.math.abs(fraction))
                    } else {
                        currentLeft
                    }
                    placeable.placeRelative(
                        x = left.roundToPx(),
                        y = constraints.maxHeight - placeable.height -
                                PagerTabIndicatorBottomGap.roundToPx()
                    )
                }
            }
            .clip(RoundedCornerShape(100))
            .background(color = LocalContentColor.current)
    )
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
                    .rotate(rotate)
                    .alpha(alpha)
            )
        }
    }
}
