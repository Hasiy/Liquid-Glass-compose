package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.screenContentColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.onAccentColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import top.hasiyliquidglassdemo.R

/**
 * 一個資料位：指標卡外加上單擊、長按與長按進度。
 *
 * 卡片本身只管呈現，手勢放在這一層，空位也才能長按換指標。
 *
 * 長按參數取自 spec 3.4：500 ms 觸發，觸發後抑制一次 click（[detectTapGestures]
 * 本身就保證長按之後不再發 onTap，不必自己記時間戳）。
 *
 * @param slot 資料位
 * @param state 當前狀態
 * @param config 玻璃主題參數
 * @param onAdjust 單擊可調節的格子時回呼
 * @param onEdit 長按時回呼
 * @param modifier 外部修飾符
 * @param density 卡片密度檔
 * @param onBounds 回報自己在根座標系裡的位置，供調節浮層貼著這張卡彈出
 * @param flat dock 裡的格子沒有自己的底，見 [MetricCard]
 * @param dragEnabled 是否處於版面編輯模式。開著時本格可以拖著換位，
 *   單擊改成開換指標抽屜——長按那條路留給非編輯模式
 * @param onDragStart 開始拖動
 * @param onDrag 拖動位移增量
 * @param onDragStop 鬆手或手勢被打斷
 * @param onRemove 把這一格清空。只在版面編輯模式下給得出來
 * @param fillCell 卡片要不要撐滿這一格。
 *
 *   只有**外面給了固定高度**的網格才傳 true（橫屏側邊與 dock，那裡同一排要等高）：
 *   那時這一格的高度是給定的，卡片按內容撐開就比格子矮一截，而編輯模式的虛線畫在
 *   格子上，於是虛線比卡片大一圈、底下露出一條空隙。
 *
 *   高度由內容決定的網格（豎屏、平板兩頁的會話區）必須是 false。那裡的父約束是
 *   「剩下的整頁」，撐滿等於把第一列拉成通天長條，後面幾列全被擠出畫面。
 */
