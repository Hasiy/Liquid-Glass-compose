package top.hasiy.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 面的四條邊。內側光影是**逐邊**畫的，見 [drawInsetEdge]。 */
private enum class Edge { TOP, LEFT, BOTTOM, RIGHT }

/**
 * 固定角度的漸層軸。
 *
 * `Offset.Infinite` 會把軸拉成**盒子的對角線**——元素越扁軸越平，漸層就越接近豎的。
 * CSS 的 `linear-gradient(145deg, …)` 是固定角度，和寬高比無關，所以這裡自己算：
 * 給一個方向，長度取 `|W·ux| + |H·uy|`（CSS 算梯度線長度的公式），漸層才剛好鋪滿。
 *
 * @param size 要鋪漸層的尺寸
 * @param ux 方向的水平分量
 * @param uy 方向的垂直分量
 */
private fun gradientEnd(size: Size, ux: Float, uy: Float): Offset {
    val length = kotlin.math.abs(size.width * ux) + kotlin.math.abs(size.height * uy)
    return Offset(length * ux, length * uy)
}

/**
 * Tactile 風格的凸起膠帽。
 *
 * 對應參考稿的 `.theme-tactile .metric`：**左、上是亮邊，右、下是暗邊**，
 * 外面再落一層影。只把顏色換深做不出「按鍵凸起來」，得靠這組相反方向的明暗。
 *
 * 光影是**逐邊**畫的：整條上邊、整條左邊都亮，整條下邊、整條右邊都暗。
 * 早先用一條對角漸層當遮罩，亮的是「左上那一半」——在扁長的元素上，上邊只有
 * 靠左的一段亮、越往右越淡，看起來就不是一圈倒角。
 *
 * @param corner 圓角半徑，與外層的裁切保持一致
 */
fun Modifier.tactileKeycap(corner: Dp = KEYCAP_CORNER): Modifier = this
    // 參考稿的第三層 `0 4px 7px rgba(0,0,0,.52)`：按鍵浮在面板上，
    // 少了這層外影，內側再怎麼畫都只是一塊有邊的平面
    .shadow(
        elevation = KEYCAP_ELEVATION,
        shape = RoundedCornerShape(corner),
        ambientColor = OUTER_SHADOW,
        spotColor = OUTER_SHADOW,
    )
    .clip(RoundedCornerShape(corner))
    // 底色是 `linear-gradient(145deg, …)`——固定角度，不是盒子對角線
    .drawBehind {
        drawRoundRect(
            brush = Brush.linearGradient(
                0f to KEYCAP_TOP,
                0.45f to KEYCAP_MID,
                1f to KEYCAP_BOTTOM,
                start = Offset.Zero,
                end = gradientEnd(size, BODY_UX, BODY_UY),
            ),
            cornerRadius = CornerRadius(corner.toPx()),
        )
    }
    .border(1.dp, KEYCAP_EDGE, RoundedCornerShape(corner))
    .drawWithContent {
        drawContent()
        drawBevel(corner)
    }

/**
 * Tactile 的次級面板：dock、換組條、分類頁籤這類容器。
 *
 * 和 [tactileKeycap] 一樣是**凸起**的，只是底色淺一階——參考稿的
 * `.theme-tactile .l-dock` 帶 `0 3px 5px` 外影與左上內高光，是浮在螢幕上的一塊板，
 * 不是嵌進去的凹槽。
 *
 * @param corner 圓角半徑
 */
fun Modifier.tactilePanel(corner: Dp = PANEL_CORNER): Modifier = this
    .shadow(
        elevation = KEYCAP_ELEVATION,
        shape = RoundedCornerShape(corner),
        ambientColor = OUTER_SHADOW,
        spotColor = OUTER_SHADOW,
    )
    .clip(RoundedCornerShape(corner))
    .drawBehind {
        drawRoundRect(
            brush = Brush.linearGradient(
                0f to PANEL_TOP,
                1f to PANEL_BOTTOM,
                start = Offset.Zero,
                end = gradientEnd(size, BODY_UX, BODY_UY),
            ),
            cornerRadius = CornerRadius(corner.toPx()),
        )
    }
    .border(1.dp, KEYCAP_EDGE, RoundedCornerShape(corner))
    .drawWithContent {
        drawContent()
        drawBevel(corner)
    }

/**
 * 一圈倒角：左、上亮，右、下暗。
 *
 * 上下比左右重一點——參考稿的 inset 偏移是不對稱的（`1px 1px` / `-3px -4px`），
 * 豎向分量更大，底下那道暗收邊才立得住。
 *
 * @param corner 圓角半徑
 */
private fun DrawScope.drawBevel(corner: Dp) {
    drawInsetEdge(corner, INNER_HIGHLIGHT, HIGHLIGHT_BAND, Edge.TOP)
    drawInsetEdge(corner, INNER_HIGHLIGHT.scaleAlpha(SIDE_RATIO), HIGHLIGHT_BAND, Edge.LEFT)
    drawInsetEdge(corner, INNER_SHADOW, SHADOW_BAND, Edge.BOTTOM)
    drawInsetEdge(corner, INNER_SHADOW.scaleAlpha(SIDE_RATIO), SHADOW_BAND, Edge.RIGHT)
}

/**
 * 沿**一條邊**畫一道柔和的內側光帶。
 *
 * 兩段柔化疊在一起：往內用 [SOFT_EDGE_LAYERS] 層遞減的描邊堆出「模糊」，
 * 再套一層垂直於這條邊的遮罩，把光限制在這條邊上、轉過圓角時淡掉。
 *
 * @param corner 圓角半徑
 * @param color 帶最外側的顏色
 * @param band 光帶寬度，相當於原稿的模糊半徑
 * @param edge 畫在哪一條邊
 */
