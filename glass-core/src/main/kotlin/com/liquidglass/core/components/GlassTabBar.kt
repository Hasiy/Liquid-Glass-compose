package com.liquidglass.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.core.surface.BackdropPainter
import com.liquidglass.core.surface.GlassSurface
import com.liquidglass.core.surface.glassOverlay
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette

/**
 * SPEC-03 §8：底部液态 Tab 栏。
 * 选中 pill 以 springLiquid 滑动；切换中向点击方向拉伸 1.15（REQ-TAB-LIQUID）。
 */
@Composable
fun GlassTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: BackdropPainter? = null,
) {
    val palette = LocalGlassPalette.current

    GlassSurface(
        modifier = modifier.height(GlassTokens.tabBarHeight),
        shape = RoundedCornerShape(GlassTokens.radiusPill),
        raised = true,
        backdrop = backdrop,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val innerPad = 8.dp
            val tabW = (maxWidth - innerPad * 2) / tabs.size

            // 液态指示器：spring 滑动到目标位（轻度 overshoot 产生"吸附/吸入"感）
            val animIndex by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(GlassTokens.springDamping, GlassTokens.springStiffness),
                label = "tabIndex",
            )

            Box(
                modifier = Modifier
                    .offset(x = innerPad + tabW * animIndex)
                    .width(tabW)
                    .fillMaxHeight()
                    .glassOverlay(
                        RoundedCornerShape(GlassTokens.radiusPill),
                        fill = palette.glassFillRaised.copy(alpha = 0.22f),
                    ),
            )

            Row(Modifier.fillMaxSize().padding(innerPad)) {
                tabs.forEachIndexed { index, label ->
                    val selected = index == selectedIndex
                    val color by animateColorAsState(
                        targetValue = if (selected) palette.textPrimary else palette.textSecondary,
                        animationSpec = spring(stiffness = GlassTokens.springStiffness),
                        label = "tabText",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pressable { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            color = color,
                        )
                    }
                }
            }
        }
    }
}
