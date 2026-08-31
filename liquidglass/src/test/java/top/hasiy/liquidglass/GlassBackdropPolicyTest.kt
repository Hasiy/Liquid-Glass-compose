package top.hasiyliquidglass

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassBackdropPolicyTest {

    @Test
    fun `API 26 through 30 use frosted fallback`() {
        for (sdkInt in 26..30) {
            assertFalse(
                shouldUseGlassBackdropBlur(
                    hasBackdropState = true,
                    sdkInt = sdkInt,
                    native = false,
                )
            )
        }
    }

    @Test
    fun `API 31 and above use backdrop blur when host exists`() {
        assertTrue(shouldUseGlassBackdropBlur(true, 31, native = false))
        assertTrue(shouldUseGlassBackdropBlur(true, 33, native = false))
    }

    @Test
    fun `missing host and native mode never use backdrop blur`() {
        assertFalse(shouldUseGlassBackdropBlur(false, 35, native = false))
        assertFalse(shouldUseGlassBackdropBlur(true, 35, native = true))
    }
}