@Composable
fun MetricSlotCell(
    slot: MetricSlot,
    state: WorkoutUiState,
    config: GlassConfig = LocalGlassConfig.current,
    onAdjust: (String) -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    density: MetricCardDensity = MetricCardDensity.REGULAR,
    onBounds: (Rect) -> Unit = {},
    flat: Boolean = false,
    dragEnabled: Boolean = false,
    onDragStart: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragStop: () -> Unit = {},
    onRemove: () -> Unit = {},
    fillCell: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val metric = state.metricAt(slot.key)
    val canAdjust = state.canAdjustAt(slot.key)

    // 抽屜開著的時候不再接受新的長按——參考稿也是這樣擋的，否則會邊拖抽屜邊觸發
    val editing = state.editingSlotKey
    val gestureEnabled = editing == null
    // 虛線描邊與鉛筆角標標的是**正在編輯的那一格**。
    //
    // 參考稿是 `body.metric-editing .phone:not(.active) [data-slot-key]`——標在
    // 「非當前」的那些格子上，但那是因為它的示意板上同時擺了好幾台手機，要表達
    // 「其他這幾台的格子也能改」。真機上只有一個畫面，標成一片反而看不出在改哪個。
    // 版面編輯模式下每一格都標上：虛線框加角標表達的正是「這一格現在可以動」，
    // 拿來當「可拖」的提示剛好。長按開抽屜那條路的表現不變——那時只標被編輯的那一格。
    val markedForEdit = editing == slot.key || dragEnabled

    // 抽屜關掉後把焦點還給原來那一格。參考稿存的是 previousFocus，Compose 這邊
    // 由格子自己認領：只有「剛才被編輯的正是我」才收回焦點。
    // 觸控下看不出差別，但實體按鍵／讀屏是靠焦點走位的，丟了就回到頁首。
    val focusRequester = remember { FocusRequester() }
    var wasEditingThis by remember(slot.key) { mutableStateOf(false) }
    LaunchedEffect(editing) {
        when {
            editing == slot.key -> wasEditingThis = true
            wasEditingThis -> {
                wasEditingThis = false
                // 格子可能已經滾出組合範圍，requestFocus 會拋
                runCatching { focusRequester.requestFocus() }
            }
        }
    }

    var pressing by remember(slot.key) { mutableStateOf(false) }
    val holdProgress by animateFloatAsState(
        targetValue = if (pressing) 1f else 0f,
        // 按下時勻速轉一圈；放開要立刻歸零，不能倒著轉回去
        animationSpec = if (pressing) tween(LONG_PRESS_MS, easing = LinearEasing) else snap(),
        label = "holdProgress",
    )

    // 手勢被外部關掉（抽屜開了、換頁）時也要歸零，光靠 onLongPress 收不乾淨
    LaunchedEffect(gestureEnabled) {
        if (!gestureEnabled) pressing = false
    }

    // pointerInput 的 lambda 只在 key 變動時重建，裡面捕獲的回呼是**手勢開始那一刻**
    // 的那一份。網格層的 onDragStop 要讀「現在拖到哪一格上了」，用舊閉包讀到的是
    // 拖動還沒開始時的 null——落點高亮亮著、鬆手卻什麼都沒換，就是這個原因。
    val latestDragStart by rememberUpdatedState(onDragStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestDragStop by rememberUpdatedState(onDragStop)
    // 點擊那條手勢同理。key 裡雖然有 slot.key 等值，但呼叫端每次重組都會產生
    // 新的 lambda 實例，key 不變時協程手上就還是舊那份。目前呼叫端傳的是
    // controller 的方法引用（指向同一個 controller），抓到舊的行為等價——
    // 但這是巧合而不是保證，換成閉包在別的狀態上的 lambda 就會出錯。
    val latestOnEdit by rememberUpdatedState(onEdit)
    val latestOnAdjust by rememberUpdatedState(onAdjust)

    // 版面編輯模式下不接長按：那條路是「按住開換指標抽屜」，而編輯模式裡按住是要拖。
    // 寫成 `if (dragEnabled) null else { ... }` 會讓 Kotlin 把後面的花括號當成
    // 普通程式塊而不是 lambda，所以顯式宣告型別。
    val longPress: ((Offset) -> Unit)? = if (dragEnabled) {
        null
    } else {
        {
            // 先收掉進度環再開抽屜。開抽屜會讓 gestureEnabled 翻成 false，
            // 這個 pointerInput 隨即被取消，onPress 裡的 tryAwaitRelease() 永遠不會
            // 回來——不在這裡歸零的話，環會一直亮在那張卡上。
            pressing = false
            // 參考稿用 navigator.vibrate(12)；Android 上交給系統的長按觸感，
            // 各家機器的振動強度才會一致
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            latestOnEdit(slot.key)
        }
    }

    // 和卡片本體取同一個圓角，否則 Digital 下環會比卡片圓一大圈
    val corner = metricCardCorner(config, density)
    val cellModifier = modifier
        .onGloballyPositioned { onBounds(it.boundsInRoot()) }
        .focusRequester(focusRequester)
        .focusable()
        // 這張卡的手勢全靠底下的 pointerInput。focusable() 只讓它拿得到焦點，
        // 但無障礙服務看不到「這裡可以點」的語意動作——讀屏使用者在 explore-by-touch
        // 下雙擊時，系統不知道要把點擊合成到哪裡去。補一個語意動作，讓它跟
        // pointerInput 做同一件事。
        .semantics {
            if (gestureEnabled) {
                onClick {
                    when {
                        dragEnabled -> latestOnEdit(slot.key)
                        markedForEdit -> latestOnEdit(slot.key)
                        canAdjust -> latestOnAdjust(slot.key)
                        else -> return@onClick false
                    }
                    true
                }
            }
        }
        // 按下時輕微內縮，給一個「按到了」的回饋
        .scale(if (pressing) PRESS_SCALE else 1f)
        // 長按進度環的幾何放進 drawWithCache，不放在每帧的繪製裡。
        //
        // 環的路徑與它的**總長度**只跟卡片尺寸和圓角有關，跟 holdProgress 無關；
        // progress 只決定「取前面多長一段」。原本整段寫在 drawWithContent 裡，
        // 於是 500ms 的長按動畫期間，每一帧都重建一次 RoundRect 的 Path、
        // 再跑一次 PathMeasure.setPath——而 setPath 要走完整條路徑算長度，是這裡
        // 最貴的一步。cache 之後每帧只剩 getSegment。
        //
        // 顏色留在 onDraw 裡讀：它跟著主題走，寫進 cache 換主題就不會更新。
        .drawWithCache {
            val inset = RING_INSET.toPx()
            val ring = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                        ),
                        cornerRadius = CornerRadius((corner - RING_INSET).toPx()),
                    )
                )
            }
            val measure = PathMeasure().apply { setPath(ring, forceClosed = true) }
            val total = measure.length
            val stroke = Stroke(width = RING_WIDTH.toPx(), cap = StrokeCap.Round)
            // 段落 Path 也複用：每帧 reset 比每帧 new 便宜，getSegment 自己會 moveTo
            val segment = Path()

            onDrawWithContent {
                drawContent()
                if (holdProgress > 0f) {
                    segment.reset()
                    measure.getSegment(0f, total * holdProgress.coerceIn(0f, 1f), segment, true)
                    drawPath(segment, config.accentToneColor, style = stroke)
                }
                if (markedForEdit) drawEditOutline(config.accentToneColor, corner)
            }
        }
        // 拖動與點擊分成兩個 pointerInput：兩者都收得到 down，移動超過 slop 之後
        // 拖動會把事件消費掉，點擊那條自己放棄。寫在同一個裡面就得自己判 slop。
        .pointerInput(slot.key, dragEnabled) {
            if (!dragEnabled) return@pointerInput
            detectDragGestures(
                onDragStart = { latestDragStart() },
                onDragEnd = { latestDragStop() },
                // 手勢被打斷也要收尾，否則卡片會停在拖到一半的位置
                onDragCancel = { latestDragStop() },
            ) { change, delta ->
                change.consume()
                latestDrag(delta)
            }
        }
        .pointerInput(slot.key, gestureEnabled, canAdjust, dragEnabled) {
            if (!gestureEnabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    // 編輯模式下不轉進度環：那個環表達的是「按住就會開抽屜」，
                    // 而這時候單擊就開了，按住是要拖
                    pressing = !dragEnabled
                    tryAwaitRelease()
                    pressing = false
                },
                onLongPress = longPress,
                onTap = {
                    when {
                        // 版面編輯模式：單擊就是換指標，不必再長按
                        dragEnabled -> latestOnEdit(slot.key)
                        // 抽屜開著時點任一格＝改成編輯那一格，與參考稿一致
                        markedForEdit -> latestOnEdit(slot.key)
                        canAdjust -> latestOnAdjust(slot.key)
                    }
                },
            )
        }

    // 參考稿的 .metric.source 在調節時 visibility: hidden，但那是因為它的浮層**正好
    // 壓在那張卡上**（.p-focus 定位在指標格區域內），藏起來不會留洞。這裡的浮層是
    // 居中彈出的，源卡多半沒被蓋住——照抄的話會在版面上開一個空槽。
    // 空格子仍然佔一格位置——它是網格的一格，只是不畫框。沒有內容撐高的話
    // 這一格會塌成一條，同一列的其它格子就對不齊了。
    val cellHeight = if (metric == null) {
        Modifier.heightIn(min = density.pick(EMPTY_MIN, EMPTY_MIN_COMPACT, EMPTY_MIN_DENSE))
    } else {
        Modifier
    }
    // 見 [fillCell]。兩條路都要撐滿高度，差別只在寬度：
    //
    // · 外面給了固定高度（側邊、dock）→ fillMaxSize，寬高都跟格子。
    // · 沒給（賽道頁、雙表頁那種跟著內容走的網格）→ 只撐高度。寬度由 Row 的
    //   weight 決定，不能再 fillMaxWidth；高度則跟著 Row 的 IntrinsicSize.Min，
    //   那是同一排最高那張卡的高度——不撐的話這一排就會一高一矮。
    //
    // 這裡**不能**寫成 fillMaxSize 一把梭：那樣在 cellHeight 為 null 時會去吃
    // 父約束的整個剩餘高度，把一張卡拉成通天長條（pad 兩頁就是這樣壞過一次）。
    // 現在 Row 有了 IntrinsicSize.Min，高度不再是「剩餘整頁」，撐高才是安全的。
    val fill = if (fillCell) Modifier.fillMaxSize() else Modifier.fillMaxHeight()
    Box(modifier = cellModifier.then(cellHeight)) {
        if (metric == null) {
            EmptyMetricCard(
                config = config,
                // 空格子這一張**永遠**撐滿，不看 fillCell：這一格的高度由上面那個
                // heightIn(min) 保證，撐滿不會把格子拉長。而 EmptyMetricCard 裡面
                // 沒有任何內容撐尺寸，不撐滿的話它就是 0×0——編輯模式下那圈虛線
                // 畫在一個零尺寸的框上，等於整格看不見。
                modifier = Modifier.fillMaxSize(),
                editing = dragEnabled,
            )
        } else if (metric.comparison != null) {
            // 桨次指標走另一種卡：左邊報數、右邊兩條橫進度比本桨與上一桨。
            // 它跟普通指標卡沒有一處共用的內容結構，所以分成兩個 composable，
            // 而不是在 MetricCard 裡長出一條「如果有對比資料就……」的分支。
            ComparisonMetricCard(
                metric = metric,
                value = state.displayValueOf(metric),
                config = config,
                modifier = fill,
                density = density,
                flat = flat,
            )
        } else {
            MetricCard(
                metric = metric,
                value = state.displayValueOf(metric),
                config = config,
                modifier = fill,
                // 問裝置，不是問目錄。WorkoutMetric.controllable 只是目錄層的預設，
                // 「這台機器支不支援」由 WorkoutDeviceCapability 說了算——室內單車只能調
                // 阻力，目標速度雖然在目錄裡標成可調，也不該擺出 ± 的樣子。
                adjustable = state.device.isControllable(metric.id),
                readOnlyReason = state.readOnlyReasonAt(slot.key),
                density = density,
                art = if (slot.drawsArt) metric.art else MetricArt.NONE,
                // 第 2 組全是平均值與極值，看趨勢才有意義；主組是實時讀數，
                // 每張卡再掛一條線會把版面吵死。
                //
                // 只有**橫屏 dock** 那一組畫：參考稿的豎屏第 2 組沒有趨勢圖，
                // 三欄下每格只有 ~110dp 寬，文字和圖擠在一起兩樣都讀不清。
                trend = state.groupIndex == SECOND_GROUP && MetricSlots.isDock(slot.key),
                live = state.runtime.isLive,
                flat = flat,
            )
        }
        if (markedForEdit) {
            EditBadge(config = config, density = density, modifier = Modifier.align(Alignment.TopEnd))
        }
        // 版面編輯模式下左上角一顆「−」，點了就把這一格清空。
        //
        // 位置與樣式照系統通知欄的編輯態：徽標壓在卡片左上角，圓底加一道橫槓。
        // 只在編輯模式出現——長按開抽屜那條路是「換成別的指標」，不是刪。
        if (dragEnabled) {
            RemoveBadge(
                config = config,
                density = density,
                onClick = onRemove,
                // 騎在卡片左上角**外側**：壓在裡面會蓋住指標名稱。
                // 系統通知欄的編輯態也是讓徽標壓在卡片角上、半個身子在外面。
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = -REMOVE_BADGE_OUTSET, y = -REMOVE_BADGE_OUTSET),
            )
        }
    }
}

