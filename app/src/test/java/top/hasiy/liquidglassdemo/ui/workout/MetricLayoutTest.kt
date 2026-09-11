package top.hasiyliquidglassdemo.ui.workout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 網格規格與拖動換位。
 *
 * 這兩件事都只動 [WorkoutUiState.layout] 與規格欄位，不碰執行狀態，所以純邏輯可測。
 */
class MetricLayoutTest {

    private fun controller() = WorkoutController(CoroutineScope(Dispatchers.Unconfined))

    // ---------- 規格 ----------

    @Test
    fun `portrait spec decides how many slots are laid out`() {
        MetricGridSpec.PortraitOptions.forEach { spec ->
            assertEquals(
                "${spec.label} 的格數",
                spec.rows * spec.columns,
                MetricSlots.portrait(spec).size,
            )
        }
    }

    @Test
    fun `two specs with the same cell count still differ`() {
        val wide = MetricGridSpec(2, 3)
        val tall = MetricGridSpec(3, 2)
        assertEquals(wide.slotCount, tall.slotCount)
        assertTrue("2×3 與 3×2 是兩種版面，不能當成同一個", wide != tall)
    }

    /**
     * 規格開大之後多出來的格子沒有預設值，就是空白的。
     *
     * 拿 dock 測：豎屏的預設值補到 15 個之後每種規格都填得滿，
     * 而 dock 只有 12 個，3×5 的最後 3 格必然是空的。
     */
    @Test
    fun `slots beyond the reference layout start empty`() {
        val state = WorkoutUiState(dockSpec = MetricGridSpec(3, 5))
        val slots = state.dockSlots
        assertEquals(15, slots.size)
        MetricSlots.DockDefaults.indices.forEach { index ->
            assertTrue("第 ${index + 1} 格該有預設值", state.metricAt(slots[index].key) != null)
        }
        (MetricSlots.DockDefaults.size until slots.size).forEach { index ->
            assertNull("第 ${index + 1} 格該是空的", state.metricAt(slots[index].key))
        }
    }

    /** 規格縮小只是少渲染幾格，改過的內容留著——切回大規格要能原樣恢復。 */
    @Test
    fun `shrinking the grid keeps overrides for the hidden slots`() {
        val controller = controller()
        controller.setGridSpec(MetricGridSpec(3, 3), portrait = true)
        controller.assignMetric("portrait-8", "steps")
        controller.setGridSpec(MetricGridSpec(2, 3), portrait = true)
        assertEquals(6, controller.uiState.portraitSlots.size)
        controller.setGridSpec(MetricGridSpec(3, 3), portrait = true)
        assertEquals("steps", controller.uiState.metricAt("portrait-8")?.id)
    }

    /**
     * 「已在其他位置顯示」只比當前規格內的格子。
     *
     * 從 3×3 收回 2×3 之後第 7～9 格的覆蓋還在，拿全集去比的話，一個只存在於
     * 更大規格裡的指標會被誤報成已被佔用。
     */
    @Test
    fun `used elsewhere ignores slots outside the current spec`() {
        val controller = controller()
        controller.setGridSpec(MetricGridSpec(3, 3), portrait = true)
        controller.assignMetric("portrait-8", "steps")
        controller.setGridSpec(MetricGridSpec(2, 3), portrait = true)
        assertTrue(
            "第 9 格已經不在畫面上，它上面的指標不該算被佔用",
            !controller.uiState.isMetricUsedElsewhere("portrait-0", "steps"),
        )
    }

    // ---------- 尾部空列不佔位 ----------

    private fun dockRows(spec: MetricGridSpec) =
        packMetricRows(MetricSlots.dock(spec), spec.columns) { 1 }

    /** 規格是上限：3×5 只填了 8 格時第 3 列整列是空的，不該白佔一列高度。 */
    @Test
    fun `trailing empty rows are not laid out`() {
        val rows = dockRows(MetricGridSpec(3, 5))
        assertEquals(2, visibleRowCount(rows, lastFilledKey = "land-dock-7"))
    }

    /**
     * 最後一列剛好填滿時**不**再多給一列。
     *
     * 多給的那一列整列是空的，畫面上就是一排空殼加一條分隔線。要填第 3 列
     * 進版面編輯模式，那時整個規格都攤開。
     */
    @Test
    fun `a full last row does not open an extra one`() {
        val rows = dockRows(MetricGridSpec(3, 5))
        assertEquals(2, visibleRowCount(rows, lastFilledKey = "land-dock-9"))
    }

