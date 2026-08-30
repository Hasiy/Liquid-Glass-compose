package com.liquidglass.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.core.surface.BackdropPainter
import com.liquidglass.core.surface.GlassSurface
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette

/** SPEC-03 §2：pill 文本按钮 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backdrop: BackdropPainter? = null,
) {
    val palette = LocalGlassPalette.current
    GlassSurface(
        modifier = modifier
            .height(56.dp)
            .pressable(onClick),
        shape = RoundedCornerShape(GlassTokens.radiusPill),
        backdrop = backdrop,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .align(Alignment.Center),
        ) {
            Text(text = text, fontSize = 16.sp, color = palette.textPrimary)
        }
    }
}

/** SPEC-03 §3：圆形图标按钮 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 56,
    backdrop: BackdropPainter? = null,
    icon: @Composable () -> Unit,
) {
    GlassSurface(
        modifier = modifier
            .size(size.dp)
            .pressable(onClick),
        shape = RoundedCornerShape(50),
        backdrop = backdrop,
    ) {
        Box(contentAlignment = Alignment.Center) { icon() }
    }
}
