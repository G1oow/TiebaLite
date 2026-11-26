package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
/**
 * 帖子详情页「评论与楼中楼」卡片视觉样式（设置 → 个性化 → 评论与楼中楼样式）。
 *
 * - [CLEAN_STREAM]：方案 B（默认）—— 通栏轻读流 + 左侧 2dp 对话引导线 + 12dp 柔和楼中楼容器，保留高阅读密度；
 * - [SOFT_CARD]：方案 A —— 16dp 柔和浮卡（纯黑 #0D0D0D 无描边）+ 12dp 嵌套二级楼中楼容器 + 胶囊展开入口；
 * - [COMPACT_QUOTE]：方案 C —— 14dp 全宽浮卡（正文取消头像左缩进）+ 左侧主题色 3dp 引用条楼中楼。
 */
enum class ThreadCommentStyle(val prefValue: String) {
    SOFT_CARD("soft_card"),
    CLEAN_STREAM("clean_stream"),
    COMPACT_QUOTE("compact_quote");

    /** 是否采用独立圆角浮卡外壳（浮卡本身带上下间距与圆角，外部不再画通栏分隔线）。 */
    val isBoxedCard: Boolean
        get() = this == SOFT_CARD || this == COMPACT_QUOTE

    /** 正文与楼中楼是否按头像宽度左缩进：方案 C 全宽正文不缩进以换取更宽行宽。 */
    val indentContent: Boolean
        get() = this != COMPACT_QUOTE
    companion object {
        fun of(value: String?): ThreadCommentStyle =
            entries.firstOrNull { it.prefValue == value } ?: CLEAN_STREAM
    }
}
/** [ThreadCommentStyle] 对应的 DataStore 键；设置页 `ListPref` 与 `AppPreferencesUtils` 共用。 */
const val ThreadCommentStylePrefKey = "thread_comment_style"

/** Activity 级共享的评论卡片样式，避免列表内每条楼层重复订阅 DataStore。 */
val LocalThreadCommentStyle = compositionLocalOf<ThreadCommentStyle?> { null }

@Composable
fun rememberThreadCommentStyle(): ThreadCommentStyle {
    LocalThreadCommentStyle.current?.let { return it }
    val value by rememberPreferenceAsState(
        key = stringPreferencesKey(ThreadCommentStylePrefKey),
        defaultValue = ThreadCommentStyle.CLEAN_STREAM.prefValue,
    )
    return ThreadCommentStyle.of(value)
}

private val ThreadCommentEasing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)

/** 浮卡外层底色：与默认信息流柔和浮卡保持同一中性表面族（纯黑 #0D0D0D）。 */
@Composable
internal fun threadFloorContainerColor(style: ThreadCommentStyle): Color {
    if (!style.isBoxedCard) return Color.Transparent
    val bg = ExtendedTheme.colors.background
    val card = ExtendedTheme.colors.card
    val pureBlack = rememberPureBlackTheme()
    val dark = ExtendedTheme.colors.isNightMode || bg.luminance() < 0.2f
    return remember(style, bg, card, pureBlack, dark) {
        when {
            pureBlack -> Color(0xFF0D0D0D)
            dark -> Color.White.copy(alpha = 0.045f).compositeOver(bg)
            else -> card
        }
    }
}

/** 浮卡极细描边：纯黑 AMOLED 自动关闭，浅色与深邃灰保持极轻层次。 */
@Composable
internal fun threadFloorBorder(style: ThreadCommentStyle): BorderStroke? {
    if (!style.isBoxedCard) return null
    if (rememberPureBlackTheme()) return null
    val dark = ExtendedTheme.colors.isNightMode || ExtendedTheme.colors.background.luminance() < 0.2f
    val color = if (dark) Color.White.copy(alpha = 0.055f) else Color.Black.copy(alpha = 0.055f)
    return remember(color) { BorderStroke(1.dp, color) }
}

@Composable
internal fun threadFloorShadowElevation(style: ThreadCommentStyle): Dp {
    if (!style.isBoxedCard) return 0.dp
    if (rememberPureBlackTheme()) return 0.dp
    return if (ExtendedTheme.colors.isNightMode) 1.dp else 1.dp
}

/** 嵌套楼中楼二级容器底色：在浮卡上再柔和提一档（纯黑 #151515），通栏流上用 #121212。 */
@Composable
internal fun threadSubPostContainerColor(style: ThreadCommentStyle): Color {
    val bg = ExtendedTheme.colors.background
    val floorCard = ExtendedTheme.colors.floorCard
    val pureBlack = rememberPureBlackTheme()
    val dark = ExtendedTheme.colors.isNightMode || bg.luminance() < 0.2f
    return remember(style, bg, floorCard, pureBlack, dark) {
        when {
            pureBlack -> if (style.isBoxedCard) Color(0xFF151515) else Color(0xFF121212)
            dark -> if (style.isBoxedCard) {
                Color.White.copy(alpha = 0.085f).compositeOver(bg)
            } else {
                Color.White.copy(alpha = 0.055f).compositeOver(bg)
            }
            else -> if (style.isBoxedCard) {
                Color.Black.copy(alpha = 0.035f).compositeOver(bg)
            } else {
                floorCard
            }
        }
    }
}

