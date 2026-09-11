package top.hasiyliquidglassdemo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.SevenSegmentText
import top.hasiy.designsystem.accentLightColor
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.isLightSurface
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.liftColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.glassOverlayColor
import top.hasiy.designsystem.quietAccentColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.tactileKeycap
import top.hasiy.designsystem.tokens.GlassVisualStyle
import top.hasiyliquidglassdemo.R

/**
 * 調節浮層。
 *
 * 對應參考稿的 `.p-focus` / `.l-focus`。這是一根**窄條**（參考稿 140×285），不是
 * 一張橫向鋪開的面板——寬度給多了，讀數、量程、滑桿之間就散開，整組比例會走樣。
 *
 * 滑桿是直立的管子，從底部往上填，管壁有刻痕、填充頂端一道「當前」標記。
 *
 * 拖動只改本地暫存值，鬆手才寫出去：每動一格就寫一次，等於對裝置連發幾十次請求。
 * 精調的 ± 是離散操作，直接寫。
 *
 * @param metric 正在調節的指標
 * @param currentValue 指標當前值的顯示字串
 * @param range 量程
 * @param config 玻璃主題參數
 * @param onCommit 鬆手後寫出新值
 * @param onStep 精調一格，參數為方向（+1 / −1）
 * @param modifier 外部修飾符
 */
