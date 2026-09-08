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
import top.hasiy.designsystem.GlassPresets
import top.hasiy.designsystem.isLightSurface
import top.hasiyliquidglassdemo.ui.DemoThemeUiState
import top.hasiyliquidglassdemo.ui.DynamicLightTabBarDemoScreen
import top.hasiyliquidglassdemo.ui.DynamicLightTabBarShowcaseScreen
import top.hasiyliquidglassdemo.ui.ThemePalettePreviewScreen
import top.hasiyliquidglassdemo.ui.theme.LiquidGlassDemoTheme
import top.hasiyliquidglassdemo.ui.theme.rememberThemePaletteState

/** Demo 的頁面。導航只在這個 Activity 內部切換，不值得為三頁引入 Navigation。 */
private enum class DemoScreen {
    /** Tab / Lens 同屏對照頁。 */
    TAB_SHOWCASE,

    /** 主題色驗收頁。 */
    PALETTE_PREVIEW,

    /** SDK 元件目錄。 */
    CATALOG,
}

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
            var screen by rememberSaveable { mutableStateOf(DemoScreen.TAB_SHOWCASE) }
            // 主題色與四組視覺預設是兩件事：這個記的是配色（8 組），presetIndex 記的是材質結構
            var palette by rememberThemePaletteState()

            val uiState = DemoThemeUiState(
                presets = presets,
                presetIndex = presetIndex,
                accentEnabled = accentEnabled,
                accentColor = accentColor,
                accentCandidates = accentCandidates,
                shadowEnabled = shadowEnabled,
            )

            // 系統列圖示的明暗要跟著當前頁的底色走：深色底配深色圖示會看不清。
            // 每頁的底色來源不同——Tab 對照頁固定淺底，主題色頁露出的是**畫布**
            // （不是屏內，所以用 isCanvasLight；Tactile 正是畫布淺、屏內深），
            // 元件目錄跟著四組視覺預設。
            val lightSurface = when (screen) {
                DemoScreen.TAB_SHOWCASE -> true
                DemoScreen.PALETTE_PREVIEW -> palette.isCanvasLight
                DemoScreen.CATALOG -> uiState.glassTheme.isLightSurface
            }
            val view = LocalView.current
            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightSurface
                    isAppearanceLightNavigationBars = lightSurface
                }
            }

            LiquidGlassDemoTheme(lightSurface = lightSurface) {
                when (screen) {
                    DemoScreen.TAB_SHOWCASE -> DynamicLightTabBarShowcaseScreen(
                        onBack = { screen = DemoScreen.CATALOG },
                    )

                    DemoScreen.PALETTE_PREVIEW -> ThemePalettePreviewScreen(
                        palette = palette,
                        onPaletteChange = { palette = it },
                        onBack = { screen = DemoScreen.CATALOG },
                    )

                    DemoScreen.CATALOG -> DynamicLightTabBarDemoScreen(
                        uiState = uiState,
                        onPresetChange = { presetIndex = it },
                        onAccentEnabledChange = { accentEnabled = it },
                        onAccentColorChange = { accentColor = it },
                        onShadowEnabledChange = { shadowEnabled = it },
                        onOpenTabShowcase = { screen = DemoScreen.TAB_SHOWCASE },
                        onOpenPalettePreview = { screen = DemoScreen.PALETTE_PREVIEW },
                    )
                }
            }
        }
    }
}
