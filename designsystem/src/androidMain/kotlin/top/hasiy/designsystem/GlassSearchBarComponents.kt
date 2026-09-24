@file:OptIn(ExperimentalMaterial3Api::class)

package top.hasiy.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

// 本檔案是 Android 專屬源集：CMP material3 的 common 实现沒有 SearchBar / DockedSearchBar，
// 這兩個組件的 native 分支直接委託給 androidx material3，因此整體留在 androidMain。
// common 部分（Dropdown / Tooltip 等）見 GlassInputComponents.kt。
// 遷移出處見 designsystem-cmp-migration-plan.md §4-E。

private val SEARCH_BAR_SHAPE = RoundedCornerShape(28.dp)

/**
 * 玻璃風格搜尋列（SearchBar）。
 *
 * @param query 目前搜尋文字
 * @param onQueryChange 搜尋文字變更回呼
 * @param onSearch 按下搜尋（IME Search）時的回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param placeholder 佔位提示文字
 * @param leadingIcon 前導圖示
 * @param trailingIcon 尾端圖示
 * @param enabled 是否可互動
 */
@Composable
fun GlassSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    if (config.native) {
        var active by remember { mutableStateOf(false) }
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { input ->
                active = false
                onSearch(input)
            },
            active = active,
            onActiveChange = { active = it },
            modifier = modifier,
            enabled = enabled,
            placeholder = placeholder.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            content = {}
        )
        return
    }

    val contentColor = config.contentColor
    val keyboardController = LocalSoftwareKeyboardController.current

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .glassSurface(shape = SEARCH_BAR_SHAPE, config = config.asControlSurface())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leadingIcon?.invoke()
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(
                    color = contentColor,
                    fontSize = 16.sp
                ),
                singleLine = true,
                enabled = enabled,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        onSearch(query)
                    }
                ),
                cursorBrush = SolidColor(contentColor),
                decorationBox = { innerTextField ->
                    if (query.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
            trailingIcon?.invoke()
        }
    }
}

/**
 * 玻璃風格嵌入式搜尋列（DockedSearchBar），展開時顯示 [content]。
 *
 * @param query 目前搜尋文字
 * @param onQueryChange 搜尋文字變更回呼
 * @param onSearch 按下搜尋（IME Search）時的回呼
 * @param modifier 外部修飾符
 * @param config 玻璃主題參數
 * @param placeholder 佔位提示文字
 * @param leadingIcon 前導圖示
 * @param trailingIcon 尾端圖示
 * @param content 展開後的建議清單內容
 */
@Composable
fun GlassDockedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (config.native) {
        var active by remember { mutableStateOf(false) }
        DockedSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { input ->
                active = false
                onSearch(input)
            },
            active = active,
            onActiveChange = { active = it },
            modifier = modifier,
            placeholder = placeholder.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            content = content
        )
        return
    }

    val contentColor = config.contentColor
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .glassSurface(shape = SEARCH_BAR_SHAPE, config = config.asControlSurface())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            leadingIcon?.invoke()
            BasicTextField(
                value = query,
                onValueChange = {
                    expanded = true
                    onQueryChange(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { expanded = it.isFocused },
                textStyle = LocalTextStyle.current.copy(
                    color = contentColor,
                    fontSize = 16.sp
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onSearch(query)
                        expanded = false
                    }
                ),
                cursorBrush = SolidColor(contentColor),
                decorationBox = { innerTextField ->
                    if (query.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
            trailingIcon?.invoke()
        }

        if (expanded) {
            Popup(
                onDismissRequest = { expanded = false },
                alignment = Alignment.TopStart,
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 52.dp)
                        .glassOverlayBackdrop(shape = RoundedCornerShape(16.dp), config = config.asOverlaySurface())
                        .glassSurface(shape = RoundedCornerShape(16.dp), config = config.asOverlaySurface()),
                    color = Color.Transparent,
                    contentColor = contentColor,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        content = content
                    )
                }
            }
        }
    }
}
