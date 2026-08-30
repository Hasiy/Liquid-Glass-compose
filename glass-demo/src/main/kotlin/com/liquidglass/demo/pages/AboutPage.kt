package com.liquidglass.demo.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liquidglass.core.components.GlassCard
import com.liquidglass.core.theme.GlassTokens
import com.liquidglass.core.theme.LocalGlassPalette
import com.liquidglass.demo.liquidBackdrop

/** SPEC-04 §4：关于页 */
@Composable
fun AboutPage(modifier: Modifier = Modifier) {
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
        GlassCard(title = "Liquid-Glass Compose", backdrop = backdrop, captureKey = captureKey) {
            InfoRow("版本", "0.1.0")
            InfoRow("Spec 目录", "specs/")
            InfoRow("AGP / Kotlin", "8.9.1 / 2.1.21")
            InfoRow("Compose BOM", "2025.01.01")
            InfoRow("minSdk / target", "26 / 35")
        }

        GlassCard(title = "SDD 规范驱动", backdrop = backdrop, captureKey = captureKey) {
            Text(
                "specs/ 目录为唯一事实来源：00 总览、01 tokens、02 玻璃面与双模糊策略、" +
                    "03 组件库、04 Tab Demo。实现与 spec 不一致时，先改 spec 再改代码。",
                fontSize = 13.sp,
                color = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val palette = LocalGlassPalette.current
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, fontSize = 13.sp, color = palette.textSecondary)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = palette.textPrimary)
    }
}