@Composable
fun ControlFocusPanel(
    metric: WorkoutMetric,
    currentValue: String,
    range: ControlRange,
    config: GlassConfig = LocalGlassConfig.current,
    onCommit: (Float) -> Unit,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val committed = currentValue.toFloatOrNull() ?: range.min
    // 換一個指標調節時要重置暫存值，否則會把上一個指標的數字帶過來
    var draft by remember(metric.id) { mutableFloatStateOf(committed) }
    // 外部寫入成功或回彈後，暫存值要跟上——否則滑桿會停在一個已經作廢的位置
    var lastCommitted by remember(metric.id) { mutableFloatStateOf(committed) }
    if (lastCommitted != committed) {
        lastCommitted = committed
        draft = committed
    }

    val unit = metric.unitRes?.let { stringResource(it) }.orEmpty()
    val shape = RoundedCornerShape(PANEL_CORNER)
    val tactile = config.visualStyle == GlassVisualStyle.TACTILE
    // Tactile 的浮層本身也是一塊凸起的面板，不是半透明玻璃
    val surface = if (tactile) {
        Modifier.tactileKeycap(PANEL_CORNER)
    } else {
        // 浮層底要讀 glass 而不是 panel：參考稿的 `.p-focus` 用的是浮層那一階
        // （深色主題 `rgba(29,33,33,.97)`、Nordic 近白的 `--glass`）。
        // 拿 panel 會比它暗一階，壓在同樣是 panel 的卡片上分不開。
        //
        // 但 glass token 自帶 alpha（Nordic 是 .72），而它在參考稿裡是配
        // `backdrop-filter: blur` 成立的——背後被糊成一片才透得有道理。這裡沒有
        // 背景模糊，直接半透明的結果是整個儀表盤連刻度數字都從浮層裡透出來。
        // 所以先把它合成到屏底上取等效實色：色相仍是 glass 那一階（比 panel 亮），
        // 但不再透視。
        Modifier
            .clip(shape)
            .background(config.glassOverlayColor.compositeOver(config.screenColor))
            .border(1.dp, config.lineColor, shape)
    }

    Column(
        modifier = modifier
            .width(PANEL_WIDTH)
            .then(surface)
            .padding(PANEL_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FocusRead(metric = metric, text = range.format(draft), unit = unit, config = config)

        Row(
            modifier = Modifier.height(RANGE_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BoundColumn(range = range, config = config)
            VerticalValueSlider(
                fraction = range.fractionOf(draft),
                config = config,
                onFractionChange = { draft = range.valueAt(it) },
                onRelease = { onCommit(draft) },
            )
            StepColumn(range = range, unit = unit, config = config)
        }

        FocusStepper(config = config, onStep = onStep)
    }
}

/** 中央讀數：數值與單位同一行，單位只有數值的三分之一大。 */
@Composable
private fun FocusRead(metric: WorkoutMetric, text: String, unit: String, config: GlassConfig) {
    Column(
        modifier = Modifier.height(READ_HEIGHT),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(metric.labelRes),
            color = config.mutedContentColor,
            fontSize = LABEL_SIZE,
        )
        // 參考稿的 `.focus-read strong` 不是 block，單位跟在數字後面同一行，
        // 而且行內元素是**對基線**的。Compose 的 Alignment.Bottom 對的是盒底，
        // 34sp 與 11sp 的盒底對齊會讓單位掉到數字下面去。
        val digital = config.visualStyle == GlassVisualStyle.DIGITAL
        Row(verticalAlignment = Alignment.Bottom) {
            // 同指標卡：Digital 的核心讀數是七段數碼管
            if (digital) {
                SevenSegmentText(text = text, color = config.screenContentColor, fontSize = VALUE_SIZE)
            } else {
                Text(
                    text = text,
                    color = config.screenContentColor,
                    fontSize = VALUE_SIZE,
                    // 參考稿是 `font-weight: 420`——比正常再重一點點，落到 Compose
                    // 只有 Normal 這一檔。之前給了 Light，讀數整個虛掉，
                    // 跟旁邊 SemiBold 的量程數字擺在一起就是「粗細對不上」
                    fontWeight = FontWeight.Normal,
                    // 34sp 的預設行高會在數字上下各留一大截，把 49dp 的讀數區撐爆
                    lineHeight = VALUE_LINE_HEIGHT,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    color = config.mutedContentColor,
                    fontSize = LABEL_SIZE,
                    // 七段數碼管是自繪的，沒有基線可對，那條分支只能退回盒底對齊
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .then(
                            if (digital) Modifier.padding(bottom = 4.dp)
                            else Modifier.alignByBaseline()
                        ),
                )
            }
        }
    }
}

/**
 * 左欄：MAX 在上、MIN 在下，與滑桿的方向對上。
 *
 * 四行**沿整根管子均分**（參考稿的 `.range-bound` 是 `justify-content: space-between`
 * 配四個子元素），不是「MAX/16」黏在頂、「MIN/1」黏在底的兩組。擠成兩組之後標籤
 * 跟數字貼在一起，看起來像兩塊小標籤而不是一根量程尺。
 */
@Composable
private fun BoundColumn(range: ControlRange, config: GlassConfig) {
    Column(
        modifier = Modifier.width(SIDE_COLUMN_WIDTH).fillMaxHeight().padding(vertical = BOUND_PADDING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        MicroCaption(stringResource(R.string.focus_max), config.mutedContentColor)
        MicroValue(range.format(range.max), config.screenContentColor)
        MicroCaption(stringResource(R.string.focus_min), config.mutedContentColor)
        MicroValue(range.format(range.min), config.screenContentColor)
    }
}

/**
 * 右欄：步進值。
 *
 * 顏色走 [quietAccentColor] 而不是強調色本身：這是說明字，淺色配色會把它降成中性
 * （參考稿的 Nordic 就把 STEP 這一批從綠色改回 `#545454`）。深色配色下它仍是強調色。
 */
@Composable
private fun StepColumn(range: ControlRange, unit: String, config: GlassConfig) {
    val tone = config.quietAccentColor
    Column(
        modifier = Modifier.width(SIDE_COLUMN_WIDTH).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        // 這一欄是**居中**的三行小字，跟左欄「沿整根管子均分」不同——
        // 參考稿 `.range-step` 覆寫掉了 `.range-bound` 的 space-between
        verticalArrangement = Arrangement.spacedBy(STEP_ROW_GAP, Alignment.CenterVertically),
    ) {
        MicroCaption(stringResource(R.string.focus_step), tone)
        MicroValue(range.format(range.step), tone)
        if (unit.isNotEmpty()) MicroCaption(unit, tone)
    }
}

/** 量程欄的說明字：小一號、不加粗。 */
@Composable
private fun MicroCaption(text: String, color: Color) {
    Text(text = text, color = color, fontSize = MICRO_CAPTION_SIZE, textAlign = TextAlign.Center)
}

/** 量程欄的數字：大一號、加粗。參考稿是 `font-weight: 650`。 */
@Composable
private fun MicroValue(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = MICRO_VALUE_SIZE,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
    )
}

/**
 * 直立滑桿：一根從底部往上填的管子。
 *
 * 三層由下而上是管壁、填充、刻痕——刻痕要壓在填充**上面**，整根貫穿，
 * 才是「量筒上的刻度」；只畫在填充上方會變成一塊條紋方塊。
 *
 * 往上拖是加大：螢幕座標往下為正，所以位移要取反。
 *
 * @param fraction 當前位置 0..1
 * @param config 玻璃主題參數
 * @param onFractionChange 拖動中的新位置
 * @param onRelease 鬆手
 */
@Composable
private fun VerticalValueSlider(
    fraction: Float,
    config: GlassConfig = LocalGlassConfig.current,
    onFractionChange: (Float) -> Unit,
    onRelease: () -> Unit,
) {
    val density = LocalDensity.current
    val trackPx = with(density) { SLIDER_HEIGHT.toPx() }
    // pointerInput 的 lambda 只在 key 變動時重建，直接讀 fraction 會一路用著手勢
    // 開始那一刻的舊值。
    //
    // 兩個回呼一樣要包：這裡的 key 是 Unit，手勢協程整個生命週期只啟動一次，
    // 而呼叫端傳進來的是閉包在 range 上的 inline lambda。量程若在拖曳途中換了
    // （裝置重連會換），協程手上還是舊那份 closure，會拿舊量程換算——寫出去的值
    // 和畫面上顯示的就對不上。fraction 會過時的道理同樣適用於回呼本身。
    val latest by rememberUpdatedState(fraction)
    val latestOnFractionChange by rememberUpdatedState(onFractionChange)
    val latestOnRelease by rememberUpdatedState(onRelease)
    val clamped = fraction.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(SLIDER_CORNER)
    val tube = tubeStyleFor(config.visualStyle)

    ValueTube(
        fraction = clamped,
        orientation = TubeOrientation.VERTICAL,
        config = config,
        modifier = Modifier
            .size(width = SLIDER_WIDTH, height = SLIDER_HEIGHT)
            // 讀屏聚焦到這根管子時要念得出目前在哪：沒有這個語意它就是一塊空白區域。
            // 用 0..1 的比例而不是實際值：這個元件只認得比例，實際值與單位由上面
            // 那一欄的讀數負責，兩邊各報一次反而會念出兩個不同的數。
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f)
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { latestOnRelease() },
                    // 手勢被打斷也要提交，否則值停在拖到一半的地方卻沒寫出去
                    onDragCancel = { latestOnRelease() },
                ) { change, delta ->
                    change.consume()
                    latestOnFractionChange((latest - delta / trackPx).coerceIn(0f, 1f))
                }
            },
    )
}

