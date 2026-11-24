package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.contentMaxWidth

/** 单列正文只限制绝对上限；嵌套在已收窄的列表中时不会再次缩小。 */
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
    val maxWidth = LocalWindowSizeClass.current.contentMaxWidth
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = modifier
                .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
