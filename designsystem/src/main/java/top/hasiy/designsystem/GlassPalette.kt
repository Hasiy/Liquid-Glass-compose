package top.hasiy.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import top.hasiy.designsystem.tokens.ThemePalette

/**
 * 一組主題語意色，[ThemePalette] 的 Compose 對應型別。
 *
 * 每個欄位預設為 [Color.Unspecified]：代表「這個主題沒有提供這個語意色」，
 * 元件端一律透過本檔的擴充屬性（[GlassConfig.accentLightColor]、
 * [GlassConfig.dangerColor] 等）讀取，未指定時自動回退到由 [GlassConfig] 既有
 * 欄位推導的值。這樣舊有的四組 SDK 預設不必補齊 18 個 token 也能照原樣運作。
 *
 * @param accentLight 強調色的淺色變體，填充段由淺到深的漸層起點
 * @param accentDeep 強調色的加深變體，壓在淺底上的小字（連線狀態、Live 標籤）
 * @param onAccent 壓在強調色上的文字色
 * @param ambient 環境光色，頁面背景的第二個徑向漸層
 * @param track 軌道色：儀表環未到達的量程、滑桿未填充段
 * @param danger 危險語意色：斷連、寫入失敗、裝置拒絕
 * @param background 畫布底色（裝置外框之外）
 * @param screen 裝置螢幕底色
 * @param panel 面板底色，比 [screen] 高一階的容器
 * @param line 分隔線與卡片描邊色
 * @param muted 次級文字色（指標標籤、單位、說明文案）
 * @param ink 畫布上的主文字色
 * @param screenContent 裝置螢幕內的主文字色
 * @param lift 比 [panel] 提亮一階的表面，用於卡片
 * @param sunk 比 [panel] 壓暗一階的表面，用於底部 dock 與內凹容器
 * @param glass 浮層表面色（調節浮層、Toast）
 * @param glassDeep 更深一階的浮層表面色（抽屜、對話框）
 */
@Immutable
data class GlassPalette(
    val accentLight: Color = Color.Unspecified,
    val accentDeep: Color = Color.Unspecified,
    val onAccent: Color = Color.Unspecified,
    val ambient: Color = Color.Unspecified,
    val track: Color = Color.Unspecified,
    val danger: Color = Color.Unspecified,
    val background: Color = Color.Unspecified,
    val screen: Color = Color.Unspecified,
    val panel: Color = Color.Unspecified,
    val line: Color = Color.Unspecified,
    val muted: Color = Color.Unspecified,
    val ink: Color = Color.Unspecified,
    val screenContent: Color = Color.Unspecified,
    val lift: Color = Color.Unspecified,
    val sunk: Color = Color.Unspecified,
    val glass: Color = Color.Unspecified,
    val glassDeep: Color = Color.Unspecified,
) {
    companion object {
        /** 未提供任何語意色：元件全部走 [GlassConfig] 的回退推導。 */
        val Unspecified: GlassPalette = GlassPalette()
    }
}

/** 把跨平台主題色令牌轉成 Compose 型別。 */
fun ThemePalette.toGlassPalette(): GlassPalette = GlassPalette(
    accentLight = accentLight.toPaletteColor(),
    accentDeep = accentDeep.toPaletteColor(),
    onAccent = onAccent.toPaletteColor(),
    ambient = ambient.toPaletteColor(),
    track = track.toPaletteColor(),
    danger = danger.toPaletteColor(),
    background = background.toPaletteColor(),
    screen = screen.toPaletteColor(),
    panel = panel.toPaletteColor(),
    line = line.toPaletteColor(),
    muted = muted.toPaletteColor(),
    ink = ink.toPaletteColor(),
    screenContent = screenContent.toPaletteColor(),
    lift = lift.toPaletteColor(),
    sunk = sunk.toPaletteColor(),
    glass = glass.toPaletteColor(),
    glassDeep = glassDeep.toPaletteColor(),
)

