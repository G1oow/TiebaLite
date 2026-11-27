package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.PhotoSizeSelectActual
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.ui.common.PbContentText
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.utils.topBarInsetsHeight
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.StringUtil.buildAnnotatedStringWithUser
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.min

@Composable
fun QuotePostCard(
    quotePostInfo: SearchThreadBean.PostInfo,
    mainPost: SearchThreadBean.MainPost,
    onMainPostClick: (SearchThreadBean.MainPost) -> Unit,
    modifier: Modifier = Modifier,
    medias: ImmutableList<SearchThreadBean.MediaInfo> = persistentListOf(),
    keyword: String? = null,
) {
    val quoteContentString = remember(quotePostInfo) {
        buildAnnotatedStringWithUser(
            quotePostInfo.user.userId,
            quotePostInfo.user.userName ?: "",
            quotePostInfo.user.showNickname,
            quotePostInfo.content
        )
    }
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HighlightText(
            text = quoteContentString,
            style = MaterialTheme.typography.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            highlightKeywords = (keyword?.split(" ") ?: emptyList()).toImmutableList()
        )
        MainPostCard(
            mainPost = mainPost,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(ExtendedTheme.colors.card)
                .clickable {
                    onMainPostClick(mainPost)
                },
            medias = medias,
            keyword = keyword
        )
    }
}

