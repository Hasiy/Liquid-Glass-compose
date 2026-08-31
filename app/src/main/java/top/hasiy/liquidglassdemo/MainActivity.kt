package top.hasiyliquidglassdemo

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import top.hasiyliquidglass.GlassPresets
import top.hasiyliquidglass.isLightSurface
import top.hasiyliquidglassdemo.ui.DemoThemeUiState
import top.hasiyliquidglassdemo.ui.DynamicLightTabBarDemoScreen
import top.hasiyliquidglassdemo.ui.theme.LiquidGlassDemoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 主題狀態放這裡，MaterialTheme 才能跟著玻璃主題切深淺
            val presets = GlassPresets.themed()
            val accentCandidates = listOf(
                colorResource(R.color.accent_green),
                colorResource(R.color.accent_blue),
                colorResource(R.color.accent_purple),
                colorResource(R.color.accent_orange),
                colorResource(R.color.accent_red),
                colorResource(R.color.accent_teal),
            )
            var presetIndex by rememberSaveable { mutableIntStateOf(0) }
            // 切主題時重置為該主題對強調色的預設（淺色的中性預設開啟，其餘關閉）
            var accentEnabled by remember(presetIndex) {
                mutableStateOf(presets[presetIndex].accentEnabled)
            }
            var accentColor by remember(presetIndex) {
                mutableStateOf(presets[presetIndex].accentColor)
            }
            var shadowEnabled by remember(presetIndex) {
                mutableStateOf(presets[presetIndex].shadowEnabled)
            }

            val uiState = DemoThemeUiState(
                presets = presets,
                presetIndex = presetIndex,
                accentEnabled = accentEnabled,
                accentColor = accentColor,
                accentCandidates = accentCandidates,
                shadowEnabled = shadowEnabled,
            )

            // 系統列圖示的明暗要跟著主題走：深色主題配深色圖示會看不清
            val lightSurface = uiState.glassTheme.isLightSurface
            val view = LocalView.current
            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightSurface
                    isAppearanceLightNavigationBars = lightSurface
                }
            }

            LiquidGlassDemoTheme(lightSurface = lightSurface) {
                DynamicLightTabBarDemoScreen(
                    uiState = uiState,
                    onPresetChange = { presetIndex = it },
                    onAccentEnabledChange = { accentEnabled = it },
                    onAccentColorChange = { accentColor = it },
                    onShadowEnabledChange = { shadowEnabled = it },
                )
            }
        }
    }
}
