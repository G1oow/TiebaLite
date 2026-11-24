package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 顶栏 / 底栏「跟随滑动自动隐藏」的共享状态。
 *
 * 设计约束（勿轻易改动）：
 * 1. **不消费滚动**。[nestedScrollConnection] 的 pre/post 回调一律返回 [Offset.Zero]，
 *    只「观察」已消费的位移方向，从不把用户的滑动增量拿去做动画。因此隐藏动画
 *    不会给滚动本身加速、减速或产生抢手（拖拽 delta 仍 1:1 交给列表）。
 * 2. **独立动画**。方向满足阈值后，栏的收起/展开用固定时长的 tween 播完，
 *    与手指位移解耦——松手后仍会平滑地滑到底，不会停在半路。
 * 3. **可反向打断**。[Animatable] 自带 MutatorMutex，方向反转时直接改目标值，
 *    动画从当前位置反向继续，不回跳。
 */
@Stable
class BarsAutoHideState internal constructor(
    private val scope: CoroutineScope,
    private val hideThresholdPx: Float,
) {
    private val fraction = Animatable(0f)

    /** 0 = 完全展示，1 = 完全隐藏。**读这个值会在动画期间触发逐帧布局**。 */
    val hiddenFraction: Float
        get() = fraction.value

    private var lastDirection = 0
    private var accumulated = 0f

    /** 关闭后不再改变隐藏状态（例如「不跟随滑动隐藏」设置关掉时）。 */
    var enabled: Boolean = true

    private fun animateTo(target: Float) {
        if (fraction.targetValue == target) return
        scope.launch {
            fraction.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = HideDurationMillis, easing = FastOutSlowInEasing)
            )
        }
    }

    /** 立即回到「完全展示」，用于页面切换、跳转等需要重置的场景。 */
    fun show() {
        accumulated = 0f
        lastDirection = 0
        animateTo(0f)
    }

    /**
     * 观察滚动位移（px，正数为朝列表头部滚动）。
     *
     * 只累加、不消费；同方向累计超过阈值才触发一次状态切换，避免像素级抖动
     * 把栏抖成频闪。
     */
    internal fun onScroll(deltaY: Float) {
        if (!enabled || deltaY == 0f) return
        val direction = if (deltaY > 0f) 1 else -1
        if (direction != lastDirection) {
            lastDirection = direction
            accumulated = 0f
        }
        accumulated += abs(deltaY)
        if (accumulated < hideThresholdPx) return
        accumulated = 0f
        // 向下滚动（看更新的内容，delta < 0）收起，向上滚动展开。
        animateTo(if (direction < 0) 1f else 0f)
    }

    internal val nestedScrollConnection: NestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                onScroll(consumed.y)
                // 不消费：滚动增量全部留给列表本身。
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // 惯性滚动同样只观察，不改速度。
                return Velocity.Zero
            }
        }
}

private const val HideDurationMillis = 220

/** 触发隐藏/展示所需的最小同向滚动距离。 */
private val HideScrollThreshold = 12.dp

/**
 * 供各 Scaffold / 栏组件读取的自动隐藏状态。
 *
 * 由 [com.huanchengfly.tieba.post.arch.BaseComposeActivity] 在 Activity 级别提供，
 * 因此任何页面（含直接用 Material `Scaffold` 的页面）都能拿到同一份状态。
 */
val LocalBarsAutoHideState = compositionLocalOf<BarsAutoHideState?> { null }

@Composable
fun rememberBarsAutoHideState(): BarsAutoHideState {
    val scope = rememberCoroutineScope()
    val thresholdPx = with(LocalDensity.current) { HideScrollThreshold.toPx() }
    // 用 rememberPreferenceAsState 而不是 appPreferences 缓存读：后者在组合期只拿到
    // DataStore delegate 的当前快照，切换开关不触发重组，要等 Activity 重建才生效。
    val enabled by rememberPreferenceAsState(
        key = booleanPreferencesKey("hide_bars_on_scroll"),
        defaultValue = true,
    )
    return remember(scope, thresholdPx, enabled) {
        BarsAutoHideState(scope = scope, hideThresholdPx = thresholdPx).apply {
            this.enabled = enabled
        }
    }
}