/** 液柱的朝向。見 [ValueTube]。 */
internal enum class TubeOrientation { VERTICAL, HORIZONTAL }

/**
 * 一根液柱：管壁、填充、貫穿的刻痕、液面白線、管腔內壁陰影。
 *
 * 豎的橫的**是同一個元件**，只差 [orientation]。調節浮層裡是一根直立的管子，
 * 橫屏 dock 的指標卡上是同一根管子躺下來——兩處長得一樣才不用讓使用者認兩次。
 * 方向只影響「填充往哪長、刻痕怎麼排、白線怎麼擺」這三件事，其餘（管壁材質、
 * 漸變的兩個端點色、內壁陰影的受光方向）兩個朝向完全共用。
 *
 * 內壁陰影的受光方向不跟著轉：光永遠從上方來，管子躺下來之後仍然是頂緣最暗、
 * 底緣提亮。跟著轉會變成「左邊暗右邊亮」，那是另一個光源。
 *
 * 尺寸由呼叫端用 [modifier] 給（浮層給定寬高，dock 給高度加 weight），手勢也一樣
 * 掛在 [modifier] 上——dock 那根只用來看，不接受拖動。
 *
 * @param fraction 當前位置 0..1
 * @param orientation 朝向
 * @param config 玻璃主題參數
 * @param modifier 外部修飾符；尺寸、語意與手勢都從這裡進來
 * @param corner 管壁圓角。預設是浮層那根的值，橫著用時通常傳「高度的一半」做成膠囊
 * @param ticks 刻痕把量程等分成幾段；`null` 表示按固定間距鋪（浮層那根的原行為）
 * @param showGlow 要不要畫液面往外散的光暈。矮的管子散不開，反而糊掉液面
 */
