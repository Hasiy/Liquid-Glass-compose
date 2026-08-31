package top.hasiyliquidglassdemo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiyliquidglassdemo.R
import top.hasiyliquidglass.GlassBackdropHost
import top.hasiyliquidglass.GlassButton
import top.hasiyliquidglass.GlassCard
import top.hasiyliquidglass.GlassColorPicker
import top.hasiyliquidglass.GlassConfig
import top.hasiyliquidglass.GlassDialog
import top.hasiyliquidglass.GlassDialogBlurHost
import top.hasiyliquidglass.GlassIconButton
import top.hasiyliquidglass.GlassListItem
import top.hasiyliquidglass.GlassPopup
import top.hasiyliquidglass.GlassPopupBlurBox
import top.hasiyliquidglass.GlassPresets
import top.hasiyliquidglass.GlassProgressBar
import top.hasiyliquidglass.GlassVolumeSlider
import top.hasiyliquidglass.GlassSwitch
import top.hasiyliquidglass.GlassTextField
import top.hasiyliquidglass.GlassThemeSelector
import top.hasiyliquidglass.GlassTopBar
import top.hasiyliquidglass.glassSurface
import top.hasiyliquidglass.isLightSurface
import kotlin.math.roundToInt

/**
 * 四角裝飾光暈：橙、紅、紫、藍，透過半透明玻璃元件隱約可見，強化玻璃質感。
 *
 * 頁面底色不在此寫死，改由 [GlassConfig.pageBackgroundTop] / [GlassConfig.pageBackgroundBottom]
 * 提供，切到淺色的 Neutral 主題時整頁才會跟著變。
 */
private val GLOW_ORANGE = Color(0xFFFF7A18)
private val GLOW_RED = Color(0xFFE0245E)
private val GLOW_PURPLE = Color(0xFF7B2FF7)
private val GLOW_BLUE = Color(0xFF00C9FF)

/** Tab Bar 浮層與畫面邊緣的垂直間距 */
private val TAB_BAR_VERTICAL_PADDING = 8.dp

/** 淺色主題下裝飾光暈的透明度縮放：全開會在淺灰底上顯髒 */
private const val GLOW_ALPHA_SCALE_LIGHT = 0.25f

/**
 * DynamicLightTabBar 的示範畫面：
 * 依主題渲染的漸層背景 + 底部半透明毛玻璃 Tab Bar，並展示可套用於任意 UI 的玻璃組件與主題切換。
 *
 * 主題狀態由呼叫端持有（見 MainActivity），MaterialTheme 才能跟著切深淺。
 *
 * @param uiState 主題設定狀態
 * @param onPresetChange 主題切換回呼，參數為 [DemoThemeUiState.presets] 的索引
 * @param onAccentEnabledChange 強調色開關回呼
 * @param onAccentColorChange 強調色選擇回呼
 * @param onShadowEnabledChange 陰影開關回呼
 */