    /** 一個都沒填也要留一列，否則沒有任何格子可以點進去填。 */
    @Test
    fun `an empty grid still shows one row`() {
        val rows = dockRows(MetricGridSpec(3, 5))
        assertEquals(1, visibleRowCount(rows, lastFilledKey = null))
    }

    /** 編輯版面時整個規格攤開，空列也要在。 */
    @Test
    fun `editing the layout shows every row`() {
        val rows = dockRows(MetricGridSpec(3, 5))
        assertEquals(3, visibleRowCount(rows, lastFilledKey = "land-dock-7", showEmpty = true))
        assertEquals(3, visibleRowCount(rows, lastFilledKey = null, showEmpty = true))
    }

    // ---------- 不是「一個數」的顯示 ----------

    /** 要外接感測器的指標沒配對上時顯示橫線的哨兵，而不是 0。 */
    @Test
    fun `sensor metrics report no data when the strap is not paired`() {
        val heart = MetricCatalog.find("heart")!!
        val paired = WorkoutUiState()
        assertEquals(heart.value, paired.displayValueOf(heart))

        val unpaired = WorkoutUiState(device = paired.device.copy(sensorConnected = false))
        assertEquals(DISPLAY_NO_DATA, unpaired.displayValueOf(heart))
    }

    /** 0 是「量到了，就是 0」，跟「沒量到」不是一回事——別把哨兵混成 0。 */
    @Test
    fun `no data is not zero`() {
        assertTrue(DISPLAY_NO_DATA != "0")
        assertTrue(DISPLAY_NO_DATA.isNotEmpty())
        assertTrue("哨兵不能撞上「這一格是空的」", DISPLAY_NO_DATA != EMPTY_SLOT)
    }

    /** 只有心率這類要感測器的指標受影響，其餘照常。 */
    @Test
    fun `metrics that need no sensor are unaffected`() {
        val distance = MetricCatalog.find("distance")!!
        val state = WorkoutUiState(device = WorkoutUiState().device.copy(sensorConnected = false))
        assertEquals(distance.value, state.displayValueOf(distance))
    }

    /** 阻力沒有電控時在數值位置寫「手動」，而不是報一個假的數字。 */
    @Test
    fun `resistance reads manual when the device cannot control it`() {
        val resistance = MetricCatalog.find("resistance")!!
        val electronic = WorkoutUiState()
        assertEquals(resistance.value, electronic.displayValueOf(resistance))

        val manual = WorkoutUiState(
            device = electronic.device.copy(controllableMetricIds = emptySet())
        )
        assertEquals(DISPLAY_MANUAL, manual.displayValueOf(resistance))
    }

    /** 「手動」只給阻力。別的指標不可調不代表它是使用者自己轉的。 */
    @Test
    fun `manual only applies to resistance`() {
        val speed = MetricCatalog.find("target-speed")!!
        val state = WorkoutUiState(
            device = WorkoutUiState().device.copy(controllableMetricIds = emptySet())
        )
        assertEquals(speed.value, state.displayValueOf(speed))
    }

    // ---------- 換位 ----------

    @Test
    fun `swapping two slots exchanges their metrics`() {
        val controller = controller()
        val before2 = controller.uiState.metricAt("portrait-2")?.id
        val before5 = controller.uiState.metricAt("portrait-5")?.id
        assertTrue("前置條件：兩格本來不同", before2 != before5)

        controller.swapSlots("portrait-5", "portrait-2")

        assertEquals(before5, controller.uiState.metricAt("portrait-2")?.id)
        assertEquals(before2, controller.uiState.metricAt("portrait-5")?.id)
    }

    /** 兩邊都要落成顯式覆蓋——只寫一邊的話另一邊會繼續走預設值，看起來像沒換。 */
    @Test
    fun `swapping writes both sides explicitly`() {
        val controller = controller()
        controller.swapSlots("portrait-5", "portrait-2")
        val layout = controller.uiState.layout
        assertTrue(layout.containsKey("0:portrait-2"))
        assertTrue(layout.containsKey("0:portrait-5"))
    }

