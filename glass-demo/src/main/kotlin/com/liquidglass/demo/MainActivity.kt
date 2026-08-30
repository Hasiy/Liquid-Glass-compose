package com.liquidglass.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.liquidglass.core.blur.BlurStrategy
import com.liquidglass.core.components.GlassTabBar
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LiquidGlassTheme
import com.liquidglass.demo.pages.AboutPage
import com.liquidglass.demo.pages.ControlsPage
import com.liquidglass.demo.pages.HERO_KEY
import com.liquidglass.demo.pages.HeroDetail
import com.liquidglass.demo.pages.MaterialsPage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LiquidGlassTheme { LiquidGlassApp() } }
    }
}

/**
 * 页面状态：0 home / 1 materials / 2 about / 3 detail（连续变焦，仅在 hero 点击时进入）。
 */
const val TAB_HOME = 0
const val TAB_MATERIALS = 1
const val TAB_ABOUT = 2
const val PAGE_DETAIL = 3

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LiquidGlassApp() {
    var selectedTab by remember { mutableIntStateOf(TAB_HOME) }
    var strategy by remember { mutableStateOf(BlurStrategy.RENDER_EFFECT) }
    val palette = com.liquidglass.core.theme.LocalGlassPalette.current
    val backdrop = remember { liquidBackdrop(palette) }

    Box(Modifier.fillMaxSize()) {
        BackdropLayer()

        // SPEC-05：SharedTransitionLayout 提供跨页面连续变焦
        SharedTransitionLayout {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState == PAGE_DETAIL) {
                        // 进入详情：淡入（共享边界自行变焦）
                        (fadeIn(tween(GlassTokens.animMediumMs)))
                            .togetherWith(fadeOut(tween(GlassTokens.animMediumMs)))
                    } else {
                        // tab 间切换：滑入
                        (fadeIn(tween(GlassTokens.animMediumMs)) +
                            slideInHorizontally(tween(GlassTokens.animMediumMs)) { it / 6 })
                            .togetherWith(
                                fadeOut(tween(GlassTokens.animMediumMs)) +
                                    slideOutHorizontally(tween(GlassTokens.animMediumMs)) { -it / 6 },
                            )
                    }
                },
                label = "tabContent",
            ) { page ->
                when (page) {
                    TAB_HOME -> ControlsPage(
                        heroModifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(HERO_KEY),
                            animatedVisibilityScope = this,
                        ),
                        onHeroClick = { selectedTab = PAGE_DETAIL },
                    )
                    TAB_MATERIALS -> MaterialsPage(
                        globalStrategy = strategy,
                        onToggleStrategy = {
                            strategy = if (strategy == BlurStrategy.RENDER_EFFECT) {
                                BlurStrategy.OFFSCREEN_CAPTURE
                            } else {
                                BlurStrategy.RENDER_EFFECT
                            }
                        },
                    )
                    TAB_ABOUT -> AboutPage()
                    else -> HeroDetail(
                        coverModifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(HERO_KEY),
                            animatedVisibilityScope = this,
                        ),
                        onClose = { selectedTab = TAB_HOME },
                    )
                }
            }
        }

        // 底部液态 Tab 栏（悬浮固定；detail 时高亮 home）
        GlassTabBar(
            tabs = listOf("tab0", "tab1", "tab2"),
            selectedIndex = if (selectedTab == PAGE_DETAIL) TAB_HOME else selectedTab,
            onSelect = { selectedTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = GlassTokens.pagePadding)
                .padding(bottom = GlassTokens.tabBarBottomMargin),
            backdrop = backdrop,
        )
    }
}