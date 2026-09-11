package top.hasiyliquidglassdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.DesignSystemTheme
import top.hasiy.designsystem.GlassCheckbox
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.GlassFilterChip
import top.hasiy.designsystem.GlassPaletteSelector
import top.hasiy.designsystem.GlassProgressBar
import top.hasiy.designsystem.GlassRadioButton
import top.hasiy.designsystem.GlassSlider
import top.hasiy.designsystem.GlassSwitch
import top.hasiy.designsystem.LocalGlassConfig
import top.hasiy.designsystem.accentDeepColor
import top.hasiy.designsystem.accentLightColor
import top.hasiy.designsystem.accentToneColor
import top.hasiy.designsystem.ambientColor
import top.hasiy.designsystem.canvasColor
import top.hasiy.designsystem.canvasLineColor
import top.hasiy.designsystem.dangerColor
import top.hasiy.designsystem.glassDeepOverlayColor
import top.hasiy.designsystem.glassOverlayColor
import top.hasiy.designsystem.inkColor
import top.hasiy.designsystem.liftColor
import top.hasiy.designsystem.lineColor
import top.hasiy.designsystem.mutedContentColor
import top.hasiy.designsystem.onAccentColor
import top.hasiy.designsystem.panelColor
import top.hasiy.designsystem.screenColor
import top.hasiy.designsystem.screenContentColor
import top.hasiy.designsystem.sunkColor
import top.hasiy.designsystem.trackColor
import top.hasiy.designsystem.tokens.ThemePalette
import top.hasiyliquidglassdemo.R
import java.util.Locale

/**
 * 主題色驗收頁。
 *
 * 這一頁的用途是驗證「切換主題後所有語意色同步變化」——上半部是活體語意
 * （連線狀態、主操作、進度填充、危險態），下半部是每個 token 的實際色值。
 * 兩者都在同一個 [DesignSystemTheme] 底下，任一 token 沒接上都會在這裡露出來。
 *
 * @param palette 當前主題色
 * @param onPaletteChange 主題切換回呼
 * @param onBack 返回上一頁
 * @param modifier 外部修飾符
 */
@Composable
fun ThemePalettePreviewScreen(
    palette: ThemePalette,
    onPaletteChange: (ThemePalette) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DesignSystemTheme(palette = palette) {
        val config = LocalGlassConfig.current
        Column(
            modifier = modifier
                .fillMaxSize()
                // 畫布底色：設備外框之外的頁面底
                .background(config.canvasColor)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            PreviewHeader(config = config, onBack = onBack)
            GlassPaletteSelector(
                selected = palette.id,
                onSelect = onPaletteChange,
                // padding 在外：選擇器自己的容器背景不該貼到螢幕邊緣
                modifier = Modifier.padding(
                    horizontal = PAGE_PADDING,
                    vertical = SECTION_GAP,
                ),
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = PAGE_PADDING),
                verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
            ) {
                SemanticShowcase(config = config)
                ComponentShowcase(config = config)
                TokenSwatches(config = config)
                Spacer(Modifier.height(PAGE_PADDING))
            }
        }
    }
}

@Composable
private fun PreviewHeader(config: GlassConfig, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HEADER_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.palette_preview_back_desc),
                tint = config.inkColor,
            )
        }
        Column {
            Text(
                text = stringResource(R.string.palette_preview_title),
                color = config.inkColor,
                fontSize = TITLE_FONT_SIZE,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.palette_preview_subtitle),
                color = config.canvasMutedColor,
                fontSize = CAPTION_FONT_SIZE,
            )
        }
    }
}

/**
 * 活體語意：模擬設備屏幕內的一小塊，把 token 用在它真正的角色上。
 *
 * 只看色板無法發現「軌道色跟填充色撞在一起」這類問題，必須讓它們並排出現。
 */