/**
 * 版面編輯模式下每格左上角的移除鈕。
 *
 * 照系統通知欄編輯態的做法：一顆圓底徽標加一道橫槓，壓在卡片角上。
 * 不用叉號——叉是「關閉／取消」，這裡是「把這一格拿掉」，減號才對得上。
 *
 * @param config 玻璃主題參數
 * @param density 卡片密度檔，決定徽標大小
 * @param onClick 點擊回呼
 * @param modifier 外部修飾符
 */
@Composable
private fun RemoveBadge(
    config: GlassConfig,
    density: MetricCardDensity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val size = if (density.isCompact) BADGE_COMPACT_SIZE else BADGE_SIZE
    Box(
        modifier = modifier
            // 命中區比徽標本身大一圈：運動中手會抖，18dp 的圓點按不準
            .size(size + REMOVE_TOUCH_GROWTH)
            .tactileClickable(
                onClickLabel = stringResource(R.string.matrix_slot_remove),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(config.screenContentColor.copy(alpha = REMOVE_FILL_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = size * REMOVE_BAR_RATIO, height = REMOVE_BAR_HEIGHT)
                    .clip(RoundedCornerShape(REMOVE_BAR_HEIGHT / 2))
                    .background(config.screenColor)
            )
        }
    }
}

/** 編輯模式下每格右上角的鉛筆角標。 */
@Composable
private fun EditBadge(
    config: GlassConfig,
    density: MetricCardDensity,
    modifier: Modifier = Modifier,
) {
    val size = if (density.isCompact) BADGE_COMPACT_SIZE else BADGE_SIZE
    Box(
        modifier = modifier
            .padding(if (density.isCompact) 2.dp else 5.dp)
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(config.accentToneColor),
        contentAlignment = Alignment.Center,
    ) {
        // 參考稿用的是 "✎"（U+270E）。系統字體不保證有這個字，真機上會變成空白的
        // 圓點，所以改用 Material 的鉛筆圖示。
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = null,
            tint = config.onAccentColor,
            modifier = Modifier.size(size * GLYPH_SIZE_RATIO),
        )
    }
}