    /** 和空格子換位：對方的內容搬過來，自己這邊變空。 */
    @Test
    fun `swapping with an empty slot moves the metric and leaves a hole`() {
        val controller = controller()
        controller.setGridSpec(MetricGridSpec(3, 5), portrait = false)
        val moved = controller.uiState.metricAt("land-dock-0")?.id
        assertTrue("前置條件：第 1 格有東西", moved != null)
        assertNull("前置條件：第 15 格是空的", controller.uiState.metricAt("land-dock-14"))

        controller.swapSlots("land-dock-0", "land-dock-14")

        assertEquals(moved, controller.uiState.metricAt("land-dock-14")?.id)
        assertNull(controller.uiState.metricAt("land-dock-0"))
    }

    @Test
    fun `swapping across layout families is refused`() {
        val controller = controller()
        val before = controller.uiState.metricAt("portrait-0")?.id
        controller.swapSlots("portrait-0", "land-dock-0")
        assertEquals(before, controller.uiState.metricAt("portrait-0")?.id)
    }

    @Test
    fun `swapping a slot with itself changes nothing`() {
        val controller = controller()
        controller.swapSlots("portrait-0", "portrait-0")
        assertTrue(controller.uiState.layout.isEmpty())
    }

    /** 換位是分組的：在主組挪過的位置不該讓第 2 組跟著動。 */
    @Test
    fun `swap is scoped to the group it was made in`() {
        val controller = controller()
        val group2Before = run {
            controller.switchGroup()
            val ids = controller.uiState.portraitSlots.map { controller.uiState.metricAt(it.key)?.id }
            controller.switchGroup()
            ids
        }
        controller.swapSlots("portrait-5", "portrait-2")
        controller.switchGroup()
        assertEquals(
            group2Before,
            controller.uiState.portraitSlots.map { controller.uiState.metricAt(it.key)?.id },
        )
    }

    // ---------- 移除與補位 ----------

    /** 移除一格，後面的依次往前補位——中間不留洞。 */
    @Test
    fun `removing a slot shifts the rest forward`() {
        val controller = controller()
        val before = controller.uiState.portraitSlots.map { controller.uiState.metricAt(it.key)?.id }

        controller.clearSlot("portrait-1")

        val after = controller.uiState.portraitSlots.map { controller.uiState.metricAt(it.key)?.id }
        // 第 1 格不動，第 2 格起全部前移一位，末尾空出來
        assertEquals(before[0], after[0])
        assertEquals(before[2], after[1])
        assertEquals(before[3], after[2])
        assertEquals(before[4], after[3])
        assertEquals(before[5], after[4])
        assertNull("末尾該空出一格", after[5])
    }

    /**
     * 補位後每一格都要落成顯式覆蓋。
     *
     * 只寫被動過的那幾格不夠：沒寫的格子會繼續走預設值，把補位的結果蓋回去。
     */
    @Test
    fun `shifting writes every slot explicitly`() {
        val controller = controller()
        controller.clearSlot("portrait-1")
        controller.uiState.portraitSlots.forEach { slot ->
            assertTrue(
                "${slot.key} 沒落成覆蓋，會被預設值蓋回去",
                controller.uiState.layout.containsKey(controller.uiState.layoutKeyOf(slot.key)),
            )
        }
    }

    /** 補位只在同一塊網格內：刪 dock 的一格不該把側卡的內容吸過去。 */
    @Test
    fun `shifting stays inside one grid block`() {
        val controller = controller()
        val sideSlots = controller.uiState.sideSlots
        val sideBefore = sideSlots.map { controller.uiState.metricAt(it.key)?.id }

        controller.clearSlot("land-dock-0")

        assertEquals(
            sideBefore,
            sideSlots.map { controller.uiState.metricAt(it.key)?.id },
        )
    }

    /** 移除末尾那一格就只是空出來，不影響前面。 */
    @Test
    fun `removing the last slot leaves the others alone`() {
        val controller = controller()
        val before = controller.uiState.portraitSlots.map { controller.uiState.metricAt(it.key)?.id }

        controller.clearSlot("portrait-5")

        val after = controller.uiState.portraitSlots.map { controller.uiState.metricAt(it.key)?.id }
        assertEquals(before.dropLast(1), after.dropLast(1))
        assertNull(after.last())
    }

    /** 全部移除之後整塊網格都是空的，可見列數收到 1（總得留一格能點）。 */
    @Test
    fun `removing everything empties the block`() {
        val controller = controller()
        repeat(controller.uiState.portraitSlots.size) { controller.clearSlot("portrait-0") }
        assertTrue(controller.uiState.portraitSlots.all { controller.uiState.metricAt(it.key) == null })
        assertEquals(
            1,
            visibleRowCount(packMetricRows(controller.uiState.portraitSlots, 3) { 1 }, null),
        )
    }