/**
 * 把無符號 ARGB 令牌轉成 Compose 顏色。
 *
 * 必須走 `Color(Long)` 這個重載——它才把參數當 `0xAARRGGBB` 解讀。
 * `Color(ULong)` 收的是 Compose 的**內部打包格式**（高 32 位 RGBA、低 6 位是
 * color space id），把 ARGB 直接塞進去會讓 color space id 取到 ARGB 的低 6 位，
 * 之後任何 `copy()` / `luminance()` 都會 ArrayIndexOutOfBoundsException。
 */
private fun Long.toPaletteColor(): Color = Color(this)

// ---------- 語意色讀取入口 ----------
// 元件只讀這些屬性，不直接讀 GlassConfig.palette：未指定的 token 需要回退，
// 散在各元件裡各寫一套回退會讓同一個語意在不同元件長得不一樣。

/**
 * 主強調色。
 *
 * 永遠讀 [GlassConfig.accentColor]，[GlassPalette] 刻意不再持有 `accent`——
 * 兩邊都存一份的話，`copy(accentColor = ...)` 這個既有的覆寫手段（Demo 的強調色
 * 選色器就是這樣做的）會被 palette 裡的舊值悄悄蓋掉，看起來像沒生效。
 * 主題要換強調色請走 [GlassConfig.withAccent]。
 */
val GlassConfig.accentToneColor: Color
    get() = accentColor

/** 強調色的淺色變體。未指定時回退到強調色本身，漸層兩端同色即退化為實色。 */
val GlassConfig.accentLightColor: Color
    get() = palette.accentLight.takeOrElse { accentColor }

/** 強調色的加深變體。未指定時回退到強調色本身。 */
val GlassConfig.accentDeepColor: Color
    get() = palette.accentDeep.takeOrElse { accentColor }

/**
 * 換掉強調色，並讓它的淺／深變體跟著回退。
 *
 * 直接 `copy(accentColor = ...)` 只換主色，palette 裡的 [GlassPalette.accentLight]
 * 與 [GlassPalette.accentDeep] 還是舊主題的，於是進度漸層與連線狀態會留在舊色系上。
 * 這裡把兩個變體清成未指定，讓它們回退到新的主色。
 *
 * @param color 新的強調色
 */
fun GlassConfig.withAccent(color: Color): GlassConfig = copy(
    accentColor = color,
    palette = palette.copy(
        accentLight = Color.Unspecified,
        accentDeep = Color.Unspecified,
    ),
)

/** 這組主題色的強調色。供選擇器預覽「切過去長什麼樣」，不受當前主題影響。 */
val ThemePalette.accentColor: Color
    get() = accent.toPaletteColor()

/**
 * 壓在強調色上的文字色。
 *
 * 未指定時回退到白色：SDK 自帶預設的強調色是中深綠 #00A15C，壓白字可讀。
 */
val GlassConfig.onAccentColor: Color
    get() = palette.onAccent.takeOrElse { Color.White }

/** 環境光色。未指定時回退到強調色。 */
val GlassConfig.ambientColor: Color
    get() = palette.ambient.takeOrElse { accentToneColor }

/**
 * 軌道色：未到達的量程、未填充的滑桿段。
 *
 * 未指定時由表面色推導——深色表面往白提亮一階，淺色表面往黑壓暗一階。
 */
val GlassConfig.trackColor: Color
    get() = palette.track.takeOrElse {
        lerp(baseColor, if (isLightSurface) Color.Black else Color.White, TRACK_CONTRAST)
    }

/** 危險語意色。未指定時回退到參考稿的斷連紅，依表面明暗選深淺。 */
val GlassConfig.dangerColor: Color
    get() = palette.danger.takeOrElse {
        if (isLightSurface) FALLBACK_DANGER_ON_LIGHT else FALLBACK_DANGER_ON_DARK
    }

/** 畫布底色。未指定時回退到頁面背景漸層終點。 */
val GlassConfig.canvasColor: Color
    get() = palette.background.takeOrElse { pageBackgroundBottom }

