package com.liquidglass.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import com.liquidglass.core.surface.BackdropPainter
import com.liquidglass.core.surface.GlassSurface
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette

/** SPEC-03 §1 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    strategy: com.liquidglass.core.blur.BlurStrategy? = null,
    backdrop: BackdropPainter? = null,
    captureKey: Any? = null,
    content: @Composable () -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlassTokens.radiusCardL),
        strategy = strategy,
        backdrop = backdrop,
        captureKey = captureKey,
    ) {
        Column(modifier = Modifier.padding(GlassTokens.cardPadding)) {
            if (title != null) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textPrimary,
                    modifier = Modifier.padding(bottom = GlassTokens.cardPadding),
                )
            }
            content()
        }
    }
}
