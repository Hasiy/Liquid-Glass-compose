package top.hasiyliquidglassdemo.ui

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.hasiy.designsystem.GlassBackdropHost
import top.hasiy.designsystem.GlassConfig
import top.hasiy.designsystem.GlassPresets
import top.hasiy.designsystem.GlassSwitch
import top.hasiyliquidglassdemo.R
import top.hasiy.designsystem.legacy.DynamicLightTabBarLegacy
import top.hasiy.designsystem.GlassLensTabBar
import top.hasiy.designsystem.GlassLensTabItem

private val TAB_PAGE_BACKGROUND = Color.White
private val TAB_PAGE_CONTENT = Color(0xFF202124)
private val TAB_PAGE_SUBTLE = Color(0xFF6B7280)
private val TAB_PAGE_VERTICAL_PADDING = 8.dp
private val TAB_COMPARISON_RESERVED_HEIGHT = 370.dp

/**
 * DynamicLightTabBar 的獨立展示頁。
 *
 * 頁面固定採白色主背景與 Neutral 玻璃配置，讓 Tab/Lens 的輪廓和深色內容在白底上
 * 清楚可見；底部淡色裝飾透過透明鏡片顯示，放大來源僅包含固定的 Tab 內容。
 */
@Composable
fun DynamicLightTabBarShowcaseScreen(
    onBack: () -> Unit,
) {
    val showCenterAction = LocalConfiguration.current.orientation != Configuration.ORIENTATION_PORTRAIT
    val glassTheme = GlassPresets.neutral().copy(
        pageBackgroundTop = TAB_PAGE_BACKGROUND,
        pageBackgroundBottom = TAB_PAGE_BACKGROUND,
        accentEnabled = false,
        shadowEnabled = true,
    )
    // 參考 Android 大螢幕的中性灰層次：淺灰容器上浮一塊白色選中 Pill，
    // 兩者不共用基色，避免選中態和軌道糊成同一片白。
    val androidLegacyTheme = GlassPresets.neutral().copy(
        baseColor = Color.White,
        bodyTopAlpha = 0.94f,
        bodyBottomAlpha = 0.86f,
        highlightInnerAlpha = 0.015f,
        highlightOuterAlpha = 0f,
        borderColor = Color.White,
        borderTopAlpha = 0.68f,
        borderBottomAlpha = 0.12f,
        contentColor = Color(0xFF17171B),
        accentEnabled = false,
        shadowElevation = 2.dp,
        softShadowSpread = 8.dp,
        softShadowOffsetY = 3.dp,
        softShadowAlpha = 0.05f,
    )
    var legacySelectedIndex by remember { mutableIntStateOf(0) }
    var androidLegacySelectedIndex by remember { mutableIntStateOf(0) }
    var lensSelectedIndex by remember { mutableIntStateOf(0) }
    var lensCenterSelected by remember { mutableStateOf(false) }
    var refractionEnabled by remember {
        mutableStateOf(true)
    }
    val tabs = listOf(
        GlassLensTabItem(
            key = "tab0",
            label = stringResource(R.string.tab_0),
            icon = Icons.Default.Home,
        ),
        GlassLensTabItem(
            key = "tab1",
            label = stringResource(R.string.tab_1),
            icon = Icons.Default.Search,
        ),
        GlassLensTabItem(
            key = "tab2",
            label = stringResource(R.string.tab_2),
            icon = Icons.Default.Favorite,
        ),
        GlassLensTabItem(
            key = "tab3",
            label = stringResource(R.string.tab_3),
            icon = Icons.Default.Settings,
        ),
    )
    val bottomInset = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val tabBarReservedHeight = TAB_COMPARISON_RESERVED_HEIGHT + bottomInset

    GlassBackdropHost(
        modifier = Modifier.fillMaxSize(),
        overlay = {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = TAB_PAGE_VERTICAL_PADDING),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.tab_component_legacy_name),
                    color = TAB_PAGE_CONTENT,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LegacyVariant(
                        title = stringResource(R.string.tab_legacy_original_variant),
                        selectedIndex = legacySelectedIndex,
                        onSelect = { legacySelectedIndex = it },
                        items = tabs.map { it.label },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LegacyVariant(
                        title = stringResource(R.string.tab_legacy_android_variant),
                        selectedIndex = androidLegacySelectedIndex,
                        onSelect = { androidLegacySelectedIndex = it },
                        items = tabs.map { it.label },
                        pillGlassConfig = androidLegacyTheme,
                        showBarBackground = true,
                        showBarBorder = false,
                        barBackgroundColor = Color(0xFFADB0B5),
                        barBackgroundAlpha = 0.36f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.tab_component_lens_name),
                        color = TAB_PAGE_CONTENT,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.tab_lens_refraction),
                            color = TAB_PAGE_CONTENT,
                            fontSize = 13.sp,
                        )
                        GlassSwitch(
                            checked = refractionEnabled,
                            onCheckedChange = { refractionEnabled = it },
                            config = glassTheme,
                        )
                    }
                }
                GlassLensTabBar(
                    items = tabs,
                    selectedIndex = lensSelectedIndex,
                    onSelect = {
                        lensSelectedIndex = it
                        lensCenterSelected = false
                    },
                    centerAction = if (showCenterAction) {
                        {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    } else null,
                    centerActionSelected = lensCenterSelected,
                    onCenterActionClick = { lensCenterSelected = true },
                    centerActionDescription = stringResource(R.string.center_action_desc),
                    contentColor = glassTheme.contentColor,
                    lensEnabled = refractionEnabled,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(DynamicLightTabBarConfig.BAR_WIDTH_FRACTION),
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TAB_PAGE_BACKGROUND),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                        )
                    )
                    .padding(
                        start = 24.dp,
                        top = 16.dp,
                        end = 24.dp,
                        bottom = 24.dp + tabBarReservedHeight,
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.tab_showcase_back_desc),
                            tint = TAB_PAGE_CONTENT,
                        )
                    }
                    Text(
                        text = stringResource(R.string.tab_showcase_title),
                        color = TAB_PAGE_CONTENT,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(R.string.tab_showcase_subtitle),
                    color = TAB_PAGE_SUBTLE,
                    fontSize = 14.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ShowcaseBlock(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFFFF2E9),
                        title = stringResource(R.string.tab_showcase_block_one),
                    )
                    ShowcaseBlock(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFEAF6FF),
                        title = stringResource(R.string.tab_showcase_block_two),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowcaseBlock(
    modifier: Modifier,
    color: Color,
    title: String,
) {
    Box(
        modifier = modifier
            .height(112.dp)
            .background(color, RoundedCornerShape(24.dp))
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        Text(
            text = title,
            color = TAB_PAGE_CONTENT,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 同屏比較 Legacy 的原始深色與 Android 淺灰配色，兩者狀態互不影響。 */
@Composable
private fun LegacyVariant(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    pillGlassConfig: GlassConfig? = null,
    showBarBackground: Boolean = true,
    showBarBorder: Boolean = showBarBackground,
    barBackgroundColor: Color? = null,
    barBackgroundAlpha: Float = 0.22f,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            color = TAB_PAGE_SUBTLE,
            fontSize = 12.sp,
        )
        DynamicLightTabBarLegacy(
            items = items,
            selectedIndex = selectedIndex,
            onSelect = onSelect,
            pillGlassConfig = pillGlassConfig,
            showBarBackground = showBarBackground,
            showBarBorder = showBarBorder,
            barBackgroundColor = barBackgroundColor,
            barBackgroundAlpha = barBackgroundAlpha,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
