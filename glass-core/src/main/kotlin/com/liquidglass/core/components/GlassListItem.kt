package com.liquidglass.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/** SPEC-03 §7：列表项 = 圆形图标底 + 标题/副标题 + trailing */
@Composable
fun GlassListItem(
    title: String,
    subtitle: String,
    trailing: String,
    modifier: Modifier = Modifier,
    backdrop: BackdropPainter? = null,
    captureKey: Any? = null,
    icon: @Composable () -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(GlassTokens.listItemHeight),
        shape = RoundedCornerShape(GlassTokens.radiusCard),
        raised = true,
        backdrop = backdrop,
        captureKey = captureKey,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GlassTokens.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 圆形图标底
            Box(
                modifier = Modifier
                    .size(GlassTokens.iconSize)
                    .glassOverlay(RoundedCornerShape(50), fill = palette.glassFillRaised),
                contentAlignment = Alignment.Center,
            ) { icon() }

            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
                Text(subtitle, fontSize = 13.sp, color = palette.textSecondary)
            }

            Text(trailing, fontSize = 15.sp, color = palette.textSecondary)
        }
    }
}
