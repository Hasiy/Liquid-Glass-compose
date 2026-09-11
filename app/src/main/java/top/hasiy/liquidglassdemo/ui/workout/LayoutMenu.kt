package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Size
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
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.screenContentColor
import top.hasiyliquidglassdemo.R

/**
 * 版面選單。
 *
 * 四件事：切左對齊、進出拖動換位的編輯模式、換網格規格、換側邊欄數（僅橫屏）。
 * 兩個開關排在最上面——見下面關於選單高度的說明。
 *
 * 規格候選跟著當前方向給——豎屏那一組和橫屏 dock 能選的檔位不一樣（豎屏最多 3×3、
 * dock 最多 3×5），把兩邊的候選混在一張選單裡會讓人選到當前方向用不上的檔。
 * 側邊規格只在橫屏才有意義，豎屏時 [sideSpec] 傳 null 就不畫那一段。
 *
 * @param landscape 當前是不是橫屏，決定改哪一組規格、列哪些候選
 * @param current 當前規格（豎屏時是豎屏那組，橫屏時是 dock）
 * @param editingLayout 是否正處於編輯模式
 * @param onSelectSpec 選了一種規格
 * @param onToggleEditing 切換編輯模式
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 * @param sideSpec 橫屏側邊當前的規格；null 表示不提供側邊選項（豎屏、或不支援側邊配置的頁面）
 * @param onSelectSideSpec 選了一種側邊規格
 * @param alignStart 是否把有內容的格子往前收攏
 * @param onToggleAlignStart 切換收攏
 */
