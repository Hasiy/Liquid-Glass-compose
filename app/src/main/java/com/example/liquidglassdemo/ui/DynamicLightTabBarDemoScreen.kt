package com.example.liquidglassdemo.ui

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liquidglassdemo.R
import com.example.liquidglass.GlassButton
import com.example.liquidglass.GlassCard
import com.example.liquidglass.GlassDialog
import com.example.liquidglass.GlassDialogBlurHost
import com.example.liquidglass.GlassIconButton
import com.example.liquidglass.GlassListItem
import com.example.liquidglass.GlassPopup
import com.example.liquidglass.GlassPopupBlurBox
import com.example.liquidglass.GlassPresets
import com.example.liquidglass.GlassProgressBar
import com.example.liquidglass.GlassVolumeSlider
import com.example.liquidglass.GlassSwitch
import com.example.liquidglass.GlassTextField
import com.example.liquidglass.GlassThemeSelector
import com.example.liquidglass.GlassTopBar
import com.example.liquidglass.glassSurface
import kotlin.math.roundToInt

/** 頁面底色：暖紫褐 → 深紫 → 深藍黑，讓橙紅與紫藍兩側的光暈都有可融合的底 */
private val BACKGROUND_TOP = Color(0xFF2A1630)
private val BACKGROUND_MIDDLE = Color(0xFF19142B)
private val BACKGROUND_BOTTOM = Color(0xFF0B0D1A)

/** 四角裝飾光暈：橙、紅、紫、藍，透過半透明玻璃元件隱約可見，強化玻璃質感 */
private val GLOW_ORANGE = Color(0xFFFF7A18)
private val GLOW_RED = Color(0xFFE0245E)
private val GLOW_PURPLE = Color(0xFF7B2FF7)
private val GLOW_BLUE = Color(0xFF00C9FF)

/**
 * DynamicLightTabBar 的示範畫面：
 * 橙紅紫藍混合的漸層背景 + 底部半透明毛玻璃 Tab Bar，並展示可套用於任意 UI 的玻璃組件與主題切換。
 */
@Composable
fun DynamicLightTabBarDemoScreen() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var glassTheme by remember { mutableStateOf(GlassPresets.Drop) }
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

    GlassDialogBlurHost(
        visible = showDialog,
        modifier = Modifier.fillMaxSize(),
        dialog = {
            GlassDialog(
                onDismissRequest = { showDialog = false },
                title = stringResource(R.string.glass_dialog_title),
                message = stringResource(R.string.glass_dialog_message),
                onConfirm = { showDialog = false },
                onDismiss = { showDialog = false },
                config = glassTheme
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BACKGROUND_TOP,
                            BACKGROUND_MIDDLE,
                            BACKGROUND_BOTTOM
                        )
                    )
                )
        ) {
        // 內容區：weight(1f) 佔滿剩餘空間，與底部 Tab Bar 完全分離，不會互相遮擋
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 裝飾色塊：四角各放一顆光暈（橙、紅、紫、藍），讓背景透過半透明的
            // 玻璃元件隱約可見，並在畫面中段自然混色，強化玻璃質感
            GlowBlob(
                color = GLOW_ORANGE,
                alpha = 0.50f,
                size = 260.dp,
                alignment = Alignment.TopStart,
                offsetX = (-70).dp,
                offsetY = (-50).dp
            )
            GlowBlob(
                color = GLOW_RED,
                alpha = 0.42f,
                size = 230.dp,
                alignment = Alignment.TopEnd,
                offsetX = 70.dp,
                offsetY = 20.dp
            )
            GlowBlob(
                color = GLOW_PURPLE,
                alpha = 0.55f,
                size = 280.dp,
                alignment = Alignment.BottomStart,
                offsetX = (-80).dp,
                offsetY = 60.dp
            )
            GlowBlob(
                color = GLOW_BLUE,
                alpha = 0.42f,
                size = 240.dp,
                alignment = Alignment.BottomEnd,
                offsetX = 70.dp,
                offsetY = 40.dp
            )

            // 內容：玻璃主題設置 + 組件展示 + 操作提示
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 玻璃主題設置
                Text(
                    text = stringResource(R.string.glass_theme_title),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                GlassThemeSelector(
                    selected = glassTheme,
                    onSelect = { glassTheme = it }
                )

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
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.demo_hint),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 底部 Tab Bar，固定於畫面最下層；Pill 玻璃效果隨主題切換
        DynamicLightTabBar(
            items = tabs,
            selectedIndex = selectedIndex,
            onSelect = { selectedIndex = it },
            pillGlassConfig = glassTheme,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
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
