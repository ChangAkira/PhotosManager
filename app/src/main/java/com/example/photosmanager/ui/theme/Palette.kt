package com.example.photosmanager.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 一套配色 = 亮色 + 暗色两个 ColorScheme。
 * 暗色不单独作为一个开关暴露给用户，跟着系统的深色模式走，
 * 所以每个主题都必须成对准备，不然系统切到深色时只有背景变黑、控件还是亮色的。
 */
class Palette(
    val id: String,
    val displayName: String,
    val swatch: Color,
    val light: ColorScheme,
    val dark: ColorScheme
)

/** 米白：应用原本的低饱和度暖灰，默认主题 */
private val WARM_BEIGE = Palette(
    id = "beige",
    displayName = "米白",
    swatch = Color(0xFF8B7D6B),
    light = lightColorScheme(
        primary = MutedPrimary,
        secondary = MutedSecondary,
        tertiary = MutedTertiary,
        background = BeigeBackground,
        surface = BeigeSurface,
        surfaceVariant = Color(0xFFF2EEE8),
        onBackground = MutedOnSurface,
        onSurface = MutedOnSurface,
        onSurfaceVariant = Color(0xFF6E6558),
        error = MutedError,
        errorContainer = MutedError.copy(alpha = 0.15f),
        primaryContainer = MutedPrimary.copy(alpha = 0.15f),
        secondaryContainer = MutedSecondary.copy(alpha = 0.15f)
    ),
    dark = darkColorScheme(
        primary = Color(0xFFC4B6A3),
        secondary = Color(0xFFB3A795),
        tertiary = Color(0xFFA89B8A),
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = Color(0xFF453E37),
        onBackground = DarkOnSurface,
        onSurface = DarkOnSurface,
        onSurfaceVariant = Color(0xFFCFC6BA),
        error = MutedError,
        errorContainer = MutedError.copy(alpha = 0.2f),
        primaryContainer = MutedPrimary.copy(alpha = 0.2f),
        secondaryContainer = MutedSecondary.copy(alpha = 0.2f)
    )
)

/** 青灰：偏冷的灰蓝，白天不刺眼 */
private val SLATE = Palette(
    id = "slate",
    displayName = "青灰",
    swatch = Color(0xFF5F7480),
    light = lightColorScheme(
        primary = Color(0xFF5F7480),
        secondary = Color(0xFF6E828D),
        tertiary = Color(0xFF8295A0),
        background = Color(0xFFF6F9FA),
        surface = Color(0xFFFCFDFE),
        surfaceVariant = Color(0xFFE7EEF1),
        onBackground = Color(0xFF3F4B52),
        onSurface = Color(0xFF3F4B52),
        onSurfaceVariant = Color(0xFF586670),
        error = Color(0xFFB08A8A),
        errorContainer = Color(0xFFB08A8A).copy(alpha = 0.15f),
        primaryContainer = Color(0xFF5F7480).copy(alpha = 0.15f),
        secondaryContainer = Color(0xFF6E828D).copy(alpha = 0.15f)
    ),
    dark = darkColorScheme(
        primary = Color(0xFF9FBAC7),
        secondary = Color(0xFF93AEBB),
        tertiary = Color(0xFF889FAA),
        background = Color(0xFF171D20),
        surface = Color(0xFF222A2E),
        surfaceVariant = Color(0xFF2E383D),
        onBackground = Color(0xFFDCE4E8),
        onSurface = Color(0xFFDCE4E8),
        onSurfaceVariant = Color(0xFFBAC7CE),
        error = Color(0xFFC9A0A0),
        errorContainer = Color(0xFFC9A0A0).copy(alpha = 0.2f),
        primaryContainer = Color(0xFF9FBAC7).copy(alpha = 0.2f),
        secondaryContainer = Color(0xFF93AEBB).copy(alpha = 0.2f)
    )
)

/** 陶土：砖红偏暖，比米白更有辨识度 */
private val TERRACOTTA = Palette(
    id = "terracotta",
    displayName = "陶土",
    swatch = Color(0xFFA96F57),
    light = lightColorScheme(
        primary = Color(0xFFA96F57),
        secondary = Color(0xFFB3806B),
        tertiary = Color(0xFFBC8E79),
        background = Color(0xFFFBF6F3),
        surface = Color(0xFFFEFAF8),
        surfaceVariant = Color(0xFFF4E7E1),
        onBackground = Color(0xFF5A4038),
        onSurface = Color(0xFF5A4038),
        onSurfaceVariant = Color(0xFF7A5A4E),
        error = Color(0xFFB4614F),
        errorContainer = Color(0xFFB4614F).copy(alpha = 0.15f),
        primaryContainer = Color(0xFFA96F57).copy(alpha = 0.15f),
        secondaryContainer = Color(0xFFB3806B).copy(alpha = 0.15f)
    ),
    dark = darkColorScheme(
        primary = Color(0xFFE0A189),
        secondary = Color(0xFFD39480),
        tertiary = Color(0xFFC78A76),
        background = Color(0xFF201612),
        surface = Color(0xFF2B201B),
        surfaceVariant = Color(0xFF3A2B25),
        onBackground = Color(0xFFEDDDD5),
        onSurface = Color(0xFFEDDDD5),
        onSurfaceVariant = Color(0xFFD6BFB5),
        error = Color(0xFFE29A86),
        errorContainer = Color(0xFFE29A86).copy(alpha = 0.2f),
        primaryContainer = Color(0xFFE0A189).copy(alpha = 0.2f),
        secondaryContainer = Color(0xFFD39480).copy(alpha = 0.2f)
    )
)

