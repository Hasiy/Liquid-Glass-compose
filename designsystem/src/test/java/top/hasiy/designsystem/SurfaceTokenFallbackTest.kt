package top.hasiy.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import top.hasiy.designsystem.tokens.ThemePalette

/**
 * 軌道與浮層的表面配置：有 token 時用 token，沒有時退回舊行為。
 *
 * 回退分支才是這組測試的重點。四組既有 SDK 預設（Drop / Neutral / Dark / Native）
 * 沒有 track / glass / glassDeep 這些 token，接新語意時**不能順手改掉它們的觀感**——
 * 元件目錄頁跑的就是這四組。
 */
class SurfaceTokenFallbackTest {

    private val presetsWithoutPalette = listOf(
        GlassPresets.Drop,
        GlassPresets.Neutral,
        GlassPresets.Dark,
        GlassPresets.Native,
    )

    // ---------- 軌道 ----------

    @Test
    fun `track surface uses the palette token`() {
        ThemePalette.All.forEach { palette ->
            val config = palette.toGlassConfig()
            assertEquals(
                "${palette.id.id} track",
                palette.track.toInt(),
                config.asTrackSurface().baseColor.toArgb(),
            )
        }
    }

    /** 沒有 track token 時要退回 asControlSurface，而不是走 trackColor 的推導回退。 */
    @Test
    fun `track surface falls back to the control surface`() {
        presetsWithoutPalette.forEach { config ->
            assertEquals(
                config.asControlSurface().baseColor.toArgb(),
                config.asTrackSurface().baseColor.toArgb(),
            )
        }
    }

    @Test
    fun `disabled track dims without changing its color`() {
        val config = ThemePalette.Lime.toGlassConfig()
        val enabled = config.asTrackSurface(enabled = true)
        val disabled = config.asTrackSurface(enabled = false)
        assertEquals(enabled.baseColor.toArgb(), disabled.baseColor.toArgb())
        assert(disabled.bodyTopAlpha < enabled.bodyTopAlpha)
    }

    // ---------- 裝飾性小字的強調色 ----------

    /** 沒給 quietAccent 的配色要照舊走強調色，不能因為新增 token 就整批變色。 */
    @Test
    fun `quiet accent falls back to the accent color`() {
        (ThemePalette.All.filter { it.quietAccent == 0L }.map { it.toGlassConfig() } +
            presetsWithoutPalette).forEach { config ->
            assertEquals(
                config.accentToneColor.toArgb(),
                config.quietAccentColor.toArgb(),
            )
        }
    }

    /**
     * 給了 quietAccent 的配色要真的降下來。
     *
     * `0L` 是「未指定」的哨兵，經過 `Color(0L)` 會變成透明黑而不是 `Color.Unspecified`，
     * 回退分支就永遠走不到——這個案例守的是那條轉換。
     */
    @Test
    fun `quiet accent uses the palette token when given`() {
        ThemePalette.All.filter { it.quietAccent != 0L }.forEach { palette ->
            val config = palette.toGlassConfig()
            assertEquals(
                "${palette.id.id} quietAccent",
                palette.quietAccent.toInt(),
                config.quietAccentColor.toArgb(),
            )
            assert(config.quietAccentColor != config.accentToneColor) {
                "${palette.id.id} 的 quietAccent 跟強調色同色，這個 token 就沒有意義"
            }
        }
    }

    // ---------- 填充段 ----------

    @Test
    fun `fill surface builds an accentLight to accent gradient`() {
        ThemePalette.All.forEach { palette ->
            val fill = palette.toGlassConfig().asFillSurface()
            assertEquals(
                "${palette.id.id} 漸層起點",
                palette.accentLight.toInt(),
                fill.baseColor.toArgb(),
            )
            assertEquals(
                "${palette.id.id} 漸層終點",
                palette.accent.toInt(),
                fill.bodyEndColor.toArgb(),
            )
        }
    }

    /**
     * 沒有 accentLight 的主題要退化成實色，兩端同色。
     *
     * Neutral 是唯一 accentEnabled 為 true 但沒有 palette 的既有預設，
     * 元件目錄頁的進度條與滑桿跑的就是它——不能因為接了漸層就換掉觀感。
     */
    @Test
    fun `fill surface degenerates to a solid color without accentLight`() {
        val fill = GlassPresets.Neutral.asFillSurface()
        assertEquals(fill.baseColor.toArgb(), fill.bodyEndColor.toArgb())
        assertEquals(GlassPresets.Neutral.accentColor.toArgb(), fill.baseColor.toArgb())
    }

    /** 其他表面不該帶上漸層終點色，否則基底會意外變成雙色。 */
    @Test
    fun `other surfaces leave the gradient end unspecified`() {
        val config = ThemePalette.Lime.toGlassConfig()
        listOf(
            "selected" to config.asSelectedSurface(),
            "selectedStrong" to config.asSelectedSurface(strong = true),
            "track" to config.asTrackSurface(),
            "overlay" to config.asOverlaySurface(),
            "control" to config.asControlSurface(),
        ).forEach { (name, surface) ->
            assertEquals("$name 不該有漸層終點", Color.Unspecified, surface.bodyEndColor)
        }
    }

    // ---------- 浮層 ----------

    @Test
    fun `overlay surface uses glass and glassDeep`() {
        ThemePalette.All.forEach { palette ->
            val config = palette.toGlassConfig()
            assertEquals(
                "${palette.id.id} glass",
                palette.glass.toInt(),
                config.asOverlaySurface().baseColor.toArgb(),
            )
            assertEquals(
                "${palette.id.id} glassDeep",
                palette.glassDeep.toInt(),
                config.asOverlaySurface(deep = true).baseColor.toArgb(),
            )
        }
    }

    /**
     * 浮層的底其實是 glassBackdrop 畫的，它讀 overlayFallbackAlpha 而不是 body 那組。
     * 兩邊都要帶上 token 的 alpha，否則 `#F71D2121` 這種 97% 的浮層會被畫成 62%。
     */
    @Test
    fun `overlay surface carries the token alpha to both paths`() {
        val config = ThemePalette.Lime.toGlassConfig()
        val overlay = config.asOverlaySurface()
        val tokenAlpha = overlay.baseColor.alpha
        assertEquals(tokenAlpha, overlay.bodyTopAlpha, 0.001f)
        assertEquals(tokenAlpha, overlay.bodyBottomAlpha, 0.001f)
        assertEquals(tokenAlpha, overlay.overlayFallbackAlpha, 0.001f)
    }

    /** 沒有 token 的預設要原樣回傳——既有浮層一個像素都不該動。 */
    @Test
    fun `overlay surface returns the same config without tokens`() {
        presetsWithoutPalette.forEach { config ->
            assertSame(config, config.asOverlaySurface())
            assertSame(config, config.asOverlaySurface(deep = true))
        }
    }
}
