package com.example.photosmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * 深浅色跟着系统设置走，不在应用里单独放一个开关：
 * 用户在系统里开了深色模式，应用就该是深色的，悬浮窗也一样，不然亮屏时突然一块白板更刺眼。
 * 配色（米白/青灰/陶土/靛蓝/石墨）由 ThemeState 提供，它是可观察状态，设置页点一下就整体重绘。
 */
@Composable
fun PHOTOSMANAGERTheme(
    content: @Composable () -> Unit
) {
    val palette = ThemeState.palette
    val colorScheme = if (isSystemInDarkTheme()) palette.dark else palette.light

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
