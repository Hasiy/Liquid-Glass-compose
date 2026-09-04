package top.hasiy.designsystem.tokens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlassThemeSpecTest {
    @Test
    fun `neutral preset is light and accent enabled`() {
        val spec = GlassThemeSpec.default(GlassVisualStyle.NEUTRAL)
        assertTrue(spec.isLight)
        assertTrue(spec.accentEnabled)
        assertEquals("neutral", spec.id)
    }

    @Test
    fun `preset ids remain stable`() {
        assertEquals("drop", GlassThemeSpec.default(GlassVisualStyle.DROP).id)
        assertEquals("dark", GlassThemeSpec.default(GlassVisualStyle.DARK).id)
        assertEquals("native", GlassThemeSpec.default(GlassVisualStyle.NATIVE).id)
    }
}