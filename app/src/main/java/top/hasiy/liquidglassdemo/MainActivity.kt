package top.hasiyliquidglassdemo

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import top.hasiy.designsystem.GlassPresets
import top.hasiy.designsystem.isLightSurface
import top.hasiyliquidglassdemo.ui.DemoThemeUiState
import top.hasiyliquidglassdemo.ui.DynamicLightTabBarDemoScreen
import top.hasiyliquidglassdemo.ui.DynamicLightTabBarShowcaseScreen
import top.hasiyliquidglassdemo.ui.ThemePalettePreviewScreen
import top.hasiyliquidglassdemo.ui.theme.LiquidGlassDemoTheme
import top.hasiyliquidglassdemo.ui.theme.rememberThemePaletteState
import top.hasiyliquidglassdemo.ui.workout.DualGaugeClusterScreen
import top.hasiyliquidglassdemo.ui.workout.RouteMapScreen
import top.hasiyliquidglassdemo.ui.workout.ResponsiveStateMatrixScreen

/** Demo 的頁面。導航只在這個 Activity 內部切換，不值得為三頁引入 Navigation。 */
private enum class DemoScreen {
    /** Tab / Lens 同屏對照頁。 */
    TAB_SHOWCASE,

    /** 主題色驗收頁。 */
    PALETTE_PREVIEW,

    /** SDK 元件目錄。 */
    CATALOG,

    /** 雙表儀表舱。平板專屬顯示模式。 */
    DUAL_GAUGE,

    /** 路線與賽道地圖。平板專屬顯示模式。 */
    ROUTE_MAP,

    /** 響應式狀態矩陣主畫面。 */
    STATE_MATRIX,
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
            var screen by rememberSaveable { mutableStateOf(DemoScreen.CATALOG) }
            // 方向鎖放在這一層：轉屏會重建 Activity，鎖如果存在子頁的 composable 裡，
            // 重建時就跟著被清掉，畫面剛轉過去又彈回來。rememberSaveable 扛得住重建。
            var matrixOrientation by rememberSaveable {
                mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            }
            LaunchedEffect(screen, matrixOrientation) {
                // 只有狀態矩陣頁鎖方向，離開就放開
                requestedOrientation = if (screen == DemoScreen.STATE_MATRIX) {
                    matrixOrientation
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
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
            val isLandscape = LocalConfiguration.current.orientation ==
                Configuration.ORIENTATION_LANDSCAPE

            val lightSurface = when (screen) {
                DemoScreen.TAB_SHOWCASE -> true
                DemoScreen.PALETTE_PREVIEW -> palette.isCanvasLight
                // 狀態矩陣頁鋪的是屏內底色，所以看的是 isLight 而不是 isCanvasLight
                DemoScreen.STATE_MATRIX,
                DemoScreen.DUAL_GAUGE,
                DemoScreen.ROUTE_MAP -> palette.isLight
                DemoScreen.CATALOG -> uiState.glassTheme.isLightSurface
            }
            // 状态矩阵横屏是全屏：器材前的读数面板不该被状态栏和导航栏切掉一截。
            // 竖屏保留系统栏——那时页面顶部是标题栏，藏了反而不知道时间和电量。
            //
            // 两个平板专属的显示模式一律全屏：它们只有横屏一种形态，画的就是
            // 器材前的整块仪表舱，留一条状态栏会把两块表往下压掉一截。
            val immersive = (screen == DemoScreen.STATE_MATRIX && isLandscape) ||
                screen == DemoScreen.DUAL_GAUGE ||
                screen == DemoScreen.ROUTE_MAP
            val view = LocalView.current
            SideEffect {
                val window = (view.context as Activity).window
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightSurface
                    isAppearanceLightNavigationBars = lightSurface
                    if (immersive) {
                        hide(WindowInsetsCompat.Type.systemBars())
                        // 边缘上滑能临时唤出系统栏，几秒后自动收回；
                        // 用 TOUCH 的话第一次触屏就把栏叫出来了，调阻力时会一直闪
                        systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }

            // 子頁的系統返回鍵要回到元件目錄。不接的話會直接退出應用——
            // 這是單 Activity 內部切頁，回退棧裡本來就沒有上一頁。
            BackHandler(enabled = screen != DemoScreen.CATALOG) {
                screen = DemoScreen.CATALOG
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

                    DemoScreen.DUAL_GAUGE -> DualGaugeClusterScreen(
                        palette = palette,
                        onBack = { screen = DemoScreen.CATALOG },
                        onPaletteChange = { palette = it },
                    )

                    DemoScreen.ROUTE_MAP -> RouteMapScreen(
                        palette = palette,
                        onBack = { screen = DemoScreen.CATALOG },
                        onPaletteChange = { palette = it },
                    )

                    DemoScreen.STATE_MATRIX -> ResponsiveStateMatrixScreen(
                        palette = palette,
                        onBack = { screen = DemoScreen.CATALOG },
                        onToggleOrientation = {
                            matrixOrientation = if (isLandscape) {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        },
                        // 变化量与它的对比窗口都是业务资料，Demo 在这里喂示例值；
                        // 真实接入时换成 runtime 算出来的差值，不传就整行不显示
                        speedDelta = DEMO_SPEED_DELTA,
                        speedDeltaWindow = stringResource(R.string.gauge_delta_window_minute),
                        onPaletteChange = { palette = it },
                    )

                    DemoScreen.CATALOG -> DynamicLightTabBarDemoScreen(
                        uiState = uiState,
                        onPresetChange = { presetIndex = it },
                        onAccentEnabledChange = { accentEnabled = it },
                        onAccentColorChange = { accentColor = it },
                        onShadowEnabledChange = { shadowEnabled = it },
                        onOpenTabShowcase = { screen = DemoScreen.TAB_SHOWCASE },
                        onOpenPalettePreview = { screen = DemoScreen.PALETTE_PREVIEW },
                        onOpenStateMatrix = { screen = DemoScreen.STATE_MATRIX },
                        onOpenDualGauge = { screen = DemoScreen.DUAL_GAUGE },
                        onOpenRouteMap = { screen = DemoScreen.ROUTE_MAP },
                    )
                }
            }
        }
    }
}

/**
 * Demo 页喂给状态矩阵的速度变化量示例值（km/h）。
 *
 * 这一页只做 UI 复刻，没有接真实的运动数据；接上之后由 runtime 算出差值传进去。
 */
private const val DEMO_SPEED_DELTA = 1.6f
