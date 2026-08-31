package com.example.liquidglass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

private val BOTTOM_SHEET_SHAPE = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val NAVIGATION_BAR_SHAPE = RoundedCornerShape(28.dp)
private val NAVIGATION_RAIL_SHAPE = RoundedCornerShape(28.dp)
private val NAVIGATION_ITEM_SHAPE = RoundedCornerShape(22.dp)
private val NAVIGATION_DRAWER_SHAPE = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)

/**
 * 導航項的選中高亮設定。
 *
 * 導航項是疊在已經有玻璃的容器之上，若再套一層完整玻璃基底（含陰影），未選中的項
 * 也會留下一塊看得出邊界的方形色差，所以未選中的項不畫底、只裁形狀。
 * 選中態的畫法見 [GlassConfig.asSelectedSurface]。
 */
private fun GlassConfig.asNavigationItemHighlight(): GlassConfig = asSelectedSurface()

/** 玻璃導航列高度 */
private val NAVIGATION_BAR_HEIGHT = 80.dp

/** 玻璃導航軌寬度 */
private val NAVIGATION_RAIL_WIDTH = 80.dp

/**
 * 玻璃底部面板（非模態）：僅提供玻璃質感的底部 Sheet 容器，可用於自建底部面板或
 * 作為 [GlassModalBottomSheet] 的玻璃外殼。
 *
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param onDismissRequest 返回/關閉請求回呼
 * @param sheetState Sheet 狀態（與 Material3 保持一致）
 * @param content 面板內容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheet(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    onDismissRequest: () -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable () -> Unit,
) {
    // 原生模式：不提供非模態容器，fallback 為透明 Surface 包裝，保持 API 一致
    if (config.native) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = BOTTOM_SHEET_SHAPE,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Box(modifier = Modifier.padding(20.dp)) { content() }
        }
        return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = BOTTOM_SHEET_SHAPE,
        color = Color.Transparent,
        contentColor = config.contentColor,
    ) {
        Box(
            modifier = Modifier
                .glassOverlayBackdrop(shape = BOTTOM_SHEET_SHAPE, config = config)
                .glassSurface(shape = BOTTOM_SHEET_SHAPE, config = config)
                .padding(20.dp)
        ) {
            content()
        }
    }
}

/**
 * 可持續停留於頁面底部的 Material3 BottomSheetScaffold 玻璃版本。
 * 與 [GlassModalBottomSheet] 不同，它不會阻斷背景內容操作。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassBottomSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    sheetPeekHeight: Dp = 64.dp,
    content: @Composable (PaddingValues) -> Unit,
) {
    BottomSheetScaffold(
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (config.native) Modifier
                        else Modifier
                            .glassOverlayBackdrop(
                                shape = BOTTOM_SHEET_SHAPE,
                                config = config
                            )
                            .glassSurface(
                                shape = BOTTOM_SHEET_SHAPE,
                                config = config
                            )
                    )
                    .padding(20.dp),
                content = sheetContent
            )
        },
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = sheetPeekHeight,
        sheetShape = BOTTOM_SHEET_SHAPE,
        sheetContainerColor = if (config.native) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            Color.Transparent
        },
        sheetContentColor = if (config.native) {
            MaterialTheme.colorScheme.onSurface
        } else {
            config.contentColor
        },
        sheetShadowElevation = if (config.native) BottomSheetDefaults.Elevation else 0.dp,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(
                color = if (config.native) MaterialTheme.colorScheme.onSurfaceVariant
                else config.contentColor.copy(alpha = 0.55f)
            )
        },
        content = content
    )
}

/**
 * 玻璃模態底部面板：包裝 Material3 [ModalBottomSheet]，玻璃模式下以透明容器搭配
 * [GlassBottomSheet] 外觀呈現。
 *
 * @param onDismissRequest 點擊外部或返回鍵時觸發
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param sheetState Sheet 狀態
 * @param content 面板內容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    // 原生模式：直接使用 Material3 ModalBottomSheet
    if (config.native) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
        return
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = config.contentColor,
        scrimColor = Color.Transparent,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassOverlayBackdrop(shape = BOTTOM_SHEET_SHAPE, config = config)
                .glassSurface(shape = BOTTOM_SHEET_SHAPE, config = config)
                .padding(20.dp)
        ) {
            content()
        }
    }
}

/**
 * 玻璃底部導航列：置於螢幕底部的水平導航容器。
 *
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param content 導航項目內容（作用域為 [RowScope]）
 */
@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    content: @Composable RowScope.() -> Unit,
) {
    if (config.native) {
        NavigationBar(
            modifier = modifier,
            content = content
        )
        return
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(NAVIGATION_BAR_HEIGHT)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = NAVIGATION_BAR_SHAPE,
        color = Color.Transparent,
        contentColor = config.contentColor,
    ) {
        Row(
            modifier = Modifier
                .glassSurface(shape = NAVIGATION_BAR_SHAPE, config = config)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            content = content
        )
    }
}

/**
 * 玻璃底部導航列項目。
 *
 * @param selected 是否選中
 * @param onClick 點擊回呼
 * @param icon 圖示內容
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param label 標籤內容（可為 null）
 * @param alwaysShowLabel 是否始終顯示標籤；false 時僅在選中時顯示
 */
