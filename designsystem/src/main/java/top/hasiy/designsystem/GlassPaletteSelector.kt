package top.hasiy.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.tokens.ThemePalette
import top.hasiy.designsystem.tokens.ThemePaletteId

/**
 * 主題色選擇器：色點 + 名稱的橫向清單。
 *
 * 與 [GlassThemeSelector] 的差別是選的東西不同——那個選「材質結構」（水滴／中性／
 * 深色／原生），這個選「配色」。兩者可以並存：同一組材質換不同配色。
 *
 * ## 為什麼不用玻璃質感
 *
 * 這個選擇器待在**畫布**上（設備外框之外），不是屏內，所以走參考稿的
 * 「浮片 + 畫布文字色」而不是 [Modifier.glassSurface]：深色主題的半透明深玻璃
 * 壓在淺灰畫布上會變成淺灰片，配上屏內的近白文字色就讀不出來了。
 *
 * 浮片與文字都由同一個 [GlassConfig.isCanvasLight] 推導——淺畫布用白浮片配
 * 深字，深畫布用黑浮片配淺字。兩者共用一個明暗訊號，就不會出現「白浮片配白字」
 * 這種互相矛盾的組合（沒有 palette 在 scope 裡時 [GlassConfig.inkColor] 會回退到
 * contentColor，對深色預設就是白色）。
 *
 * 每個選項預覽的是該主題的強調色而不是它的表面色：8 組主題裡有 5 組共用同一套
 * 深色表面，只預覽表面會變成 5 個一模一樣的灰塊。
 *
 * @param selected 目前選中的主題標識
 * @param onSelect 選擇回呼
 * @param modifier 外部修飾符
 * @param palettes 可選主題，預設為全部 8 組
 */
@Composable
fun GlassPaletteSelector(
    selected: ThemePaletteId,
    onSelect: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier,
    palettes: List<ThemePalette> = ThemePalette.All,
) {
    val config = LocalGlassConfig.current
    val onCanvas = config.isCanvasLight
    val inkColor = config.inkColor
    // 浮片的方向跟著畫布走：淺畫布往白提亮，深畫布往黑壓暗
    val liftTint = if (onCanvas) Color.White else Color.Black
    LazyRow(
        modifier = modifier
            .clip(RoundedCornerShape(CONTAINER_CORNER))
            .background(liftTint.copy(alpha = CONTAINER_ALPHA))
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(SPACING),
        contentPadding = PaddingValues(CONTAINER_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(
            count = palettes.size,
            // 主題清單可能被使用端裁剪或重排，用穩定 ID 而不是索引當 key
            key = { index -> palettes[index].id.id },
        ) { index ->
            val palette = palettes[index]
            PaletteOption(
                palette = palette,
                isSelected = palette.id == selected,
                inkColor = inkColor,
                liftTint = liftTint,
                onClick = { onSelect(palette) },
            )
        }
    }
}

@Composable
private fun PaletteOption(
    palette: ThemePalette,
    isSelected: Boolean,
    inkColor: Color,
    liftTint: Color,
    onClick: () -> Unit,
) {
    val swatch = palette.accentColor
    val shape = RoundedCornerShape(OPTION_CORNER)
    Row(
        modifier = Modifier
            .height(OPTION_HEIGHT)
            .clip(shape)
            .background(
                if (isSelected) liftTint.copy(alpha = SELECTED_ALPHA) else Color.Transparent
            )
            // 選中態用該主題自己的強調色描邊，而不是當前主題的——選項要預覽
            // 「切過去長什麼樣」。未選中時描邊仍佔 1dp 但為透明，選中時才不會
            // 因為多出一圈邊而讓文字位移。
            .border(
                width = OPTION_BORDER,
                color = if (isSelected) swatch else Color.Transparent,
                shape = shape,
            )
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = OPTION_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SWATCH_GAP),
    ) {
        Box(
            modifier = Modifier
                .size(SWATCH_SIZE)
                .drawBehind { drawCircle(color = swatch) },
        )
        Text(
            text = stringResource(paletteNameRes(palette.id)),
            color = if (isSelected) inkColor else inkColor.copy(alpha = UNSELECTED_TEXT_ALPHA),
            fontSize = OPTION_FONT_SIZE,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * 主題顯示名稱的字串資源。
 *
 * 文案由 SDK 自帶（與 [GlassThemeSelector] 的做法一致），使用端在自己的
 * `strings.xml` 定義同名資源即可覆蓋。
 */
@StringRes
private fun paletteNameRes(id: ThemePaletteId): Int = when (id) {
    ThemePaletteId.LIME -> R.string.glass_palette_lime
    ThemePaletteId.SKY -> R.string.glass_palette_sky
    ThemePaletteId.NORDIC -> R.string.glass_palette_nordic
    ThemePaletteId.OCEAN -> R.string.glass_palette_ocean
    ThemePaletteId.EMBER -> R.string.glass_palette_ember
    ThemePaletteId.AURORA -> R.string.glass_palette_aurora
    ThemePaletteId.DIGITAL -> R.string.glass_palette_digital
    ThemePaletteId.TACTILE -> R.string.glass_palette_tactile
}

/** 選擇器容器的浮片濃度 */
private const val CONTAINER_ALPHA = 0.58f

/** 選中項的浮片濃度 */
private const val SELECTED_ALPHA = 0.82f

/** 未選中項的文字淡化比例 */
private const val UNSELECTED_TEXT_ALPHA = 0.72f

private val CONTAINER_CORNER = 18.dp
private val CONTAINER_PADDING = 6.dp
private val OPTION_HEIGHT = 32.dp
private val OPTION_CORNER = 12.dp
private val OPTION_PADDING = 10.dp
private val OPTION_BORDER = 1.dp
private val SWATCH_SIZE = 11.dp
private val SWATCH_GAP = 6.dp
private val SPACING = 7.dp
private val OPTION_FONT_SIZE = 11.sp
