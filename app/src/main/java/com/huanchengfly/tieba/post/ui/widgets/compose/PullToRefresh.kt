package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults as MaterialPullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme

@Stable
class PullRefreshOverlayState internal constructor() {
    var indicator by mutableStateOf<(@Composable BoxScope.() -> Unit)?>(null)
    var topInset by mutableStateOf(0.dp)
}

val LocalPullRefreshOverlay = compositionLocalOf<PullRefreshOverlayState?> { null }

@Composable
fun PullRefreshIndicatorContent(
    isRefreshing: Boolean,
    willLoad: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = ExtendedTheme.colors.primary,
            )
        } else {
            Text(
                text = stringResource(if (willLoad) R.string.release_to_refresh else R.string.pull_down_to_refresh),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private val RefreshIndicatorSize = 40.dp

@Composable
fun PullToRefreshLayout(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    indicator: (@Composable BoxScope.(isRefreshing: Boolean, willLoad: Boolean) -> Unit)? = null,
    refreshDistance: Dp = PullToRefreshDefaults.RefreshDistance,
    refreshingOffset: Dp = PullToRefreshDefaults.RefreshingOffset,
    threshold: Float = 0.75f,
    indicatorTopInset: Dp = 0.dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    val overlay = LocalPullRefreshOverlay.current
    val activePage = LocalShouldLoad.current && enabled
    val currentRefreshing by rememberUpdatedState(refreshing)
    val currentIndicator by rememberUpdatedState(indicator)
    val maxDistance by rememberUpdatedState(refreshingOffset + RefreshIndicatorSize)
    val drawIndicator: @Composable BoxScope.() -> Unit = remember(state) {
        {
            val custom = currentIndicator
            if (custom == null) {
                MaterialPullToRefreshDefaults.Indicator(
                    state = state,
                    isRefreshing = currentRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = ExtendedTheme.colors.card,
                    color = ExtendedTheme.colors.primary,
                    maxDistance = maxDistance,
                )
            } else {
                MaterialPullToRefreshDefaults.IndicatorBox(
                    state = state,
                    isRefreshing = currentRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = ExtendedTheme.colors.card,
                    maxDistance = maxDistance,
                ) {
                    custom(currentRefreshing, state.distanceFraction >= 1f)
                }
            }
        }
    }
    DisposableEffect(overlay, activePage, drawIndicator) {
        if (overlay != null && activePage) overlay.indicator = drawIndicator
        onDispose {
            // 已退出的页不能清掉随后接管 overlay 的页签指示器。
            if (overlay?.indicator === drawIndicator) overlay.indicator = null
        }
    }
    SideEffect {
        if (overlay?.indicator === drawIndicator) overlay.topInset = indicatorTopInset
    }
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        state = state,
        enabled = activePage,
        threshold = refreshDistance * threshold,
        indicator = {
            if (overlay == null && activePage) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = indicatorTopInset)) {
                    drawIndicator()
                }
            }
        },
    ) {
        content()
    }
}

object PullToRefreshDefaults {
    val RefreshingOffset = 36.dp
    val RefreshDistance = 72.dp
}
