package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.hasiy.designsystem.TACTILE_LAMP
import top.hasiy.designsystem.TACTILE_LAMP_HEIGHT
import top.hasiy.designsystem.TACTILE_LAMP_WIDTH

/**
 * 實體面板上的點擊回饋。
 *
 * 這一頁的表面都是「實體零件」——膠帽、面板、管子。Material 的水波紋鋪在這種
 * 大面積深色表面上會渲染成一層看得見的顆粒，而且它表達的是「墨水擴散」，
 * 跟按鍵按下去的語義對不上。
 *
 * 換成按下時輕微內縮：真按鍵按下去就是往下走一點，這才是這套風格該有的回饋。
 *
 * **要排在表面修飾符之前**：`scale` 開的圖層只包得住它右邊的東西，排在表面之後的話
 * 按下時只有文字縮、底和描邊不動，看起來就像樣式掉了。
 *
 * @param enabled 是否可點
 * @param onClickLabel 讀屏用的動作說明
 * @param onClick 點擊回呼
 */
@Composable
fun Modifier.tactileClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier {
    // 用 @Composable 擴充而不是 composed{}：composed 裡的 remember 不跟著 item 走，
    // 在 LazyGrid 這種會回收的容器裡，按壓狀態會串到別的格子上，看起來就是
    // 「按了一下之後某張卡的樣式不對了」。
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    return this
        .scale(if (pressed && enabled) PRESS_SCALE else 1f)
        .clickable(
            interactionSource = interaction,
            // 不要水波紋
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
}

/** 按下時的內縮量，與指標卡長按的回饋保持一致。 */
private const val PRESS_SCALE = 0.97f

/**
 * Tactile 的選中指示燈。
 *
 * 實體面板上「選中」是同一顆按鍵把燈點亮，不是換一塊底色——參考稿的
 * `.theme-tactile .metric.adjust::before` 就是這一盞。
 *
 * @param modifier 外部修飾符
 * @param width 燈的寬度；窄容器（例如分類頁籤）要收一號
 */
@Composable
fun TactileLamp(modifier: Modifier = Modifier, width: Dp = TACTILE_LAMP_WIDTH) {
    Box(
        modifier
            .size(width = width, height = TACTILE_LAMP_HEIGHT)
            // 燈是亮的，周圍要有光暈。參考稿是 `box-shadow: 0 0 8px rgba(16,230,107,.9)`，
            // Compose 沒有彩色外發光（Modifier.shadow 的顏色在多數機型上不生效），
            // 所以往外疊幾層漸淡的圓角矩形把光暈堆出來。
            .drawBehind {
                val spread = GLOW_SPREAD.toPx()
                val step = spread / GLOW_LAYERS
                repeat(GLOW_LAYERS) { layer ->
                    val grow = step * (layer + 1)
                    val t = (layer + 1).toFloat() / GLOW_LAYERS
                    drawRoundRect(
                        color = TACTILE_LAMP.copy(alpha = GLOW_ALPHA * (1f - t) * (1f - t)),
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = CornerRadius(size.height / 2f + grow),
                    )
                }
            }
            .clip(RoundedCornerShape(99.dp))
            .background(TACTILE_LAMP)
            // 燈罩頂上的一道反光，參考稿的 `inset 0 1px rgba(255,255,255,.4)`
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    color = LAMP_SHEEN,
                    size = Size(size.width, size.height / 2f),
                    cornerRadius = CornerRadius(size.height / 2f),
                )
            }
    )
}

/**
 * 光暈的擴散、層數與**每層**的不透明度。
 *
 * 這幾層是疊加上去的，靠近燈的位置會把所有層加在一起——每層給 0.55 的話那裡
 * 直接堆到接近不透明，看起來是一圈硬邊而不是散開的光。層數要多、每層要淡，
 * 累積起來才是平滑的。擴散也要比參考稿的 `0 0 8px` 再寬一點：堆疊出來的邊緣
 * 沒有真高斯模糊那麼會漫。
 */
private val GLOW_SPREAD = 12.dp
private const val GLOW_LAYERS = 16
private const val GLOW_ALPHA = 0.1f
private val LAMP_SHEEN = Color(0x66FFFFFF)
