package com.example.photosmanager

import androidx.compose.material3.ColorScheme
import com.example.photosmanager.ui.theme.PaletteCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 用户反馈「换主题时字体颜色也跟着变」，根因是中性角色被按配色摊开了：
 * 米白下正文是暖棕、靛蓝下是冷蓝。现在中性层由 Palette.kt 里的 Neutral 统一提供，
 * 这条测试就是钉住这件事——谁哪天又把 onSurface、outline 之类的塞回配色里，这里立刻红。
 *
 * 控件层反过来应当每套都不一样，所以一并断言，防止哪天把 primary 也一起「中性化」了。
 */
class PaletteNeutralityTest {

    private val neutralRoles: List<Pair<String, ColorScheme.() -> ULong>> = listOf(
        "background" to { background.value },
        "onBackground" to { onBackground.value },
        "surface" to { surface.value },
        "onSurface" to { onSurface.value },
        "surfaceVariant" to { surfaceVariant.value },
        "onSurfaceVariant" to { onSurfaceVariant.value },
        "surfaceContainerLowest" to { surfaceContainerLowest.value },
        "surfaceContainerLow" to { surfaceContainerLow.value },
        "surfaceContainer" to { surfaceContainer.value },
        "surfaceContainerHigh" to { surfaceContainerHigh.value },
        "surfaceContainerHighest" to { surfaceContainerHighest.value },
        "surfaceBright" to { surfaceBright.value },
        "surfaceDim" to { surfaceDim.value },
        "inverseSurface" to { inverseSurface.value },
        "inverseOnSurface" to { inverseOnSurface.value },
        "outline" to { outline.value },
        "outlineVariant" to { outlineVariant.value },
    )

    private val controlRoles: List<Pair<String, ColorScheme.() -> ULong>> = listOf(
        "primary" to { primary.value },
        "secondary" to { secondary.value },
        "tertiary" to { tertiary.value },
        "error" to { error.value },
    )

    private fun assertLayer(schemeOf: (com.example.photosmanager.ui.theme.Palette) -> ColorScheme, label: String) {
        val palettes = PaletteCatalog.ordered
        neutralRoles.forEach { (role, get) ->
            val values = palettes.associate { it.id to get(schemeOf(it)) }
            assertEquals("$label/$role 五套配色必须共用同一个中性色：$values", values.values.distinct().size, 1)
        }
        controlRoles.forEach { (role, get) ->
            val values = palettes.map { get(schemeOf(it)) }
            assertEquals("$label/$role 控件色必须随配色变化，不能也钉成中性", values.distinct().size, palettes.size)
        }
    }

    @Test
    fun `亮色下中性层共用一套、控件层各不同`() = assertLayer({ it.light }, "light")

    @Test
    fun `暗色下中性层共用一套、控件层各不同`() = assertLayer({ it.dark }, "dark")
}