@Composable
internal fun ValueTube(
    fraction: Float,
    orientation: TubeOrientation,
    config: GlassConfig = LocalGlassConfig.current,
    modifier: Modifier = Modifier,
    corner: Dp = SLIDER_CORNER,
    ticks: Int? = null,
    showGlow: Boolean = true,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val vertical = orientation == TubeOrientation.VERTICAL
    val shape = RoundedCornerShape(corner)
    val innerCorner = (corner - FILL_INSET).coerceAtLeast(0.dp)
    val tube = tubeStyleFor(config.visualStyle)

    Box(
        modifier = modifier
            .clip(shape)
            .background(tube.body)
            .border(1.dp, tube.border, shape)
            // 刻痕畫在最後，才會蓋在填充之上
            .drawWithContent {
                drawContent()
                val inset = FILL_INSET.toPx()
                // 刻痕要跟液柱**同一個形狀**地被裁掉。參考稿的 `.slider::after` 自帶
                // `border-radius: 30px`，所以刻度線在管子兩端的圓角處會跟著收窄；
                // 直接畫直線的話，端點圓角那一段線就會伸出綠色液柱之外。
                val clip = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = inset,
                            top = inset,
                            right = size.width - inset,
                            bottom = size.height - inset,
                            cornerRadius = CornerRadius(innerCorner.toPx(), innerCorner.toPx()),
                        )
                    )
                }
                clipPath(clip) {
                    // 1f 是一個**物理像素**，在 2.75x 的螢幕上細到幾乎看不見；
                    // 參考稿的 1px 是 CSS 像素，要換成 dp
                    val width = RIDGE_WIDTH.toPx()
                    if (ticks != null) {
                        // 等分成 ticks 段，畫中間那些分隔線
                        if (ticks > 1) {
                            val span = if (vertical) size.height else size.width
                            val step = span / ticks
                            for (index in 1 until ticks) {
                                val at = step * index
                                if (vertical) {
                                    drawLine(tube.ridge, Offset(inset, at), Offset(size.width - inset, at), width)
                                } else {
                                    drawLine(tube.ridge, Offset(at, inset), Offset(at, size.height - inset), width)
                                }
                            }
                        }
                    } else {
                        val gap = RIDGE_GAP.toPx()
                        val edge = RIDGE_INSET_Y.toPx()
                        if (vertical) {
                            var y = size.height - edge
                            while (y > edge) {
                                drawLine(tube.ridge, Offset(inset, y), Offset(size.width - inset, y), width)
                                y -= gap
                            }
                        } else {
                            var x = edge
                            while (x < size.width - edge) {
                                drawLine(tube.ridge, Offset(x, inset), Offset(x, size.height - inset), width)
                                x += gap
                            }
                        }
                    }
                }

                // 管腔的內壁陰影。參考稿的 `.slider` 帶三道 inset：
                //   inset 0 0 10px  rgba(24,24,24,.12)   整圈往內收的暗邊，做出深度
                //   inset 0 3px 7px rgba(24,24,24,.13)   受光方向：頂部內壁最暗
                //   inset 0 -2px 4px rgba(255,255,255,.95) 底緣提亮
                // Compose 沒有 inset shadow，用多層遞減的同心描邊把模糊堆出來；
                // 畫在刻痕之後，連填充一起壓住——這是「管子」而不是「一塊色條」的來源。
                drawTubeWell(cornerPx = corner.toPx())
            },
        contentAlignment = if (vertical) Alignment.BottomCenter else Alignment.CenterStart,
    ) {
        // 內圈：四邊各縮 4dp、圓角跟著管壁收 4dp。填充畫在這一層裡面，
        // 由它負責裁切——這樣填充本身可以保持平頭，等它長到管口時，
        // 端點自然被這圈圓角削成和管壁一樣的弧，中途則是一條平的液面。
        Box(
            modifier = Modifier
                // 四邊都留一圈內縮。參考稿的 `.slider-fill` 只寫了 left/right/bottom，
                // 拉到滿量程時填充直接被 `overflow: hidden` 切在管口上，另一端沒有邊距——
                // 那是 CSS 定位的副作用，不是想要的樣子。
                .padding(FILL_INSET)
                .fillMaxSize()
                .clip(RoundedCornerShape(innerCorner)),
            contentAlignment = if (vertical) Alignment.BottomCenter else Alignment.CenterStart,
        ) {
            // 填充：平頭 + 起始端圓角，從管底（左端）長出來
            Box(
                modifier = Modifier
                    .then(
                        if (vertical) {
                            Modifier.fillMaxWidth().fillMaxHeight(clamped)
                        } else {
                            Modifier.fillMaxHeight().fillMaxWidth(clamped)
                        }
                    )
                    .clip(
                        if (vertical) {
                            RoundedCornerShape(bottomStart = innerCorner, bottomEnd = innerCorner)
                        } else {
                            RoundedCornerShape(topStart = innerCorner, bottomStart = innerCorner)
                        }
                    )
                    .background(
                        if (vertical) {
                            Brush.verticalGradient(
                                listOf(config.accentLightColor, config.accentToneColor)
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(config.accentLightColor, config.accentToneColor)
                            )
                        }
                    )
            )

            // 液面往外散出去的光暈。參考稿 `.slider-fill` 帶一道
            // `box-shadow: 0 -5px 16px rgba(lime,.24)`（Tactile 是 `0 0 12px …,.48`）：
            // 液柱是會發光的，光從液面往空管腔裡打。
            //
            // Compose 沒有彩色外發光（Modifier.shadow 的顏色在多數機型上不生效），
            // 改成在液面外側鋪一道漸淡的漸層。畫在填充**之後**、白線之前——
            // 它只佔液面以外的空管腔，跟兩者都不重疊，但白線必須壓在最上面。
            //
            // 尺寸用 fraction 撐出來再往外位移，這樣光暈永遠跟著液面走；
            // 拉到滿量程時它被管口的 clip 裁掉，跟參考稿的 `overflow: hidden` 一致。
            if (showGlow) {
                Box(
                    modifier = if (vertical) {
                        Modifier.fillMaxWidth().fillMaxHeight(clamped)
                    } else {
                        Modifier.fillMaxHeight().fillMaxWidth(clamped)
                    },
                    contentAlignment = if (vertical) Alignment.TopCenter else Alignment.CenterEnd,
                ) {
                    Box(
                        Modifier
                            .then(
                                if (vertical) {
                                    Modifier.offset(y = -tube.glowHeight)
                                        .fillMaxWidth()
                                        .height(tube.glowHeight)
                                } else {
                                    Modifier.offset(x = tube.glowHeight)
                                        .fillMaxHeight()
                                        .width(tube.glowHeight)
                                }
                            )
                            .background(
                                if (vertical) {
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        1f to config.accentToneColor.copy(alpha = tube.glowAlpha),
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        0f to config.accentToneColor.copy(alpha = tube.glowAlpha),
                                        1f to Color.Transparent,
                                    )
                                }
                            )
                    )
                }
            }

            // 液面標記：一道白線。參考稿在線上還騎了一枚「當前」小膠囊，那個不要——
            // 液面本身就是「當前」，標籤只是把刻度擋掉。
            //
            // 白色是對的，不分主題——這條線壓在**填充**的頂端，填充是強調色（綠 / 紅），
            // 白線在上面一清二楚。之前為了「淺色管壁上看不見白線」把它改成深色，
            // 前提就搞錯了：線永遠貼著液面，不會落在空管壁上。
            Box(
                modifier = if (vertical) {
                    Modifier.fillMaxWidth().fillMaxHeight(clamped)
                } else {
                    Modifier.fillMaxHeight().fillMaxWidth(clamped)
                },
                contentAlignment = if (vertical) Alignment.TopCenter else Alignment.CenterEnd,
            ) {
                Box(
                    Modifier
                        .then(
                            if (vertical) {
                                Modifier.padding(horizontal = THUMB_INSET_X)
                                    .fillMaxWidth()
                                    .height(THUMB_HEIGHT)
                            } else {
                                Modifier.padding(vertical = THUMB_INSET_X)
                                    .fillMaxHeight()
                                    .width(THUMB_HEIGHT)
                            }
                        )
                        .background(THUMB_COLOR)
                )
            }
        }
    }
}