@Composable
fun RowScope.GlassNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
) {
    if (config.native) {
        NavigationBarItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            modifier = modifier,
            label = label,
            alwaysShowLabel = alwaysShowLabel
        )
        return
    }
    val showLabel = alwaysShowLabel || selected
    val selectedAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.6f,
        label = "glassNavItemAlpha"
    )
    // 未選中：只裁形狀、不畫底，避免在容器玻璃上疊出一塊方形色差。
    // 選中：畫一層很淡的高亮亮片。
    val surface = if (selected) {
        Modifier.glassSurface(
            shape = NAVIGATION_ITEM_SHAPE,
            config = config.asNavigationItemHighlight()
        )
    } else {
        Modifier.clip(NAVIGATION_ITEM_SHAPE)
    }
    Column(
        // selectable 必須在裁切之後：漣漪畫在 clip 之前會以直角矩形溢出圓角。
        modifier = modifier
            .then(surface)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(
            // 啟用強調色時，選中項的圖示/文字與淡底同色；否則沿用內容色
            LocalContentColor provides if (selected && config.accentEnabled) {
                config.accentColor
            } else {
                config.contentColor.copy(alpha = selectedAlpha)
            }
        ) {
            icon()
            if (showLabel && label != null) {
                Box(modifier = Modifier.alpha(selectedAlpha)) { label() }
            }
        }
    }
}

/**
 * 玻璃側邊導航軌：適用於平板/桌面佈局的垂直導航容器。
 *
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param content 導航項目內容（作用域為 [ColumnScope]）
 */
@Composable
fun GlassNavigationRail(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (config.native) {
        NavigationRail(
            modifier = modifier,
            content = content
        )
        return
    }
    Surface(
        modifier = modifier
            .width(NAVIGATION_RAIL_WIDTH)
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        shape = NAVIGATION_RAIL_SHAPE,
        color = Color.Transparent,
        contentColor = config.contentColor,
    ) {
        Column(
            modifier = Modifier
                .glassSurface(shape = NAVIGATION_RAIL_SHAPE, config = config)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            content = content
        )
    }
}

/**
 * 玻璃側邊導航軌項目。
 *
 * @param selected 是否選中
 * @param onClick 點擊回呼
 * @param icon 圖示內容
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param label 標籤內容（可為 null）
 * @param alwaysShowLabel 是否始終顯示標籤；false 時僅在選中時顯示
 */
@Composable
fun GlassNavigationRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    label: @Composable (() -> Unit)? = null,
    alwaysShowLabel: Boolean = true,
) {
    if (config.native) {
        androidx.compose.material3.NavigationRailItem(
            selected = selected,
            onClick = onClick,
            icon = icon,
            modifier = modifier,
            label = label,
            alwaysShowLabel = alwaysShowLabel
        )
        return
    }
    val showLabel = alwaysShowLabel || selected
    val selectedAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.6f,
        label = "glassRailItemAlpha"
    )
    // 同 GlassNavigationBarItem：未選中不畫底，只有選中項顯示淡高亮。
    val surface = if (selected) {
        Modifier.glassSurface(
            shape = NAVIGATION_ITEM_SHAPE,
            config = config.asNavigationItemHighlight()
        )
    } else {
        Modifier.clip(NAVIGATION_ITEM_SHAPE)
    }
    Column(
        modifier = modifier
            .then(surface)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(
            // 啟用強調色時，選中項的圖示/文字與淡底同色；否則沿用內容色
            LocalContentColor provides if (selected && config.accentEnabled) {
                config.accentColor
            } else {
                config.contentColor.copy(alpha = selectedAlpha)
            }
        ) {
            icon()
            if (showLabel && label != null) {
                Box(modifier = Modifier.alpha(selectedAlpha)) { label() }
            }
        }
    }
}

/**
 * 玻璃模態導航抽屜：包裝 Material3 [ModalNavigationDrawer]，玻璃模式下將
 * [drawerContent] 包覆於玻璃質感面板中。
 *
 * @param drawerContent 抽屜內容（作用域為 [ColumnScope]）
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param drawerState 抽屜狀態
 * @param content 主內容
 */
@Composable
fun GlassModalNavigationDrawer(
    drawerContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    drawerState: DrawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed),
    content: @Composable () -> Unit,
) {
    if (config.native) {
        ModalNavigationDrawer(
            drawerContent = { Column { drawerContent() } },
            modifier = modifier,
            drawerState = drawerState,
            content = content
        )
        return
    }
    ModalNavigationDrawer(
        drawerContent = {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                shape = NAVIGATION_DRAWER_SHAPE,
                color = Color.Transparent,
                contentColor = config.contentColor,
            ) {
                Column(
                    modifier = Modifier
                        .glassOverlayBackdrop(shape = NAVIGATION_DRAWER_SHAPE, config = config)
                        .glassSurface(shape = NAVIGATION_DRAWER_SHAPE, config = config)
                        .padding(16.dp),
                    content = drawerContent
                )
            }
        },
        modifier = modifier,
        drawerState = drawerState,
        scrimColor = Color.Transparent,
        content = content
    )
}
