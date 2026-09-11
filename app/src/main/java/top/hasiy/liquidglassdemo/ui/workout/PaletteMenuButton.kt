package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.GlassDropdownMenu
import top.hasiy.designsystem.GlassDropdownMenuItem
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.paletteNameRes
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tokens.ThemePalette
import top.hasiyliquidglassdemo.R

/**
 * 主題色選單。
 *
 * 和 [OrientationToggle] 一樣是**驗收用的入口**，不是產品功能：這一頁要在八組配色
 * 下逐一對照參考稿，每次都退回目錄、進主題專頁、再走回來太費事，直接在標題欄開一顆。
 *
 * 選單只換配色，不換視覺結構——結構跟著 [ThemePalette.visualStyle] 走，
 * 這是配色自己帶的屬性。
 *
 * @param current 當前配色
 * @param onSelect 選了另一組配色
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun PaletteMenuButton(
    current: ThemePalette,
    onSelect: (ThemePalette) -> Unit,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier) {
        Box(
            modifier = Modifier
                .size(TOUCH_SIZE)
                .tactileClickable(
                    onClickLabel = stringResource(R.string.matrix_palette_menu_desc),
                    onClick = { expanded = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            // `material-icons-core` 裡沒有 MoreHoriz，為三顆點引進整包 extended
            // 不值得，直接畫。
            Canvas(Modifier.size(GLYPH_BOX)) {
                val radius = DOT_RADIUS.toPx()
                val gap = DOT_GAP.toPx()
                val y = size.height / 2f
                repeat(DOT_COUNT) { index ->
                    drawCircle(
                        color = config.screenContentColor,
                        radius = radius,
                        center = Offset(size.width / 2f + (index - 1) * gap, y),
                    )
                }
            }
        }

        GlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            config = config,
        ) {
            ThemePalette.All.forEach { palette ->
                val isSelected = palette.id == current.id
                GlassDropdownMenuItem(
                    // 文案讀 SDK 自帶的那一份，不在這裡另抄一套
                    text = stringResource(paletteNameRes(palette.id)),
                    onClick = {
                        expanded = false
                        onSelect(palette)
                    },
                    // 選中態該用 selected 語意表達，不能只靠行尾的「✓」——不過這條
                    // 在真機上驗不到，見 LayoutMenu 裡同一處的說明。
                    //
                    // 外部的 val 不能叫 selected：semantics 的 receiver 自己就有一個
                    // 同名屬性，`selected = selected` 右邊會解析成 receiver 的屬性
                    // 而不是外面的值，編譯得過但語意值恆錯。
                    modifier = Modifier.semantics { selected = isSelected },
                    config = config,
                    trailingIcon = if (isSelected) {
                        {
                            Text(
                                text = CHECK,
                                color = config.accentToneColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

private const val CHECK = "✓"
private val TOUCH_SIZE = 44.dp
private val GLYPH_BOX = 20.dp
private val DOT_RADIUS = 1.8.dp
private val DOT_GAP = 6.dp
private const val DOT_COUNT = 3