@Composable
private fun SemanticShowcase(config: GlassConfig) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CARD_CORNER))
            .background(config.screenColor)
            .padding(CARD_PADDING),
        verticalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        SectionLabel(
            text = stringResource(R.string.palette_preview_semantics),
            color = config.screenContentColor,
        )

        // 連線狀態：壓在屏底上的小字，用 accentDeep 才過得了對比度
        StatusChip(
            text = stringResource(R.string.palette_preview_connected),
            textColor = config.accentDeepColor,
            backgroundColor = config.accentToneColor.copy(alpha = CHIP_BACKGROUND_ALPHA),
        )

        // 主操作：accent 底 + onAccent 字
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(CTA_HEIGHT)
                .clip(RoundedCornerShape(CTA_CORNER))
                .background(config.accentToneColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.palette_preview_cta),
                color = config.onAccentColor,
                fontSize = BODY_FONT_SIZE,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // 進度填充：accentLight → accent 的漸層壓在 track 上
        Text(
            text = stringResource(R.string.palette_preview_progress),
            color = config.mutedContentColor,
            fontSize = CAPTION_FONT_SIZE,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TRACK_HEIGHT)
                .clip(RoundedCornerShape(TRACK_HEIGHT / 2))
                .background(config.trackColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(PROGRESS_FRACTION)
                    .height(TRACK_HEIGHT)
                    .background(
                        Brush.horizontalGradient(
                            listOf(config.accentLightColor, config.accentToneColor)
                        )
                    ),
            )
        }

        // 危險態：斷連 / 寫入失敗 / 設備拒絕共用同一個 danger
        StatusChip(
            text = stringResource(R.string.palette_preview_disconnected),
            textColor = config.dangerColor,
            backgroundColor = config.dangerColor.copy(alpha = CHIP_BACKGROUND_ALPHA),
        )
    }
}

@Composable
private fun StatusChip(text: String, textColor: Color, backgroundColor: Color) {
    Text(
        text = text,
        color = textColor,
        fontSize = CAPTION_FONT_SIZE,
        modifier = Modifier
            .clip(RoundedCornerShape(CHIP_CORNER))
            .background(backgroundColor)
            .padding(horizontal = CHIP_PADDING_H, vertical = CHIP_PADDING_V),
    )
}

/**
 * SDK 元件在這組配色下的樣子：選中態與軌道。
 *
 * 這一區存在的理由是元件目錄頁用的是四組 SDK 預設（palette 為 Unspecified，
 * 語意色都會回退到舊值），看不出 8 組配色的差別。要驗收
 * accent / onAccent / accentDeep / track 有沒有接對，只能在這裡看——
 * 尤其 Lime 這種亮色強調，白色前景會直接消失。
 *
 * 進度條與滑桿必須用 SDK 元件，不能用手繪的 Box：軌道色走的是
 * GlassConfig.asTrackSurface，手繪的看不出它有沒有生效。
 */
@Composable
private fun ComponentShowcase(config: GlassConfig) {
    var checked by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(true) }
    var switched by remember { mutableStateOf(true) }
    var chipSelected by remember { mutableStateOf(true) }
    var sliderValue by remember { mutableFloatStateOf(PROGRESS_FRACTION) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CARD_CORNER))
            .background(config.screenColor)
            .padding(CARD_PADDING),
        verticalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        SectionLabel(
            text = stringResource(R.string.palette_preview_selection),
            color = config.screenContentColor,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(SELECTION_GAP),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 每種元件都放選中與未選中各一個，方便直接對照
            GlassCheckbox(checked = checked, onCheckedChange = { checked = it }, config = config)
            GlassCheckbox(checked = false, onCheckedChange = {}, config = config)
            GlassRadioButton(selected = selected, onClick = { selected = !selected }, config = config)
            GlassRadioButton(selected = false, onClick = {}, config = config)
            GlassSwitch(checked = switched, onCheckedChange = { switched = it }, config = config)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SELECTION_GAP)) {
            GlassFilterChip(
                selected = chipSelected,
                onClick = { chipSelected = !chipSelected },
                label = stringResource(R.string.palette_preview_chip_selected),
                config = config,
            )
            GlassFilterChip(
                selected = false,
                onClick = {},
                label = stringResource(R.string.palette_preview_chip_unselected),
                config = config,
            )
        }
        // 軌道：填充段壓在 track 上，兩者撞色就會在這裡看出來
        GlassProgressBar(progress = PROGRESS_FRACTION, config = config)
        GlassSlider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            config = config,
        )
    }
}