/** 編輯模式下其餘格子的虛線描邊。 */
private fun DrawScope.drawEditOutline(color: Color, corner: Dp) {
    val inset = EDIT_OUTLINE_INSET.toPx()
    val width = 1.dp.toPx()
    drawRoundRect(
        color = color.copy(alpha = EDIT_OUTLINE_ALPHA),
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = CornerRadius((corner - EDIT_OUTLINE_INSET).toPx()),
        style = Stroke(
            width = width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        ),
    )
}

/** 第 2 組的組別索引。 */
private const val SECOND_GROUP = 1

/** spec 3.4：長按 500 ms 觸發編輯。 */
private const val LONG_PRESS_MS = 500

private const val PRESS_SCALE = 0.985f
/**
 * 長按進度環的內縮與線寬。
 *
 * 環沿著卡片的圓角邊框走。參考稿用的是 conic-gradient 加遮罩，那在 Compose 裡
 * 沒有直接對應，所以改成量出圓角矩形的路徑再取前一段——視覺上同樣是「沿著邊框
 * 轉一圈」，只是起點落在左上角圓角之後而不是正上方。
 */
private val RING_INSET = 2.dp
private val RING_WIDTH = 2.dp
/**
 * 編輯虛線往內縮多少。
 *
 * 只縮半個線寬多一點：描邊是沿路徑**置中**畫的，不縮的話外側那一半會被卡片的
 * clip 切掉，虛線看起來粗細不勻。縮太多（原本是 3dp）虛線框就明顯比卡片小一圈——
 * 側邊那種卡片底色看得見的地方，6dp 落在 44dp 的卡上是 14%，一眼就看出來對不齊。
 */
private val EDIT_OUTLINE_INSET = 1.dp
private const val EDIT_OUTLINE_ALPHA = 0.75f

/** 圖示佔角標的比例，留一圈邊 */
private const val GLYPH_SIZE_RATIO = 0.68f
private val BADGE_SIZE = 16.dp
private val BADGE_COMPACT_SIZE = 12.dp



/** 移除鈕：徽標外的命中餘量、橫槓相對徽標的長度與粗細，以及圓底濃度。 */
/** 空格子的最小高度，按密度檔跟同列的卡片對齊。 */
private val EMPTY_MIN = 64.dp
private val EMPTY_MIN_COMPACT = 56.dp
private val EMPTY_MIN_DENSE = 48.dp

private val REMOVE_TOUCH_GROWTH = 12.dp

/** 徽標往卡片外挪多少，讓它騎在角上而不是壓住標籤。 */
private val REMOVE_BADGE_OUTSET = 7.dp
private const val REMOVE_BAR_RATIO = 0.46f
private val REMOVE_BAR_HEIGHT = 2.dp
private const val REMOVE_FILL_ALPHA = 0.55f
