package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 把 [ExtendedColors] 映射成 M3 [ColorScheme]（AGENTS.md §5.2 的桥接约定）。
 *
 * 约定：
 * 1. 配色唯一来源仍是 [ExtendedColors]，这里只做「改名」，不引入新配色；
 * 2. `surfaceTint` 固定 Transparent —— M3 会拿主色给有高程的表面染色，本工程是扁平观感，
 *    染色后胶囊、卡片、栏都会偏色；
 * 3. `surfaceContainer*` / `surfaceBright` / `surfaceDim` 是 M3 1.2+ 新增槽位，
 *    [ExtendedColors] 没有对位字段，按「背景 / 卡片 / 芯片」三档就近填充，
 *    避免 M3 组件回落到基线紫灰；
 * 4. `error` 系列没有主题来源，沿用 M3 基线色；
 * 5. [ExtendedColors.isNightMode] 不能直接当深浅判断 —— 半透明浅色主题也走深色基线，
 *    调用方要传 `TiebaLiteTheme` 里算好的 `isDarkColorPalette`。
 */
fun material3ColorScheme(
    colors: ExtendedColors,
    darkTheme: Boolean,
): ColorScheme {
    val baseline = if (darkTheme) darkColorScheme() else lightColorScheme()
    return baseline.copy(
        primary = colors.accent,
        onPrimary = colors.onAccent,
        primaryContainer = colors.chip,
        onPrimaryContainer = colors.onChip,
        inversePrimary = colors.primary,
        secondary = colors.primary,
        onSecondary = colors.onPrimary,
        secondaryContainer = colors.chip,
        onSecondaryContainer = colors.onChip,
        tertiary = colors.accent,
        onTertiary = colors.onAccent,
        tertiaryContainer = colors.chip,
        onTertiaryContainer = colors.onChip,
        background = colors.background,
        onBackground = colors.text,
        surface = colors.card,
        onSurface = colors.text,
        surfaceVariant = colors.chip,
        onSurfaceVariant = colors.textSecondary,
        surfaceTint = Color.Transparent,
        // 反色面用于 Snackbar 一类「浮在上面」的提示，取值与 M2 的 onSurface / surface 反色一致。
        inverseSurface = colors.text,
        inverseOnSurface = colors.background,
        outline = colors.textDisabled,
        outlineVariant = colors.divider,
        scrim = Color.Black,
        surfaceBright = colors.card,
        surfaceDim = colors.background,
        surfaceContainerLowest = colors.background,
        surfaceContainerLow = colors.card,
        surfaceContainer = colors.card,
        surfaceContainerHigh = colors.chip,
        surfaceContainerHighest = colors.chip,
    )
}