/**
 * 管腔內壁的內陰影。
 *
 * 一圈整體的暗收邊 + 頂部再暗一道 + 底緣一道提亮，三者都用「多層遞減同心描邊」
 * 模擬高斯模糊：層數越多、每層越淡，堆起來才是漸層而不是硬邊。
 *
 * @param cornerPx 管壁圓角，描邊要跟著它走否則四角會露出直角
 */
private fun DrawScope.drawTubeWell(cornerPx: Float) {
    val corner = CornerRadius(cornerPx, cornerPx)
    val rim = WELL_RIM.toPx()
    repeat(WELL_LAYERS) { layer ->
        val t = (layer + 1f) / WELL_LAYERS
        // 二次方衰減：靠近邊緣濃、往內迅速散開
        val fade = (1f - t) * (1f - t)
        val width = rim / WELL_LAYERS
        val inset = width * layer + width / 2f
        drawRoundRect(
            color = WELL_SHADOW.copy(alpha = WELL_SHADOW.alpha * fade),
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2f, size.height - inset * 2f),
            cornerRadius = corner,
            style = Stroke(width = width),
        )
    }
    // 受光方向：頂緣再壓一道，底緣提亮
    drawRect(
        brush = Brush.verticalGradient(
            0f to WELL_TOP_SHADE,
            WELL_TOP_STOP to Color.Transparent,
            1f - WELL_BOTTOM_STOP to Color.Transparent,
            1f to WELL_BOTTOM_SHEEN,
        ),
    )
}

