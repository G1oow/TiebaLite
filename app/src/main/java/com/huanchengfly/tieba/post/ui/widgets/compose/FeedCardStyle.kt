package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.SwapCalls
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString

/**
 * 帖子卡片视觉样式（设置 → 个性化 → 帖子卡片样式）。
 *
 * - [ELEVATED]：方案 A（默认）—— 16dp 圆角柔和浮卡（带作者头、浅色/深灰微投影 + 极细描边，纯黑自动去描边），吧名收进底栏左侧；
 * - [TONAL_CAPSULE]：方案 D —— 18dp 大圆角无描边色调表面，吧名置顶左上角，胶囊式点赞/评论按钮；
 * - [MINIMAL]：方案 B —— 极简通栏信息流，单行轻量作者头 + 更突出标题与正文呼吸感 + 右对齐轻量互动计数；
 * - [CLASSIC]：原版样式 —— 保持历史 TiebaLite 通栏布局与底部三等分操作栏。
 */
enum class FeedCardStyle(val prefValue: String) {
    TONAL_CAPSULE("tonal_capsule"),
    ELEVATED("elevated"),
    MINIMAL("minimal"),
    CLASSIC("classic");

    /** 是否为独立圆角浮卡（卡片自带外边距与圆角，外部列表不再叠加通栏分隔线）。 */
    val isBoxedCard: Boolean
        get() = this == TONAL_CAPSULE || this == ELEVATED

    companion object {
        fun of(value: String?): FeedCardStyle =
            entries.firstOrNull { it.prefValue == value } ?: ELEVATED
    }
}

/** [FeedCardStyle] 对应的 DataStore 键；设置页 `ListPref` 与 `AppPreferencesUtils` 须共用同一常量。 */
const val FeedCardStylePrefKey = "feed_card_style"

/**
 * 顶层共享的帖子卡片样式（由 `BaseComposeActivity` 统一订阅 DataStore 后提供）。
 *
 * 列表页每个 item 都会读卡片样式：若在 `FeedCard` / `FeedListDivider` 内部直接订阅 DataStore Flow，
 * 滚动时每新进一个 item 都会新开协程监听，且首帧可能按默认值闪烁一次。通过 CompositionLocal 共享
 * 让整个 Activity 只订阅一次 DataStore。
 */
val LocalFeedCardStyle = compositionLocalOf<FeedCardStyle?> { null }

/** 读取当前帖子卡片样式，优先从 Activity 级 [LocalFeedCardStyle] O(1) 读取，预览/无宿主时兜底。 */
@Composable
fun rememberFeedCardStyle(): FeedCardStyle {
    LocalFeedCardStyle.current?.let { return it }
    val value by rememberPreferenceAsState(
        key = stringPreferencesKey(FeedCardStylePrefKey),
        defaultValue = FeedCardStyle.ELEVATED.prefValue,
    )
    return FeedCardStyle.of(value)
}
/**
 * 计算独立卡片容器底色（低对比柔和分层，兼容浅色 / 深邃灰 / 纯黑 AMOLED）。
 *
 * 纯黑主题下页面背景是 `#000000`，卡片固定用克制的中性 `#0D0D0D`（无偏色、不刺眼）；
 * 深邃灰与浅色同样压低叠加透明度，避免卡片与背景对比过强。
 */
@Composable
internal fun feedCardContainerColor(style: FeedCardStyle): Color {
    val bg = ExtendedTheme.colors.background
    val card = ExtendedTheme.colors.card
    val pureBlack = rememberPureBlackTheme()
    val dark = ExtendedTheme.colors.isNightMode || bg.luminance() < 0.2f
    return remember(style, bg, card, pureBlack, dark) {
        when (style) {
            FeedCardStyle.TONAL_CAPSULE -> when {
                pureBlack -> Color(0xFF0D0D0D)
                dark -> Color.White.copy(alpha = 0.035f).compositeOver(bg)
                else -> Color.Black.copy(alpha = 0.025f).compositeOver(bg)
            }

            FeedCardStyle.ELEVATED -> when {
                pureBlack -> Color(0xFF0D0D0D)
                dark -> Color.White.copy(alpha = 0.045f).compositeOver(bg)
                else -> card
            }

            FeedCardStyle.MINIMAL, FeedCardStyle.CLASSIC -> Color.Transparent
        }
    }
}

@Composable
internal fun feedCardBorder(style: FeedCardStyle): BorderStroke? {
    if (style != FeedCardStyle.ELEVATED) return null
    if (rememberPureBlackTheme()) return null
    val dark = ExtendedTheme.colors.isNightMode || ExtendedTheme.colors.background.luminance() < 0.2f
    val borderColor = if (dark) Color.White.copy(alpha = 0.055f) else Color.Black.copy(alpha = 0.05f)
    return remember(borderColor) { BorderStroke(1.dp, borderColor) }
}

@Composable
internal fun feedCardShadowElevation(style: FeedCardStyle): Dp {
    if (style != FeedCardStyle.ELEVATED) return 0.dp
    if (rememberPureBlackTheme()) return 0.dp
    return if (ExtendedTheme.colors.isNightMode) 1.5.dp else 1.dp
}

