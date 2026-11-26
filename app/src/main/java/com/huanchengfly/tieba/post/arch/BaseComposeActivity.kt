package com.huanchengfly.tieba.post.arch

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.huanchengfly.tieba.post.activities.BaseActivity
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.calculateWindowSizeClass
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalBarsAutoHideState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberBarsAutoHideState
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardStylePrefKey
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingBarStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.FloatingBarStylePrefKey
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalFeedCardStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalThreadCommentStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadCommentStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadCommentStylePrefKey
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalFloatingBarStyle
import com.huanchengfly.tieba.post.utils.RefreshRateUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccountProvider
import com.huanchengfly.tieba.post.utils.ThemeUtil

abstract class BaseComposeActivityWithParcelable<DATA : Parcelable> : BaseComposeActivityWithData<DATA>() {
    abstract val dataExtraKey: String

    override fun parseData(intent: Intent): DATA? {
        return intent.extras?.getParcelable(dataExtraKey)
    }
}

abstract class BaseComposeActivityWithData<DATA> : BaseComposeActivity() {
    var data: DATA? = null

    abstract fun parseData(intent: Intent): DATA?

    override fun onCreate(savedInstanceState: Bundle?) {
        data = parseData(intent)
        super.onCreate(savedInstanceState)
    }

    @Composable
    final override fun Content() {
        data?.let { data ->
            Content(data)
        }
    }

    @Composable
    abstract fun Content(data: DATA)
}

abstract class BaseComposeActivity : BaseActivity() {
    override val isNeedImmersionBar: Boolean = false
    override val isNeedFixBg: Boolean = false
    override val isNeedSetTheme: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        RefreshRateUtil.applyRefreshRate(this, appPreferences.preferredRefreshRate)
        setContent {
            TiebaLiteTheme {
                val systemUiController = rememberSystemUiController()
                SideEffect {
                    systemUiController.apply {
                        setStatusBarColor(
                            Color.Transparent,
                            darkIcons = ThemeUtil.isStatusBarFontDark()
                        )
                        setNavigationBarColor(
                            Color.Transparent,
                            darkIcons = ThemeUtil.isNavigationBarFontDark(),
                            navigationBarContrastEnforced = false
                        )
                    }
                }

                LaunchedEffect(key1 = "onCreateContent") {
                    onCreateContent(systemUiController)
                }

                LocalAccountProvider {
                    val windowSizeClass = calculateWindowSizeClass(activity = this)
                    val barsAutoHideState = rememberBarsAutoHideState()
                    val feedCardStylePref by rememberPreferenceAsState(
                        key = stringPreferencesKey(FeedCardStylePrefKey),
                        defaultValue = appPreferences.feedCardStyle ?: FeedCardStyle.ELEVATED.prefValue,
                    )
                    val threadCommentStylePref by rememberPreferenceAsState(
                        key = stringPreferencesKey(ThreadCommentStylePrefKey),
                        defaultValue = appPreferences.threadCommentStyle ?: ThreadCommentStyle.CLEAN_STREAM.prefValue,
                    )
                    val floatingBarStylePref by rememberPreferenceAsState(
                        key = stringPreferencesKey(FloatingBarStylePrefKey),
                        defaultValue = appPreferences.floatingBarStyle ?: FloatingBarStyle.SHADOW.prefValue,
                    )
                    val refreshRatePref by rememberPreferenceAsState(
                        key = stringPreferencesKey(RefreshRateUtil.PREF_KEY_REFRESH_RATE),
                        defaultValue = appPreferences.preferredRefreshRate ?: RefreshRateUtil.MODE_AUTO,
                    )
                    LaunchedEffect(refreshRatePref) {
                        RefreshRateUtil.applyRefreshRate(this@BaseComposeActivity, refreshRatePref)
                    }
                    CompositionLocalProvider(
                        LocalWindowSizeClass provides windowSizeClass,
                        LocalBarsAutoHideState provides barsAutoHideState,
                        LocalFeedCardStyle provides FeedCardStyle.of(feedCardStylePref),
                        LocalThreadCommentStyle provides ThreadCommentStyle.of(threadCommentStylePref),
                        LocalFloatingBarStyle provides FloatingBarStyle.of(floatingBarStylePref),
                    ) {
                        // 根部铺主题底色：窗口与多数页面的 Scaffold 背景都是 Transparent，
                        // 不铺底时未被内容覆盖的区域（状态栏 / 挖孔 / 导航栏）会直接露出系统黑底。
                        // 横向安全区（横屏刘海/挖孔、三键导航侧栏）：只做水平避让，
                        // 纵向由各栏自己的 insets 处理，避免重复内缩。
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colors.background)
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                                )
                                .nestedScroll(barsAutoHideState.nestedScrollConnection)
                        ) {
                            Content()
                        }
                    }
                }
            }
        }
    }

    /**
     * 在创建内容前执行
     *
     * @param systemUiController SystemUiController
     */
    open fun onCreateContent(
        systemUiController: SystemUiController
    ) {}

    @Composable
    abstract fun Content()

    fun handleCommonEvent(event: CommonUiEvent) {
        when (event) {
            is CommonUiEvent.Toast -> {
                Toast.makeText(this, event.message, event.length).show()
            }

            else -> {}
        }
    }

    companion object {
        val LocalWindowSizeClass =
            staticCompositionLocalOf<WindowSizeClass> {
                WindowSizeClass.calculateFromSize(DpSize(0.dp, 0.dp))
            }
    }
}



sealed interface CommonUiEvent : UiEvent {
    object ScrollToTop : CommonUiEvent

    object NavigateUp : CommonUiEvent

    data class Toast(
        val message: CharSequence,
        val length: Int = android.widget.Toast.LENGTH_SHORT
    ) : CommonUiEvent

    @Composable
    fun BaseViewModel<*, *, *, *>.bindScrollToTopEvent(lazyListState: LazyListState) {
        onEvent<ScrollToTop> {
            lazyListState.scrollToItem(0, 0)
        }
    }
}