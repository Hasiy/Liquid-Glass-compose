/*
 * Copyright 2026 FitDash contributors.
 */
package top.hasiy.designsystem.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ThemePaletteTest {

    @Test
    fun `all eight palettes are available in reference order`() {
        assertEquals(
            listOf("lime", "sky", "nordic", "ocean", "ember", "aurora", "digital", "tactile"),
            ThemePalette.All.map { it.id.id },
        )
    }

    @Test
    fun `every palette id resolves to exactly one palette`() {
        ThemePaletteId.entries.forEach { id ->
            assertSame(id, ThemePalette.of(id).id, "palette for $id")
        }
        assertEquals(ThemePaletteId.entries.size, ThemePalette.All.size)
    }

    @Test
    fun `unknown id falls back to lime`() {
        assertEquals(ThemePaletteId.LIME, ThemePaletteId.fromId(null))
        assertEquals(ThemePaletteId.LIME, ThemePaletteId.fromId(""))
        assertEquals(ThemePaletteId.LIME, ThemePaletteId.fromId("no-such-theme"))
        assertSame(ThemePalette.Lime, ThemePalette.fromId("no-such-theme"))
    }

    /**
     * 参考设计稿的六组基础主题色必须保持原值——这些是设计交付的硬性数值，
     * 任何「顺手调亮一点」都要先改设计稿。
     */
    @Test
    fun `reference accent colors keep their exact values`() {
        assertAccent(ThemePalette.Lime, 0xFFDFFF32L, 0xFFEFFF9AL, 0xFF69E2CEL, 0xFF151800L)
        assertAccent(ThemePalette.Sky, 0xFF5AA9EEL, 0xFFC4E2FBL, 0xFF6FD3E8L, 0xFF06202FL)
        assertAccent(ThemePalette.Nordic, 0xFF079D68L, 0xFF74D4ACL, 0xFF72C7A9L, 0xFFFFFFFFL)
        // 冰川蓝挪到白色系后整组重定过：原来那套是给深色屏配的
        assertAccent(ThemePalette.Ocean, 0xFF168FB4L, 0xFF7FCADFL, 0xFF5B93C7L, 0xFFFFFFFFL)
        assertAccent(ThemePalette.Ember, 0xFFFF9A3DL, 0xFFFFD0A3L, 0xFFFF635BL, 0xFF2A1400L)
        assertAccent(ThemePalette.Aurora, 0xFFB78CFFL, 0xFFDECAFFL, 0xFF45E2BEL, 0xFF1A0F2EL)
    }

    /**
     * [ThemePalette.isLight] 判断的是屏内表面，不是画布。
     *
     * Tactile 的画布是浅色塑胶，屏内却是深色——按画布判断会让屏内的柔光方向反掉。
     */
    @Test
    fun `light surface follows screen not canvas`() {
        assertTrue(ThemePalette.Nordic.isLight)
        assertTrue(ThemePalette.Ocean.isLight)
        assertTrue(ThemePalette.Digital.isLight)
        assertFalse(ThemePalette.Tactile.isLight)
        assertFalse(ThemePalette.Lime.isLight)
    }

    @Test
    fun `dark palettes reuse accent as its deep variant`() {
        listOf(
            ThemePalette.Lime,
            ThemePalette.Sky,
            ThemePalette.Ember,
            ThemePalette.Aurora,
        ).forEach { palette ->
            assertEquals(palette.accent, palette.accentDeep, "accentDeep of ${palette.id.id}")
        }
    }

    /** 只有压在浅底上的小字需要加深变体，也就是白色系那两组。 */
    @Test
    fun `light palettes use a darker accent for text on light surfaces`() {
        assertEquals(0xFF0A6D4BL, ThemePalette.Nordic.accentDeep)
        assertEquals(0xFF0A6A87L, ThemePalette.Ocean.accentDeep)
    }

    /**
     * 8 组主题的画布一律是浅色（参考稿的 board 是一张浅色纸）。
     *
     * 系统列图示的明暗依赖这个不变式；哪天加了深画布的主题，这个测试会先失败，
     * 提醒去检查所有「待在画布上」的元件是否还读得出来。
     */
    @Test
    fun `every palette has a light canvas`() {
        ThemePalette.All.forEach { palette ->
            assertTrue(palette.isCanvasLight, "canvas of ${palette.id.id}")
        }
    }

    /**
     * 画布明暗要按对比度判断，不能拿亮度跟 0.5 比。
     *
     * Nordic 的画布 #ABABAB 亮度约 0.40，跟 0.5 比会被判成深色；
     * 但它对黑字 8.9:1、对白字只有 2.4:1，实际是浅色底。
     */
    @Test
    fun `canvas lightness is judged by contrast not raw luminance`() {
        assertTrue(ThemePalette.Nordic.isCanvasLight)
        // Tactile 的画布浅、屏内深，两个判断必须给出相反的结果
        assertTrue(ThemePalette.Tactile.isCanvasLight)
        assertFalse(ThemePalette.Tactile.isLight)
    }

    /** 真正的深色画布要能被判出来，否则上面的不变式测试只是恒真。 */
    @Test
    fun `a dark canvas is reported as dark`() {
        val darkCanvas = ThemePalette.Lime.copy(background = 0xFF111111L)
        assertFalse(darkCanvas.isCanvasLight)
    }

    @Test
    fun `each palette declares its own visual style`() {
        assertEquals(GlassVisualStyle.DROP, ThemePalette.Lime.visualStyle)
        assertEquals(GlassVisualStyle.NEUTRAL, ThemePalette.Nordic.visualStyle)
        assertEquals(GlassVisualStyle.DIGITAL, ThemePalette.Digital.visualStyle)
        assertEquals(GlassVisualStyle.TACTILE, ThemePalette.Tactile.visualStyle)
    }

    private fun assertAccent(
        palette: ThemePalette,
        accent: Long,
        accentLight: Long,
        ambient: Long,
        onAccent: Long,
    ) {
        val name = palette.id.id
        assertEquals(accent, palette.accent, "accent of $name")
        assertEquals(accentLight, palette.accentLight, "accentLight of $name")
        assertEquals(ambient, palette.ambient, "ambient of $name")
        assertEquals(onAccent, palette.onAccent, "onAccent of $name")
    }
}
