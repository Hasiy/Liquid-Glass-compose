package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import top.hasiy.designsystem.accentToneColor
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.TACTILE_SEAM
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.tokens.GlassVisualStyle

/**
 * 一組資料位的網格。
 *
 * 豎屏那一組與橫屏 dock 共用這一個，兩者只差在 [columns] 與 [density]。
 * 格數由 [MetricGridSpec] 決定（最多 15），用 Row/Column 排即可，不需要 LazyGrid。
 * 規格是上限：尾部空著的列不畫出來。
 *
 * 互動與參考稿一致：可調節的格子單擊開調節浮層，任何格子長按開編輯抽屜。
 * 另外多一個**版面編輯模式**（[editingLayout]）：格子可以拖著和別的格子換位。
 *
 * @param slots 要排的資料位，順序即顯示順序
 * @param state 當前狀態，決定每格顯示什麼、能不能調
 * @param config 玻璃主題參數
 * @param onAdjust 單擊可調節的格子時回呼，參數為資料位 key
 * @param onEdit 長按任一格子時回呼，參數為資料位 key
 * @param modifier 外部修飾符
 * @param columns 每列格數
 * @param spacing 格與格的間距
 * @param cellHeight 每格高度；null 表示由內容撐開
 * @param density 卡片密度檔
 * @param onSlotBounds 回報每個資料位在根座標系裡的位置
 * @param flat 整組格子共用外層的一塊面板，彼此只用分隔線隔開（橫屏 dock）
 * @param editingLayout 版面編輯模式：格子可拖著換位
 * @param onSwap 兩格換位，參數為（拖起來的 key，放下的 key）
 * @param onRemove 把某一格清空，參數為資料位 key
 * @param onDraggingChange 本塊網格是否正在被拖。使用端拿它把整塊提到同層最上面——
 *   卡片拖過這塊網格的邊界時，才不會被旁邊那塊蓋住
 * @param wideCells 讓「本桨 vs 上一桨」那類對比卡佔兩欄。豎屏開著、橫屏關掉——
 *   橫屏一格本來就有 ~135dp 寬，放得下左文右條；豎屏一格只有半屏寬，
 *   兩條橫進度會壓成兩根短線，看不出差
 * @param alignStart 把有內容的格子往前收攏，空槽全推到末尾。
 *
 *   關著時每個指標待在自己那一格不動——中間那幾格空著就空著，看起來像整行右對齊。
 *   開著時只改**顯示順序**，不動 [WorkoutUiState.layout]：關回去每一格還在原位。
 */
