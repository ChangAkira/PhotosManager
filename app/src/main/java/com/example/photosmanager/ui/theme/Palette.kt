package com.example.photosmanager.ui.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.photosmanager.R

/**
 * 一套配色 = 亮色 + 暗色两个 ColorScheme。
 * 暗色不单独作为开关暴露给用户，跟着系统的深色模式走，
 * 所以每个主题都必须成对准备，不然系统切到深色时只有背景变黑、控件还是亮色的。
 *
 * 色值与孪生项目 ScreenshotBye 的 res/values(-night)/colors_palettes.xml 一一对应：
 * 那边是 Views，只能把这些值写成 XML 主题，而这里就是那份表的源头，
 * 两边改一处要同步改另一处，否则"换主题"在两个应用里会长得不一样。
 */
class Palette(
    val id: String,
    @param:StringRes val nameRes: Int,
    val swatch: Color,
    val light: ColorScheme,
    val dark: ColorScheme
)

/*
 * 页面与卡片刻意不随配色变化：下面每套 scheme 里 background / surface / surfaceVariant /
 * surfaceContainer* 都是同一组中性值（亮 #F6F5F4 页面 + #EEEDEB 卡片，暗 #171718 + #2F2E30），
 * 主题颜色只作用于控件本身（按钮、旋钮、开关走 colorPrimary）。
 * 陶土配色的卡片不该是粉的，靛蓝配色的卡片不该是蓝的——对齐 ScreenshotBye 的
 * Widget.*.Card.Filled 与 page_background 做法，卡片一律用 PaletteCard 里的 surface。
 */

/** 米白 */
private val WARM_BEIGE = Palette(
    id = "beige",
    nameRes = R.string.palette_beige,
    swatch = Color(0xFF8B7D6B),
    light = lightColorScheme(
        primary = Color(0xFF8B7D6B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEBE8E4),
        onPrimaryContainer = Color(0xFF61584B),
        secondary = Color(0xFF9B8E7D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFEDEBE6),
        onSecondaryContainer = Color(0xFF6C6358),
        tertiary = Color(0xFFA89B8A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEFEDE8),
        onTertiaryContainer = Color(0xFF766C61),
        error = Color(0xFFB89A8A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF2ECE8),
        onErrorContainer = Color(0xFF816C61),
        background = Color(0xFFF6F5F4),
        onBackground = Color(0xFF5C5247),
        surface = Color(0xFFEEEDEB),
        onSurface = Color(0xFF5C5247),
        surfaceVariant = Color(0xFFE1E0DE),
        onSurfaceVariant = Color(0xFF6E6558),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F5F4),
        surfaceContainer = Color(0xFFF2F1F0),
        surfaceContainerHigh = Color(0xFFEAE9E7),
        surfaceContainerHighest = Color(0xFFE3E2E1),
        surfaceBright = Color(0xFFF8F7F6),
        surfaceDim = Color(0xFFDCDBD9),
        inverseSurface = Color(0xFF746B62),
        inverseOnSurface = Color(0xFFFEFEFD),
        inversePrimary = Color(0xFFB4AA9F),
        outline = Color(0xFFB5B0A8),
        outlineVariant = Color(0xFFD1CCC4),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFC4B6A3),
        onPrimary = Color(0xFF171412),
        primaryContainer = Color(0xFF49423A),
        onPrimaryContainer = Color(0xFFD9D0C3),
        secondary = Color(0xFFB3A795),
        onSecondary = Color(0xFF171412),
        secondaryContainer = Color(0xFF453F37),
        onSecondaryContainer = Color(0xFFCEC6BA),
        tertiary = Color(0xFFA89B8A),
        onTertiary = Color(0xFF171412),
        tertiaryContainer = Color(0xFF433D35),
        onTertiaryContainer = Color(0xFFC6BEB3),
        error = Color(0xFFB89A8A),
        onError = Color(0xFF171412),
        errorContainer = Color(0xFF463C35),
        onErrorContainer = Color(0xFFD1BDB3),
        background = Color(0xFF171718),
        onBackground = Color(0xFFE5DFD5),
        surface = Color(0xFF2F2E30),
        onSurface = Color(0xFFE5DFD5),
        surfaceVariant = Color(0xFF2E2E31),
        onSurfaceVariant = Color(0xFFCFC6BA),
        surfaceContainerLowest = Color(0xFF0E0E0F),
        surfaceContainerLow = Color(0xFF171718),
        surfaceContainer = Color(0xFF1E1E20),
        surfaceContainerHigh = Color(0xFF262628),
        surfaceContainerHighest = Color(0xFF2F2E30),
        surfaceBright = Color(0xFF232325),
        surfaceDim = Color(0xFF131314),
        inverseSurface = Color(0xFFC9C3BA),
        inverseOnSurface = Color(0xFF1D1A18),
        inversePrimary = Color(0xFF7F766A),
        outline = Color(0xFF7C766D),
        outlineVariant = Color(0xFF686058),
    )
)