@Composable
fun MainPostCard(
    mainPost: SearchThreadBean.MainPost,
    modifier: Modifier = Modifier,
    medias: ImmutableList<SearchThreadBean.MediaInfo> = persistentListOf(),
    keyword: String? = null,
) {
    val titleString = remember(mainPost) {
        buildAnnotatedStringWithUser(
            mainPost.user.userId,
            mainPost.user.userName ?: "",
            mainPost.user.showNickname,
            mainPost.title
        )
    }
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HighlightText(
            text = titleString,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            highlightKeywords = (keyword?.split(" ") ?: emptyList()).toImmutableList()
        )
        if (mainPost.content.isNotBlank()) {
            PbContentText(
                text = mainPost.content,
                style = MaterialTheme.typography.body2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SearchMedia(medias = medias)
    }
}

@Composable
fun SearchThreadList(
    data: ImmutableList<SearchThreadBean.ThreadInfoBean>,
    lazyListState: LazyListState,
    onItemClick: (SearchThreadBean.ThreadInfoBean) -> Unit,
    onItemUserClick: (SearchThreadBean.UserInfoBean) -> Unit,
    onItemForumClick: (SearchThreadBean.ForumInfo) -> Unit,
    modifier: Modifier = Modifier,
    onQuotePostClick: (SearchThreadBean.PostInfo) -> Unit = {},
    onMainPostClick: (SearchThreadBean.MainPost) -> Unit = {},
    hideForum: Boolean = false,
    searchKeyword: String? = null,
    header: LazyListScope.() -> Unit = {},
) {
    MyLazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {
        header()
        itemsIndexed(data) { index, item ->
            if (index > 0) {
                FeedListDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            SearchThreadItem(
                item = item,
                onClick = onItemClick,
                onUserClick = onItemUserClick,
                onForumClick = onItemForumClick,
                onQuotePostClick = onQuotePostClick,
                onMainPostClick = onMainPostClick,
                hideForum = hideForum,
                searchKeyword = searchKeyword
            )
        }
    }
}

@Composable
fun SearchThreadUserHeader(
    user: SearchThreadBean.UserInfoBean,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UserHeader(
        avatar = {
            Avatar(
                data = StringUtil.getAvatarUrl(user.portrait),
                size = Sizes.Small,
                contentDescription = null
            )
        },
        name = {
            Text(
                text = StringUtil.getUsernameAnnotatedString(
                    LocalContext.current,
                    user.userName.orEmpty(),
                    user.showNickname,
                    color = LocalContentColor.current
                )
            )
        },
        desc = {
            Text(
                text = DateTimeUtils.getRelativeTimeString(LocalContext.current, time)
            )
        },
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun SearchThreadItem(
    item: SearchThreadBean.ThreadInfoBean,
    onClick: (SearchThreadBean.ThreadInfoBean) -> Unit,
    onUserClick: (SearchThreadBean.UserInfoBean) -> Unit,
    onForumClick: (SearchThreadBean.ForumInfo) -> Unit,
    modifier: Modifier = Modifier,
    onQuotePostClick: (SearchThreadBean.PostInfo) -> Unit = {},
    onMainPostClick: (SearchThreadBean.MainPost) -> Unit = {},
    hideForum: Boolean = false,
    searchKeyword: String? = null,
) {
    val style = rememberFeedCardStyle()
    val replyNum = remember(item.postNum) { item.postNum.toIntOrNull() ?: 0 }
    val agreeNum = remember(item.likeNum) { item.likeNum.toIntOrNull() ?: 0 }
    val shareNum = remember(item.shareNum) { item.shareNum.toLongOrNull() ?: 0L }
    val forumName = if (!hideForum && item.forumName.isNotEmpty()) item.forumName else null
    val forumAvatar = if (!hideForum) item.forumInfo.avatar.takeIf { it.isNotBlank() } else null
    StyledFeedCardBody(
        style = style,
        authorPortrait = item.user.portrait,
        authorName = item.user.userName.orEmpty(),
        authorNameShow = item.user.showNickname.orEmpty(),
        timeString = item.time,
        forumName = forumName,
        forumAvatar = forumAvatar,
        replyNum = replyNum,
        agreeNum = agreeNum,
        hasAgree = false,
        onClick = { onClick(item) },
        onClickReply = { onClick(item) },
        onAgree = {},
        onClickUser = { onUserClick(item.user) },
        onClickForum = { onForumClick(item.forumInfo) },
        modifier = modifier,
        classicHeader = {
            SearchThreadUserHeader(
                user = item.user,
                time = item.time,
                onClick = { onUserClick(item.user) }
            )
        },
        contentBlock = {
            ThreadContent(
                title = item.title,
                abstractText = item.content,
                showTitle = item.mainPost == null && item.title.isNotBlank(),
                showAbstract = item.content.isNotBlank(),
                maxLines = 2,
                highlightKeywords = (searchKeyword?.split(" ") ?: emptyList()).toImmutableList(),
            )
            if (item.mainPost != null) {
                if (item.postInfo != null) {
                    QuotePostCard(
                        quotePostInfo = item.postInfo,
                        mainPost = item.mainPost,
                        onMainPostClick = onMainPostClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .clickable {
                                onQuotePostClick(item.postInfo)
                            },
                        medias = item.media.toImmutableList(),
                        keyword = searchKeyword
                    )
                } else {
                    MainPostCard(
                        mainPost = item.mainPost,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExtendedTheme.colors.floorCard)
                            .clickable {
                                onMainPostClick(item.mainPost)
                            },
                        medias = item.media.toImmutableList(),
                        keyword = searchKeyword
                    )
                }
            } else {
                SearchMedia(medias = item.media.toImmutableList())
            }
        },
        classicAction = {
            Row(modifier = Modifier.fillMaxWidth()) {
                ThreadReplyBtn(
                    replyNum = replyNum,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                ThreadAgreeBtn(
                    hasAgree = false,
                    agreeNum = agreeNum,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )

                ThreadShareBtn(
                    shareNum = shareNum,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }
        },
    )
}

@Composable
fun SearchMedia(
    medias: ImmutableList<SearchThreadBean.MediaInfo>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val pics = medias.filter { it.type == "pic" }
//    val video = medias.filter { it.type == "flash" }

    val picCount = remember(pics) {
        pics.size
    }
    val hasPhoto = remember(picCount) { picCount > 0 }
    val isSinglePhoto = remember(picCount) { picCount == 1 }
    val hideMedia = context.appPreferences.hideMedia

    val windowWidthSizeClass = BaseComposeActivity.LocalWindowSizeClass.current.widthSizeClass
    val singleMediaFraction = remember(windowWidthSizeClass) {
        if (windowWidthSizeClass == WindowWidthSizeClass.Compact)
            1f
        else 0.5f
    }

    if (hasPhoto && !hideMedia) {
        val mediaWidthFraction = remember(isSinglePhoto, singleMediaFraction) {
            if (isSinglePhoto) singleMediaFraction else 1f
        }
        val mediaAspectRatio = remember(isSinglePhoto) {
            if (isSinglePhoto) 2f else 3f
        }
        val showMediaCount = remember(pics) { min(pics.size, 3) }
        val hasMoreMedia = remember(pics) { pics.size > 3 }
        val showMedias = remember(pics) { pics.subList(0, showMediaCount) }
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(mediaWidthFraction)
                        .aspectRatio(mediaAspectRatio)
                        .clip(RoundedCornerShape(8.dp)),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    showMedias.fastForEach { pic ->
                        NetworkImage(
                            imageUri = pic.bigPic ?: pic.smallPic ?: pic.waterPic ?: "",
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            contentScale = ContentScale.Crop,
                            enablePreview = true
                        )
                    }
                }
                if (hasMoreMedia) {
                    Badge(
                        icon = Icons.Rounded.PhotoSizeSelectActual,
                        text = "${medias.size}",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBox(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onKeywordSubmit: (String) -> Unit = {},
    placeholder: @Composable () -> Unit = {},
    prependIcon: @Composable () -> Unit = {},
    appendIcon: @Composable () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    shape: Shape = RectangleShape,
    color: Color = ExtendedTheme.colors.topBarSurface,
    contentColor: Color = ExtendedTheme.colors.onTopBarSurface,
    elevation: Dp = 0.dp,
) {
    val isKeywordNotEmpty = remember(keyword) { keyword.isNotEmpty() }
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        elevation = elevation
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            prependIcon()
            BaseTextField(
                value = keyword,
                onValueChange = {
                    onKeywordChange(it)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onKeywordSubmit(keyword)
                    }
                ),
                placeholder = placeholder,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusEvent {
                        isFocused = it.isFocused
                    }
            )
            appendIcon()
            AnimatedVisibility(visible = isKeywordNotEmpty && isFocused) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 24.dp),
                                role = Role.Button
                            ) { onKeywordChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = stringResource(id = R.string.button_clear)
                        )
                    }
                }
            }
            AnimatedVisibility(visible = isKeywordNotEmpty) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 24.dp),
                            role = Role.Button
                        ) { onKeywordSubmit(keyword) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.button_search)
                    )
                }
            }
        }
    }
}