@Composable
fun LayoutMenuButton(
    landscape: Boolean,
    current: MetricGridSpec,
    editingLayout: Boolean,
    onSelectSpec: (MetricGridSpec) -> Unit,
    onToggleEditing: () -> Unit,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
    sideSpec: MetricGridSpec? = null,
    onSelectSideSpec: (MetricGridSpec) -> Unit = {},
    alignStart: Boolean = false,
    onToggleAlignStart: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val options = if (landscape) MetricGridSpec.DockOptions else MetricGridSpec.PortraitOptions

    Box(modifier) {
        Box(
            modifier = Modifier
                .size(TOUCH_SIZE)
                .tactileClickable(
                    onClickLabel = stringResource(R.string.matrix_layout_menu_desc),
                    onClick = { expanded = true },
                ),
            contentAlignment = Alignment.Center,
        ) {
            GridGlyph(
                color = if (editingLayout) config.accentToneColor else config.screenContentColor,
            )
        }

        GlassDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            config = config,
        ) {
            // 兩個開關擺在最上面，規格候選排在下面。
            //
            // 手機橫屏只有 ~359dp 高，這張選單放得下大約 7 項；規格候選（dock 4 個
            // 加側邊 3 行）本來就會把選單撐到要捲動。開關是每次都可能點的，
            // 排在尾巴上就得先捲一段才看得到——按頻率排，不按「先選規格再切開關」
            // 這種想像中的順序排。
            // 勾勾是畫給眼睛看的。無障礙服務讀不出「✓」是什麼意思——它只會把那個
            // 字元當普通文字念出來。選中與否要走 selected 語意，讀屏才會講「已選中」。
            GlassDropdownMenuItem(
                text = stringResource(R.string.matrix_align_start),
                onClick = {
                    expanded = false
                    onToggleAlignStart()
                },
                modifier = Modifier.semantics { selected = alignStart },
                config = config,
                trailingIcon = if (alignStart) {
                    { MenuCheck(config) }
                } else {
                    null
                },
            )
            GlassDropdownMenuItem(
                text = stringResource(
                    if (editingLayout) R.string.matrix_layout_edit_done
                    else R.string.matrix_layout_edit_start
                ),
                onClick = {
                    expanded = false
                    onToggleEditing()
                },
                modifier = Modifier.semantics { selected = editingLayout },
                config = config,
                trailingIcon = if (editingLayout) {
                    { MenuCheck(config) }
                } else {
                    null
                },
            )
            options.forEach { spec ->
                val isSelected = spec == current
                GlassDropdownMenuItem(
                    text = spec.label,
                    onClick = {
                        expanded = false
                        onSelectSpec(spec)
                    },
                    // 外部那個 val 不能叫 selected：semantics 的 receiver 自己就有一個
                    // 同名屬性，`selected = selected` 右邊會解析成 receiver 的屬性
                    // 而不是外面的值——編譯得過、測試也過，但語意值永遠是錯的。
                    // 真機 dump 出來八個選項全是 selected="false" 才看得出來。
                    // 這個語意在真機的 uiautomator dump 上驗不到：八個選項的節點
                    // isSelected 全是 false。專案裡能正確透出選中態的地方走的都是
                    // Modifier.selectable——它除了設 Selected 還設了 Role，產生的
                    // 節點結構跟掛在 GlassDropdownMenuItem 上不同。
                    //
                    // 標準寫法保留在這裡（選中態本來就該用這個語意表達），但**刻意
                    // 不再往下追**：要讓它透出得給 GlassDropdownMenuItem 加 selected
                    // 參數、內部改用 selectable 取代 clickable，那是 SDK 簽名與互動
                    // 行為的變更。而使用者看到的行尾「✓」已經把選中講清楚了，
                    // 為這一條動 SDK 不值得。
                    modifier = Modifier.semantics { selected = isSelected },
                    config = config,
                    trailingIcon = if (isSelected) {
                        { MenuCheck(config) }
                    } else {
                        null
                    },
                )
            }
            // 側邊獨立一段：它是第三個維度（只選欄數），跟上面 dock 的規格不是同一件事，
            // 混在同一排選項裡使用者分不出「這顆在改哪一塊」。
            if (sideSpec != null) {
                Text(
                    text = stringResource(R.string.matrix_side_menu_label),
                    color = config.mutedContentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 2.dp),
                )
                MetricGridSpec.SideOptions.forEach { spec ->
                    val isSelected = spec == sideSpec
                    GlassDropdownMenuItem(
                        text = stringResource(R.string.matrix_side_columns, spec.columns),
                        onClick = {
                            expanded = false
                            onSelectSideSpec(spec)
                        },
                        modifier = Modifier.semantics { selected = isSelected },
                        config = config,
                        trailingIcon = if (isSelected) {
                            { MenuCheck(config) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

/**
 * 版面編輯的開關。
 *
 * 平板那兩個顯示模式（雙表儀表舱、路線與賽道地圖）用這一顆，不用 [LayoutMenuButton]：
 * 它們的格數是照參考稿定死的骨架，沒有 2×4 / 3×5 那種選項可選，掛一個只有一項的
 * 下拉菜單只是多一次點擊。能改的只有「哪一格放什麼」，所以直接切開關。
 *
 * @param editing 是否已在編輯版面
 * @param onToggle 切換編輯狀態
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符
 */
@Composable
fun LayoutEditToggle(
    editing: Boolean,
    onToggle: () -> Unit,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(TOUCH_SIZE)
            .tactileClickable(
                onClickLabel = stringResource(
                    if (editing) R.string.matrix_layout_edit_done
                    else R.string.matrix_layout_edit_start
                ),
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        GridGlyph(color = if (editing) config.accentToneColor else config.screenContentColor)
    }
}

@Composable
private fun MenuCheck(config: GlassConfig) {
    Text(
        text = CHECK,
        color = config.accentToneColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

/**
 * 四格網格的圖示。
 *
 * `material-icons-core` 裡沒有 GridView，為一顆按鈕引進整包 extended 不值得，
 * 直接畫四個小方塊——它表達的正是這顆按鈕在調的東西。
 */
@Composable
private fun GridGlyph(color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(GLYPH_BOX)) {
        val gap = GLYPH_GAP.toPx()
        val cell = (size.width - gap) / 2f
        repeat(2) { row ->
            repeat(2) { column ->
                drawRect(
                    color = color,
                    topLeft = Offset(column * (cell + gap), row * (cell + gap)),
                    size = Size(cell, cell),
                )
            }
        }
    }
}

private const val CHECK = "✓"
private val TOUCH_SIZE = 44.dp
private val GLYPH_BOX = 16.dp
private val GLYPH_GAP = 3.dp