/** 精調：兩顆 ± 夾一段說明。 */
@Composable
private fun FocusStepper(
    config: GlassConfig = LocalGlassConfig.current,
    onStep: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", stringResource(R.string.focus_step_down), config) { onStep(-1) }
        // 參考稿在兩顆按鈕之間夾了一行「細調 / 每次 1 級」。那句話不放：
        // 步進值旁邊的 STEP 欄已經把「每次幾級」寫清楚了，這裡再說一遍是重複，
        // 而且中間只剩 ~36dp，那行字怎麼排都要折成兩行。
        Spacer(Modifier.weight(1f))
        StepButton("＋", stringResource(R.string.focus_step_up), config) { onStep(+1) }
    }
}

@Composable
private fun StepButton(
    label: String,
    description: String,
    config: GlassConfig = LocalGlassConfig.current,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(STEP_CORNER)
    // 這兩顆是精調，不是主操作，符號跟著 STEP 欄一起降調——深色配色下 quietAccent
    // 回落到強調色（參考稿 base 的 `color: var(--lime)`），淺色配色下是中性深灰
    // （參考稿 Nordic 的 `#3d3d3d`）。
    val tone = config.quietAccentColor
    // Tactile 的 ± 是兩顆真的按鍵，不是描邊色塊
    val surface = if (config.visualStyle == GlassVisualStyle.TACTILE) {
        Modifier.tactileKeycap(STEP_CORNER)
    } else if (config.isLightSurface) {
        // 淺色屏：一塊 lift 的實心方塊、不帶描邊（參考稿 Nordic 覆寫了
        // `background: var(--lift); border-color: transparent`）。
        // 淺色屏的 lift 比浮層底亮一階，實心就分得開。
        Modifier.clip(shape).background(config.liftColor)
    } else {
        // 深色屏：強調色 8% 的淡底 + 32% 的描邊（參考稿 base 的
        // `background: rgba(lime,.08); border: 1px solid rgba(lime,.32)`）。
        //
        // 這裡**不能**跟淺色屏一樣用 lift：深色配色的 lift 是 #1D2121，
        // 而浮層底就是 #1D2121，兩者同色，按鈕整塊消失只剩符號浮在面板上。
        Modifier
            .clip(shape)
            .background(tone.copy(alpha = STEP_FILL_ALPHA))
            .border(1.dp, tone.copy(alpha = STEP_BORDER_ALPHA), shape)
    }
    Box(
        modifier = Modifier
            .size(STEP_BUTTON_SIZE)
            // 縮放要在表面之前，否則按下時只有文字縮、底不動
            .tactileClickable(onClickLabel = description, onClick = onClick)
            .then(surface),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = tone,
            fontSize = STEP_SYMBOL_SIZE,
            // 參考稿的 `.focus-stepper button` 沒有指定字重，就是正常體
            fontWeight = FontWeight.Normal,
        )
    }
}

