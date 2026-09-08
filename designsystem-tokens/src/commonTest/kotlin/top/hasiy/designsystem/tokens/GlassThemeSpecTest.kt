package top.hasiy.designsystem.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlassThemeSpecTest {
    @Test
    fun `neutral preset is light and accent enabled`() {
        val spec = GlassThemeSpec.default(GlassVisualStyle.NEUTRAL)
        assertTrue(spec.isLight)
        assertTrue(spec.accentEnabled)
        assertEquals("neutral", spec.id)
    }

    @Test
    fun `preset ids remain stable`() {
        assertEquals("drop", GlassThemeSpec.default(GlassVisualStyle.DROP).id)
        assertEquals("dark", GlassThemeSpec.default(GlassVisualStyle.DARK).id)
        assertEquals("native", GlassThemeSpec.default(GlassVisualStyle.NATIVE).id)
    }

    @Test
    fun `spec from palette carries palette id style and accent`() {
        val spec = GlassThemeSpec.fromPalette(ThemePalette.Nordic)
        assertEquals("nordic", spec.id)
        assertEquals(GlassVisualStyle.NEUTRAL, spec.visualStyle)
        assertTrue(spec.isLight)
        assertEquals(ThemePalette.Nordic.accent, spec.accent)
        assertEquals(ThemePalette.Nordic.accent, spec.primary)
        assertEquals(ThemePalette.Nordic.onAccent, spec.onPrimary)
    }

    /** 8 组主题都有明确的强调色语义，不再回退到「深色提亮／浅色压暗」的玻璃质感。 */
    @Test
    fun `every palette enables accent semantics`() {
        ThemePalette.All.forEach { palette ->
            assertTrue(
                GlassThemeSpec.fromPalette(palette).accentEnabled,
                "accentEnabled of ${palette.id.id}",
            )
        }
    }

    /**
     * 参考稿的屏底是纯色，上面那层光晕是单独用强调色画的径向渐层，
     * 因此背景渐层两端同色；若哪天两端分岔，说明有人把光晕塞进了背景色。
     */
    @Test
    fun `palette background gradient has no lightness ramp`() {
        ThemePalette.All.forEach { palette ->
            val spec = GlassThemeSpec.fromPalette(palette)
            assertEquals(palette.screen, spec.backgroundTop, "backgroundTop of ${palette.id.id}")
            assertEquals(palette.screen, spec.backgroundBottom, "backgroundBottom of ${palette.id.id}")
        }
    }

    /**
     * TACTILE 是深色表面，但它的描边是近黑的机械切边（胶帽之间的缝），
     * 不能按「深色表面往白」推。
     *
     * 渲染层的描边与内缘阴影共用同一个颜色，一旦刷成白色，每个胶帽会多一圈
     * 74% 的白框加一层白色内发光，凸起感直接反掉。
     */
    @Test
    fun `tactile keeps its dark cut edge instead of a white glass border`() {
        val tactile = GlassThemeSpec.fromPalette(ThemePalette.Tactile)
        assertFalse(tactile.isLight)
        assertEquals(0xFF080909L, tactile.glassBorder)
        // 柔光方向仍然要跟着表面明暗走：深色表面往白提亮
        assertEquals(0xFFFFFFFFL, tactile.glassHighlight)
    }

    /** Digital 的卡片描边是 #0C110F，不是纯黑。 */
    @Test
    fun `digital keeps its own edge color`() {
        val digital = GlassThemeSpec.fromPalette(ThemePalette.Digital)
        assertEquals(0xFF0C110FL, digital.glassBorder)
        assertEquals(0xFF000000L, digital.glassHighlight)
    }

    /** 前四组 Liquid Glass 结构的描边仍按明暗推。 */
    @Test
    fun `liquid glass styles derive border from surface lightness`() {
        assertEquals(0xFFFFFFFFL, GlassThemeSpec.fromPalette(ThemePalette.Lime).glassBorder)
        assertEquals(0xFF000000L, GlassThemeSpec.fromPalette(ThemePalette.Nordic).glassBorder)
    }

    /** Digital 与 Tactile 没有 SDK 自带配色的历史值，一律以对应 palette 为唯一来源。 */
    @Test
    fun `digital and tactile defaults come from their palettes`() {
        assertEquals(
            GlassThemeSpec.fromPalette(ThemePalette.Digital),
            GlassThemeSpec.default(GlassVisualStyle.DIGITAL),
        )
        assertEquals(
            GlassThemeSpec.fromPalette(ThemePalette.Tactile),
            GlassThemeSpec.default(GlassVisualStyle.TACTILE),
        )
    }
}