@Composable
fun MetricGrid(
    slots: List<MetricSlot>,
    state: WorkoutUiState,
    config: GlassConfig = LocalGlassConfig.current,
    onAdjust: (String) -> Unit,
    onEdit: (String) -> Unit,
    onStep: (String, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    columns: Int = 2,
    spacing: Dp = 8.dp,
    cellHeight: Dp? = null,
    density: MetricCardDensity = MetricCardDensity.REGULAR,
    onSlotBounds: (String, Rect) -> Unit = { _, _ -> },
    flat: Boolean = false,
    editingLayout: Boolean = false,
    onSwap: (String, String) -> Unit = { _, _ -> },
    onRemove: (String) -> Unit = {},
    onDraggingChange: (Boolean) -> Unit = {},
    wideCells: Boolean = false,
    inlineAdjust: Boolean = false,
    alignStart: Boolean = false,
) {
    // 尾部空著的列不佔位。
    //
    // 規格是**上限**不是下限：設成 3×5 但只有 8 個讀數時，第 3 列整列是空的，
    // 畫出來就是一排空殼白佔一列高度。
    //
    // 但也不能只留到最後一個有內容的格子——那一列剛好填滿時，後面的格子就再也
    // 夠不到了，3×5 這個設定等於白給。所以最後一列滿了就再放一列出來當「下一個位置」。
    // 這一格要不要畫成「不用點開就能調」的那種卡。
    //
    // 判斷用「這個指標**是不是控制項**」（量程存不存在），不是「現在能不能調」。
    // 後者會讓版面跟著斷線與裝置只讀狀態重排——線一斷格子就跳位。只讀時位置
    // 照佔，按鍵灰掉（見 InlineAdjustCard）。
    val inlineAdjustAt: (MetricSlot) -> Boolean = { slot ->
        inlineAdjust && state.metricAt(slot.key)?.let { metric ->
            // 兩個條件都要：有量程（知道一格跳多少），而且**這台裝置支援調它**。
            //
            // 只看量程的話，目標速度在只能調阻力的室內單車上也會佔掉兩欄——
            // 而按鍵是掛在 MetricCard 的 adjustable 分支裡的（問的是裝置），
            // 那一格就白佔一欄的寬度卻什麼也沒多出來。兩邊的判斷必須是同一個。
            state.device.isControllable(metric.id) && ControlRange.forMetric(metric.id) != null
        } == true
    }
    // 每格佔幾欄。內聯調節卡與對比卡佔兩欄，其餘一欄；欄數本來就只有 2 的時候佔滿一列。
    val spanOf: (MetricSlot) -> Int = { slot ->
        when {
            inlineAdjustAt(slot) -> WIDE_SPAN.coerceAtMost(columns)
            wideCells && state.metricAt(slot.key)?.comparison != null ->
                WIDE_SPAN.coerceAtMost(columns)
            else -> 1
        }
    }
    // 左對齊：把有內容的格子提到前面。只動顯示順序，不落成版面覆蓋——
    // 落成覆蓋就等於幫使用者重排了他的版面，關掉開關也回不去了。
    val ordered = if (alignStart) {
        compactSlots(slots) { state.metricAt(it.key) != null }
    } else {
        slots
    }
    // 規格「幾列 × 幾欄」給的是**欄位**總數，不是格子數。跨欄的格子吃掉兩個欄位，
    // 能放的格子數就得跟著減——不減的話擠不下的格子會換到新的一列去，2×4 的 dock
    // 會長成三列。橫屏只有 ~359dp 高，多一列就得捲，而「能捲到但看不全」跟
    // 「看不到」對使用者是同一件事。
    //
    // 編輯版面時不截：那時要把所有資料位都攤開來給人拖，少一個就是少一個拖放目標。
    val fitted = if (editingLayout) ordered else takeWithinBudget(ordered, slots.size, spanOf)
    val allRows = packMetricRows(fitted, columns, spanOf)
    val lastFilledKey = fitted.lastOrNull { state.metricAt(it.key) != null }?.key
    val rows = allRows.take(
        visibleRowCount(allRows, lastFilledKey, showEmpty = editingLayout)
    )

    // 拖動狀態。每格的位置收在這裡，用**根座標**。
    //
    // 不能用 boundsInParent：格子的直接父節點是它所在的那一列 Row，每一列各自
    // 從 0 算起，於是不同列的格子座標幾乎重合——跨列拖動時算出來的落點會落在
    // 沒有任何格子的地方，看起來就是「明明拖到目標上了卻換不了」。
    val cellBounds = remember { mutableStateMapOf<String, Rect>() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableStateOf(Offset.Zero) }

    /**
     * 拖到哪一格上了。
     *
     * 拿被拖格子的中心加上累計位移，看落在誰的範圍裡——用中心而不是手指位置，
     * 手指按在卡片邊角時落點才不會判到隔壁去。
     *
     * 寫成函數而不是算好的值：鬆手時要**重新算一次**。`dragDelta` 與 [cellBounds]
     * 都是狀態，在這裡讀到的永遠是當下；算成一個 val 的話，收尾那段閉包持著的
     * 是某次重組時的快照，鬆手時讀出來是 null——落點高亮明明亮著卻換不了位，
     * 就是這個原因。
     *
     * @param key 被拖的資料位
     */
    fun hitTargetOf(key: String): String? {
        val origin = cellBounds[key] ?: return null
        val point = origin.center + dragDelta
        return cellBounds.entries
            .firstOrNull { (other, rect) -> other != key && rect.contains(point) }
            ?.key
    }

    val hoverKey = draggingKey?.let(::hitTargetOf)

    // 上報給使用端，讓它把整塊網格提起來
    val dragging = draggingKey != null
    LaunchedEffect(dragging) { onDraggingChange(dragging) }

    /** 收尾：提交換位並清掉拖動狀態。 */
    fun stopDrag() {
        val from = draggingKey
        val to = from?.let(::hitTargetOf)
        draggingKey = null
        dragDelta = Offset.Zero
        if (from != null && to != null) onSwap(from, to)
    }
    // 一整塊的時候格子之間不留縫，改用分隔線——參考稿的 .l-dock .metric 是
    // `border-right` 加第一列的 `border-bottom`，不是八張各自成塊的小卡
    val gap = if (flat) 0.dp else spacing
    // Tactile 的接縫是暗槽，不是亮線
    val divider = if (config.visualStyle == GlassVisualStyle.TACTILE) {
        TACTILE_SEAM
    } else {
        config.lineColor
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // 分隔線畫在**容器**上，不是畫在每個格子身上。
            //
            // 掛在格子的修飾符上時，被拖起來的那一格會把自己的那兩條線一起帶走，
            // 於是拖動中會看到一條斜著跑的分隔線。線屬於這塊面板，不屬於格子。
            .then(
                if (flat) {
                    Modifier.gridDividers(divider, columns, rows.map { row -> row.map(spanOf) })
                } else {
                    Modifier
                }
            ),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        rows.forEach { row ->
            // 被拖的格子要浮在別的格子之上，而 zIndex 只在同一個 parent 內比較——
            // 跨列拖動時，光給格子提 zIndex 還是會被下一列的 Row 蓋住，所以整列一起提。
            val rowHasDragging = row.any { it.key == draggingKey }
            // 高度走 IntrinsicSize.Min：同一排的格子必須等高。
            //
            // 沒有這個約束時，Row 不給子項任何高度意見，每張卡各自按內容撐高——
            // 「18:42」是純七段碼，「3.82 km」多一個上標單位，兩者內容高度就差一截。
            // Row 本身取最高那張的高度，矮的那張**不會**被拉平，底色只畫到自己的
            // 高度，看起來就是同一排兩格一高一矮。
            //
            // IntrinsicSize.Min 讓這一排的高度等於「各子項最小內在高度」中的最大值，
            // 卡片再 fillMaxHeight 撐滿它。這也是 cellHeight 為 null（賽道頁、雙表頁
            // 那種高度跟著內容走的網格）時唯一能等高的辦法——那裡沒有外部算好的高度
            // 可以給，總高只能由內容自己決定。
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .zIndex(if (rowHasDragging) DRAG_LAYER else 0f),
            ) {
                row.forEach { slot ->
                    val dragging = slot.key == draggingKey
                    val hovered = slot.key == hoverKey
                    val span = spanOf(slot)
                    MetricSlotCell(
                        slot = slot,
                        state = state,
                        config = config,
                        onAdjust = onAdjust,
                        onEdit = onEdit,
                        modifier = Modifier
                            // 權重就是欄數：佔兩欄的卡拿兩份寬度，和上下兩列的
                            // 欄線才對得上
                            .weight(span.toFloat())
                            .then(if (cellHeight != null) Modifier.height(cellHeight) else Modifier)
                            .onGloballyPositioned { cellBounds[slot.key] = it.boundsInRoot() }
                            // 拖走之後原位留一個凹槽。
                            //
                            // 卡片是靠 graphicsLayer 位移飛出去的，節點的**布局位置沒動**，
                            // 所以畫在 graphicsLayer 之前的東西留在原地。少了這一筆，
                            // 拖起來的那一格就是面板上一個沒有邊界的洞。
                            .then(
                                if (dragging) {
                                    Modifier.dragSocket(
                                        color = config.lineColor,
                                        corner = metricCardCorner(config, density),
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .zIndex(if (dragging) DRAG_LAYER else 0f)
                            // 跟手：位移與提起來的縮放都在這一層，卡片本體不用知道自己在被拖
                            .graphicsLayer {
                                if (dragging) {
                                    translationX = dragDelta.x
                                    translationY = dragDelta.y
                                    scaleX = DRAG_SCALE
                                    scaleY = DRAG_SCALE
                                }
                            }
                            .then(
                                if (hovered) {
                                    Modifier.dropTarget(
                                        color = config.accentToneColor,
                                        corner = metricCardCorner(config, density),
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            ,
                        density = density,
                        onBounds = { onSlotBounds(slot.key, it) },
                        flat = flat,
                        dragEnabled = editingLayout,
                        onDragStart = {
                            draggingKey = slot.key
                            dragDelta = Offset.Zero
                        },
                        onDrag = { dragDelta += it },
                        onDragStop = ::stopDrag,
                        onRemove = { onRemove(slot.key) },
                        // 高度是外面給的（同一排要等高）才讓卡片撐滿——見 MetricSlotCell.fillCell
                        fillCell = cellHeight != null,
                        inlineAdjust = inlineAdjustAt(slot),
                        onStep = onStep,
                    )
                }
                // 最後一列不滿時補空位，否則剩下的格子會被拉寬，與上面幾列對不齊。
                // 補的是**剩下的欄數**而不是格數——一張佔兩欄的卡只算一格，
                // 按格數補會多留出一欄。
                val free = columns - row.sumOf(spanOf)
                if (free > 0) {
                    Spacer(Modifier.weight(free.toFloat()))
                }
            }
        }
    }
}

/**
 * 把有內容的格子排到前面，空的排到後面，各自維持原有的相對順序。
 *
 * 中間空著的槽位會讓那一行看起來右對齊（前面幾格空著、內容擠在右邊）。收攏之後
 * 空槽集中在末尾，尾部空行本來就不畫（見 [visibleRowCount]），版面自然齊頭。
 *
 * 這是**顯示層**的重排：呼叫端拿它排版，但 key 與指標的對應關係一點沒動。
 *
 * @param slots 資料位，順序即原始顯示順序
 * @param filled 判斷一格有沒有內容
 */
internal fun compactSlots(
    slots: List<MetricSlot>,
    filled: (MetricSlot) -> Boolean,
): List<MetricSlot> = slots.filter(filled) + slots.filterNot(filled)

/**
 * 按**欄位預算**截斷資料位。
 *
 * 規格是「幾列 × 幾欄」，乘起來是總欄位數；[slots] 的長度剛好就是這個數。
 * 一格佔一欄時兩者相等，有跨欄的格子時就不等了——那時要按欄位算，多出來的
 * 格子放不進規格給的列數裡。
 *
 * 放不下的就丟掉，不去後面找一個佔一欄的補：補進來會讓顯示順序和資料位順序
 * 不一致，使用者拖動換位時就對不上了（同 [packMetricRows] 的理由）。
 *
 * @param slots 資料位，順序即顯示順序
 * @param budget 欄位總數
 * @param spanOf 每格佔幾欄
 */
internal fun takeWithinBudget(
    slots: List<MetricSlot>,
    budget: Int,
    spanOf: (MetricSlot) -> Int,
): List<MetricSlot> {
    if (budget <= 0) return emptyList()
    val kept = mutableListOf<MetricSlot>()
    var used = 0
    slots.forEach { slot ->
        val span = spanOf(slot).coerceAtLeast(1)
        if (used + span <= budget) {
            kept += slot
            used += span
        }
    }
    return kept
}

/**
 * 把資料位按欄數打包成列，允許某些格子佔多欄。
 *
 * 一格放不進當前這一列的剩餘欄位時就換列——不去後面找一個小格子填空。
 * 填空會讓顯示順序和資料位順序不一致，使用者拖動換位時就對不上了。
 *
 * @param slots 資料位，順序即顯示順序
 * @param columns 每列幾欄
 * @param spanOf 每格佔幾欄
 */
internal fun packMetricRows(
    slots: List<MetricSlot>,
    columns: Int,
    spanOf: (MetricSlot) -> Int,
): List<List<MetricSlot>> {
    if (columns <= 0) return emptyList()
    val rows = mutableListOf<List<MetricSlot>>()
    var row = mutableListOf<MetricSlot>()
    var used = 0
    slots.forEach { slot ->
        val span = spanOf(slot).coerceIn(1, columns)
        if (used + span > columns) {
            rows += row
            row = mutableListOf()
            used = 0
        }
        row += slot
        used += span
        if (used == columns) {
            rows += row
            row = mutableListOf()
            used = 0
        }
    }
    if (row.isNotEmpty()) rows += row
    return rows
}

/**
 * 該畫幾列。
 *
 * 規格是上限不是下限：設成 3×5 但只有 10 個讀數時，第 3 列整列是空的，
 * 畫出來就是一排空殼白佔一列高度、還多一條分隔線。所以留到最後一個有內容的
 * 格子所在的那一列就收手——**整列沒有資料的不畫**。
 *
 * 那些夠不到的空格子沒有丟掉：進版面編輯模式（[showEmpty]）時整個規格攤開，
 * 要往第 3 列填東西就在那裡填。平時不畫，是因為看讀數的人不需要看到空槽。
 *
 * @param rows 打包好的列
 * @param lastFilledKey 最後一個有內容的格子；一個都沒有時為 null
 * @param showEmpty 攤開整個規格，含整列空著的
 */
internal fun visibleRowCount(
    rows: List<List<MetricSlot>>,
    lastFilledKey: String?,
    showEmpty: Boolean = false,
): Int {
    if (rows.isEmpty()) return 0
    if (showEmpty) return rows.size
    // 一個都沒填也要留一列：否則沒有任何格子可以長按進去填
    if (lastFilledKey == null) return 1
    val rowIndex = rows.indexOfFirst { row -> row.any { it.key == lastFilledKey } }
    if (rowIndex < 0) return 1
    return rowIndex + 1
}

/**
 * 拖走之後原位的凹槽。
 *
 * 一塊比面板略深的底加一圈虛線，讓人看得出「這一格的東西被拿起來了，還沒放下」。
 *
 * @param color 描邊色
 * @param corner 跟卡片本體取同一個圓角
 */
private fun Modifier.dragSocket(color: Color, corner: Dp): Modifier = drawBehind {
    val radius = CornerRadius(corner.toPx())
    // 底就用分隔線本色：它已經是一層很淡的壓暗（8% 黑），疊在面板上剛好比周圍
    // 深一檔。這裡**不能**再 copy(alpha = ...)——那是**設定**不是相乘，
    // 8% 會被直接改成 35%，凹槽就成了一塊突兀的中灰。
    drawRoundRect(color = color, cornerRadius = radius)
    drawRoundRect(
        color = color.copy(alpha = (color.alpha * SOCKET_STROKE_GAIN).coerceAtMost(1f)),
        cornerRadius = radius,
        style = Stroke(
            width = SOCKET_STROKE.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(SOCKET_DASH.toPx(), SOCKET_DASH.toPx())
            ),
        ),
    )
}

/**
 * 拖動的落點提示。
 *
 * 畫在內容**之上**：被拖的卡片這時正浮在半空，落點格子只剩底色，
 * 描邊畫在下面會被自己的卡片蓋掉。
 *
 * @param color 描邊色
 * @param corner 跟卡片本體取同一個圓角
 */
private fun Modifier.dropTarget(color: Color, corner: Dp): Modifier = drawWithContent {
    drawContent()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(corner.toPx()),
        style = Stroke(width = DROP_STROKE.toPx()),
    )
}

/**
 * dock 面板上的分隔線。
 *
 * 畫在內容**之下**：flat 排法的格子本身是透明的（底由外層面板給），線在下面透得出來，
 * 而且不會蓋在拖起來浮在上面的那張卡上。
 *
 * 豎線**按每一列自己的格子邊界**畫，不是整塊等分。有跨欄的格子（內聯調節卡、
 * 對比卡）之後，那一列的縫就不在等分位置上了——照等分畫會有一條線直接穿過
 * 跨欄卡的中間，看起來像那張卡裂成兩半。所以豎線也只畫在自己那一列的高度內，
 * 不再貫穿整塊面板。
 *
 * 橫線仍是等分：每一列的高度由 `cellHeight` 統一給定（見 dock 的呼叫處）。
 *
 * @param color 線色
 * @param columns 欄數
 * @param rowSpans 每一列裡各格佔幾欄，順序與畫出來的格子一致
 */
private fun Modifier.gridDividers(
    color: Color,
    columns: Int,
    rowSpans: List<List<Int>>,
): Modifier = drawBehind {
    if (columns <= 0 || rowSpans.isEmpty()) return@drawBehind
    val width = 1.dp.toPx()
    val columnStep = size.width / columns
    val rowStep = size.height / rowSpans.size
    rowSpans.forEachIndexed { rowIndex, spans ->
        val top = rowStep * rowIndex
        val bottom = top + rowStep
        // 最後一格的右邊就是面板邊界，不用畫
        var used = 0
        spans.dropLast(1).forEach { span ->
            used += span
            drawLine(color, Offset(columnStep * used, top), Offset(columnStep * used, bottom), width)
        }
    }
    for (row in 1 until rowSpans.size) {
        val y = rowStep * row
        drawLine(color, Offset(0f, y), Offset(size.width, y), width)
    }
}

/** 被拖的格子提到最上層，並稍微放大一點，表達「拿起來了」。 */
/** 對比卡佔的欄數。 */
private const val WIDE_SPAN = 2

private const val DRAG_LAYER = 1f
private const val DRAG_SCALE = 1.06f
private val DROP_STROKE = 2.dp

/** 凹槽虛線相對分隔線本色加深幾倍，以及虛線的節距。 */
private const val SOCKET_STROKE_GAIN = 3.5f
private val SOCKET_STROKE = 1.dp
private val SOCKET_DASH = 3.dp
