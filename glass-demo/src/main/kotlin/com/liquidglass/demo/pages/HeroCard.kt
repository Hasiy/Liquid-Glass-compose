package com.liquidglass.demo.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.core.components.pressable
import com.liquidglass.core.surface.BackdropPainter
import com.liquidglass.core.surface.GlassSurface
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette

/** SPEC-05 §2 共享 key：首页 hero 卡与详情页封面共用 */
const val HERO_KEY = "hero_cover"

/**
 * SPEC-05：首页 hero 玻璃卡。
 * 外部以 [HERO_KEY] 注入的 [Modifier.sharedBounds] 作为共享边界（见 MainActivity），
 * 与详情页封面桥接连续变焦。整个玻璃面（HeroArt + 玻璃叠层）参与缩放。
 */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    backdrop: BackdropPainter? = null,
    onClick: () -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .pressable(onClick),
        shape = RoundedCornerShape(GlassTokens.radiusCardL),
        raised = true,
        backdrop = backdrop,
    ) {
        HeroArt(Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(GlassTokens.cardPadding),
        ) {
            Text(
                "Liquid Glass",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
            )
            Text("点我体验连续变焦", fontSize = 13.sp, color = palette.textSecondary)
        }
    }
}