package top.hasiyliquidglassdemo.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DynamicLightTabBarLogicTest {

    @Test
    fun `touch position resolves and clamps to tab index`() {
        assertEquals(0, tabIndexAtPosition(-20f, 300f, 3))
        assertEquals(0, tabIndexAtPosition(20f, 300f, 3))
        assertEquals(1, tabIndexAtPosition(150f, 300f, 3))
        assertEquals(2, tabIndexAtPosition(300f, 300f, 3))
        assertEquals(2, tabIndexAtPosition(400f, 300f, 3))
    }

    @Test
    fun `invalid geometry has no target tab`() {
        assertNull(tabIndexAtPosition(Float.NaN, 300f, 3))
        assertNull(tabIndexAtPosition(10f, 0f, 3))
        assertNull(tabIndexAtPosition(10f, 300f, 0))
    }

    @Test
    fun `stretch origin follows selection direction`() {
        assertEquals(0f, indicatorTransformOriginFor(0, 2), 0f)
        assertEquals(1f, indicatorTransformOriginFor(2, 0), 0f)
        assertEquals(0.5f, indicatorTransformOriginFor(1, 1), 0f)
    }

    @Test
    fun `cancelled drag targets external selected index`() {
        assertEquals(
            200f,
            indicatorTargetOffsetPx(
                isDragging = false,
                touchX = 20f,
                itemWidthPx = 100f,
                barWidthPx = 300f,
                selectedIndex = 2,
            ),
            0f,
        )
    }
}