/** 青灰 */
private val SLATE = Palette(
    id = "slate",
    nameRes = R.string.palette_slate,
    swatch = Color(0xFF5F7480),
    light = lightColorScheme(
        primary = Color(0xFF5F7480),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDFE5E8),
        onPrimaryContainer = Color(0xFF42515A),
        secondary = Color(0xFF6E828D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE2E7EA),
        onSecondaryContainer = Color(0xFF4D5B63),
        tertiary = Color(0xFF8295A0),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE5EAEC),
        onTertiaryContainer = Color(0xFF5B6870),
        error = Color(0xFFB08A8A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFECE8E9),
        onErrorContainer = Color(0xFF7B6161),
        background = Color(0xFFF6F5F4),
        onBackground = Color(0xFF3F4B52),
        surface = Color(0xFFEEEDEB),
        onSurface = Color(0xFF3F4B52),
        surfaceVariant = Color(0xFFE1E0DE),
        onSurfaceVariant = Color(0xFF586670),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F5F4),
        surfaceContainer = Color(0xFFF2F1F0),
        surfaceContainerHigh = Color(0xFFEAE9E7),
        surfaceContainerHighest = Color(0xFFE3E2E1),
        surfaceBright = Color(0xFFF8F7F6),
        surfaceDim = Color(0xFFDCDBD9),
        inverseSurface = Color(0xFF5A656B),
        inverseOnSurface = Color(0xFFFEFEFE),
        inversePrimary = Color(0xFF97A5AC),
        outline = Color(0xFFA7B0B5),
        outlineVariant = Color(0xFFC3CCD1),
    ),
    dark = darkColorScheme(
        primary = Color(0xFF9FBAC7),
        onPrimary = Color(0xFF0D1012),
        primaryContainer = Color(0xFF323C41),
        onPrimaryContainer = Color(0xFFC1D2DB),
        secondary = Color(0xFF93AEBB),
        onSecondary = Color(0xFF0D1012),
        secondaryContainer = Color(0xFF303A3F),
        onSecondaryContainer = Color(0xFFB9CAD3),
        tertiary = Color(0xFF889FAA),
        onTertiary = Color(0xFF0D1012),
        tertiaryContainer = Color(0xFF2E373C),
        onTertiaryContainer = Color(0xFFB2C1C8),
        error = Color(0xFFC9A0A0),
        onError = Color(0xFF0D1012),
        errorContainer = Color(0xFF3B373A),
        onErrorContainer = Color(0xFFDCC1C1),
        background = Color(0xFF171718),
        onBackground = Color(0xFFDCE4E8),
        surface = Color(0xFF2F2E30),
        onSurface = Color(0xFFDCE4E8),
        surfaceVariant = Color(0xFF2E2E31),
        onSurfaceVariant = Color(0xFFBAC7CE),
        surfaceContainerLowest = Color(0xFF0E0E0F),
        surfaceContainerLow = Color(0xFF171718),
        surfaceContainer = Color(0xFF1E1E20),
        surfaceContainerHigh = Color(0xFF262628),
        surfaceContainerHighest = Color(0xFF2F2E30),
        surfaceBright = Color(0xFF232325),
        surfaceDim = Color(0xFF131314),
        inverseSurface = Color(0xFFBEC6CA),
        inverseOnSurface = Color(0xFF111517),
        inversePrimary = Color(0xFF677981),
        outline = Color(0xFF687277),
        outlineVariant = Color(0xFF515C61),
    )
)