/**
 * 管子的三種樣子。
 *
 * 這根管子不是跟著主題色走的表面，而是一個實體零件——參考稿為每套風格單獨寫了
 * 一組值：一般主題與 Digital 是白色塑膠管，Tactile 換成深色金屬（而且漸層是
 * **橫向**的，做出圓柱的受光）。所以這裡按風格分派，不接 `track` token。
 *
 * @param body 管壁
 * @param border 管口描邊
 * @param ridge 管壁上的刻痕
 * @param glowAlpha 液面往上那道光暈的濃度
 * @param glowHeight 光暈往上散開的距離
 */
internal data class TubeStyle(
    val body: Brush,
    val border: Color,
    val ridge: Color,
    val glowAlpha: Float,
    val glowHeight: Dp,
)

internal fun tubeStyleFor(style: GlassVisualStyle): TubeStyle = when (style) {
    GlassVisualStyle.TACTILE -> TubeStyle(
        // 橫向三段：兩側暗、中間亮，看起來才是一根圓柱而不是一塊板
        body = Brush.horizontalGradient(
            0f to Color(0xFF171818),
            0.48f to Color(0xFF3B3D3C),
            1f to Color(0xFF111212),
        ),
        border = Color(0xFF090A0A),
        ridge = Color(0x45182018),
        // 深色管壁上綠光本來就顯眼，參考稿那組 `0 0 12px …,.48` 直接照搬會糊成
        // 一大塊分不清液面在哪。散開的距離收一半、濃度也壓下來。
        glowAlpha = TACTILE_GLOW_ALPHA,
        glowHeight = TACTILE_GLOW_HEIGHT,
    )

    // 中性軸的配色（Nordic）配的是一根乾淨的白管：縱向由上往下微微轉暗，
    // 不帶基礎主題那層綠味（`#EDF1EA` 壓在中性灰面板上會泛綠）
    GlassVisualStyle.NEUTRAL -> TubeStyle(
        body = Brush.verticalGradient(listOf(Color(0xFFFAFAFA), Color(0xFFEBEBEB))),
        border = Color(0x14000000),
        ridge = Color(0x451A1A1A),
        glowAlpha = FILL_GLOW_ALPHA,
        glowHeight = FILL_GLOW_HEIGHT,
    )

    GlassVisualStyle.DIGITAL -> TubeStyle(
        body = SolidColor(Color(0xFFF8F9F7)),
        border = Color(0x9E0C110F),
        ridge = Color(0x45111514),
        glowAlpha = FILL_GLOW_ALPHA,
        glowHeight = FILL_GLOW_HEIGHT,
    )

    else -> TubeStyle(
        body = SolidColor(Color(0xFFEDF1EA)),
        border = Color(0x8AFFFFFF),
        ridge = Color(0x45182018),
        glowAlpha = FILL_GLOW_ALPHA,
        glowHeight = FILL_GLOW_HEIGHT,
    )
}

private val RIDGE_WIDTH = 1.dp

