package top.hasiy.designsystem

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import top.hasiy.designsystem.tokens.ThemePalette

/**
 * [GlassConfig.asSelectedSurface] 給出的前景色。
 *
 * 這組測試守的是「選中態的字看不看得見」。改之前每個元件各自判斷前景色，
 * `GlassCheckbox` 甚至寫死 `Color.White`——Lime 的強調色是 `#DFFF32`，
 * 白勾壓在上面等於沒有勾。現在統一由 asSelectedSurface 依底色決定。
 */
class SelectedSurfaceContentColorTest {

    /** strong 是接近實色的強調色底，前景必須是 onAccent。 */
    @Test
    fun `strong selection uses onAccent as its foreground`() {
        ThemePalette.All.forEach { palette ->
            val config = palette.toGlassConfig()
            assertEquals(
                "${palette.id.id} strong 前景",
                palette.onAccent.toInt(),
                config.asSelectedSurface(strong = true).contentColor.toArgb(),
            )
        }
    }

    /** 22% 淡底透出來的還是原表面色，前景用 accentDeep。 */
    @Test
    fun `light selection uses accentDeep as its foreground`() {
        ThemePalette.All.forEach { palette ->
            val config = palette.toGlassConfig()
            assertEquals(
                "${palette.id.id} 淡底前景",
                palette.accentDeep.toInt(),
                config.asSelectedSurface(strong = false).contentColor.toArgb(),
            )
        }
    }

    /**
     * Lime 是最容易暴露問題的一組：強調色是亮黃綠，白色前景會消失。
     * onAccent 的 `#151800` 才壓得住。
     */
    @Test
    fun `lime strong selection does not fall back to white`() {
        val strong = ThemePalette.Lime.toGlassConfig().asSelectedSurface(strong = true)
        assertEquals(0xFF151800.toInt(), strong.contentColor.toArgb())
        assertNotEquals(0xFFFFFFFF.toInt(), strong.contentColor.toArgb())
    }

    /**
     * Nordic 是唯一 accentDeep 與 accent 不同的一組。
     * accent `#079D68` 壓在屏底 `#D1D1D1` 上只有 2.3:1，accentDeep 才拉得到 3.8:1。
     */
    @Test
    fun `nordic light selection deepens the accent`() {
        val light = ThemePalette.Nordic.toGlassConfig().asSelectedSurface(strong = false)
        assertEquals(0xFF0A6D4B.toInt(), light.contentColor.toArgb())
        assertNotEquals(ThemePalette.Nordic.accent.toInt(), light.contentColor.toArgb())
    }

    /**
     * 沒有啟用強調色的預設不受影響——選中態沿用玻璃質感，前景仍是原內容色。
     */
    @Test
    fun `presets without accent keep their content color`() {
        listOf(GlassPresets.Drop, GlassPresets.Dark, GlassPresets.Native).forEach { config ->
            assertEquals(
                config.contentColor.toArgb(),
                config.asSelectedSurface(strong = true).contentColor.toArgb(),
            )
            assertEquals(
                config.contentColor.toArgb(),
                config.asSelectedSurface(strong = false).contentColor.toArgb(),
            )
        }
    }
}