@Composable
fun DynamicLightTabBarDemoScreen(
    uiState: DemoThemeUiState,
    onPresetChange: (Int) -> Unit,
    onAccentEnabledChange: (Boolean) -> Unit,
    onAccentColorChange: (Color) -> Unit,
    onShadowEnabledChange: (Boolean) -> Unit,
) {
    val glassTheme = uiState.glassTheme
    var selectedIndex by remember { mutableIntStateOf(0) }
    var buttonClicked by remember { mutableStateOf(false) }
    var switchChecked by remember { mutableStateOf(true) }
    var textValue by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0.6f) }
    var showDialog by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    val tabs = listOf(
        stringResource(R.string.tab_0),
        stringResource(R.string.tab_1),
        stringResource(R.string.tab_2),
    )
    // Tab Bar 是浮層，捲動內容要自己讓出它佔的高度（含它避開導覽列的內距）
    val bottomInset = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val tabBarReservedHeight =
        DynamicLightTabBarConfig.BAR_HEIGHT_DP.dp + TAB_BAR_VERTICAL_PADDING * 2 + bottomInset

    // overlay 裡的東西不算背景取樣來源，所以浮層可以對 content 做背景模糊；
    // Tab Bar 若留在 content 裡會取樣到自己。
    GlassBackdropHost(
        modifier = Modifier.fillMaxSize(),
        overlay = {
            // 底部 Tab Bar：浮在內容之上，內容從它的毛玻璃底下捲過去
            DynamicLightTabBar(
                items = tabs,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                pillGlassConfig = glassTheme,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // 與內容區一致用 safeDrawing，橫放時側邊的導覽列與挖孔也一併避開
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = TAB_BAR_VERTICAL_PADDING)
            )
            if (showDialog) {
                GlassDialog(
                    onDismissRequest = { showDialog = false },
                    title = stringResource(R.string.glass_dialog_title),
                    message = stringResource(R.string.glass_dialog_message),
                    onConfirm = { showDialog = false },
                    onDismiss = { showDialog = false },
                    config = glassTheme
                )
            }
        }
    ) {
        // 頁面用 Box 疊層：內容鋪滿整個畫面，Tab Bar 浮在最上層。
        // 原本是 Column 分成上下兩段，Tab Bar 會佔掉版面高度，內容捲到底就停在它上緣，
        // 看起來像被「墊」在頁面下方而不是浮在頁面上。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            glassTheme.pageBackgroundTop,
                            lerp(
                                glassTheme.pageBackgroundTop,
                                glassTheme.pageBackgroundBottom,
                                0.5f
                            ),
                            glassTheme.pageBackgroundBottom
                        )
                    )
                )
        ) {
            // 裝飾色塊：四角各放一顆光暈（橙、紅、紫、藍），讓背景透過半透明的
            // 玻璃元件隱約可見，並在畫面中段自然混色，強化玻璃質感。
            // 淺色主題（Neutral）的底本身就亮，光暈全開會顯髒，統一調淡。
            val glowScale = if (glassTheme.isLightSurface) GLOW_ALPHA_SCALE_LIGHT else 1f
            GlowBlob(
                color = GLOW_ORANGE,
                alpha = 0.50f * glowScale,
                size = 260.dp,
                alignment = Alignment.TopStart,
                offsetX = (-70).dp,
                offsetY = (-50).dp
            )
            GlowBlob(
                color = GLOW_RED,
                alpha = 0.42f * glowScale,
                size = 230.dp,
                alignment = Alignment.TopEnd,
                offsetX = 70.dp,
                offsetY = 20.dp
            )
            GlowBlob(
                color = GLOW_PURPLE,
                alpha = 0.55f * glowScale,
                size = 280.dp,
                alignment = Alignment.BottomStart,
                offsetX = (-80).dp,
                offsetY = 60.dp
            )
            GlowBlob(
                color = GLOW_BLUE,
                alpha = 0.42f * glowScale,
                size = 240.dp,
                alignment = Alignment.BottomEnd,
                offsetX = 70.dp,
                offsetY = 40.dp
            )

            // 內容：玻璃主題設置 + 組件展示 + 操作提示
            //
            // safeDrawing 的頂部內距要加在 verticalScroll **之前**：加在之後的話內距
            // 屬於捲動內容，一往上捲就跟著跑掉，標題仍會鑽到狀態列與挖孔底下。
            // 用 safeDrawing 而不是 statusBarsPadding，橫放時的鏡頭挖孔才一併避開。
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    // 底部預留 Tab Bar 的高度：內容可以捲到它底下，但捲到盡頭時
                    // 最後一項仍露得出來，不會被永久遮住
                    .padding(top = 24.dp, bottom = 24.dp + tabBarReservedHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 玻璃主題設置
                Text(
                    text = stringResource(R.string.glass_theme_title),
                    color = glassTheme.contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                GlassThemeSelector(
                    selected = uiState.preset,
                    onSelect = { picked ->
                        val index = uiState.presets.indexOf(picked)
                        if (index >= 0) onPresetChange(index)
                    }
                )

                // 強調色：開關 + 選色盤。關閉時選中態與填充段回到玻璃質感
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.glass_accent_title),
                        color = glassTheme.contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    GlassSwitch(
                        checked = uiState.accentEnabled,
                        onCheckedChange = onAccentEnabledChange,
                        config = glassTheme
                    )
                }
                if (uiState.accentEnabled) {
                    GlassColorPicker(
                        colors = uiState.accentCandidates,
                        selected = uiState.accentColor,
                        onSelect = onAccentColorChange,
                        config = glassTheme
                    )
                }

                // 陰影開關：關掉後畫面更平，輪廓改由描邊與內陰影撐住
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.glass_shadow_title),
                        color = glassTheme.contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    GlassSwitch(
                        checked = uiState.shadowEnabled,
                        onCheckedChange = onShadowEnabledChange,
                        config = glassTheme
                    )
                }

                // 玻璃組件展示（隨主題切換）
                GlassTopBar(
                    title = stringResource(R.string.glass_topbar_title),
                    config = glassTheme,
                    navigationIcon = {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.glass_topbar_back_desc),
                            onClick = {},
                            config = glassTheme,
                            size = 36.dp
                        )
                    }
                )

                GlassCard(config = glassTheme) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(R.string.glass_box_label),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.glass_box_content),
                            fontSize = 13.sp
                        )
                        // 玻璃開關
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.glass_switch_label),
                                fontSize = 14.sp
                            )
                            GlassSwitch(
                                checked = switchChecked,
                                onCheckedChange = { switchChecked = it },
                                config = glassTheme
                            )
                        }
                        // 玻璃輸入框
                        GlassTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            config = glassTheme,
                            placeholder = stringResource(R.string.glass_textfield_placeholder)
                        )
                        // 玻璃進度條（可交互增減）
                        Text(
                            text = stringResource(R.string.glass_progress_label),
                            fontSize = 13.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .glassSurface(
                                        shape = RoundedCornerShape(17.dp),
                                        config = glassTheme
                                    )
                                    .clickable {
                                        progress = (progress - 0.1f).coerceAtLeast(0f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "−",
                                    color = glassTheme.contentColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            GlassProgressBar(
                                progress = progress,
                                config = glassTheme,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .glassSurface(
                                        shape = RoundedCornerShape(17.dp),
                                        config = glassTheme
                                    )
                                    .clickable {
                                        progress = (progress + 0.1f).coerceAtMost(1f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+",
                                    color = glassTheme.contentColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = stringResource(
                                R.string.glass_progress_value_format,
                                (progress * 100).roundToInt()
                            ),
                            fontSize = 13.sp
                        )
                        // 玻璃進度條（加寬版）
                        Text(
                            text = stringResource(R.string.glass_progress_wide_label),
                            fontSize = 13.sp
                        )
                        GlassProgressBar(
                            progress = progress,
                            config = glassTheme,
                            height = 26.dp
                        )
                        // 音量條：整條可按可拖，直接控制上面同一個 progress
                        Text(
                            text = stringResource(R.string.glass_volume_label),
                            fontSize = 13.sp
                        )
                        GlassVolumeSlider(
                            value = progress,
                            onValueChange = { progress = it },
                            config = glassTheme
                        )
                        // 玻璃進度條（不確定 / 載入中）
                        Text(
                            text = stringResource(R.string.glass_progress_loading),
                            fontSize = 13.sp
                        )
                        GlassProgressBar(
                            progress = 0f,
                            config = glassTheme,
                            indeterminate = true
                        )
                    }
                }

                GlassCard(config = glassTheme) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(R.string.glass_components_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        GlassButton(
                            text = stringResource(R.string.glass_button_label),
                            onClick = { buttonClicked = true },
                            config = glassTheme
                        )
                        if (buttonClicked) {
                            Text(
                                text = stringResource(R.string.glass_button_clicked),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            GlassIconButton(
                                icon = Icons.Default.Add,
                                contentDescription = stringResource(R.string.glass_icon_add_desc),
                                onClick = {},
                                config = glassTheme
                            )
                            GlassIconButton(
                                icon = Icons.Default.Favorite,
                                contentDescription = stringResource(R.string.glass_icon_heart_desc),
                                onClick = {},
                                config = glassTheme
                            )
                        }
                    }
                }

                // 玻璃列表樣例
                GlassCard(config = glassTheme) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.glass_list_label),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        GlassListItem(
                            title = stringResource(R.string.glass_list_item1_title),
                            subtitle = stringResource(R.string.glass_list_item1_sub),
                            leadingIcon = Icons.Default.Settings,
                            trailing = stringResource(R.string.glass_list_item1_trailing),
                            config = glassTheme,
                            onClick = {}
                        )
                        GlassListItem(
                            title = stringResource(R.string.glass_list_item2_title),
                            subtitle = stringResource(R.string.glass_list_item2_sub),
                            leadingIcon = Icons.Default.Notifications,
                            trailing = stringResource(R.string.glass_list_item2_trailing),
                            config = glassTheme,
                            onClick = {}
                        )
                        GlassListItem(
                            title = stringResource(R.string.glass_list_item3_title),
                            subtitle = stringResource(R.string.glass_list_item3_sub),
                            leadingIcon = Icons.Default.Info,
                            trailing = stringResource(R.string.glass_list_item3_trailing),
                            config = glassTheme,
                            onClick = {}
                        )
                    }
                }

                // Dialog / Popup 玻璃樣例
                GlassCard(config = glassTheme) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(R.string.glass_dialog_popup_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassButton(
                                text = stringResource(R.string.glass_dialog_open),
                                onClick = { showDialog = true },
                                config = glassTheme,
                                modifier = Modifier.weight(1f)
                            )
                            GlassPopupBlurBox(
                                expanded = showPopup,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp),
                                popupContent = {
                                    GlassPopup(
                                        onDismissRequest = { showPopup = false },
                                        alignment = Alignment.TopCenter,
                                        offset = IntOffset(0, 80),
                                        config = glassTheme
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = stringResource(R.string.glass_popup_title),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = stringResource(R.string.glass_popup_message),
                                                fontSize = 13.sp,
                                                color = glassTheme.contentColor.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                },
                                anchorContent = {
                                    GlassButton(
                                        text = stringResource(R.string.glass_popup_open),
                                        onClick = { showPopup = true },
                                        config = glassTheme,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            )
                        }
                    }
                }

                // :liquidglass SDK 完整元件目錄
                GlassSdkCatalog(config = glassTheme)

                // 操作提示
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.demo_selected_label, tabs[selectedIndex]),
                        color = glassTheme.contentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.demo_hint),
                        color = glassTheme.contentColor.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        }

}
}

/**
 * 背景裝飾光暈：一顆由中心向外淡出的圓形色塊，用來在深色底上混出橙紅紫藍的層次。
 *
 * @param color 光暈顏色
 * @param alpha 中心透明度（0.0~1.0），越高顏色越實
 * @param size 光暈直徑
 * @param alignment 在父 Box 中的對齊位置
 * @param offsetX 水平偏移，負值可讓光暈超出畫面邊界只露出一部分
 * @param offsetY 垂直偏移
 */
@Composable
private fun BoxScope.GlowBlob(
    color: Color,
    alpha: Float,
    size: Dp,
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .align(alignment)
            .offset(x = offsetX, y = offsetY)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent)
                ),
                shape = CircleShape
            )
    )
}