/** 管腔內壁陰影：收邊寬度、層數，以及頂暗底亮那一組。 */
private val WELL_RIM = 9.dp
private const val WELL_LAYERS = 10
private val WELL_SHADOW = Color(0x1F181818)
private val WELL_TOP_SHADE = Color(0x21181818)
private val WELL_BOTTOM_SHEEN = Color(0x40FFFFFF)
private const val WELL_TOP_STOP = 0.09f
private const val WELL_BOTTOM_STOP = 0.05f

/*
 * 尺寸換算：參考稿的手機屏內寬 300px 對應真機 ~360dp，但這個檔跟指標卡一樣
 * 採「**距離 1:1、字號按可讀性上調**」——6px 的說明字直接乘 1.2 還是只有 7sp，
 * 在真機上看不清。所以 dp 照抄參考稿的 px，sp 在參考稿的基礎上加兩號。
 *
 * 距離必須整組照抄，不能只放大其中一項：面板寬度單獨從 140 調到 168 之後，
 * 管子（58）佔面板的比例從 41% 掉到 35%，整根管子看起來就細了一圈。
 */

/** 參考稿 `.p-focus`：140×285、padding 13、radius 30。 */
private val PANEL_WIDTH = 140.dp
private val PANEL_PADDING = 13.dp
private val PANEL_CORNER = 30.dp

/** `.focus-read` 高 49；字號上調後行高跟著讓一點，否則讀數會被切。 */
private val READ_HEIGHT = 56.dp
private val LABEL_SIZE = 11.sp
private val VALUE_SIZE = 34.sp
private val VALUE_LINE_HEIGHT = 38.sp

/** `.focus-range` 高 164，滑桿 58×154。 */
private val RANGE_HEIGHT = 164.dp
private val SLIDER_WIDTH = 58.dp
private val SLIDER_HEIGHT = 154.dp
private val SLIDER_CORNER = 36.dp
/** 內圈的圓角：管壁半徑減掉一圈內縮，填充的下緣才貼得住管底。 */
private val INNER_CORNER = 32.dp

/**
 * 兩側量程欄的寬度。
 *
 * 參考稿是 `31px | 1fr | 31px` 的網格，中間那格只有 42px 而管子有 58px，靠 CSS
 * 的溢出往兩邊各壓 8px。這裡不做溢出，改成三欄剛好填滿內容寬（114 = 28+58+28），
 * 視覺結果一樣、量測結果可預期。
 */
private val SIDE_COLUMN_WIDTH = 28.dp
private val BOUND_PADDING = 7.dp
private val MICRO_CAPTION_SIZE = 8.sp
private val MICRO_VALUE_SIZE = 10.sp
private val STEP_ROW_GAP = 4.dp

private val FILL_INSET = 4.dp

/**
 * 刻痕的左右內縮。
 *
 * 跟 [FILL_INSET] 取同一個值，讓刻度線和綠色液柱**等寬**——參考稿的刻痕
 * (`inset: 7px 5px`) 和填充 (`left/right: 4px`) 差 1px，落到真機上那 1dp 的差
 * 反而讓刻度看起來是照管子外壁量的。
 */
private val RIDGE_INSET_X = FILL_INSET
private val RIDGE_INSET_Y = 7.dp
private val RIDGE_GAP = 11.dp
/**
 * 液面光暈的高度與濃度。
 *
 * 參考稿是 `0 -5px 16px`：往上偏 5、模糊 16，可見範圍約 21px。
 */
private val FILL_GLOW_HEIGHT = 20.dp
private const val FILL_GLOW_ALPHA = 0.24f
private val TACTILE_GLOW_HEIGHT = 11.dp
private const val TACTILE_GLOW_ALPHA = 0.3f

private val THUMB_HEIGHT = 3.dp
private val THUMB_INSET_X = 3.dp

/** 液面標記。參考稿三套風格都沒有覆寫 `.slider-thumb`，一律白色。 */
private val THUMB_COLOR = Color.White

private val STEP_BUTTON_SIZE = 34.dp
private val STEP_CORNER = 12.dp
private val STEP_SYMBOL_SIZE = 17.sp

/** 深色屏上 ± 的淡底與描邊濃度。參考稿 base 是 `.08` / `.32`。 */
private const val STEP_FILL_ALPHA = 0.08f
private const val STEP_BORDER_ALPHA = 0.32f
