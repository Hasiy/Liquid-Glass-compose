@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package top.hasiy.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * 玻璃日期選擇對話框。狀態由 [state] 持有，確認時回傳 UTC epoch millis。
 * 放在 [GlassBackdropHost] 內時，只會在 Picker 卡片自身輪廓內模糊背景。
 */
@Composable
fun GlassDatePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    state: DatePickerState = rememberDatePickerState(),
    confirmText: String = "OK",
    dismissText: String = "Cancel",
) {
    Popup(
        onDismissRequest = onDismissRequest,
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true)
    ) {
        val pickerShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        GlassCard(
            modifier = modifier
                .padding(20.dp)
                .widthIn(max = 520.dp)
                .glassOverlayBackdrop(shape = pickerShape, config = config),
            config = config,
            shape = pickerShape,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DatePicker(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (config.native) {
                        DatePickerDefaults.colors()
                    } else {
                        DatePickerDefaults.colors(containerColor = Color.Transparent)
                    }
                )
                DialogActions(
                    confirmText = confirmText,
                    dismissText = dismissText,
                    contentColor = if (config.native) null else config.contentColor,
                    onDismiss = onDismissRequest,
                    onConfirm = { onConfirm(state.selectedDateMillis) }
                )
            }
        }
    }
}

/**
 * 玻璃時間選擇對話框。確認時回傳 24 小時制的 hour/minute。
 * 放在 [GlassBackdropHost] 內時，只會在 Picker 卡片自身輪廓內模糊背景。
 */
@Composable
fun GlassTimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    state: TimePickerState = rememberTimePickerState(),
    confirmText: String = "OK",
    dismissText: String = "Cancel",
) {
    Popup(
        onDismissRequest = onDismissRequest,
        alignment = Alignment.Center,
        properties = PopupProperties(focusable = true)
    ) {
        val pickerShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        GlassCard(
            modifier = modifier
                .padding(20.dp)
                .widthIn(max = 420.dp)
                .glassOverlayBackdrop(shape = pickerShape, config = config),
            config = config,
            shape = pickerShape,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                TimePicker(state = state)
                DialogActions(
                    confirmText = confirmText,
                    dismissText = dismissText,
                    contentColor = if (config.native) null else config.contentColor,
                    onDismiss = onDismissRequest,
                    onConfirm = { onConfirm(state.hour, state.minute) }
                )
            }
        }
    }
}

@Composable
private fun DialogActions(
    confirmText: String,
    dismissText: String,
    contentColor: Color?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) {
            Text(
                text = dismissText,
                color = contentColor?.copy(alpha = 0.78f) ?: Color.Unspecified
            )
        }
        TextButton(onClick = onConfirm) {
            Text(
                text = confirmText,
                color = contentColor ?: Color.Unspecified
            )
        }
    }
}
