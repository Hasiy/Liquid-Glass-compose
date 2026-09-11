package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.screenContentColor
import top.hasiyliquidglassdemo.R

/**
 * 橫豎屏切換。
 *
 * 這是**驗收用的開關**，不是產品功能：橫屏排法要靠轉動裝置才看得到，接上實體
 * 器材之後更不方便轉，留一顆按鈕直接切。
 *
 * 圖示畫的是**切過去之後**的方向——顯示橫的長方形就表示按下去會變橫屏。
 * `material-icons-core` 裡沒有旋轉圖示，與其為一顆按鈕引進整包 extended，
 * 不如直接畫一個輪廓，而且輪廓比抽象的旋轉箭頭更直接。
 *
 * 這裡只負責畫和回報，方向鎖由外層持有——轉屏會重建 Activity，鎖存在這個
 * composable 裡的話，重建時就跟著被清掉，畫面剛轉過去又彈回來。
 *
 * @param landscape 當前是不是橫屏
 * @param onToggle 要求切到另一個方向
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun OrientationToggle(
    landscape: Boolean,
    onToggle: () -> Unit,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(TOUCH_SIZE)
            .tactileClickable(
                onClickLabel = stringResource(
                    if (landscape) R.string.matrix_to_portrait else R.string.matrix_to_landscape
                ),
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(GLYPH_BOX)) {
            // 畫的是切換後的方向：現在是橫的就畫豎的
            val w = if (landscape) size.width * NARROW else size.width
            val h = if (landscape) size.height else size.height * NARROW
            drawRoundRect(
                color = config.screenContentColor,
                topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f),
                size = Size(w, h),
                cornerRadius = CornerRadius(GLYPH_CORNER.toPx()),
                style = Stroke(width = GLYPH_STROKE.toPx()),
            )
        }
    }
}

private val TOUCH_SIZE = 44.dp
private val GLYPH_BOX = 20.dp
private val GLYPH_CORNER = 3.dp
private val GLYPH_STROKE = 1.5.dp

/** 短邊佔長邊的比例，做出手機的長寬比。 */
private const val NARROW = 0.62f