/** 卡片内部极细分隔线色（方案 A 底栏上边线 / 方案 B 分隔用）。 */
@Composable
internal fun feedCardHairlineColor(): Color {
    val dark = ExtendedTheme.colors.isNightMode || ExtendedTheme.colors.background.luminance() < 0.2f
    return if (dark) Color.White.copy(alpha = 0.055f) else Color.Black.copy(alpha = 0.05f)
}
/**
 * 方案 A（柔和浮卡）与方案 D（色调胶囊卡）共用的 M3 [Surface] 卡片外壳。
 *
 * 外边距 `horizontal = 12.dp, vertical = 5.dp` 让相邻浮卡保持 10dp 呼吸间距，
 * 调用方在 `isBoxedCard` 时隐藏通栏 `VerticalDivider`，即可自然过渡为卡片流。
 */
@Composable
internal fun StyledFeedCardSurface(
    style: FeedCardStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    innerPaddingBottom: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = if (style == FeedCardStyle.TONAL_CAPSULE) {
        RoundedCornerShape(18.dp)
    } else {
        RoundedCornerShape(16.dp)
    }
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp),
        shape = shape,
        color = feedCardContainerColor(style),
        contentColor = ExtendedTheme.colors.text,
        border = feedCardBorder(style),
        shadowElevation = feedCardShadowElevation(style),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = innerPaddingBottom),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** 方案 A / B / D 使用的胶囊式吧名标签（与原版方角 Chip 区分，更贴合 M3 视觉族）。 */
