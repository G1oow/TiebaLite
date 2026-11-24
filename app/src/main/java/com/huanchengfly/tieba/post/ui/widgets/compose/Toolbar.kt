package com.huanchengfly.tieba.post.ui.widgets.compose

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ProvideTextStyle as M2ProvideTextStyle
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass.Companion.Compact
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.compose.calcStatusBarColor

/** M3 `DropdownMenuItem` 默认左右内边距 12dp，这里保持迁移前的 16dp，菜单观感不变。 */
private val TopBarMenuItemPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

@Composable
fun accountNavIconIfCompact(): (@Composable () -> Unit)? =
    if (LocalWindowSizeClass.current.widthSizeClass == Compact) (@Composable { AccountNavIcon() })
    else null

@Composable
fun AccountNavIcon(
    onClick: (() -> Unit)? = null,
    spacer: Boolean = true,
    size: Dp = Sizes.Small
) {
    val navigator = LocalNavigator.current
    val currentAccount = LocalAccount.current
    if (spacer) Spacer(modifier = Modifier.width(12.dp))
    if (currentAccount == null) {
        Image(
            painter = rememberDrawablePainter(
                drawable = ContextCompat.getDrawable(
                    LocalContext.current,
                    R.drawable.ic_launcher_new_round
                )
            ),
            contentDescription = null,
            modifier = Modifier
                .clip(CircleShape)
                .size(size)
        )
    } else {
        val context = LocalContext.current
        val menuState = rememberMenuState()
        LongClickMenu(
            menuContent = {
                val allAccounts = AccountUtil.allAccounts
                allAccounts.forEach {
                    DropdownMenuItem(
                        onClick = { AccountUtil.switchAccount(context, it.id) },
                        contentPadding = TopBarMenuItemPadding,
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(Sizes.Small)
                                ) {
                                    Avatar(
                                        data = StringUtil.getAvatarUrl(it.portrait),
                                        contentDescription = stringResource(id = R.string.title_switch_account_long_press),
                                        size = Sizes.Small,
                                    )
                                    if (currentAccount.id == it.id) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = stringResource(id = R.string.desc_current_account),
                                            tint = Color.White,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(color = Color.Black.copy(0.35f))
                                                .padding(8.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = it.nameShow ?: it.name)
                            }
                        }
                    )
                }
                VerticalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                DropdownMenuItem(
                    onClick = {
                        navigator.navigate("login")
                    },
                    contentPadding = TopBarMenuItemPadding,
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(id = R.string.title_new_account),
                                tint = ExtendedTheme.colors.onChip,
                                modifier = Modifier
                                    .size(Sizes.Small)
                                    .clip(CircleShape)
                                    .background(color = ExtendedTheme.colors.chip)
                                    .padding(8.dp),
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = stringResource(id = R.string.title_new_account))
                        }
                    }
                )
            },
            menuState = menuState,
            onClick = onClick,
            shape = CircleShape
        ) {
            Avatar(
                data = StringUtil.getAvatarUrl(currentAccount.portrait),
                size = size,
                contentDescription = stringResource(id = R.string.title_switch_account_long_press)
            )
        }
    }
}

@Composable
fun ActionItem(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    ProvideContentColor(color = ExtendedTheme.colors.onTopBar) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
fun BackNavigationIcon(onBackPressed: () -> Unit) {
    ProvideContentColor(color = ExtendedTheme.colors.onTopBar) {
        IconButton(onClick = onBackPressed) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_round_arrow_back),
                contentDescription = stringResource(id = R.string.button_back)
            )
        }
    }
}

@Deprecated(
    "Use the non deprecated overload",
    ReplaceWith(
        """TitleCentredToolbar(
                title = { Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                modifier = modifier,
                insets = insets,
                navigationIcon = navigationIcon,
                actions = actions,
                content = content
            )""",
        "androidx.compose.ui.text.font.FontWeight",
        "androidx.compose.material3.MaterialTheme",
        "androidx.compose.material3.Text"
    )
)
@Composable
fun TitleCentredToolbar(
    title: String,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TitleCentredToolbar(
        title = {
            Text(text = title)
        },
        modifier = modifier,
        insets = insets,
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )
}

