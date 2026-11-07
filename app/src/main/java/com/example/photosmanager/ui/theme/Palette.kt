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
 * 每套配色只负责「控件层」那 17 个角色：primary / secondary / tertiary / error 四条线
 * 及各自的 on*、*Container，外加 inversePrimary。页面、卡片、文字、描边一律走下面那份
 * 共用的中性色阶——这是刻意的：之前 34 个角色全按配色摊开，结果米白下正文是暖棕色、
 * 靛蓝下是冷蓝色，换主题像换了字体颜色，而底色早就中性化了，文字没有理由还跟着走。
 * 孪生项目 ScreenshotBye 的 res/values(-night)/colors_neutral.xml 是同一组数值，
 * 两边改一处要同步改另一处。
 */
class Palette(
    val id: String,
    @param:StringRes val nameRes: Int,
    val swatch: Color,
    val light: ColorScheme,
    val dark: ColorScheme
)

/** 中性色阶：五套配色共用，深浅各一组。取值见 ScreenshotBye 的 colors_neutral.xml */
private object Neutral {
    // 亮色：文字、描边都不带色温，五套配色共用
    val lightBackground = Color(0xFFF6F5F4)
    val lightOnBackground = Color(0xFF474645)
    val lightSurface = Color(0xFFEEEDEB)
    val lightOnSurface = Color(0xFF474645)
    val lightSurfaceVariant = Color(0xFFE1E0DE)
    val lightOnSurfaceVariant = Color(0xFF656462)
    val lightSurfaceContainerLowest = Color(0xFFFFFFFF)
    val lightSurfaceContainerLow = Color(0xFFF6F5F4)
    val lightSurfaceContainer = Color(0xFFF2F1F0)
    val lightSurfaceContainerHigh = Color(0xFFEAE9E7)
    val lightSurfaceContainerHighest = Color(0xFFE3E2E1)
    val lightSurfaceBright = Color(0xFFF8F7F6)
    val lightSurfaceDim = Color(0xFFDCDBD9)
    val lightInverseSurface = Color(0xFF666361)
    val lightInverseOnSurface = Color(0xFFFCFBFA)
    val lightOutline = Color(0xFF9B9A97)
    val lightOutlineVariant = Color(0xFFCAC9C6)

    // 暗色：同上，只是换一档亮度
    val darkBackground = Color(0xFF171718)
    val darkOnBackground = Color(0xFFE4E3E1)
    val darkSurface = Color(0xFF2F2E30)
    val darkOnSurface = Color(0xFFE4E3E1)
    val darkSurfaceVariant = Color(0xFF2E2E31)
    val darkOnSurfaceVariant = Color(0xFFC5C4C2)
    val darkSurfaceContainerLowest = Color(0xFF0E0E0F)
    val darkSurfaceContainerLow = Color(0xFF171718)
    val darkSurfaceContainer = Color(0xFF1E1E20)
    val darkSurfaceContainerHigh = Color(0xFF262628)
    val darkSurfaceContainerHighest = Color(0xFF2F2E30)
    val darkSurfaceBright = Color(0xFF232325)
    val darkSurfaceDim = Color(0xFF131314)
    val darkInverseSurface = Color(0xFFC5C5C4)
    val darkInverseOnSurface = Color(0xFF151513)
    val darkOutline = Color(0xFF71706E)
    val darkOutlineVariant = Color(0xFF585857)
}

/**
 * 配色只填控件层，中性层由 Neutral 统一提供。
 * 参数顺序固定，新增配色时照抄一份调用即可，不需要再写一遍底色和文字色。
 */