/** 靛蓝：饱和度略高的蓝紫，给想要"正常一点"的用户 */
private val INDIGO = Palette(
    id = "indigo",
    displayName = "靛蓝",
    swatch = Color(0xFF5C608F),
    light = lightColorScheme(
        primary = Color(0xFF5C608F),
        secondary = Color(0xFF6E72A0),
        tertiary = Color(0xFF8185B2),
        background = Color(0xFFF7F7FC),
        surface = Color(0xFFFEFEFF),
        surfaceVariant = Color(0xFFE8E9F4),
        onBackground = Color(0xFF3D4063),
        onSurface = Color(0xFF3D4063),
        onSurfaceVariant = Color(0xFF5B5F85),
        error = Color(0xFFA8607A),
        errorContainer = Color(0xFFA8607A).copy(alpha = 0.15f),
        primaryContainer = Color(0xFF5C608F).copy(alpha = 0.15f),
        secondaryContainer = Color(0xFF6E72A0).copy(alpha = 0.15f)
    ),
    dark = darkColorScheme(
        primary = Color(0xFFAEB2E4),
        secondary = Color(0xFFA0A5D6),
        tertiary = Color(0xFF949BC9),
        background = Color(0xFF161722),
        surface = Color(0xFF20212E),
        surfaceVariant = Color(0xFF2C2D3E),
        onBackground = Color(0xFFE1E2F2),
        onSurface = Color(0xFFE1E2F2),
        onSurfaceVariant = Color(0xFFC0C2DA),
        error = Color(0xFFDE9DB4),
        errorContainer = Color(0xFFDE9DB4).copy(alpha = 0.2f),
        primaryContainer = Color(0xFFAEB2E4).copy(alpha = 0.2f),
        secondaryContainer = Color(0xFFA0A5D6).copy(alpha = 0.2f)
    )
)

/** 石墨：接近纯中性灰，几乎没有色温倾向 */
private val GRAPHITE = Palette(
    id = "graphite",
    displayName = "石墨",
    swatch = Color(0xFF5F6368),
    light = lightColorScheme(
        primary = Color(0xFF5F6368),
        secondary = Color(0xFF71767C),
        tertiary = Color(0xFF84898F),
        background = Color(0xFFF6F6F7),
        surface = Color(0xFFFCFCFD),
        surfaceVariant = Color(0xFFE9EAEB),
        onBackground = Color(0xFF3C4043),
        onSurface = Color(0xFF3C4043),
        onSurfaceVariant = Color(0xFF5F6368),
        error = Color(0xFFA15555),
        errorContainer = Color(0xFFA15555).copy(alpha = 0.15f),
        primaryContainer = Color(0xFF5F6368).copy(alpha = 0.15f),
        secondaryContainer = Color(0xFF71767C).copy(alpha = 0.15f)
    ),
    dark = darkColorScheme(
        primary = Color(0xFFC7CBD1),
        secondary = Color(0xFFB6BBC1),
        tertiary = Color(0xFFA6ABB1),
        background = Color(0xFF151618),
        surface = Color(0xFF1F2123),
        surfaceVariant = Color(0xFF2B2D30),
        onBackground = Color(0xFFE3E4E6),
        onSurface = Color(0xFFE3E4E6),
        onSurfaceVariant = Color(0xFFC2C5C8),
        error = Color(0xFFE0A3A3),
        errorContainer = Color(0xFFE0A3A3).copy(alpha = 0.2f),
        primaryContainer = Color(0xFFC7CBD1).copy(alpha = 0.2f),
        secondaryContainer = Color(0xFFB6BBC1).copy(alpha = 0.2f)
    )
)

object PaletteCatalog {
    val ordered: List<Palette> = listOf(WARM_BEIGE, SLATE, TERRACOTTA, INDIGO, GRAPHITE)
    val default: Palette = WARM_BEIGE

    fun fromId(id: String?): Palette = ordered.firstOrNull { it.id == id } ?: default
}

/**
 * 当前配色放在可观察状态里：设置页点下去之后，正在显示的悬浮窗和界面要立刻跟着重绘。
 * 用普通 var 不会触发重组，用户会看到"点了没反应，要重开一次才行"。
 */
object ThemeState {
    var palette: Palette by mutableStateOf(PaletteCatalog.default)
        private set

    fun select(id: String?) {
        palette = PaletteCatalog.fromId(id)
    }
}
