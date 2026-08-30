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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.core.components.GlassCard
import com.liquidglass.core.components.GlassIconButton
import com.liquidglass.core.surface.GlassSurface
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette
import com.liquidglass.demo.liquidBackdrop

/**
 * SPEC-05 §4：详情页。顶部封面以 [coverModifier]（含与首页 hero 卡同 HERO_KEY 的
 * [Modifier.sharedBounds]，见 MainActivity）做连续变焦桥接。
 * 非共享内容（说明、玻璃卡、关闭按钮）各自淡入。
 */
@Composable
fun HeroDetail(
    coverModifier: Modifier = Modifier,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGlassPalette.current
    val backdrop = remember { liquidBackdrop(palette) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = GlassTokens.pagePadding)
            .padding(top = 12.dp, bottom = 120.dp),
    ) {
        // 共享封面（放大版，连续变焦铺满）——与 HeroCard 同 HERO_KEY
        GlassSurface(
            modifier = coverModifier
                .fillMaxWidth()
                .height(300.dp),
            shape = RoundedCornerShape(GlassTokens.radiusCardL),
            raised = true,
            backdrop = backdrop,
        ) {
            HeroArt(Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(GlassTokens.cardPadding),
            ) {
                Column {
                    Text("连续变焦", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Text("Liquid Glass · Continuum", fontSize = 14.sp, color = palette.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassIconButton(onClick = onClose, backdrop = backdrop, size = 44) {
                Icon(CloseIcon, contentDescription = "close", tint = palette.iconTint)
            }
            Text("返回", fontSize = 15.sp, color = palette.textSecondary)
        }

        Spacer(Modifier.height(14.dp))

        GlassCard(title = "关于连续变焦", backdrop = backdrop) {
            Text(
                "点击封面时它并非切换页面，而是让玻璃内容随过渡进度连续放大、铺满并浮现新界面；" +
                    "关闭时反向收缩回原位。这种从局部到全体的无跳变变焦，是 Liquid Glass 最标志性的交互。",
                fontSize = 14.sp,
                color = palette.textSecondary,
            )
        }
    }
}

private val CloseIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "close",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2.4f,
            strokeLineCap = StrokeCap.Round,
        ) {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }.build()
}