/** 陶土 */
private val TERRACOTTA = Palette(
    id = "terracotta",
    nameRes = R.string.palette_terracotta,
    swatch = Color(0xFFA96F57),
    light = lightColorScheme(
        primary = Color(0xFFA96F57),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEFE2DC),
        onPrimaryContainer = Color(0xFF764E3D),
        secondary = Color(0xFFB3806B),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFF0E4DF),
        onSecondaryContainer = Color(0xFF7D5A4B),
        tertiary = Color(0xFFBC8E79),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFF2E6E1),
        onTertiaryContainer = Color(0xFF846355),
        error = Color(0xFFB4614F),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF0E0DA),
        onErrorContainer = Color(0xFF7E4437),
        background = Color(0xFFF6F5F4),
        onBackground = Color(0xFF5A4038),
        surface = Color(0xFFEEEDEB),
        onSurface = Color(0xFF5A4038),
        surfaceVariant = Color(0xFFE1E0DE),
        onSurfaceVariant = Color(0xFF7A5A4E),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F5F4),
        surfaceContainer = Color(0xFFF2F1F0),
        surfaceContainerHigh = Color(0xFFEAE9E7),
        surfaceContainerHighest = Color(0xFFE3E2E1),
        surfaceBright = Color(0xFFF8F7F6),
        surfaceDim = Color(0xFFDCDBD9),
        inverseSurface = Color(0xFF725B54),
        inverseOnSurface = Color(0xFFFEFCFC),
        inversePrimary = Color(0xFFC7A192),
        outline = Color(0xFFBAA8A0),
        outlineVariant = Color(0xFFD6C4BC),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFE0A189),
        onPrimary = Color(0xFF120C0A),
        primaryContainer = Color(0xFF46322A),
        onPrimaryContainer = Color(0xFFEBC2B2),
        secondary = Color(0xFFD39480),
        onSecondary = Color(0xFF120C0A),
        secondaryContainer = Color(0xFF442F28),
        onSecondaryContainer = Color(0xFFE2B9AC),
        tertiary = Color(0xFFC78A76),
        onTertiary = Color(0xFF120C0A),
        tertiaryContainer = Color(0xFF412D26),
        onTertiaryContainer = Color(0xFFDBB3A6),
        error = Color(0xFFE29A86),
        onError = Color(0xFF120C0A),
        errorContainer = Color(0xFF473029),
        onErrorContainer = Color(0xFFECBDB0),
        background = Color(0xFF171718),
        onBackground = Color(0xFFEDDDD5),
        surface = Color(0xFF2F2E30),
        onSurface = Color(0xFFEDDDD5),
        surfaceVariant = Color(0xFF2E2E31),
        onSurfaceVariant = Color(0xFFD6BFB5),
        surfaceContainerLowest = Color(0xFF0E0E0F),
        surfaceContainerLow = Color(0xFF171718),
        surfaceContainer = Color(0xFF1E1E20),
        surfaceContainerHigh = Color(0xFF262628),
        surfaceContainerHighest = Color(0xFF2F2E30),
        surfaceBright = Color(0xFF232325),
        surfaceDim = Color(0xFF131314),
        inverseSurface = Color(0xFFCEBFB8),
        inverseOnSurface = Color(0xFF16100E),
        inversePrimary = Color(0xFF926959),
        outline = Color(0xFF7B6A64),
        outlineVariant = Color(0xFF615049),
    )
)

/** 靛蓝 */
private val INDIGO = Palette(
    id = "indigo",
    nameRes = R.string.palette_indigo,
    swatch = Color(0xFF5C608F),
    light = lightColorScheme(
        primary = Color(0xFF5C608F),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE0E0EC),
        onPrimaryContainer = Color(0xFF404364),
        secondary = Color(0xFF6E72A0),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE2E3EE),
        onSecondaryContainer = Color(0xFF4D5070),
        tertiary = Color(0xFF8185B2),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE5E6F1),
        onTertiaryContainer = Color(0xFF5A5D7D),
        error = Color(0xFFA8607A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFEBE0E8),
        onErrorContainer = Color(0xFF764355),
        background = Color(0xFFF6F5F4),
        onBackground = Color(0xFF3D4063),
        surface = Color(0xFFEEEDEB),
        onSurface = Color(0xFF3D4063),
        surfaceVariant = Color(0xFFE1E0DE),
        onSurfaceVariant = Color(0xFF5B5F85),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F5F4),
        surfaceContainer = Color(0xFFF2F1F0),
        surfaceContainerHigh = Color(0xFFEAE9E7),
        surfaceContainerHighest = Color(0xFFE3E2E1),
        surfaceBright = Color(0xFFF8F7F6),
        surfaceDim = Color(0xFFDCDBD9),
        inverseSurface = Color(0xFF595B7A),
        inverseOnSurface = Color(0xFFFEFEFF),
        inversePrimary = Color(0xFF9598B6),
        outline = Color(0xFFA9ABC0),
        outlineVariant = Color(0xFFC5C6D8),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFAEB2E4),
        onPrimary = Color(0xFF0C0D13),
        primaryContainer = Color(0xFF343649),
        onPrimaryContainer = Color(0xFFCACDED),
        secondary = Color(0xFFA0A5D6),
        onSecondary = Color(0xFF0C0D13),
        secondaryContainer = Color(0xFF323346),
        onSecondaryContainer = Color(0xFFC1C4E4),
        tertiary = Color(0xFF949BC9),
        onTertiary = Color(0xFF0C0D13),
        tertiaryContainer = Color(0xFF2F3143),
        onTertiaryContainer = Color(0xFFB9BEDC),
        error = Color(0xFFDE9DB4),
        onError = Color(0xFF0C0D13),
        errorContainer = Color(0xFF3E323F),
        onErrorContainer = Color(0xFFEABFCE),
        background = Color(0xFF171718),
        onBackground = Color(0xFFE1E2F2),
        surface = Color(0xFF2F2E30),
        onSurface = Color(0xFFE1E2F2),
        surfaceVariant = Color(0xFF2E2E31),
        onSurfaceVariant = Color(0xFFC0C2DA),
        surfaceContainerLowest = Color(0xFF0E0E0F),
        surfaceContainerLow = Color(0xFF171718),
        surfaceContainer = Color(0xFF1E1E20),
        surfaceContainerHigh = Color(0xFF262628),
        surfaceContainerHighest = Color(0xFF2F2E30),
        surfaceBright = Color(0xFF232325),
        surfaceDim = Color(0xFF131314),
        inverseSurface = Color(0xFFC3C4D3),
        inverseOnSurface = Color(0xFF101017),
        inversePrimary = Color(0xFF717494),
        outline = Color(0xFF6B6C7E),
        outlineVariant = Color(0xFF515265),
    )
)