/** 每個 token 的實際色值，用來核對是否與設計稿一致。 */
@Composable
private fun TokenSwatches(config: GlassConfig) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        SectionLabel(
            text = stringResource(R.string.palette_preview_tokens),
            color = config.inkColor,
        )
        // token 名是 API 標識而不是給終端使用者看的文案，所以不進 strings.xml：
        // 換語言時它們也不該跟著變，否則沒法拿來對照設計稿或程式碼。
        tokenRows(config).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
            ) {
                row.forEach { (name, color) ->
                    SwatchCell(
                        name = name,
                        color = color,
                        config = config,
                        modifier = Modifier.weight(1f),
                    )
                }
                // 最後一列不滿時補空位，色塊寬度才不會被撐大
                repeat(SWATCH_COLUMNS - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SwatchCell(
    name: String,
    color: Color,
    config: GlassConfig,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SWATCH_GAP)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(SWATCH_HEIGHT)
                .clip(RoundedCornerShape(SWATCH_CORNER))
                // 半透明 token 要疊在面板色上才看得出實際觀感，所以先鋪一層面板色
                .background(config.panelColor)
                .background(color)
                // 描邊讓近白／近黑的色塊在同色底上還有輪廓，否則 Lime 的
                // accentLight 與 screenContent 這幾個近白 token 會糊進畫布。
                // 用 canvasLineColor 而不是 lineColor：後者是屏內的分隔線，
                // 深色主題下是白色 9%，畫在淺色畫布上等於沒畫。
                .border(
                    width = SWATCH_BORDER,
                    color = config.canvasLineColor,
                    shape = RoundedCornerShape(SWATCH_CORNER),
                ),
        )
        Text(
            text = name,
            color = config.inkColor,
            fontSize = SWATCH_FONT_SIZE,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = color.toHexLabel(),
            color = config.canvasMutedColor,
            fontSize = SWATCH_FONT_SIZE,
        )
    }
}

/**
 * 區塊標題。
 *
 * [color] 必須由呼叫端傳入該區塊底色對應的前景色：屏內卡片用
 * `screenContentColor`，畫布上用 `inkColor`。深色主題的兩者剛好相反
 * （屏內近白、畫布近黑），寫死任何一個都會有一半的區塊讀不出來。
 */
@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = CAPTION_FONT_SIZE,
        fontWeight = FontWeight.SemiBold,
    )
}

/** 把 token 切成每行 [SWATCH_COLUMNS] 個。 */
private fun tokenRows(config: GlassConfig): List<List<Pair<String, Color>>> = listOf(
    "accent" to config.accentToneColor,
    "accentLight" to config.accentLightColor,
    "accentDeep" to config.accentDeepColor,
    "onAccent" to config.onAccentColor,
    "ambient" to config.ambientColor,
    "track" to config.trackColor,
    "danger" to config.dangerColor,
    "background" to config.canvasColor,
    "screen" to config.screenColor,
    "panel" to config.panelColor,
    "line" to config.lineColor,
    "muted" to config.mutedContentColor,
    "ink" to config.inkColor,
    "screenContent" to config.screenContentColor,
    "lift" to config.liftColor,
    "sunk" to config.sunkColor,
    "glass" to config.glassOverlayColor,
    "glassDeep" to config.glassDeepOverlayColor,
).chunked(SWATCH_COLUMNS)

/**
 * 畫布上的次級文字色。
 *
 * 不能直接用 `mutedContentColor`：那是**屏內**的次級色，深色主題下它是中灰
 * （#818A85），壓在淺灰畫布上只有 3:1 左右，小字讀不出來。畫布上的次級文字
 * 一律由 `inkColor` 淡化而來。
 */
private val GlassConfig.canvasMutedColor: Color
    get() = inkColor.copy(alpha = CANVAS_MUTED_ALPHA)

/** 帶 alpha 的 token 要把 alpha 印出來，否則核對不出半透明浮層的濃度。 */
private fun Color.toHexLabel(): String {
    val argb = toArgb()
    val rgb = argb and 0xFFFFFF
    val alpha = (argb ushr 24) and 0xFF
    return if (alpha == 0xFF) {
        String.format(Locale.ROOT, "#%06X", rgb)
    } else {
        String.format(Locale.ROOT, "#%02X%06X", alpha, rgb)
    }
}

private const val SWATCH_COLUMNS = 3
private const val PROGRESS_FRACTION = 0.62f
private const val CHIP_BACKGROUND_ALPHA = 0.13f
private const val CANVAS_MUTED_ALPHA = 0.62f

private val PAGE_PADDING = 16.dp
private val HEADER_PADDING = 4.dp
private val SECTION_GAP = 14.dp
private val ROW_GAP = 8.dp
private val SELECTION_GAP = 14.dp
private val CARD_CORNER = 20.dp
private val CARD_PADDING = 14.dp
private val CTA_HEIGHT = 44.dp
private val CTA_CORNER = 14.dp
private val TRACK_HEIGHT = 10.dp
private val CHIP_CORNER = 99.dp
private val CHIP_PADDING_H = 9.dp
private val CHIP_PADDING_V = 5.dp
private val SWATCH_HEIGHT = 40.dp
private val SWATCH_CORNER = 8.dp
private val SWATCH_GAP = 3.dp
private val SWATCH_BORDER = 1.dp

private val TITLE_FONT_SIZE = 18.sp
private val BODY_FONT_SIZE = 14.sp
private val CAPTION_FONT_SIZE = 11.sp
private val SWATCH_FONT_SIZE = 9.sp
