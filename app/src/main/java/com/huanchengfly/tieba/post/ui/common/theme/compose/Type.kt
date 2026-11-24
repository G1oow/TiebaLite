package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.material.Typography
import androidx.compose.material3.Typography as M3Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val DefaultTextStyle = TextStyle.Default.copy()

// Set of Material typography styles to start with
val Typography = Typography(
    body1 = DefaultTextStyle.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    subtitle1 = DefaultTextStyle.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    button = DefaultTextStyle.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp
    )
)

/**
 * M3 侧字号表，与 [Typography] 逐角色同值（AGENTS.md §5.2 的桥接约定）。
 *
 * 只覆盖与 M3 默认值不同的角色，迁移时按「M2 角色 → M3 角色」搬运：
 * `h6 → titleLarge`、`body1 → bodyLarge`、`subtitle1 → titleMedium`、`button → labelLarge`。
 *
 * - h6 在 M2 是 20sp / 行高 32sp / Medium，M3 的 titleLarge 默认 22sp，直接套用标题会变大；
 * - body1 / subtitle1 / button 在本工程里没有行高（继承 [TextStyle.Default]），M3 默认带行高，
 *   这里同样不带，保证迁移后行距不跳。
 *
 * M3 的 bodyMedium / bodySmall / titleSmall / headlineSmall 与 M2 的 body2 / caption / subtitle2 / h5
 * 本来就同值，不在此重复声明，沿用 M3 默认即可。
 */
val Material3Typography = M3Typography(
    titleLarge = DefaultTextStyle.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = DefaultTextStyle.copy(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    titleMedium = DefaultTextStyle.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = DefaultTextStyle.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp
    )
)