    // ---------- 左對齊（有內容的往前收攏）----------

    /** 中間空著的槽位收到末尾，兩邊各自維持原有的相對順序。 */
    @Test
    fun `compacting moves filled slots to the front`() {
        val all = MetricSlots.dock(MetricGridSpec(3, 5))
        val filled = setOf("land-dock-0", "land-dock-3", "land-dock-13")
        val compacted = compactSlots(all) { it.key in filled }

        assertEquals(
            listOf("land-dock-0", "land-dock-3", "land-dock-13"),
            compacted.take(3).map { it.key },
        )
        // 空槽也保持原順序，不是隨機亂排
        assertEquals("land-dock-1", compacted[3].key)
        assertEquals("land-dock-2", compacted[4].key)
        assertEquals(all.size, compacted.size)
    }

    /** 全滿或全空時順序不變——收攏不該無事生非。 */
    @Test
    fun `compacting a uniform grid keeps the order`() {
        val all = MetricSlots.dock(MetricGridSpec(2, 4))
        assertEquals(all, compactSlots(all) { true })
        assertEquals(all, compactSlots(all) { false })
    }

    /** 開關只改顯示順序，不動版面覆蓋：關回去每一格還在原位。 */
    @Test
    fun `toggling align start does not touch the layout`() {
        val controller = controller()
        controller.assignMetric("land-dock-13", "best-lap")
        val before = controller.uiState.layout

        controller.setAlignStart(true)

        assertTrue(controller.uiState.alignStart)
        assertEquals(before, controller.uiState.layout)
        assertEquals("best-lap", controller.uiState.metricAt("land-dock-13")?.id)
    }

    /** 左對齊要跟著存檔走，重建之後還是使用者選的那一檔。 */
    @Test
    fun `align start survives a round trip`() {
        val controller = controller()
        controller.setAlignStart(true)
        controller.setSideSpec(MetricGridSpec(MetricSlots.SIDE_ROWS, 2))

        val restored = controller()
        restored.importLayout(controller.exportLayout())

        assertTrue(restored.uiState.alignStart)
        assertEquals(MetricGridSpec(MetricSlots.SIDE_ROWS, 2), restored.uiState.sideSpec)
    }

    /** 舊存檔沒有這兩段，各自退回預設而不是崩掉。 */
    @Test
    fun `a save without the newer fields falls back`() {
        val restored = controller()
        restored.importLayout("2x3|2x4|")

        assertEquals(false, restored.uiState.alignStart)
        assertEquals(MetricGridSpec.SideDefault, restored.uiState.sideSpec)
    }

    // ---------- 跨欄的對比卡 ----------

    private fun slots(rows: Int, columns: Int) = MetricSlots.portrait(MetricGridSpec(rows, columns))

    /** 沒有跨欄時，打包結果就是按欄數等分。 */
    @Test
    fun `packing without wide cells matches plain chunking`() {
        val all = slots(rows = 2, columns = 3)
        val packed = packMetricRows(all, columns = 3) { 1 }
        assertEquals(all.chunked(3), packed)
    }

    /** 佔兩欄的卡吃掉兩個欄位，同一列只剩一格的位置。 */
    @Test
    fun `a wide cell takes two columns`() {
        val all = slots(rows = 2, columns = 3)
        val packed = packMetricRows(all, columns = 3) { slot ->
            if (slot.key == "portrait-0") 2 else 1
        }
        assertEquals(
            listOf(
                listOf("portrait-0", "portrait-1"),
                listOf("portrait-2", "portrait-3", "portrait-4"),
                listOf("portrait-5"),
            ),
            packed.map { row -> row.map { it.key } },
        )
    }

    /** 兩欄的網格上，跨兩欄就是佔滿一列。 */
    @Test
    fun `a wide cell fills a two column row`() {
        val all = slots(rows = 2, columns = 2)
        val packed = packMetricRows(all, columns = 2) { slot ->
            if (slot.key == "portrait-0") 2 else 1
        }
        assertEquals(
            listOf(
                listOf("portrait-0"),
                listOf("portrait-1", "portrait-2"),
                listOf("portrait-3"),
            ),
            packed.map { row -> row.map { it.key } },
        )
    }

