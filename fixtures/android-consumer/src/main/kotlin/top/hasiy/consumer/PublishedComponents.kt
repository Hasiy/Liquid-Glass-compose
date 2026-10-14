/* Copyright 2026 FitDash contributors. */
package top.hasiy.consumer

import androidx.compose.runtime.Composable
import top.hasiy.designsystem.DesignSystemTheme
import top.hasiy.designsystem.GlassCheckbox
import top.hasiy.designsystem.GlassSlider
import top.hasiy.designsystem.tokens.ThemePalette

/** Compile both SDK components and transitive tokens from Maven coordinates only. */
@Composable
fun PublishedComponents() {
    DesignSystemTheme(palette = ThemePalette.Lime) {
        GlassCheckbox(checked = true, onCheckedChange = {})
        GlassSlider(value = 0.5f, onValueChange = {})
    }
}