private fun lightScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
    error: Color,
    onError: Color,
    errorContainer: Color,
    onErrorContainer: Color,
    inversePrimary: Color
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    inversePrimary = inversePrimary,
    background = Neutral.lightBackground,
    onBackground = Neutral.lightOnBackground,
    surface = Neutral.lightSurface,
    onSurface = Neutral.lightOnSurface,
    surfaceVariant = Neutral.lightSurfaceVariant,
    onSurfaceVariant = Neutral.lightOnSurfaceVariant,
    surfaceContainerLowest = Neutral.lightSurfaceContainerLowest,
    surfaceContainerLow = Neutral.lightSurfaceContainerLow,
    surfaceContainer = Neutral.lightSurfaceContainer,
    surfaceContainerHigh = Neutral.lightSurfaceContainerHigh,
    surfaceContainerHighest = Neutral.lightSurfaceContainerHighest,
    surfaceBright = Neutral.lightSurfaceBright,
    surfaceDim = Neutral.lightSurfaceDim,
    inverseSurface = Neutral.lightInverseSurface,
    inverseOnSurface = Neutral.lightInverseOnSurface,
    outline = Neutral.lightOutline,
    outlineVariant = Neutral.lightOutlineVariant,
)

private fun darkScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
    error: Color,
    onError: Color,
    errorContainer: Color,
    onErrorContainer: Color,
    inversePrimary: Color
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,
    inversePrimary = inversePrimary,
    background = Neutral.darkBackground,
    onBackground = Neutral.darkOnBackground,
    surface = Neutral.darkSurface,
    onSurface = Neutral.darkOnSurface,
    surfaceVariant = Neutral.darkSurfaceVariant,
    onSurfaceVariant = Neutral.darkOnSurfaceVariant,
    surfaceContainerLowest = Neutral.darkSurfaceContainerLowest,
    surfaceContainerLow = Neutral.darkSurfaceContainerLow,
    surfaceContainer = Neutral.darkSurfaceContainer,
    surfaceContainerHigh = Neutral.darkSurfaceContainerHigh,
    surfaceContainerHighest = Neutral.darkSurfaceContainerHighest,
    surfaceBright = Neutral.darkSurfaceBright,
    surfaceDim = Neutral.darkSurfaceDim,
    inverseSurface = Neutral.darkInverseSurface,
    inverseOnSurface = Neutral.darkInverseOnSurface,
    outline = Neutral.darkOutline,
    outlineVariant = Neutral.darkOutlineVariant,
)

private val WARM_BEIGE = Palette(
    id = "beige",
    nameRes = R.string.palette_beige,
    swatch = Color(0xFF8B7D6B),
    light = lightScheme(
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
        inversePrimary = Color(0xFFB4AA9F)
    ),
    dark = darkScheme(
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
        inversePrimary = Color(0xFF7F766A)
    ),
)

private val SLATE = Palette(
    id = "slate",
    nameRes = R.string.palette_slate,
    swatch = Color(0xFF5F7480),
    light = lightScheme(
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
        inversePrimary = Color(0xFF97A5AC)
    ),
    dark = darkScheme(
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
        inversePrimary = Color(0xFF677981)
    ),
)

private val TERRACOTTA = Palette(
    id = "terracotta",
    nameRes = R.string.palette_terracotta,
    swatch = Color(0xFFA96F57),
    light = lightScheme(
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
        inversePrimary = Color(0xFFC7A192)
    ),
    dark = darkScheme(
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
        inversePrimary = Color(0xFF926959)
    ),
)

private val INDIGO = Palette(
    id = "indigo",
    nameRes = R.string.palette_indigo,
    swatch = Color(0xFF5C608F),
    light = lightScheme(
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
        inversePrimary = Color(0xFF9598B6)
    ),
    dark = darkScheme(
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
        inversePrimary = Color(0xFF717494)
    ),
)

private val GRAPHITE = Palette(
    id = "graphite",
    nameRes = R.string.palette_graphite,
    swatch = Color(0xFF5F6368),
    light = lightScheme(
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
        inversePrimary = Color(0xFF979A9D)
    ),
    dark = darkScheme(
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
        inversePrimary = Color(0xFF818488)
    ),
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