private fun DrawScope.drawInsetEdge(corner: Dp, color: Color, band: Dp, edge: Edge) {
    val bandPx = band.toPx()
    val step = bandPx / SOFT_EDGE_LAYERS
    val cornerPx = corner.toPx()
    // 遮罩的淡出距離：太短會在圓角處硬切，太長就漫到對邊去
    val reach = bandPx * MASK_REACH

    repeat(SOFT_EDGE_LAYERS) { layer ->
        val t = layer.toFloat() / SOFT_EDGE_LAYERS
        // 二次方衰減：線性的尾巴是硬切的，帶的內緣會看出一條界線
        val tint = color.scaleAlpha((1f - t) * (1f - t))
        val inset = step * layer + step / 2f
        val brush = when (edge) {
            Edge.TOP -> Brush.verticalGradient(
                0f to tint,
                1f to Color.Transparent,
                startY = 0f,
                endY = reach,
            )

            Edge.BOTTOM -> Brush.verticalGradient(
                0f to Color.Transparent,
                1f to tint,
                startY = size.height - reach,
                endY = size.height,
            )

            Edge.LEFT -> Brush.horizontalGradient(
                0f to tint,
                1f to Color.Transparent,
                startX = 0f,
                endX = reach,
            )

            Edge.RIGHT -> Brush.horizontalGradient(
                0f to Color.Transparent,
                1f to tint,
                startX = size.width - reach,
                endX = size.width,
            )
        }
        drawRoundRect(
            brush = brush,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2),
            cornerRadius = CornerRadius((cornerPx - inset).coerceAtLeast(0f)),
            style = Stroke(width = step),
        )
    }
}

private fun Color.scaleAlpha(factor: Float): Color = copy(alpha = alpha * factor)

/** 參考稿 `.theme-tactile .metric` 的三段漸層與描邊。 */
private val KEYCAP_TOP = Color(0xFF3B3C3D)
private val KEYCAP_MID = Color(0xFF292A2B)
private val KEYCAP_BOTTOM = Color(0xFF1B1C1D)
private val KEYCAP_EDGE = Color(0xBD000000)

/** 內側明暗。 */
private val INNER_HIGHLIGHT = Color(0x30FFFFFF)
private val INNER_SHADOW = Color(0x94000000)

/** 光帶寬度：對應原稿的 `inset … 3px` 與 `inset … 7px` 兩個模糊半徑。 */
private val HIGHLIGHT_BAND = 4.dp
private val SHADOW_BAND = 8.dp

/** 左右兩條邊比上下輕：原稿的 inset 偏移豎向分量更大。 */
private const val SIDE_RATIO = 0.7f

/** 遮罩淡出距離佔光帶寬度的倍數。 */
private const val MASK_REACH = 3.5f

/** 疊幾層堆出模糊。層太少每一層都看得出來，是一圈一圈的階梯。 */
private const val SOFT_EDGE_LAYERS = 12

/** 底色的 145deg：`(sin145°, -cos145°)`。 */
private const val BODY_UX = 0.574f
private const val BODY_UY = 0.819f

/** 外影：讓按鍵離開面板。 */
private val OUTER_SHADOW = Color(0xFF000000)
private val KEYCAP_ELEVATION = 7.dp

/** 次級面板：`.theme-tactile .l-dock` 的 `linear-gradient(145deg,#333536,#191a1b)`。 */
private val PANEL_TOP = Color(0xFF333536)
private val PANEL_BOTTOM = Color(0xFF191A1B)

val KEYCAP_CORNER = 18.dp
val PANEL_CORNER = 16.dp

/**
 * 格與格之間的接縫。
 *
 * 實體面板上兩顆鍵之間是一道**暗槽**，不是亮線——用一般的 line token 會畫成
 * 淺色分隔線，整塊 dock 就散成幾張貼在一起的卡。
 */
val TACTILE_SEAM = Color(0xB3000000)

/** 可調節指標的綠色指示燈，參考稿 `.theme-tactile .metric.adjust::before`。 */
val TACTILE_LAMP = Color(0xFF10E66B)
val TACTILE_LAMP_WIDTH = 22.dp
val TACTILE_LAMP_HEIGHT = 5.dp

/**
 * Tactile 的螢幕底紋。
 *
 * 參考稿 `.theme-tactile .phone-screen` 在底色上鋪了一層
 * `radial-gradient(circle at 15% 12%, rgba(255,255,255,.035) 0 1px, transparent 1.5px)`，
 * 8px 一格。那層極淡的點陣是「磨砂塑料面板」的質感來源，少了它屏底就是一塊死平的黑。
 *
 * @param enabled 只有 Tactile 需要；其餘風格傳 false 直接跳過
 */
fun Modifier.tactileScreenTexture(enabled: Boolean = true): Modifier =
    // drawBehind 而不是 drawWithContent：這層是**底紋**，要壓在所有內容底下。
    if (!enabled) this else drawBehind {
        val cell = TEXTURE_CELL.toPx()
        val radius = TEXTURE_DOT_RADIUS.toPx()
        val offsetX = cell * TEXTURE_DOT_X
        val offsetY = cell * TEXTURE_DOT_Y
        var y = offsetY
        while (y < size.height) {
            var x = offsetX
            while (x < size.width) {
                drawCircle(TEXTURE_DOT, radius, Offset(x, y))
                x += cell
            }
            y += cell
        }
    }

private val TEXTURE_CELL = 8.dp
private val TEXTURE_DOT_RADIUS = 1.dp
private val TEXTURE_DOT = Color(0x09FFFFFF)
private const val TEXTURE_DOT_X = 0.15f
private const val TEXTURE_DOT_Y = 0.12f
