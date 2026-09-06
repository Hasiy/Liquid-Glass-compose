package top.hasiyliquidglassdemo.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DynamicLightTabBarLogicTest {

    // ---------- 槽位布局 ----------

    @Test
    fun `slots without center action are all tabs`() {
        assertEquals(listOf(TabSlot.Tab, TabSlot.Tab, TabSlot.Tab), tabSlots(3, false))
    }

    @Test
    fun `center slot splits items evenly`() {
        // 4 個 item：左 2、中央、右 2
        assertEquals(
            listOf(TabSlot.Tab, TabSlot.Tab, TabSlot.Center, TabSlot.Tab, TabSlot.Tab),
            tabSlots(4, true)
        )
        // 3 個 item：左 1、中央、右 2
        assertEquals(
            listOf(TabSlot.Tab, TabSlot.Center, TabSlot.Tab, TabSlot.Tab),
            tabSlots(3, true)
        )
    }

    @Test
    fun `slot item indices map business index to visual slots`() {
        val slots = tabSlots(4, true)
        assertEquals(listOf(0, 1, null, 2, 3), slotItemIndices(slots))
        assertEquals(listOf(0, 1, 2), slotItemIndices(tabSlots(3, false)))
    }

    // ---------- 拖動命中 ----------

    @Test
    fun `touch position resolves to nearest tab slot center`() {
        val slots = tabSlots(4, true) // 5 槽位，寬 100，中心 50/150/250/350/450
        assertEquals(0, tabIndexAtPosition(20f, 500f, slots))
        assertEquals(1, tabIndexAtPosition(150f, 500f, slots))
        assertEquals(2, tabIndexAtPosition(350f, 500f, slots))
        assertEquals(3, tabIndexAtPosition(500f, 500f, slots))
    }

    @Test
    fun `drag over center slot keeps nearest tab`() {
        val slots = tabSlots(4, true)
        // 中央槽位範圍 200~300：左半歸左側最近 Tab，右半歸右側最近 Tab，
        // 永遠不會選出「中央按鈕」
        assertEquals(1, tabIndexAtPosition(220f, 500f, slots))
        assertEquals(2, tabIndexAtPosition(280f, 500f, slots))
    }

    @Test
    fun `touch position resolves and clamps without center action`() {
        val slots = tabSlots(3, false)
        assertEquals(0, tabIndexAtPosition(-20f, 300f, slots))
        assertEquals(0, tabIndexAtPosition(20f, 300f, slots))
        assertEquals(1, tabIndexAtPosition(150f, 300f, slots))
        assertEquals(2, tabIndexAtPosition(300f, 300f, slots))
        assertEquals(2, tabIndexAtPosition(400f, 300f, slots))
    }

    @Test
    fun `invalid geometry has no target tab`() {
        assertNull(tabIndexAtPosition(Float.NaN, 300f, tabSlots(3, false)))
        assertNull(tabIndexAtPosition(10f, 0f, tabSlots(3, false)))
        assertNull(tabIndexAtPosition(10f, 300f, emptyList()))
    }

    // ---------- Indicator 位移與限位 ----------

    @Test
    fun `indicator offset skips center slot for right side items`() {
        val slots = tabSlots(4, true) // 槽位寬 100
        // 左側：槽位序號 = 業務索引
        assertEquals(0f, indicatorTargetOffsetPx(false, 0f, 100f, 500f, 0, slots), 0f)
        assertEquals(100f, indicatorTargetOffsetPx(false, 0f, 100f, 500f, 1, slots), 0f)
        // 右側：跳過中央槽位（序號 3、4）
        assertEquals(300f, indicatorTargetOffsetPx(false, 0f, 100f, 500f, 2, slots), 0f)
        assertEquals(400f, indicatorTargetOffsetPx(false, 0f, 100f, 500f, 3, slots), 0f)
    }

    @Test
    fun `cancelled drag targets external selected index`() {
        val slots = tabSlots(3, false)
        assertEquals(
            200f,
            indicatorTargetOffsetPx(
                isDragging = false,
                touchX = 20f,
                itemWidthPx = 100f,
                barWidthPx = 300f,
                selectedIndex = 2,
                slots = slots,
            ),
            0f,
        )
    }

    @Test
    fun `dragging offset clamps within bar bounds`() {
        val slots = tabSlots(4, true)
        assertEquals(0f, indicatorTargetOffsetPx(true, -50f, 100f, 500f, 0, slots), 0f)
        assertEquals(400f, indicatorTargetOffsetPx(true, 900f, 100f, 500f, 0, slots), 0f)
    }

    // ---------- Lens 幾何與限位 ----------

    @Test
    fun `lens centers on selected slot`() {
        // 槽位寬 100，factor 1.45 → lens 寬 145
        // 選中第 0 槽：offset=0，中心=50，lensLeft = 50 - 72.5 = -22.5（向左伸出，中心對齊選中項）
        assertEquals(-22.5f, lensLeftOffsetPx(0f, 100f, 500f, 1.45f), 0f)
        // 選中第 1 槽：offset=100，中心=150，lensLeft = 150 - 72.5 = 77.5
        assertEquals(77.5f, lensLeftOffsetPx(100f, 100f, 500f, 1.45f), 0f)
    }

    @Test
    fun `lens centers on selected slot even at bar edges`() {
        val barWidth = 500f
        val itemWidth = 100f
        // 尾槽：offset=400，中心=450，lensLeft=450-72.5=377.5（向右伸出，中心仍對齊）
        assertEquals(377.5f, lensLeftOffsetPx(400f, itemWidth, barWidth, 1.45f), 0f)
        // 越界拖動（左側）：中心隨之左移，Lens 左緣為負（伸出 Bar 左界，中心對齊）
        assertEquals(-222.5f, lensLeftOffsetPx(-200f, itemWidth, barWidth, 1.45f), 0f)
    }

    @Test
    fun `lens geometry degrades gracefully`() {
        assertEquals(0f, lensLeftOffsetPx(10f, 0f, 500f, 1.45f), 0f)
        assertEquals(0f, lensLeftOffsetPx(10f, 100f, 0f, 1.45f), 0f)
    }

    // ---------- 拉伸方向 ----------

    @Test
    fun `stretch origin follows selection direction`() {
        assertEquals(0f, indicatorTransformOriginFor(0, 2), 0f)
        assertEquals(1f, indicatorTransformOriginFor(2, 0), 0f)
        assertEquals(0.5f, indicatorTransformOriginFor(1, 1), 0f)
    }
}
