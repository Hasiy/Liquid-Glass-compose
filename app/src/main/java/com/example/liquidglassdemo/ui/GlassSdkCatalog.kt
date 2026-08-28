@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.liquidglassdemo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liquidglass.GlassAssistChip
import com.example.liquidglass.GlassBadge
import com.example.liquidglass.GlassBottomSheet
import com.example.liquidglass.GlassButton
import com.example.liquidglass.GlassCard
import com.example.liquidglass.GlassCheckbox
import com.example.liquidglass.GlassConfig
import com.example.liquidglass.GlassContextMenuArea
import com.example.liquidglass.GlassDatePickerDialog
import com.example.liquidglass.GlassDropdownMenu
import com.example.liquidglass.GlassDropdownMenuItem
import com.example.liquidglass.GlassExtendedFloatingActionButton
import com.example.liquidglass.GlassFilterChip
import com.example.liquidglass.GlassFloatingActionButton
import com.example.liquidglass.GlassInputChip
import com.example.liquidglass.GlassModalBottomSheet
import com.example.liquidglass.GlassModalNavigationDrawer
import com.example.liquidglass.GlassNavigationBar
import com.example.liquidglass.GlassNavigationBarItem
import com.example.liquidglass.GlassNavigationRail
import com.example.liquidglass.GlassNavigationRailItem
import com.example.liquidglass.GlassPullToRefreshBox
import com.example.liquidglass.GlassRadioButton
import com.example.liquidglass.GlassSearchBar
import com.example.liquidglass.GlassSegmentedTabBar
import com.example.liquidglass.GlassSlider
import com.example.liquidglass.GlassSnackbar
import com.example.liquidglass.GlassSuggestionChip
import com.example.liquidglass.GlassTimePickerDialog
import com.example.liquidglass.glassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Demo catalog for every component added to the reusable :liquidglass SDK. */
@Composable
fun GlassSdkCatalog(
    config: GlassConfig,
    modifier: Modifier = Modifier,
) {
    var checked by remember { mutableStateOf(true) }
    var radioIndex by remember { mutableIntStateOf(0) }
    var sliderValue by remember { mutableFloatStateOf(0.45f) }
    var selectedChip by remember { mutableStateOf(false) }
    var selectedNavigation by remember { mutableIntStateOf(0) }
    var segmentedIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var dropdownValue by remember { mutableStateOf("尚未選擇") }
    var contextResult by remember { mutableStateOf("長按此區域") }
    var snackbarVisible by remember { mutableStateOf(false) }
    var showModalSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableLongStateOf(0L) }
    var selectedTime by remember { mutableStateOf("--:--") }
    var refreshing by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshing) {
        if (refreshing) {
            delay(900)
            refreshing = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        CatalogTitle("選擇類")
        GlassCard(config = config) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Checkbox")
                    GlassCheckbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        config = config
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    repeat(2) { index ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlassRadioButton(
                                selected = radioIndex == index,
                                onClick = { radioIndex = index },
                                config = config
                            )
                            Text("選項 ${index + 1}")
                        }
                    }
                }
                GlassSlider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    config = config,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        CatalogTitle("Chip、Badge、FAB、Snackbar")
        GlassCard(config = config) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassAssistChip(onClick = {}, label = "Assist", config = config)
                    GlassSuggestionChip(onClick = {}, label = "Suggestion", config = config)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassFilterChip(
                        selected = selectedChip,
                        onClick = { selectedChip = !selectedChip },
                        label = "Filter",
                        config = config
                    )
                    GlassInputChip(
                        label = "Input",
                        onClick = {},
                        trailingIcon = Icons.Default.Close,
                        config = config
                    )
                    GlassBadge(config = config) { Text("3") }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassFloatingActionButton(onClick = {}, config = config) {
                        Icon(Icons.Default.Add, contentDescription = "新增")
                    }
                    GlassExtendedFloatingActionButton(
                        onClick = {},
                        text = "新增項目",
                        icon = Icons.Default.Add,
                        config = config
                    )
                }
                GlassButton(
                    text = "顯示 Snackbar",
                    onClick = { snackbarVisible = true },
                    config = config
                )
                if (snackbarVisible) {
                    GlassSnackbar(
                        config = config,
                        action = {
                            GlassButton(
                                text = "關閉",
                                onClick = { snackbarVisible = false },
                                config = config
                            )
                        }
                    ) { Text("操作已完成") }
                }
            }
        }

        CatalogTitle("菜單與輸入")
        GlassCard(config = config) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {},
                    placeholder = "搜尋 SDK 元件",
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    config = config
                )
                Box {
                    GlassButton(
                        text = "Dropdown：$dropdownValue",
                        onClick = { dropdownExpanded = true },
                        config = config
                    )
                    GlassDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        config = config
                    ) {
                        listOf("第一項", "第二項", "第三項").forEach { item ->
                            GlassDropdownMenuItem(
                                text = item,
                                onClick = {
                                    dropdownValue = item
                                    dropdownExpanded = false
                                },
                                config = config
                            )
                        }
                    }
                }
                GlassContextMenuArea(
                    modifier = Modifier.fillMaxWidth(),
                    config = config,
                    onLongClickLabel = "開啟選單",
                    menuContent = { dismiss ->
                        GlassDropdownMenuItem(
                            text = "複製",
                            onClick = {
                                contextResult = "已選擇複製"
                                dismiss()
                            },
                            config = config
                        )
                        GlassDropdownMenuItem(
                            text = "分享",
                            onClick = {
                                contextResult = "已選擇分享"
                                dismiss()
                            },
                            config = config
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .glassSurface(RoundedCornerShape(16.dp), config),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(contextResult)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = "DatePicker",
                        onClick = { showDatePicker = true },
                        config = config,
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "TimePicker",
                        onClick = { showTimePicker = true },
                        config = config,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "日期：${selectedDate.takeIf { it > 0 } ?: "--"}  時間：$selectedTime",
                    fontSize = 12.sp
                )
            }
        }

        CatalogTitle("導航類")
        GlassSegmentedTabBar(
            items = listOf("首頁", "收藏", "設定"),
            selectedIndex = segmentedIndex,
            onSelect = { segmentedIndex = it },
            config = config
        )
        GlassNavigationBar(config = config) {
            val icons = listOf(Icons.Default.Home, Icons.Default.Favorite, Icons.Default.Settings)
            icons.forEachIndexed { index, icon ->
                GlassNavigationBarItem(
                    selected = selectedNavigation == index,
                    onClick = { selectedNavigation = index },
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text("N${index + 1}", fontSize = 10.sp) },
                    config = config,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        GlassCard(config = config) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassNavigationRail(config = config) {
                    listOf(Icons.Default.Home, Icons.Default.Settings).forEachIndexed { index, icon ->
                        GlassNavigationRailItem(
                            selected = selectedNavigation == index,
                            onClick = { selectedNavigation = index },
                            icon = { Icon(icon, contentDescription = null) },
                            config = config
                        )
                    }
                }
                GlassModalNavigationDrawer(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    config = config,
                    drawerState = drawerState,
                    drawerContent = {
                        Text("Drawer", fontWeight = FontWeight.SemiBold)
                        Text("首頁")
                        Text("設定")
                    }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassButton(
                            text = "打開 Drawer",
                            onClick = { scope.launch { drawerState.open() } },
                            config = config
                        )
                    }
                }
            }
        }

        CatalogTitle("BottomSheet / ModalBottomSheet")
        GlassCard(config = config) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassBottomSheet(config = config) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("BottomSheet 容器", fontWeight = FontWeight.SemiBold)
                        Text("GlassBottomSheetScaffold 可作为页面根布局", fontSize = 12.sp)
                    }
                }
                GlassButton(
                    text = "打開 ModalBottomSheet",
                    onClick = { showModalSheet = true },
                    config = config
                )
            }
        }

        CatalogTitle("PullToRefresh")
        GlassCard(config = config) {
            GlassPullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { refreshing = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                config = config
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items((1..8).toList()) { index ->
                        Text(
                            text = "下拉刷新項目 $index",
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }

    if (showModalSheet) {
        GlassModalBottomSheet(
            onDismissRequest = { showModalSheet = false },
            config = config
        ) {
            Text("ModalBottomSheet", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("點擊外部或向下滑動即可關閉")
        }
    }
    if (showDatePicker) {
        GlassDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onConfirm = {
                selectedDate = it ?: 0L
                showDatePicker = false
            },
            confirmText = "確定",
            dismissText = "取消",
            config = config
        )
    }
    if (showTimePicker) {
        GlassTimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            onConfirm = { hour, minute ->
                selectedTime = "%02d:%02d".format(hour, minute)
                showTimePicker = false
            },
            confirmText = "確定",
            dismissText = "取消",
            config = config
        )
    }
}

@Composable
private fun CatalogTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    )
}
