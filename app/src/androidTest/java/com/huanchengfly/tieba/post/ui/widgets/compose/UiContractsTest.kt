package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiContractsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun barsAndStickyHeaderKeepLayoutSizeDuringAnimation() {
        lateinit var bars: BarsAutoHideState
        lateinit var list: androidx.compose.foundation.lazy.LazyListState
        compose.setContent {
            TiebaLiteTheme {
                bars = rememberBarsAutoHideState()
                bars.enabled = true
                list = rememberLazyListState()
                CompositionLocalProvider(LocalBarsAutoHideState provides bars) {
                    MyScaffold(topBar = { Toolbar("Toolbar") }) {
                        Box(Modifier.fillMaxSize().nestedScroll(bars.nestedScrollConnection)) {
                            MyLazyColumn(state = list) {
                                myStickyHeader(state = list, key = "header") {
                                    Box(Modifier.fillMaxWidth().height(48.dp)) { Text("Group") }
                                }
                                items(40) { Text("Row $it", Modifier.height(64.dp)) }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val before = compose.runOnIdle {
            listOf(list.layoutInfo.visibleItemsInfo.first { it.key == "header" }.size,
                list.layoutInfo.viewportSize.height, list.layoutInfo.beforeContentPadding)
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle {
            assertEquals(Offset.Zero, bars.nestedScrollConnection.onPostScroll(
                Offset(0f, -200f), Offset.Zero, NestedScrollSource.UserInput))
        }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle {
            assertEquals(before, listOf(list.layoutInfo.visibleItemsInfo.first { it.key == "header" }.size,
                list.layoutInfo.viewportSize.height, list.layoutInfo.beforeContentPadding))
        }
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle { assertEquals(1f, bars.hiddenFraction, 0.001f) }
        compose.mainClock.autoAdvance = true
    }

    private data object Signal : UiEvent

    @Test
    fun replacingEventCallbackKeepsOneSubscriptionAndUsesLatestHandler() {
        val bus = MutableSharedFlow<UiEvent>()
        var subscriptions = 0
        var received = 0
        val flow = bus.onSubscription { subscriptions++ }
        val handler = mutableStateOf<suspend (Signal) -> Unit>({ received = 1 })
        lateinit var scope: CoroutineScope
        compose.setContent {
            scope = rememberCoroutineScope()
            flow.onEvent<Signal>(handler.value)
        }
        compose.waitForIdle()
        compose.runOnIdle { handler.value = { received = 2 } }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, subscriptions)
            scope.launch { bus.emit(Signal) }
        }
        compose.waitUntil(5000) { received == 2 }
    }

    @Test
    fun pullRefreshUsesLatestCallbackAndKeepsRefreshingIndicator() {
        val refreshing = mutableStateOf(false)
        val firstCalls = mutableIntStateOf(0)
        val latestCalls = mutableIntStateOf(0)
        val callback = mutableStateOf<() -> Unit>({ firstCalls.intValue++ })
        compose.setContent {
            TiebaLiteTheme {
                MyScaffold(topBar = { Toolbar("Toolbar") }) {
                    PullToRefreshLayout(
                        refreshing = refreshing.value,
                        onRefresh = callback.value,
                        indicator = { isRefreshing, _ ->
                            Box(Modifier.testTag(if (isRefreshing) "refreshing" else "pulling")) {
                                Text("Indicator")
                            }
                        },
                    ) {
                        MyLazyColumn(modifier = Modifier.fillMaxSize().testTag("list")) {
                            items(30) { Text("Row $it", Modifier.height(64.dp)) }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { callback.value = { latestCalls.intValue++; refreshing.value = true } }
        compose.onNodeWithTag("list").performTouchInput { swipeDown() }
        compose.waitUntil(5000) { latestCalls.intValue == 1 }
        compose.runOnIdle { assertEquals(0, firstCalls.intValue) }
        compose.onNodeWithTag("refreshing").assertIsDisplayed()
        compose.runOnIdle { refreshing.value = false }
    }

    @Test
    fun snackbarClosesUpwardAndNextSnackbarIsNotMovedAway() {
        val host = SnackbarHostState()
        lateinit var scope: CoroutineScope
        compose.setContent {
            scope = rememberCoroutineScope()
            TiebaLiteTheme { MyScaffold(snackbarHostState = host) { Box(Modifier.fillMaxSize()) } }
        }
        compose.runOnIdle { scope.launch { host.showSnackbar("First", duration = SnackbarDuration.Indefinite) } }
        compose.onNodeWithText("First").assertIsDisplayed()
        compose.onNodeWithText("First").performTouchInput {
            down(center)
            moveBy(Offset(0f, -180f))
            up()
        }
        compose.waitUntil(5000) { host.currentSnackbarData == null }
        compose.runOnIdle { scope.launch { host.showSnackbar("Second", duration = SnackbarDuration.Indefinite) } }
        compose.onNodeWithText("Second").assertIsDisplayed()
        compose.runOnIdle { assertTrue(host.currentSnackbarData != null); host.currentSnackbarData?.dismiss() }
    }
}
