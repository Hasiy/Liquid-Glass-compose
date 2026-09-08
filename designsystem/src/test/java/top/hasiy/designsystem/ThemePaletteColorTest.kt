package top.hasiy.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test
import top.hasiy.designsystem.tokens.ThemePalette

/**
 * 主題色令牌（無符號 ARGB `Long`）轉成 Compose [Color] 的正確性。
 *
 * 這組測試存在的原因是一個真機崩潰：
 * ```
 * java.lang.ArrayIndexOutOfBoundsException: length=18; index=41
 *     at androidx.compose.ui.graphics.Color.getColorSpace-impl
 *     at androidx.compose.ui.graphics.Color.copy-wmQWz5c
 *     at top.hasiy.designsystem.DesignSystemThemeKt.toMaterialColorScheme
 * ```
 * 當時轉換寫成 `Color(argb.toULong())`。`Color(ULong)` 是 value class 的主構造，
 * 收的是 Compose 的內部打包格式（高 32 位 RGBA、低 6 位 color space id），
 * 把 ARGB 直接塞進去會讓 color space id 取到 ARGB 的低 6 位——
 * danger `0xFFFF6969` 算出 `0x29 = 41`，而 `ColorSpaces` 只有 18 項。
 *
 * 編譯期抓不到這個錯，只有真的去讀 color space 時才炸，所以必須有測試守著。
 */
class ThemePaletteColorTest {

    @Test
    fun `palette colors keep srgb color space`() {
        ThemePalette.All.forEach { palette ->
            val glass = palette.toGlassPalette()
            val named = listOf(
                "accentLight" to glass.accentLight,
                "accentDeep" to glass.accentDeep,
                "onAccent" to glass.onAccent,
                "ambient" to glass.ambient,
                "track" to glass.track,
                "danger" to glass.danger,
                "background" to glass.background,
                "screen" to glass.screen,
                "panel" to glass.panel,
                "line" to glass.line,
                "muted" to glass.muted,
                "ink" to glass.ink,
                "screenContent" to glass.screenContent,
                "lift" to glass.lift,
                "sunk" to glass.sunk,
                "glass" to glass.glass,
                "glassDeep" to glass.glassDeep,
            )
            named.forEach { (token, color) ->
                assertEquals(
                    "${palette.id.id}.$token color space",
                    ColorSpaces.Srgb,
                    color.colorSpace,
                )
            }
        }
    }

    @Test
    fun `palette colors round trip to the original argb`() {
        ThemePalette.All.forEach { palette ->
            val glass = palette.toGlassPalette()
            assertEquals(
                "${palette.id.id}.danger",
                palette.danger.toInt(),
                glass.danger.toArgb(),
            )
            assertEquals(
                "${palette.id.id}.screen",
                palette.screen.toInt(),
                glass.screen.toArgb(),
            )
            // 帶 alpha 的令牌也要原樣還原：浮層濃度錯了整個玻璃感就沒了
            assertEquals(
                "${palette.id.id}.glass",
                palette.glass.toInt(),
                glass.glass.toArgb(),
            )
        }
    }

    @Test
    fun `palette accent preview keeps its exact value`() {
        assertEquals(0xFFDFFF32.toInt(), ThemePalette.Lime.accentColor.toArgb())
        assertEquals(0xFF10E66B.toInt(), ThemePalette.Tactile.accentColor.toArgb())
    }

    /** 真機崩潰的那一行：`danger.copy(alpha = ...)` 會去讀 color space。 */
    @Test
    fun `material color scheme builds for every palette`() {
        ThemePalette.All.forEach { palette ->
            val scheme = palette.toMaterialColorScheme()
            assertEquals(
                "${palette.id.id}.error",
                palette.danger.toInt(),
                scheme.error.toArgb(),
            )
            assertEquals(
                "${palette.id.id}.primary",
                palette.accent.toInt(),
                scheme.primary.toArgb(),
            )
        }
    }

    @Test
    fun `glass config builds for every palette`() {
        ThemePalette.All.forEach { palette ->
            val config = palette.toGlassConfig()
            assertEquals(
                "${palette.id.id}.accentColor",
                palette.accent.toInt(),
                config.accentColor.toArgb(),
            )
            // 語意色讀取入口也要能安全求值——多數會走 copy()/lerp()，都要讀 color space
            assertEquals(
                "${palette.id.id}.dangerColor",
                palette.danger.toInt(),
                config.dangerColor.toArgb(),
            )
            assertEquals(
                "${palette.id.id}.canvasColor",
                palette.background.toInt(),
                config.canvasColor.toArgb(),
            )
            config.mutedContentColor
            config.canvasLineColor
            config.trackColor
            config.isCanvasLight
        }
    }

    /**
     * 沒有提供 palette 的舊有預設也要能求值。
     *
     * 回退路徑會對 [GlassConfig] 的既有欄位做 `copy()` 與 `lerp()`，
     * 那些顏色是 Compose 字面量，本來就正常；這裡守的是回退分支不要漏掉。
     */
    @Test
    fun `semantic colors resolve for presets without a palette`() {
        listOf(GlassPresets.Drop, GlassPresets.Neutral, GlassPresets.Dark, GlassPresets.Native)
            .forEach { config ->
                assertEquals(ColorSpaces.Srgb, config.dangerColor.colorSpace)
                assertEquals(ColorSpaces.Srgb, config.trackColor.colorSpace)
                assertEquals(ColorSpaces.Srgb, config.mutedContentColor.colorSpace)
                assertEquals(ColorSpaces.Srgb, config.canvasLineColor.colorSpace)
                assertEquals(Color.Unspecified, config.palette.danger)
            }
    }
}
