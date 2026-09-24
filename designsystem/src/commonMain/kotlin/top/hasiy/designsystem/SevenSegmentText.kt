package top.hasiy.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * 七段數碼管讀數。
 *
 * Digital 風格的核心讀數是**逐段繪製**的，不是找一個像數碼管的字體——參考稿在
 * `.seven-char` 裡把 a–g 七段各畫成一個帶斜切的多邊形，未點亮的段留 5.5% 的淡影。
 * 那層淡影是關鍵：真的數碼管熄滅的段仍然看得見輪廓，只用字體做不出來。
 *
 * 非數字字元（`.` `,` `:` 空格）照參考稿直接畫標點，不進七段。
 *
 * @param text 要顯示的讀數，例如 `"18:42"`、`"12.5"`
 * @param color 點亮段的顏色；熄滅段用同色的 [OFF_ALPHA]
 * @param fontSize 一個字的高度，與同位置的普通文字取一樣的字級即可
 * @param modifier 外部修飾符
 */
@Composable
fun SevenSegmentText(
    text: String,
    color: Color,
    fontSize: TextUnit = LocalTextStyle.current.fontSize,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val charHeight: Dp = with(density) { fontSize.toDp() }
    val charWidth = charHeight * CHAR_ASPECT
    val gap = charHeight * CHAR_GAP

    Row(
        // 讀屏念原始文字，不要逐段去猜
        modifier = modifier.semantics { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        text.forEach { character ->
            when (character) {
                '.' -> Punctuation(charHeight, gap, color, PunctuationKind.DOT)
                ',' -> Punctuation(charHeight, gap, color, PunctuationKind.COMMA)
                ':' -> Punctuation(charHeight, gap, color, PunctuationKind.COLON)
                ' ' -> Spacer(Modifier.size(charWidth / 2, charHeight))
                else -> {
                    val lit = SEGMENT_MAP[character]
                    if (lit == null) {
                        // 目錄外的字元（單位、中文）原樣輸出，不要吞掉
                        Text(text = character.toString(), color = color, fontSize = fontSize)
                    } else {
                        Canvas(Modifier.size(charWidth, charHeight)) { drawDigit(lit, color) }
                    }
                    Spacer(Modifier.size(gap, charHeight))
                }
            }
        }
    }
}

private enum class PunctuationKind { DOT, COMMA, COLON }

@Composable
private fun Punctuation(height: Dp, gap: Dp, color: Color, kind: PunctuationKind) {
    val width = height * PUNCT_ASPECT
    Canvas(Modifier.size(width + gap, height)) {
        val radius = size.width * PUNCT_RADIUS
        val cx = size.width / 2f
        when (kind) {
            PunctuationKind.DOT -> drawCircle(color, radius, Offset(cx, size.height * 0.9f))
            PunctuationKind.COMMA -> drawCircle(color, radius, Offset(cx, size.height * 0.92f))
            PunctuationKind.COLON -> {
                drawCircle(color, radius, Offset(cx, size.height * 0.34f))
                drawCircle(color, radius, Offset(cx, size.height * 0.72f))
            }
        }
    }
}

/**
 * 畫一個字的七段。
 *
 * 段的位置與斜切比例照參考稿的 CSS：橫段 `left 13% / width 74% / height 10%`、
 * 豎段 `width 13% / height 42%`，兩端切成尖角，段與段之間才會留出那道縫。
 */
private fun DrawScope.drawDigit(lit: String, color: Color) {
    val w = size.width
    val h = size.height
    val onColor = color
    val offColor = color.copy(alpha = OFF_ALPHA)

    fun horizontal(top: Float, segment: Char) {
        translate(left = w * 0.13f, top = top) {
            drawPath(
                path = horizontalPath(Size(w * 0.74f, h * 0.10f)),
                color = if (segment in lit) onColor else offColor,
            )
        }
    }

    fun vertical(left: Float, top: Float, segment: Char) {
        translate(left = left, top = top) {
            drawPath(
                path = verticalPath(Size(w * 0.13f, h * 0.42f)),
                color = if (segment in lit) onColor else offColor,
            )
        }
    }

    horizontal(top = 0f, segment = 'a')
    horizontal(top = h * 0.45f, segment = 'g')
    horizontal(top = h - h * 0.10f, segment = 'd')
    vertical(left = w - w * 0.13f, top = h * 0.05f, segment = 'b')
    vertical(left = w - w * 0.13f, top = h - h * 0.05f - h * 0.42f, segment = 'c')
    vertical(left = 0f, top = h - h * 0.05f - h * 0.42f, segment = 'e')
    vertical(left = 0f, top = h * 0.05f, segment = 'f')
}

/** 橫段：左右兩端收成尖角。 */
private fun horizontalPath(size: Size): Path = Path().apply {
    moveTo(size.width * 0.08f, 0f)
    lineTo(size.width * 0.92f, 0f)
    lineTo(size.width, size.height / 2f)
    lineTo(size.width * 0.92f, size.height)
    lineTo(size.width * 0.08f, size.height)
    lineTo(0f, size.height / 2f)
    close()
}

/** 豎段：上下兩端收成尖角。 */
private fun verticalPath(size: Size): Path = Path().apply {
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height * 0.09f)
    lineTo(size.width, size.height * 0.91f)
    lineTo(size.width / 2f, size.height)
    lineTo(0f, size.height * 0.91f)
    lineTo(0f, size.height * 0.09f)
    close()
}

/**
 * 某個字元要點亮哪幾段；不是數字時回傳 null。
 *
 * @param character 要顯示的字元
 */
internal fun sevenSegmentsFor(character: Char): String? = SEGMENT_MAP[character]

/** 參考稿的 `sevenSegmentMap`。 */
private val SEGMENT_MAP = mapOf(
    '0' to "abcedf",
    '1' to "bc",
    '2' to "abged",
    '3' to "abgcd",
    '4' to "fgbc",
    '5' to "afgcd",
    '6' to "afgecd",
    '7' to "abc",
    '8' to "abcdefg",
    '9' to "abfgcd",
    '-' to "g",
)

/** 參考稿 `.seven-char` 是 `.54em` 寬、`1em` 高，字距 `.045em`。 */
private const val CHAR_ASPECT = 0.54f
private const val CHAR_GAP = 0.045f
private const val PUNCT_ASPECT = 0.22f
private const val PUNCT_RADIUS = 0.28f

/** 熄滅段的殘影，參考稿 `.seg { opacity: .055 }`。 */
private const val OFF_ALPHA = 0.055f