    /** 放不進剩餘欄位時換列，**不去後面找小格子填空**——填了顯示順序就亂了。 */
    @Test
    fun `a wide cell that does not fit starts a new row`() {
        val all = slots(rows = 2, columns = 3)
        val packed = packMetricRows(all, columns = 3) { slot ->
            if (slot.key == "portrait-2") 2 else 1
        }
        assertEquals(
            listOf(
                listOf("portrait-0", "portrait-1"),
                listOf("portrait-2", "portrait-3"),
                listOf("portrait-4", "portrait-5"),
            ),
            packed.map { row -> row.map { it.key } },
        )
    }

    /** 尾部空列不佔位：留到最後一個有內容的格子那一列，再多給一列。 */
    @Test
    fun `rows stop after the last filled one`() {
        val all = slots(rows = 3, columns = 3)
        val packed = packMetricRows(all, columns = 3) { 1 }
        assertEquals(1, visibleRowCount(packed, lastFilledKey = "portrait-2"))
        assertEquals(2, visibleRowCount(packed, lastFilledKey = "portrait-4"))
        // 一個都沒有時仍留一列，總得有個能點的地方
        assertEquals(1, visibleRowCount(packed, lastFilledKey = null))
    }

    /** 桨次指標都帶對比資料，而且量程是正的——除以 0 會畫出滿格的條。 */
    @Test
    fun `stroke metrics carry comparison data`() {
        val ids = listOf(
            "peak-pull", "avg-pull", "drive-time", "recovery-time",
            "stroke-power", "drive-length", "stroke-distance", "stroke-work",
        )
        ids.forEach { id ->
            val metric = MetricCatalog.find(id)
            assertNotNull("缺指標 $id", metric)
            val comparison = metric!!.comparison
            assertNotNull("$id 沒有對比資料", comparison)
            assertTrue("$id 的量程要是正數", comparison!!.max > 0f)
            // 當前值與上一桨都該落在量程內，否則條會頂到頭、比不出差
            val current = metric.value.toFloatOrNull()
            assertNotNull("$id 的示例值不是數字", current)
            assertTrue("$id 的當前值超出量程", current!! <= comparison.max)
            assertTrue("$id 的上一桨超出量程", comparison.previous <= comparison.max)
            assertTrue("$id 的基準線超出量程", comparison.reference <= comparison.max)
        }
    }

    // ---------- 橫屏側邊 ----------

    /** 儀表左右各一欄四格（現狀：單欄規格）。 */
    @Test
    fun `each landscape side column holds four slots`() {
        assertEquals(4, MetricSlots.SIDE_ROWS)
        val slots = MetricSlots.side(MetricGridSpec.SideDefault)
        assertEquals(MetricSlots.SIDE_ROWS * 2, slots.size)
    }

    /** 兩欄規格：每側翻倍到 8 格，兩側共 16 格。 */
    @Test
    fun `the wide side spec doubles each column to eight slots`() {
        val wide = MetricGridSpec(MetricSlots.SIDE_ROWS, 2)
        assertTrue(wide in MetricGridSpec.SideOptions)
        val slots = MetricSlots.side(wide)
        assertEquals(MetricSlots.SIDE_ROWS * 2 * 2, slots.size)
        assertEquals(MetricSlots.SIDE_ROWS * 2, slots.take(wide.slotCount).size)
    }

    /** 側邊規格的預設值要填滿最大的那個候選，不留空——跟豎屏那組的取捨一致。 */
    @Test
    fun `side defaults fill the largest side spec`() {
        val widest = MetricGridSpec.SideOptions.maxBy { it.slotCount }
        assertEquals(widest.slotCount * 2, MetricSlots.SideDefaults.size)
    }

    /**
     * 側邊與 dock 的預設值不能有交集。
     *
     * 兩者是同一族，撞了的話兩張卡會互相標成「已在其他位置顯示」——版面一打開
     * 就掛著一排提醒，看起來像壞了。加側邊格數或補 dock 預設值時最容易踩到這裡。
     */
    @Test
    fun `side and dock defaults do not collide`() {
        val overlap = MetricSlots.SideDefaults.intersect(MetricSlots.DockDefaults.toSet())
        assertTrue("撞了：$overlap", overlap.isEmpty())
    }

    // ---------- 平板兩頁的會話讀數區 ----------

