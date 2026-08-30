package com.liquidglass.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.core.blur.BlurStrategy
import com.liquidglass.core.components.GlassCard
import com.liquidglass.core.components.pressable
import com.liquidglass.core.surface.glassOverlay
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette
import com.liquidglass.demo.liquidBackdrop

/** SPEC-04 §3：材质页 —— 双策略同屏对比 + 全局开关 */
@Composable
fun MaterialsPage(
    globalStrategy: BlurStrategy,
    onToggleStrategy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGlassPalette.current
    val scroll = rememberScrollState()
    val backdrop = remember { liquidBackdrop(palette) }
    val captureKey = scroll.value

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = GlassTokens.pagePadding)
            .padding(top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 全局策略开关
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("模糊策略", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                Text(
                    if (globalStrategy == BlurStrategy.RENDER_EFFECT) "当前全局：RenderEffect 实时" else "当前全局：离屏合成",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                )
            }
            // 玻璃切换开关
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(32.dp)
                    .pressable { onToggleStrategy() }
                    .glassOverlay(
                        androidx.compose.foundation.shape.RoundedCornerShape(GlassTokens.radiusPill),
                        fill = palette.glassFillRaised,
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .align(if (globalStrategy == BlurStrategy.RENDER_EFFECT) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(4.dp)
                        .width(24.dp)
                        .height(24.dp)
                        .glassOverlay(
                            androidx.compose.foundation.shape.RoundedCornerShape(50),
                            fill = palette.progressFillEnd,
                        ),
                )
            }
        }

        // 两张并排对比卡：各自强制一种策略
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GlassCard(
                title = "RenderEffect",
                modifier = Modifier.weight(1f),
                strategy = BlurStrategy.RENDER_EFFECT,
                backdrop = backdrop,
                captureKey = captureKey,
            ) {
                Text(
                    "Modifier.blur 实时 GPU 模糊，零拷贝逐帧跟随。",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                )
            }
            GlassCard(
                title = "离屏合成",
                modifier = Modifier.weight(1f),
                strategy = BlurStrategy.OFFSCREEN_CAPTURE,
                backdrop = backdrop,
                captureKey = captureKey,
            ) {
                Text(
                    "GraphicsLayer 快照 + StackBlur，100ms 节流重捕。",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "滚动页面观察两种策略：实时模糊逐帧跟随；离屏快照以 100ms 节流重捕，带轻微液态滞后。",
            fontSize = 13.sp,
            color = palette.textSecondary,
        )
    }
}
