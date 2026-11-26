package com.huanchengfly.tieba.post.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 屏幕刷新率自适应与多档位切换工具。
 *
 * 偏好值（[PREF_KEY_REFRESH_RATE]）约定：
 * - [MODE_AUTO]（默认）：交还系统调度（`preferredDisplayModeId = 0`, `preferredRefreshRate = 0f`），
 *   兼容 LTPO / ARR 可变刷新率与省电策略；
 * - [MODE_MAX]：自动匹配当前物理分辨率下最高刷新率模式；
 * - 整数字符串（如 `"60"` / `"90"` / `"120"` / `"144"`）：锁定设备支持的对应刷新率档位。
 */
object RefreshRateUtil {
    const val PREF_KEY_REFRESH_RATE = "preferred_refresh_rate"
    const val MODE_AUTO = "auto"
    const val MODE_MAX = "max"

    /**
     * 获取当前屏幕在同一物理分辨率下支持的全部刷新率档位（取整升序去重，单位 Hz）。
     */
    fun getSupportedRefreshRates(context: Context): List<Int> {
        val display = resolveDisplay(context) ?: return listOf(60)
        val sameResModes = getSameResolutionModes(display)
        val rates = linkedSetOf<Int>()
        sameResModes.forEach { mode ->
            val hz = mode.refreshRate.roundToInt()
            if (hz > 0) rates.add(hz)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                mode.alternativeRefreshRates.forEach { alt ->
                    val altHz = alt.roundToInt()
                    if (altHz > 0) rates.add(altHz)
                }
            }
        }
        if (rates.isEmpty()) {
            val fallback = display.refreshRate.roundToInt().takeIf { it > 0 } ?: 60
            rates.add(fallback)
        }
        return rates.sorted()
    }

    /**
     * 将偏好设置应用到 Activity 窗口，立即提交给 WindowManager 生效（无需重启）。
     */
    fun applyRefreshRate(activity: Activity, prefValue: String?) {
        val window = activity.window ?: return
        val display = resolveDisplay(activity) ?: return
        val mode = prefValue.orEmpty().ifBlank { MODE_AUTO }
        val params = window.attributes

        when (mode) {
            MODE_AUTO -> {
                if (params.preferredDisplayModeId == 0 && params.preferredRefreshRate == 0f) return
                params.preferredDisplayModeId = 0
                params.preferredRefreshRate = 0f
                window.attributes = params
            }

            MODE_MAX -> {
                val sameResModes = getSameResolutionModes(display)
                val bestMode = sameResModes.maxByOrNull { it.refreshRate }
                val targetRate = bestMode?.refreshRate ?: display.refreshRate
                val targetModeId = bestMode?.modeId ?: 0
                if (params.preferredDisplayModeId == targetModeId &&
                    abs(params.preferredRefreshRate - targetRate) < 0.1f
                ) {
                    return
                }
                params.preferredDisplayModeId = targetModeId
                params.preferredRefreshRate = targetRate
                window.attributes = params
            }

            else -> {
                val requestedHz = mode.toFloatOrNull()
                if (requestedHz == null || requestedHz <= 0f) {
                    applyRefreshRate(activity, MODE_AUTO)
                    return
                }
                val sameResModes = getSameResolutionModes(display)
                // 优先精确匹配同分辨率 mode（±1.5Hz 误差，覆盖 59.94 / 60.000004 等浮点值），
                // 其次选不超过目标值的最高档，最后兜底最接近档。
                val matchedMode = sameResModes.minByOrNull { abs(it.refreshRate - requestedHz) }
                    ?.takeIf { abs(it.refreshRate - requestedHz) <= 1.5f }
                    ?: sameResModes.filter { it.refreshRate <= requestedHz + 1.5f }
                        .maxByOrNull { it.refreshRate }
                    ?: sameResModes.minByOrNull { abs(it.refreshRate - requestedHz) }

                val targetModeId = matchedMode?.modeId ?: 0
                val targetRate = if (matchedMode != null && abs(matchedMode.refreshRate - requestedHz) <= 1.5f) {
                    matchedMode.refreshRate
                } else {
                    requestedHz
                }
                if (params.preferredDisplayModeId == targetModeId &&
                    abs(params.preferredRefreshRate - targetRate) < 0.1f
                ) {
                    return
                }
                params.preferredDisplayModeId = targetModeId
                params.preferredRefreshRate = targetRate
                window.attributes = params
            }
        }
    }

    /**
     * 只保留与当前物理分辨率相同的显示模式，避免部分高分屏设备误切分辨率导致黑屏或重绘闪烁。
     */
    private fun getSameResolutionModes(display: Display): List<Display.Mode> {
        val currentMode = runCatching { display.mode }.getOrNull()
        val allModes = runCatching { display.supportedModes?.toList().orEmpty() }.getOrDefault(emptyList())
        if (currentMode == null || allModes.isEmpty()) return allModes
        val filtered = allModes.filter {
            it.physicalWidth == currentMode.physicalWidth &&
                it.physicalHeight == currentMode.physicalHeight
        }
        return filtered.ifEmpty { allModes }
    }

    @Suppress("DEPRECATION")
    private fun resolveDisplay(context: Context): Display? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
            }
        }.getOrNull() ?: runCatching {
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }.getOrNull()
    }
}
