package com.example.liquidglassdemo.ui

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.liquidglass.GlassConfig

/**
 * 示範畫面的主題設定狀態。
 *
 * [glassTheme] 才是實際套用到元件的設定；[preset] 只用來標示設定頁上選中的是哪一顆，
 * 因為強調色的開關與顏色是疊在 preset 之上的執行期覆寫。
 *
 * @param presets 可選主題，順序與設定頁的按鈕一致
 * @param presetIndex 目前選中的主題索引
 * @param accentEnabled 是否啟用強調色
 * @param accentColor 目前的強調色
 * @param accentCandidates 選色盤的候選顏色
 * @param shadowEnabled 是否繪製玻璃表面的接觸陰影
 */
@Immutable
data class DemoThemeUiState(
    val presets: List<GlassConfig>,
    val presetIndex: Int,
    val accentEnabled: Boolean,
    val accentColor: Color,
    val accentCandidates: List<Color>,
    val shadowEnabled: Boolean,
) {
    /** 目前選中的主題本身（未套上強調色覆寫） */
    val preset: GlassConfig get() = presets[presetIndex]

    /** 實際套用的主題：preset 疊上強調色的執行期設定 */
    val glassTheme: GlassConfig
        get() = preset.copy(
            accentEnabled = accentEnabled,
            accentColor = accentColor,
            shadowEnabled = shadowEnabled,
        )
}