@Composable
fun TitleCentredToolbar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
        topBar = {
            // 标题居中的自定义行：M3 的 TopAppBar 强制 56dp 高、标题槽自带 start inset，
            // 与这里「内容定高 + 整行居中」的观感不一致，所以不进 TopAppBar，只保留行布局。
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    ProvideContentColor(color = ExtendedTheme.colors.onTopBar) {
                        navigationIcon?.invoke()

                        Spacer(modifier = Modifier.weight(1f))

                        actions()
                    }
                }

                Row(
                    Modifier
                        .fillMaxHeight()
                        .align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ProvideTopBarTitleStyle {
                        ProvideContentColor(color = ExtendedTheme.colors.onTopBar) {
                            title()
                        }
                    }
                }
            }
        },
        modifier = modifier,
        insets = insets,
        content = content
    )
}

@Composable
fun Toolbar(
    title: String,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Toolbar(
        title = {
            Text(text = title)
        },
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    title: @Composable (() -> Unit),
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = ExtendedTheme.colors.onTopBar,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TopAppBarContainer(
        topBar = {
            TopAppBar(
                title = {
                    ProvideTopBarTitleStyle {
                        ProvideContentColor(color = contentColor, content = title)
                    }
                },
                actions = {
                    ProvideContentColor(color = contentColor) {
                        actions()
                    }
                },
                navigationIcon = (@Composable {
                    ProvideContentColor(color = contentColor) {
                        navigationIcon?.invoke()
                    }
                }),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor,
                    actionIconContentColor = contentColor,
                ),
                // 与迁移前的 M2 顶栏同高，正文起始位置不变。
                expandedHeight = 56.dp,
                // M3 自带状态栏 inset，会和 TopAppBarContainer 手画的状态栏底色带叠加。
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        },
        content = content
    )
}

/**
 * 迁移期桥接：同时提供 M2 / M3 两套 `LocalTextStyle`。
 *
 * `title` 槽由各页面传入，里面仍有大量 M2 `Text`；只提供 M3 的 local 会让标题丢掉
 * h6 / Bold 回落到 body1。等页面标题都换成 M3 `Text` 后删掉 M2 那一层。
 */
@Composable
private fun ProvideTopBarTitleStyle(content: @Composable () -> Unit) {
    val style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
    M2ProvideTextStyle(style) {
        ProvideTextStyle(style) { content() }
    }
}

/**
 * 顶栏容器：「标题栏」+「次级槽（页签 / 搜索框）」。
 *
 * 收起行为：
 * 1. **整条栏一起收起**：状态栏底色带、标题行、次级槽（页签 / 搜索框）作为一整块
 *    向上滑出，收起后顶栏区域没有任何残留，直接是滚动内容；
 * 2. 位移只发生在绘制层（`graphicsLayer`），本节点布局高度恒等于栏的完整高度，
 *    所以 `MyScaffold` 算出的内容内边距不会逐帧变化（见 AGENTS.md §6 硬性约束 3）；
 * 3. 底色画在被位移的节点之内，栏滑走后不会留一条盖住内容的底带（§6 硬性约束 4）。
 *
 * 顶栏在 [MyScaffold] 里是浮层：正文铺满整屏，收起后露出的正是滚动内容。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopAppBarContainer(
    topBar: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    insets: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val barsState = LocalBarsAutoHideState.current
    val hiddenFraction = barsState?.hiddenFraction ?: 0f
    val statusBarInset = if (insets) {
        with(density) {
            WindowInsets.statusBars.union(WindowInsets.displayCutout).getTop(density).toDp()
        }
    } else {
        0.dp
    }
    val coroutineScope = rememberCoroutineScope()
    var barHeightPx by remember { mutableIntStateOf(0) }
    // 深色 / 纯黑主题下栏底与页面底色几乎相同，整条顶栏读起来像「没渲染出来的黑边」。
    // 按 §5.3 的浮层语义与底栏胶囊共用同一提亮规则，让顶栏成为可辨识的 Surface。
    val barColor = floatingBarColor(ExtendedTheme.colors.topBar)

    // 外层高度恒定（= 栏的完整高度），只有内层做位移：内容内边距因此不随动画变化。
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { barHeightPx = it.height }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = -barHeightPx * hiddenFraction },
        ) {
            // 状态栏底色带：与栏体同色，跟着整条栏一起滑出。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarInset)
                    .background(barColor)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = barColor)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onDoubleClick = {
                            coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop)
                        },
                        onClick = {},
                    ),
                content = topBar
            )
            if (content != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = barColor),
                    content = content
                )
            }
        }
    }
}