    /** 會話讀數區按參考稿 08／09 的八格排。 */
    @Test
    fun `the bay lays out the reference eight`() {
        val state = WorkoutUiState()
        assertEquals(8, state.baySlots.size)
        assertEquals(
            listOf("duration", "distance", "heart", "power", "cadence", "calories", "avg-speed", "max-power"),
            state.baySlots.map { state.metricAt(it.key)?.id },
        )
    }

    /** 賽道模式底部固定四格圈速。 */
    @Test
    fun `the circuit bay lays out the four lap readouts`() {
        val state = WorkoutUiState()
        assertEquals(4, MetricSlots.CircuitBay.size)
        assertEquals(
            listOf("duration", "distance", "lap-time", "best-lap"),
            MetricSlots.CircuitBay.map { state.metricAt(it.key)?.id },
        )
    }

    /** 移除一格之後同一族內補位，末尾留空。 */
    @Test
    fun `removing a bay slot shifts the rest forward`() {
        val controller = controller()
        controller.clearSlot("bay-0")
        val state = controller.uiState
        assertEquals("distance", state.metricAt("bay-0")?.id)
        assertEquals("heart", state.metricAt("bay-1")?.id)
        assertEquals("max-power", state.metricAt("bay-6")?.id)
        assertNull(state.metricAt("bay-7"))
    }

    /** 兩族互不干擾：動了會話那一族，賽道那四格原樣不動。 */
    @Test
    fun `the two bay families do not disturb each other`() {
        val controller = controller()
        controller.clearSlot("bay-0")
        val state = controller.uiState
        assertEquals(
            listOf("duration", "distance", "lap-time", "best-lap"),
            MetricSlots.CircuitBay.map { state.metricAt(it.key)?.id },
        )
    }

    /** 會話那一族自成一塊，換位不會把豎屏或 dock 的格子捲進來。 */
    @Test
    fun `swapping inside the bay stays in the bay`() {
        val controller = controller()
        controller.swapSlots("bay-0", "bay-3")
        val state = controller.uiState
        assertEquals("power", state.metricAt("bay-0")?.id)
        assertEquals("duration", state.metricAt("bay-3")?.id)
        // 豎屏第一格仍是它自己的預設值
        assertEquals("power", state.metricAt("portrait-0")?.id)
    }

    // ---------- 存檔 ----------

    @Test
    fun `layout survives a round trip through the saved string`() {
        val controller = controller()
        controller.setGridSpec(MetricGridSpec(3, 3), portrait = true)
        controller.setGridSpec(MetricGridSpec(3, 5), portrait = false)
        controller.swapSlots("portrait-5", "portrait-2")
        val encoded = controller.exportLayout()
        val expected = controller.uiState.layout

        val restored = controller()
        restored.importLayout(encoded)

        assertEquals(MetricGridSpec(3, 3), restored.uiState.portraitSpec)
        assertEquals(MetricGridSpec(3, 5), restored.uiState.dockSpec)
        assertEquals(expected, restored.uiState.layout)
    }

    /** 空版面也要能存回來，不能還原出一個假的覆蓋項。 */
    @Test
    fun `an untouched layout round trips to an empty map`() {
        val controller = controller()
        val restored = controller()
        restored.importLayout(controller.exportLayout())
        assertTrue(restored.uiState.layout.isEmpty())
        assertEquals(MetricGridSpec.PortraitDefault, restored.uiState.portraitSpec)
        assertEquals(MetricGridSpec.DockDefault, restored.uiState.dockSpec)
    }

    /** 存檔可能是上一版寫的，認不出來的部分各自退回預設而不是整個崩掉。 */
    @Test
    fun `a malformed save falls back to the defaults`() {
        val restored = controller()
        restored.importLayout("garbage")
        assertEquals(MetricGridSpec.PortraitDefault, restored.uiState.portraitSpec)
        assertEquals(MetricGridSpec.DockDefault, restored.uiState.dockSpec)
        assertTrue(restored.uiState.layout.isEmpty())
    }

    /** 不在候選裡的規格（候選改過了）也要退回預設。 */
    @Test
    fun `a spec that is no longer offered falls back`() {
        val restored = controller()
        restored.importLayout("9x9|9x9|")
        assertEquals(MetricGridSpec.PortraitDefault, restored.uiState.portraitSpec)
        assertEquals(MetricGridSpec.DockDefault, restored.uiState.dockSpec)
    }
}