/**
 * 畫布是不是淺色的。
 *
 * 與 [GlassConfig.isLightSurface] 不是同一件事：那個說的是**屏內**表面。
 * 待在畫布上的元件（主題選擇器、色板、頁面標題）要用這個，
 * Tactile 正是「畫布淺、屏內深」，用 isLightSurface 會把文字刷成白的。
 *
 * 未提供 palette 時退回 [GlassConfig.isLightSurface]——舊有的四組 SDK 預設
 * 沒有分畫布與屏內，兩者本來就是同一個表面。
 */
val GlassConfig.isCanvasLight: Boolean
    get() = if (palette.background.isSpecified) {
        canvasColor.luminance().let { luminance ->
            val contrastWithBlack = (luminance + CONTRAST_OFFSET) / CONTRAST_OFFSET
            val contrastWithWhite = (1f + CONTRAST_OFFSET) / (luminance + CONTRAST_OFFSET)
            contrastWithBlack >= contrastWithWhite
        }
    } else {
        isLightSurface
    }

/**
 * 待在畫布上的分隔線／描邊色。
 *
 * 不能直接用 [GlassConfig.lineColor]：那是**屏內**的分隔線，深色主題下它是
 * 白色 9%，壓在淺色畫布上完全看不見。畫布上的線一律由 [GlassConfig.inkColor]
 * 淡化而來，因為 ink 的定義就是「在畫布上讀得出來的顏色」。
 */
val GlassConfig.canvasLineColor: Color
    get() = inkColor.copy(alpha = CANVAS_LINE_ALPHA)

/** 裝置螢幕底色。未指定時回退到頁面背景漸層終點。 */
val GlassConfig.screenColor: Color
    get() = palette.screen.takeOrElse { pageBackgroundBottom }

/** 面板底色。未指定時回退到玻璃基底色。 */
val GlassConfig.panelColor: Color
    get() = palette.panel.takeOrElse { baseColor }

/** 分隔線色。未指定時回退到描邊色。 */
val GlassConfig.lineColor: Color
    get() = palette.line.takeOrElse { borderColor }

/** 次級文字色。未指定時回退到內容色的半透明版。 */
val GlassConfig.mutedContentColor: Color
    get() = palette.muted.takeOrElse { contentColor.copy(alpha = MUTED_ALPHA) }

/** 畫布上的主文字色。未指定時回退到內容色。 */
val GlassConfig.inkColor: Color
    get() = palette.ink.takeOrElse { contentColor }

/** 裝置螢幕內的主文字色。未指定時回退到內容色。 */
val GlassConfig.screenContentColor: Color
    get() = palette.screenContent.takeOrElse { contentColor }

/** 比面板提亮一階的卡片表面。未指定時回退到玻璃基底色。 */
val GlassConfig.liftColor: Color
    get() = palette.lift.takeOrElse { baseColor }

/** 比面板壓暗一階的內凹表面。未指定時回退到玻璃基底色。 */
val GlassConfig.sunkColor: Color
    get() = palette.sunk.takeOrElse { baseColor }

/** 浮層表面色。未指定時回退到玻璃基底色。 */
val GlassConfig.glassOverlayColor: Color
    get() = palette.glass.takeOrElse { baseColor }

/** 更深一階的浮層表面色。未指定時回退到浮層表面色。 */
val GlassConfig.glassDeepOverlayColor: Color
    get() = palette.glassDeep.takeOrElse { glassOverlayColor }

/** 軌道色由表面色推導時的對比比例 */
private const val TRACK_CONTRAST = 0.15f

/** WCAG 對比度公式裡的常數項 */
private const val CONTRAST_OFFSET = 0.05f

/** 畫布上的描邊由 ink 淡化而來時的透明度 */
private const val CANVAS_LINE_ALPHA = 0.18f

/** 次級文字色由內容色推導時的透明度 */
private const val MUTED_ALPHA = 0.55f

/** 參考稿的斷連紅：深色屏用亮紅，淺色屏用壓深的紅才不刺眼 */
private val FALLBACK_DANGER_ON_DARK = Color(0xFFFF6969)
private val FALLBACK_DANGER_ON_LIGHT = Color(0xFFBD4545)
