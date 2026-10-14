/* Copyright 2026 FitDash contributors. */
package top.hasiy.designsystem

import kotlin.test.Test
import kotlin.test.assertSame

class GlassConfigDefaultTest {
    @Test fun default_afterPresetInitialization_resolvesDrop() {
        val drop = GlassPresets.Drop
        assertSame(drop, GlassConfig.Default)
    }
}
