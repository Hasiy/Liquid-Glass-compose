package com.liquidglass.demo.pages

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.core.components.GlassButton
import com.liquidglass.core.components.GlassCard
import com.liquidglass.core.components.GlassIconButton
import com.liquidglass.core.components.GlassIndeterminate
import com.liquidglass.core.components.GlassListItem
import com.liquidglass.core.components.GlassProgress
import com.liquidglass.core.components.GlassSlider
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette
import com.liquidglass.demo.icons.GlassIcons
import com.liquidglass.demo.liquidBackdrop

/** SPEC-04 §2：控件页（顶部为连续变焦 hero 卡） */
@Composable
fun ControlsPage(
    heroModifier: Modifier = Modifier,
    onHeroClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGlassPalette.current
    val scroll = rememberScrollState()
    val backdrop = remember { liquidBackdrop(palette) }
    val captureKey = scroll.value

    var sliderValue by remember { mutableFloatStateOf(0.8f) }
    var liked by remember { mutableStateOf(true) }
    val likeScale by animateFloatAsState(
        targetValue = if (liked) 1f else 0.85f,
        animationSpec = spring(GlassTokens.springDamping, GlassTokens.springStiffness),
        label = "like",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = GlassTokens.pagePadding)
            .padding(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // SPEC-05：连续变焦入口
        HeroCard(modifier = heroModifier, backdrop = backdrop, onClick = onHeroClick)

        Text("完成度 89%", fontSize = 15.sp, color = palette.textPrimary)
        GlassProgress(progress = 0.89f, backdrop = backdrop, captureKey = captureKey)

        Text(
            "音量条（按住整条即可直接调节）",
            fontSize = 15.sp,
            color = palette.textPrimary,
        )
        GlassSlider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            modifier = Modifier.fillMaxWidth(),
            backdrop = backdrop,
        )

        Text("载入中...", fontSize = 15.sp, color = palette.textPrimary)
        GlassIndeterminate(backdrop = backdrop, captureKey = captureKey)

        Spacer(Modifier.height(8.dp))

        // 玻璃组件卡
        GlassCard(title = "玻璃组件", backdrop = backdrop, captureKey = captureKey) {
            GlassButton(text = "送出", onClick = {}, backdrop = backdrop)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassIconButton(onClick = {}, backdrop = backdrop) {
                    Icon(GlassIcons.Add, contentDescription = "add", tint = palette.iconTint)
                }
                GlassIconButton(onClick = { liked = !liked }, backdrop = backdrop) {
                    Icon(
                        if (liked) GlassIcons.Favorite else GlassIcons.FavoriteBorder,
                        contentDescription = "favorite",
                        tint = palette.iconTint,
                        modifier = Modifier.graphicsLayer {
                            scaleX = likeScale
                            scaleY = likeScale
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // 玻璃列表卡
        GlassCard(title = "玻璃列表", backdrop = backdrop, captureKey = captureKey) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassListItem(
                    title = "设置",
                    subtitle = "一般",
                    trailing = "开启",
                    backdrop = backdrop,
                    captureKey = captureKey,
                ) { Icon(GlassIcons.Settings, contentDescription = null, tint = palette.iconTint) }
                GlassListItem(
                    title = "通知",
                    subtitle = "声音与震动",
                    trailing = "开启",
                    backdrop = backdrop,
                    captureKey = captureKey,
                ) { Icon(GlassIcons.Notifications, contentDescription = null, tint = palette.iconTint) }
                GlassListItem(
                    title = "存储空间",
                    subtitle = "可用空间",
                    trailing = "128 GB",
                    backdrop = backdrop,
                    captureKey = captureKey,
                ) { Icon(GlassIcons.Info, contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified) }
            }
        }
    }
}