/**
 * 搜索列表吸顶分组条（精准匹配 / 相关吧 / 相关用户）与悬浮顶栏的绘制层联动：
 * - 不在布局里额外加 `topBarInsetsHeight()`，确保列表位于顶部时紧贴页签栏下沿；
 * - 吸顶后随顶栏展开比例在顶栏下沿与状态栏下沿之间平滑过渡，不改变布局高度（AGENTS.md §6.3）；
 * - 当下一分组条（[nextHeaderKey]）顶上来时平滑向上让位并淡出，避免两级吸顶条重叠或滞留状态栏区。
 */
@Composable
fun Modifier.searchStickyHeaderFollowTopBar(
    lazyListState: LazyListState,
    headerKey: Any,
    nextHeaderKey: Any? = null,
): Modifier {
    val barsAutoHideState = LocalBarsAutoHideState.current
    val topBarHeight = LocalTopBarHeight.current
    val statusBarInset = topBarInsetsHeight()
    return this
        .zIndex(1f)
        .graphicsLayer {
            val layoutInfo = lazyListState.layoutInfo
            val headerInfo = layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == headerKey }
                ?: return@graphicsLayer
            val naturalTop = (headerInfo.offset - layoutInfo.viewportStartOffset).coerceAtLeast(0)
            val shownFraction = 1f - (barsAutoHideState?.hiddenFraction ?: 0f)
            val targetTop = statusBarInset.toPx() +
                (topBarHeight - statusBarInset).coerceAtLeast(0.dp).toPx() * shownFraction
            val pinnedY = maxOf(naturalTop.toFloat(), targetTop)
            val nextHeaderTop = nextHeaderKey?.let { nextKey ->
                layoutInfo.visibleItemsInfo
                    .firstOrNull { it.key == nextKey }
                    ?.let { (it.offset - layoutInfo.viewportStartOffset).toFloat() }
            }
            val pushedY = if (nextHeaderTop != null && size.height > 0f) {
                minOf(pinnedY, nextHeaderTop - size.height)
            } else {
                pinnedY
            }
            translationY = pushedY - naturalTop
            alpha = if (size.height > 0f && pushedY < targetTop) {
                ((pushedY + size.height - targetTop) / size.height).coerceIn(0f, 1f)
            } else {
                1f
            }
        }
}