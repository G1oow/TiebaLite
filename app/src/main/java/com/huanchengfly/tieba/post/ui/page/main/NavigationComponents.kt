package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.White
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.utils.bottomBarInsetsHeight
import com.huanchengfly.tieba.post.ui.utils.topBarInsetsPadding
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingBottomBar
import com.huanchengfly.tieba.post.ui.widgets.compose.floatingBarColor
import com.huanchengfly.tieba.post.ui.widgets.compose.floatingBarIndicatorBorderColor
import com.huanchengfly.tieba.post.ui.widgets.compose.floatingBarIndicatorColor
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberFloatingBarStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomNavigationItem
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.AdaptiveLayout

enum class LayoutType {
    HEADER, CONTENT
}

@Composable
fun PermanentNavigationDrawer(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(modifier.fillMaxSize()) {
        drawerContent()
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

private val ActiveIndicatorHeight = 56.dp
private val ActiveIndicatorWidth = 240.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NavigationDrawerItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = Color.Transparent,
    selectedBackgroundColor: Color = MaterialTheme.colors.primary.copy(0.25f),
    itemColor: Color = MaterialTheme.colors.onSurface,
    selectedItemColor: Color = MaterialTheme.colors.primary,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier
            .height(ActiveIndicatorHeight)
            .fillMaxWidth(),
        shape = shape,
        color = if (selected) selectedBackgroundColor else backgroundColor,
        interactionSource = interactionSource,
    ) {
        Row(
            Modifier.padding(start = 16.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                val iconColor = if (selected) selectedItemColor else itemColor
                CompositionLocalProvider(LocalContentColor provides iconColor, content = icon)
                Spacer(Modifier.width(12.dp))
            }
            Box(Modifier.weight(1f)) {
                val labelColor = if (selected) selectedItemColor else itemColor
                CompositionLocalProvider(LocalContentColor provides labelColor, content = label)
            }
            if (badge != null) {
                Spacer(Modifier.width(12.dp))
                val badgeColor = if (selected) selectedItemColor else itemColor
                CompositionLocalProvider(LocalContentColor provides badgeColor, content = badge)
            }
        }
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun NavigationDrawerContent(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationContentPosition: MainNavigationContentPosition
) {
    PositionLayout(
        modifier = Modifier
            .width(ActiveIndicatorWidth)
            .background(ExtendedTheme.colors.bottomBar)
            .padding(16.dp),
        content = {
            Column(
                modifier = Modifier
                    .layoutId(LayoutType.HEADER)
                    .topBarInsetsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp) // NavigationRailVerticalPadding
            ) {
                val account = LocalAccount.current
                if (account != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        AccountNavIcon(spacer = false, size = Sizes.Large)
                        Text(
                            text = account.nameShow ?: account.name,
                            style = MaterialTheme.typography.subtitle1,
                            color = ExtendedTheme.colors.text
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(
                            data = R.drawable.ic_launcher_new_round,
                            size = Sizes.Small,
                            contentDescription = stringResource(id = R.string.app_name)
                        )
                        Text(
                            text = stringResource(id = R.string.app_name).uppercase(),
                            style = MaterialTheme.typography.h6,
                            color = ExtendedTheme.colors.primary
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .layoutId(LayoutType.CONTENT)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                navigationItems.fastForEachIndexed { index, navigationItem ->
                    NavigationDrawerItem(
                        selected = index == currentPosition,
                        onClick = {
                            if (index == currentPosition) {
                                onReselected(index)
                            } else {
                                onChangePosition(index)
                            }
                        },
                        label = { Text(text = navigationItem.title(index == currentPosition)) },
                        icon = {
                            Box {
                                Icon(
                                    painter = rememberAnimatedVectorPainter(
                                        animatedImageVector = navigationItem.icon(),
                                        atEnd = index == currentPosition
                                    ),
                                    contentDescription = navigationItem.title(index == currentPosition),
                                    modifier = Modifier.size(24.dp),
                                )
                                if (navigationItem.badge) {
                                    Text(
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        color = White,
                                        text = navigationItem.badgeText ?: "",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .align(Alignment.TopEnd)
                                            .background(
                                                color = MaterialTheme.colors.secondary,
                                                shape = CircleShape
                                            ),
                                    )
                                }
                            }
                        }
                    )
                }
            }
        },
        navigationContentPosition = navigationContentPosition
    )
}

@Composable
private fun PositionLayout(
    navigationContentPosition: MainNavigationContentPosition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = content,
        measurePolicy = { measurables, constraints ->
            lateinit var headerMeasurable: Measurable
            lateinit var contentMeasurable: Measurable
            measurables.forEach {
                when (it.layoutId) {
                    LayoutType.HEADER -> headerMeasurable = it
                    LayoutType.CONTENT -> contentMeasurable = it
                    else -> error("Unknown layoutId encountered!")
                }
            }

            val headerPlaceable = headerMeasurable.measure(constraints)
            val contentPlaceable = contentMeasurable.measure(
                constraints.offset(vertical = -headerPlaceable.height)
            )
            layout(constraints.maxWidth, constraints.maxHeight) {
                // Place the header, this goes at the top
                headerPlaceable.placeRelative(0, 0)

                // Determine how much space is not taken up by the content
                val nonContentVerticalSpace = constraints.maxHeight - contentPlaceable.height

                val contentPlaceableY = when (navigationContentPosition) {
                    // Figure out the place we want to place the content, with respect to the
                    // parent (ignoring the header for now)
                    MainNavigationContentPosition.TOP -> 0
                    MainNavigationContentPosition.CENTER -> nonContentVerticalSpace / 2
                }
                    // And finally, make sure we don't overlap with the header.
                    .coerceAtLeast(headerPlaceable.height)

                contentPlaceable.placeRelative(0, contentPlaceableY)
            }
        }
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun NavigationRail(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationContentPosition: MainNavigationContentPosition
) {
    NavigationRail(
        backgroundColor = ExtendedTheme.colors.bottomBar,
        contentColor = ExtendedTheme.colors.unselected,
        modifier = Modifier
            .fillMaxHeight()
            .topBarInsetsPadding(),
        elevation = 0.dp,
        header = {
            AccountNavIcon(spacer = false)
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = if (navigationContentPosition == MainNavigationContentPosition.TOP) Arrangement.Top else Arrangement.Center
        ) {
            navigationItems.forEachIndexed { index, navigationItem ->
                NavigationRailItem(
                    selected = index == currentPosition,
                    onClick = {
                        if (index == currentPosition) {
                            onReselected(index)
                        } else {
                            onChangePosition(index)
                        }
                    },
                    icon = {
                        Box {
                            Icon(
                                painter = rememberAnimatedVectorPainter(
                                    animatedImageVector = navigationItem.icon(),
                                    atEnd = index == currentPosition
                                ),
                                contentDescription = navigationItem.title(index == currentPosition),
                                modifier = Modifier.size(16.dp),
                            )
                            if (navigationItem.badge) {
                                Text(
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    color = White,
                                    text = navigationItem.badgeText ?: "",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .align(Alignment.TopEnd)
                                        .background(
                                            color = MaterialTheme.colors.secondary,
                                            shape = CircleShape
                                        ),
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationDivider(
    themeColors: ExtendedColors = ExtendedTheme.colors
) {
    if (!themeColors.isNightMode) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(themeColors.divider)
        )
    }
}

/** 胶囊内单个入口的高度（不含胶囊上下内边距）。 */
private val FloatingItemHeight = 56.dp

/**
 * 选中项胶囊底：固定 64×44dp，四项共用同一尺寸（未选中时不画底，但照样占位）。
 * 固定尺寸是「展开均匀」的前提——位置只由四等分的单元决定，与标签文字长短无关。
 */
private val SelectedPillWidth = 64.dp
private val SelectedPillHeight = 44.dp
private val SelectedPillShape: Shape = RoundedCornerShape(percent = 50)

/** 标签行占位高度：始终预留，图标因此不会在选中/未选中之间上下跳。 */
private val FloatingLabelHeight = 12.dp

/**
 * 主界面底部导航（胶囊悬浮样式）。
 *
 * 视觉与位移都由 [FloatingBottomBar] 统一提供：胶囊底色、圆角、阴影、
 * 以及「整块滑出屏幕」的隐藏行为；这里只负责四个导航入口。
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun BottomNavigation(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    val barStyle = rememberFloatingBarStyle()
    val isNight = ExtendedTheme.colors.isNightMode
    val selectedColor = MaterialTheme.colors.secondary
    // 滑块（选中指示器）整条只有一个：它的横向位置随页签动画滑到对应单元，
    // 所以切换页签看到的是「一块滑过去」，而不是两块各自换位。
    val indicatorFill = floatingBarIndicatorColor(barStyle, selectedColor, isNight)
    val indicatorStroke = floatingBarIndicatorBorderColor(barStyle, selectedColor, isNight)
    FloatingBottomBar(color = floatingBarColor(themeColors.bottomBar)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(FloatingItemHeight),
        ) {
            val itemCount = navigationItems.size.coerceAtLeast(1)
            val cellWidth = maxWidth / itemCount
            val indicatorOffset by animateDpAsState(
                targetValue = cellWidth * currentPosition + (cellWidth - SelectedPillWidth) / 2,
                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
            )
            // 滑块画在入口下面（先声明 = 先绘制），滑过时图标压在它上面。
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = indicatorOffset)
                    .size(SelectedPillWidth, SelectedPillHeight)
                    .clip(SelectedPillShape)
                    .background(indicatorFill)
                    .then(
                        if (indicatorStroke != null) {
                            Modifier.border(1.dp, indicatorStroke, SelectedPillShape)
                        } else {
                            Modifier
                        }
                    ),
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationItems.fastForEachIndexed { index, navigationItem ->
                    FloatingNavigationItem(
                        modifier = Modifier.weight(1f),
                        label = navigationItem.title(index == currentPosition),
                        selected = index == currentPosition,
                        onClick = {
                            if (index == currentPosition) {
                                onReselected(index)
                            } else {
                                onChangePosition(index)
                            }
                            navigationItem.onClick?.invoke()
                        },
                        icon = {
                            Box {
                                Icon(
                                    painter = rememberAnimatedVectorPainter(
                                        animatedImageVector = navigationItem.icon(),
                                        atEnd = index == currentPosition
                                    ),
                                    contentDescription = navigationItem.title(index == currentPosition),
                                    modifier = Modifier.size(24.dp),
                                )
                                if (navigationItem.badge) {
                                    Text(
                                        textAlign = TextAlign.Center,
                                        fontSize = 10.sp,
                                        color = White,
                                        text = navigationItem.badgeText ?: "",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .align(Alignment.TopEnd)
                                            .background(
                                                color = MaterialTheme.colors.secondary,
                                                shape = CircleShape
                                            ),
                                    )
                                }
                            }
                        },
                        selectedContentColor = selectedColor,
                        unselectedContentColor = themeColors.unselected,
                    )
                }
            }
        }
    }
}

/**
 * 胶囊内的单个入口：图标 + 选中态短标题。
 *
 * 选中指示器（滑块）由 [BottomNavigation] 统一绘制，这里只负责图标与标题；
 * 标题固定占 [FloatingLabelHeight] 槽位并用透明度淡入淡出，显隐因此不影响任何测量。
 */
@Composable
private fun FloatingNavigationItem(
    modifier: Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    selectedContentColor: Color,
    unselectedContentColor: Color,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) selectedContentColor else unselectedContentColor
    )
    val labelAlpha by animateFloatAsState(if (selected) 1f else 0f)
    Row(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
                Box(
                    modifier = Modifier.height(FloatingLabelHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.alpha(labelAlpha),
                    )
                }
            }
        }
    }
}

@Immutable
data class NavigationItem @OptIn(ExperimentalAnimationGraphicsApi::class) constructor(
    val id: String,
    val icon: @Composable () -> AnimatedImageVector,
    val title: @Composable (selected: Boolean) -> String,
    val badge: Boolean = false,
    val badgeText: String? = null,
    val onClick: (() -> Unit)? = null,
    val content: @Composable () -> Unit = {},
)
