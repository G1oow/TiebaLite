package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Shapes
import androidx.compose.material3.Shapes as M3Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(0.dp)
)

/**
 * M3 侧圆角表，`small` / `medium` / `large` 与 [Shapes] 同值，迁移后圆角不跳变。
 *
 * `large = 0dp` 是本工程既有观感（对话框是方角），不套用 M3 默认的 16dp；
 * `extraLarge` 取 28dp，与悬浮栏胶囊同一档，M3 里默认读 `extraLarge` 的组件（如弹层）
 * 因此也落在胶囊视觉族内。
 */
val Material3Shapes = M3Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