/** 石墨 */
private val GRAPHITE = Palette(
    id = "graphite",
    nameRes = R.string.palette_graphite,
    swatch = Color(0xFF5F6368),
    light = lightColorScheme(
        primary = Color(0xFF5F6368),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDFE0E2),
        onPrimaryContainer = Color(0xFF424549),
        secondary = Color(0xFF71767C),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE2E3E5),
        onSecondaryContainer = Color(0xFF4F5357),
        tertiary = Color(0xFF84898F),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE5E6E7),
        onTertiaryContainer = Color(0xFF5C6064),
        error = Color(0xFFA15555),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFE9DEDF),
        onErrorContainer = Color(0xFF713B3B),
        background = Color(0xFFF6F5F4),
        onBackground = Color(0xFF3C4043),
        surface = Color(0xFFEEEDEB),
        onSurface = Color(0xFF3C4043),
        surfaceVariant = Color(0xFFE1E0DE),
        onSurfaceVariant = Color(0xFF5F6368),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF6F5F4),
        surfaceContainer = Color(0xFFF2F1F0),
        surfaceContainerHigh = Color(0xFFEAE9E7),
        surfaceContainerHighest = Color(0xFFE3E2E1),
        surfaceBright = Color(0xFFF8F7F6),
        surfaceDim = Color(0xFFDCDBD9),
        inverseSurface = Color(0xFF585B5E),
        inverseOnSurface = Color(0xFFFEFEFE),
        inversePrimary = Color(0xFF979A9D),
        outline = Color(0xFFAAACB0),
        outlineVariant = Color(0xFFC6C8CA),
    ),
    dark = darkColorScheme(
        primary = Color(0xFFC7CBD1),
        onPrimary = Color(0xFF0C0C0D),
        primaryContainer = Color(0xFF393A3D),
        onPrimaryContainer = Color(0xFFDBDDE1),
        secondary = Color(0xFFB6BBC1),
        onSecondary = Color(0xFF0C0C0D),
        secondaryContainer = Color(0xFF35373A),
        onSecondaryContainer = Color(0xFFD0D3D7),
        tertiary = Color(0xFFA6ABB1),
        onTertiary = Color(0xFF0C0C0D),
        tertiaryContainer = Color(0xFF323437),
        onTertiaryContainer = Color(0xFFC5C8CC),
        error = Color(0xFFE0A3A3),
        onError = Color(0xFF0C0C0D),
        errorContainer = Color(0xFF3E3234),
        onErrorContainer = Color(0xFFEBC3C3),
        background = Color(0xFF171718),
        onBackground = Color(0xFFE3E4E6),
        surface = Color(0xFF2F2E30),
        onSurface = Color(0xFFE3E4E6),
        surfaceVariant = Color(0xFF2E2E31),
        onSurfaceVariant = Color(0xFFC2C5C8),
        surfaceContainerLowest = Color(0xFF0E0E0F),
        surfaceContainerLow = Color(0xFF171718),
        surfaceContainer = Color(0xFF1E1E20),
        surfaceContainerHigh = Color(0xFF262628),
        surfaceContainerHighest = Color(0xFF2F2E30),
        surfaceBright = Color(0xFF232325),
        surfaceDim = Color(0xFF131314),
        inverseSurface = Color(0xFFC4C5C7),
        inverseOnSurface = Color(0xFF101012),
        inversePrimary = Color(0xFF818488),
        outline = Color(0xFF6C6E70),
        outlineVariant = Color(0xFF515356),
    )
)

object PaletteCatalog {
    val ordered: List<Palette> = listOf(WARM_BEIGE, SLATE, TERRACOTTA, INDIGO, GRAPHITE)

    /** 默认配色就是米白，和 ScreenshotBye 的 Theme.ScreenshotBye -> Beige 一致 */
    val default: Palette = WARM_BEIGE

    /** 用户存的选择不认得时退回默认，而不是崩在读配置上 */
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