@Composable
internal fun threadCommentHairlineColor(): Color {
    val dark = ExtendedTheme.colors.isNightMode || ExtendedTheme.colors.background.luminance() < 0.2f
    return if (dark) Color.White.copy(alpha = 0.065f) else Color.Black.copy(alpha = 0.06f)
}

/**
 * 评论楼层外壳：按 [style] 在「柔和浮卡 A / 通栏轻读 B / 全宽浮卡 C」之间统一切换。
 *
 * 1 楼主贴传入 `isFirstFloor = true` 时保持通栏清爽呈现，不强套回复浮卡。
 */
@Composable
fun ThreadFloorSurface(
    style: ThreadCommentStyle,
    isFirstFloor: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isFirstFloor || !style.isBoxedCard) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (isFirstFloor) 8.dp else 11.dp),
            content = content,
        )
        if (!isFirstFloor && style == ThreadCommentStyle.CLEAN_STREAM) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = threadCommentHairlineColor(),
            )
        }
        return
    }

    val shape = if (style == ThreadCommentStyle.SOFT_CARD) {
        RoundedCornerShape(16.dp)
    } else {
        RoundedCornerShape(14.dp)
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = if (style == ThreadCommentStyle.SOFT_CARD) 5.dp else 4.dp,
            ),
        shape = shape,
        color = threadFloorContainerColor(style),
        contentColor = ExtendedTheme.colors.text,
        border = threadFloorBorder(style),
        shadowElevation = threadFloorShadowElevation(style),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            content = content,
        )
    }
}

/**
 * 帖子内楼中楼二级预览容器：
 * - 方案 A：12dp 圆角柔和二级底块；
 * - 方案 B：左侧 2dp 竖向引导线 + 12dp 圆角轻底块；
 * - 方案 C：左侧 3dp 主题色引用竖条 + 10dp 圆角底块。
 */
@Composable
fun ThreadSubPostsContainer(
    style: ThreadCommentStyle,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val containerBg = threadSubPostContainerColor(style)
    when (style) {
        ThreadCommentStyle.SOFT_CARD -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(containerBg)
                    .padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                content = content,
            )
        }

        ThreadCommentStyle.CLEAN_STREAM -> {
            val dark = ExtendedTheme.colors.isNightMode || ExtendedTheme.colors.background.luminance() < 0.2f
            val guideColor = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .clip(CircleShape)
                        .background(guideColor)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerBg)
                        .padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    content = content,
                )
            }
        }

        ThreadCommentStyle.COMPACT_QUOTE -> {
            val accentColor = ExtendedTheme.colors.primary
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(10.dp))
                    .background(containerBg),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(accentColor)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    content = content,
                )
            }
        }
    }
}

/** 方案 A 楼中楼项间极淡分隔线。 */
@Composable
fun ThreadSubPostItemDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 12.dp),
        thickness = 0.5.dp,
        color = threadCommentHairlineColor(),
    )
}

/** 楼中楼「展开全部 N 条回复」入口：方案 A 用主色柔和胶囊，方案 B/C 用清爽文字链接 + 箭头。 */
@Composable
fun ThreadSubPostMoreButton(
    style: ThreadCommentStyle,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = ExtendedTheme.colors.primary
    if (style == ThreadCommentStyle.SOFT_CARD) {
        Row(
            modifier = modifier
                .padding(start = 12.dp, end = 12.dp, top = 2.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.13f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = accent),
                    onClick = onClick,
                )
                .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = accent,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = accent),
                    onClick = onClick,
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = accent,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** 评论 / 楼中楼胶囊点赞按钮：圆形波纹 + 绘制层按压微缩，已赞带柔和强调底色。 */
@Composable
fun ThreadCommentAgreeButton(
    hasAgreed: Boolean,
    agreeNum: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = ExtendedTheme.colors.accent
    val dark = ExtendedTheme.colors.isNightMode || ExtendedTheme.colors.background.luminance() < 0.2f
    val neutralBg = if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.045f)
    val contentColor by animateColorAsState(
        targetValue = if (hasAgreed) accent else ExtendedTheme.colors.textSecondary,
        animationSpec = tween(durationMillis = 200, easing = ThreadCommentEasing),
        label = "commentAgreeColor",
    )
    val bgColor by animateColorAsState(
        targetValue = if (hasAgreed) accent.copy(alpha = 0.14f) else neutralBg,
        animationSpec = tween(durationMillis = 200, easing = ThreadCommentEasing),
        label = "commentAgreeBgColor",
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(durationMillis = 110, easing = ThreadCommentEasing),
        label = "commentAgreeScale",
    )

    Row(
        modifier = modifier
            .graphicsLayer {
                val s = scale.value
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = accent),
                onClick = onClick,
            )
            .padding(horizontal = if (agreeNum > 0) 9.dp else 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = if (hasAgreed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(id = R.string.title_agree),
            tint = contentColor,
            modifier = Modifier.size(15.dp),
        )
        if (agreeNum > 0) {
            Text(
                text = agreeNum.getShortNumString(),
                color = contentColor,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