@Composable
internal fun FeedForumPill(
    forumName: String,
    forumAvatar: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (forumName.isBlank()) return
    val pureBlack = rememberPureBlackTheme()
    val dark = ExtendedTheme.colors.isNightMode || ExtendedTheme.colors.background.luminance() < 0.2f
    val pillBg = when {
        pureBlack -> Color.White.copy(alpha = 0.055f)
        dark -> Color.White.copy(alpha = 0.06f)
        else -> Color.Black.copy(alpha = 0.04f)
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(pillBg)
            .clickable(onClick = onClick)
            .padding(
                start = if (!forumAvatar.isNullOrBlank()) 4.dp else 9.dp,
                end = 9.dp,
                top = 3.dp,
                bottom = 3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (!forumAvatar.isNullOrBlank()) {
            Avatar(
                data = forumAvatar,
                size = 16.dp,
                contentDescription = null,
                shape = CircleShape,
            )
        }
        Text(
            text = stringResource(id = R.string.title_forum_name, forumName),
            color = ExtendedTheme.colors.text,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 方案 B / D 紧凑单行作者头部（22dp/26dp 头像 + 昵称 + · + 时间）。 */
@Composable
internal fun CompactAuthorRow(
    avatarPortrait: String?,
    authorName: AnnotatedString,
    timeText: String?,
    avatarSize: Dp,
    onClickUser: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClickUser),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (!avatarPortrait.isNullOrBlank()) {
                Avatar(
                    data = StringUtil.getAvatarUrl(avatarPortrait),
                    size = avatarSize,
                    contentDescription = null,
                    shape = CircleShape,
                )
            }
            Text(
                text = authorName,
                color = ExtendedTheme.colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (!timeText.isNullOrBlank()) {
                Text(
                    text = "· $timeText",
                    color = ExtendedTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
        }
        trailing()
    }
}

/** 方案 A / B / D 轻量或胶囊操作按钮（M3 Icon + Text）。 */
@Composable
internal fun FeedCompactActionButton(
    icon: ImageVector,
    contentDescription: String,
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ExtendedTheme.colors.textSecondary,
    pillContainer: Boolean = false,
    pillBackground: Color? = null,
) {
    val pureBlack = rememberPureBlackTheme()
    val dark = ExtendedTheme.colors.isNightMode || ExtendedTheme.colors.background.luminance() < 0.2f
    val defaultPillBg = when {
        pureBlack -> Color.White.copy(alpha = 0.055f)
        dark -> Color.White.copy(alpha = 0.06f)
        else -> Color.Black.copy(alpha = 0.04f)
    }
    val bg = when {
        pillBackground != null -> pillBackground
        pillContainer -> defaultPillBg
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (pillContainer) 11.dp else 9.dp,
                vertical = if (pillContainer) 6.dp else 6.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        if (!text.isNullOrEmpty()) {
            Text(
                text = text,
                color = tint,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 1,
            )
        }
    }
}

/** 方案 A 底栏：左侧吧名胶囊，右侧转发 / 评论 / 点赞。 */
@Composable
internal fun ElevatedFeedFooter(
    forumName: String?,
    forumAvatar: String?,
    onClickForum: () -> Unit,
    replyNum: Int,
    agreeNum: Int,
    hasAgree: Boolean,
    onClickReply: () -> Unit,
    onAgree: () -> Unit,
) {
    val likeColor by animateColorAsState(
        targetValue = if (hasAgree) ExtendedTheme.colors.primary else ExtendedTheme.colors.textSecondary,
        label = "elevatedLikeColor",
    )
    HorizontalDivider(
        modifier = Modifier.padding(top = 2.dp),
        thickness = 1.dp,
        color = feedCardHairlineColor(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (!forumName.isNullOrBlank()) {
                FeedForumPill(
                    forumName = forumName,
                    forumAvatar = forumAvatar,
                    onClick = onClickForum,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            FeedCompactActionButton(
                icon = Icons.Rounded.SwapCalls,
                contentDescription = stringResource(id = R.string.desc_share),
                text = null,
                onClick = {},
            )
            FeedCompactActionButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_comment_new),
                contentDescription = stringResource(id = R.string.desc_comment),
                text = if (replyNum == 0) null else replyNum.getShortNumString(),
                onClick = onClickReply,
            )
            FeedCompactActionButton(
                icon = if (hasAgree) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.desc_like),
                text = if (agreeNum == 0) null else agreeNum.getShortNumString(),
                tint = likeColor,
                onClick = onAgree,
            )
        }
    }
}

/** 方案 B 底栏：左侧吧名胶囊，右侧评论 / 点赞 / 转发（无顶部分隔线，整体更轻）。 */
@Composable
internal fun MinimalFeedFooter(
    forumName: String?,
    forumAvatar: String?,
    onClickForum: () -> Unit,
    replyNum: Int,
    agreeNum: Int,
    hasAgree: Boolean,
    onClickReply: () -> Unit,
    onAgree: () -> Unit,
) {
    val likeColor by animateColorAsState(
        targetValue = if (hasAgree) ExtendedTheme.colors.primary else ExtendedTheme.colors.textSecondary,
        label = "minimalLikeColor",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (!forumName.isNullOrBlank()) {
                FeedForumPill(
                    forumName = forumName,
                    forumAvatar = forumAvatar,
                    onClick = onClickForum,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FeedCompactActionButton(
                icon = ImageVector.vectorResource(id = R.drawable.ic_comment_new),
                contentDescription = stringResource(id = R.string.desc_comment),
                text = if (replyNum == 0) stringResource(id = R.string.title_reply) else replyNum.getShortNumString(),
                onClick = onClickReply,
            )
            FeedCompactActionButton(
                icon = if (hasAgree) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(id = R.string.desc_like),
                text = if (agreeNum == 0) stringResource(id = R.string.title_agree) else agreeNum.getShortNumString(),
                tint = likeColor,
                onClick = onAgree,
            )
            FeedCompactActionButton(
                icon = Icons.Rounded.SwapCalls,
                contentDescription = stringResource(id = R.string.desc_share),
                text = null,
                onClick = {},
            )
        }
    }
}

/** 方案 D 底栏：左侧胶囊点赞 + 胶囊评论，右侧轻量转发按钮。 */
@Composable
internal fun TonalCapsuleFeedFooter(
    replyNum: Int,
    agreeNum: Int,
    hasAgree: Boolean,
    onClickReply: () -> Unit,
    onAgree: () -> Unit,
) {
    val primary = ExtendedTheme.colors.primary
    val likeColor by animateColorAsState(
        targetValue = if (hasAgree) primary else ExtendedTheme.colors.textSecondary,
        label = "tonalLikeColor",
    )
    val likePillBg = if (hasAgree) primary.copy(alpha = 0.12f) else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FeedCompactActionButton(
            icon = if (hasAgree) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = stringResource(id = R.string.desc_like),
            text = if (agreeNum == 0) stringResource(id = R.string.title_agree) else agreeNum.getShortNumString(),
            tint = likeColor,
            pillContainer = true,
            pillBackground = likePillBg,
            onClick = onAgree,
        )
        FeedCompactActionButton(
            icon = ImageVector.vectorResource(id = R.drawable.ic_comment_new),
            contentDescription = stringResource(id = R.string.desc_comment),
            text = if (replyNum == 0) stringResource(id = R.string.title_reply) else replyNum.getShortNumString(),
            pillContainer = true,
            onClick = onClickReply,
        )
        Spacer(modifier = Modifier.weight(1f))
        FeedCompactActionButton(
            icon = Icons.Rounded.SwapCalls,
            contentDescription = stringResource(id = R.string.desc_share),
            text = null,
            onClick = {},
        )
    }
}

/**
 * 帖子列表项之间的分隔线：仅在通栏样式（[FeedCardStyle.MINIMAL] / [FeedCardStyle.CLASSIC]）下渲染，
 * 浮卡样式（[FeedCardStyle.TONAL_CAPSULE] / [FeedCardStyle.ELEVATED]）本身已有圆角与间距，不再画横线。
 */
@Composable
fun FeedListDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp,
) {
    val style = rememberFeedCardStyle()
    if (!style.isBoxedCard) {
        if (style == FeedCardStyle.MINIMAL) {
            HorizontalDivider(
                modifier = modifier,
                thickness = 1.dp,
                color = feedCardHairlineColor(),
            )
        } else {
            VerticalDivider(
                modifier = modifier,
                thickness = thickness,
            )
        }
    }
}
