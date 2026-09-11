package top.hasiyliquidglassdemo.ui.workout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [WorkoutController] 的狀態機。
 *
 * 對應計劃裡階段 4 的三條驗收標準：每種狀態都能重複進入與退出、
 * 不允許在斷連／只讀／暫停下誤觸發調節、滑動結束未達閾值不會結束會話。
 *
 * 用 Unconfined 讓 `scope.launch` 同步跑到第一個 `delay` 為止——
 * 這樣寫入的**樂觀更新**部分能立刻驗證，延遲之後的收尾不在這組測試範圍。
 */
class WorkoutControllerTest {

    private fun controller(
        device: WorkoutDeviceCapability = WorkoutDeviceCapability.IndoorBike,
    ) = WorkoutController(CoroutineScope(Dispatchers.Unconfined), device)

    // ---------- 狀態轉換 ----------

    @Test
    fun `pause and resume are repeatable`() {
        val c = controller()
        repeat(3) {
            c.pause()
            assertEquals(WorkoutRuntimeState.PAUSED, c.uiState.runtime)
            c.resume()
            assertEquals(WorkoutRuntimeState.ACTIVE, c.uiState.runtime)
        }
    }

    @Test
    fun `disconnect reconnecting and reconnected form a loop`() {
        val c = controller()
        repeat(2) {
            c.disconnect()
            assertEquals(WorkoutRuntimeState.DISCONNECTED, c.uiState.runtime)
            c.reconnecting()
            assertEquals(WorkoutRuntimeState.RECONNECTING, c.uiState.runtime)
            c.reconnected()
            assertEquals(WorkoutRuntimeState.ACTIVE, c.uiState.runtime)
        }
    }

    /** 暫停時要把調節浮層收掉——凍結狀態下不該還留著一個能拖的滑桿。 */
    @Test
    fun `pausing closes the adjust panel`() {
        val c = controller()
        c.beginAdjust("portrait-4")
        assertEquals("portrait-4", c.uiState.adjustingSlotKey)
        c.pause()
        assertNull(c.uiState.adjustingSlotKey)
    }

    @Test
    fun `resume does nothing unless paused`() {
        val c = controller()
        c.disconnect()
        c.resume()
        assertEquals(WorkoutRuntimeState.DISCONNECTED, c.uiState.runtime)
    }

    // ---------- 不允許誤觸發調節 ----------

    @Test
    fun `adjust is refused while offline paused or ended`() {
        listOf(
            WorkoutRuntimeState.DISCONNECTED to { c: WorkoutController -> c.disconnect() },
            WorkoutRuntimeState.PAUSED to { c: WorkoutController -> c.pause() },
            WorkoutRuntimeState.ENDED to { c: WorkoutController -> c.end() },
        ).forEach { (expected, enter) ->
            val c = controller()
            enter(c)
            assertEquals(expected, c.uiState.runtime)
            assertFalse("$expected 不該可調", c.uiState.canAdjustAt("portrait-4"))
            c.beginAdjust("portrait-4")
            assertNull("$expected 不該開得了調節浮層", c.uiState.adjustingSlotKey)
        }
    }

    @Test
    fun `read only device refuses adjustment even while active`() {
        val c = controller(WorkoutDeviceCapability.ReadOnlyBike)
        assertEquals(WorkoutRuntimeState.ACTIVE, c.uiState.runtime)
        assertFalse(c.uiState.canAdjustAt("portrait-4"))
        c.beginAdjust("portrait-4")
        assertNull(c.uiState.adjustingSlotKey)
    }

    /** 只讀裝置與「暫時不能調」是兩種提示，不能混用。 */
    @Test
    fun `read only reason distinguishes device from runtime`() {
        val readOnly = controller(WorkoutDeviceCapability.ReadOnlyBike)
        assertEquals(ReadOnlyReason.DEVICE_READ_ONLY, readOnly.uiState.readOnlyReasonAt("portrait-4"))

        val offline = controller()
        offline.disconnect()
        assertEquals(ReadOnlyReason.TEMPORARILY_LOCKED, offline.uiState.readOnlyReasonAt("portrait-4"))

        val active = controller()
        assertNull("可調的指標在 ACTIVE 下不該有提示", active.uiState.readOnlyReasonAt("portrait-4"))
    }

    /**
     * 目錄把目標速度標成可調，但室內單車只支援阻力。
     *
     * 指標卡的 ± 標記要跟著裝置走：問目錄的話，換一台機器就會擺出一個按不動的
     * 調節入口。
     */
    @Test
    fun `device capability decides which metrics look adjustable`() {
        val bike = controller().uiState
        assertTrue("阻力在室內單車上可調", bike.device.isControllable("resistance"))
        assertFalse("目標速度在室內單車上不可調", bike.device.isControllable("target-speed"))
        assertTrue("但目錄層仍標成可調", MetricCatalog.find("target-speed")!!.controllable)

        val mill = WorkoutController(CoroutineScope(Dispatchers.Unconfined), WorkoutDeviceCapability.Treadmill)
        assertTrue("跑步機兩個都可調", mill.uiState.device.isControllable("target-speed"))
    }

    /** 本來就不可調的指標（熱量）不該被說成「裝置只讀」。 */
    @Test
    fun `non controllable metric has no read only hint on a normal device`() {
        val c = controller()
        assertNull(c.uiState.readOnlyReasonAt("portrait-5"))
    }

    /**
     * 只讀裝置上也一樣：只有被降級的控制項掛提示。
     *
     * 少了這條，只讀裝置會讓每一張卡都寫上「設備只讀」，整面板看起來像壞了。
     */
    @Test
    fun `read only device only marks the metrics it downgraded`() {
        val c = controller(WorkoutDeviceCapability.ReadOnlyBike)
        assertEquals(
            "阻力本來可調，被降級了",
            ReadOnlyReason.DEVICE_READ_ONLY,
            c.uiState.readOnlyReasonAt("portrait-4"),
        )
        assertNull("熱量在任何裝置上都只是讀數", c.uiState.readOnlyReasonAt("portrait-5"))
        assertNull("距離同理", c.uiState.readOnlyReasonAt("portrait-3"))
    }

    @Test
    fun `write is ignored when the device cannot be adjusted`() {
        val c = controller(WorkoutDeviceCapability.ReadOnlyBike)
        c.writeValue("resistance", 12f)
        assertTrue("不該寫進任何覆蓋值", c.uiState.overrides.isEmpty())
        assertEquals(ControlWriteState.Idle, c.uiState.controlWrite)
    }

    // ---------- 寫入 ----------

    @Test
    fun `write updates optimistically and enters writing`() {
        val c = controller()
        c.writeValue("resistance", 12f)
        assertEquals("12", c.uiState.overrides["resistance"])
        val state = c.uiState.controlWrite
        assertTrue(state is ControlWriteState.Writing)
        assertEquals("12", (state as ControlWriteState.Writing).target)
    }

    @Test
    fun `write snaps to the range`() {
        val c = controller()
        c.writeValue("resistance", 99f)
        assertEquals("16", c.uiState.overrides["resistance"])
        c.writeValue("resistance", -5f)
        assertEquals("1", c.uiState.overrides["resistance"])
    }

    @Test
    fun `step moves by exactly one increment`() {
        val c = controller()
        c.stepValue("resistance", +1)
        assertEquals("9", c.uiState.overrides["resistance"])
        c.stepValue("resistance", -1)
        assertEquals("8", c.uiState.overrides["resistance"])
    }

    /**
     * 滑桿的分隔點數。
     *
     * 目標速度是 (20 − 0.5) / 0.1，浮點算出來是 194.99998，用截斷會少一格，
     * 滑桿最右端就到不了 20.0。
     */
    @Test
    fun `slider step count survives floating point division`() {
        assertEquals("阻力 1–16 步進 1：15 段、14 個內部分隔點", 14, ControlRange.Resistance.discreteSteps)
        assertEquals("目標速度 0.5–20.0 步進 0.1：195 段、194 個分隔點", 194, ControlRange.TargetSpeed.discreteSteps)
    }

    /** 分隔點對得上量程兩端，否則滑桿拖到底也到不了最大值。 */
    @Test
    fun `the last slider step lands on the maximum`() {
        listOf(ControlRange.Resistance, ControlRange.TargetSpeed).forEach { range ->
            assertEquals(range.max, range.valueAt(1f), 0.001f)
            assertEquals(range.min, range.valueAt(0f), 0.001f)
        }
    }

    /** 模擬結果是給場景切換用的開關，預設仍是成功。 */
    @Test
    fun `simulated outcome defaults to success and is switchable`() {
        val c = controller()
        assertEquals(WriteOutcome.SUCCESS, c.simulatedOutcome)
        c.simulatedOutcome = WriteOutcome.REJECTED
        assertEquals(WriteOutcome.REJECTED, c.simulatedOutcome)
    }

    // ---------- 編輯抽屜 ----------

    /** 候選項由裝置能力提供：未支援的指標不出現，也不用 0 佔位。 */
    @Test
    fun `candidates come from device capability`() {
        val limited = WorkoutDeviceCapability(
            name = "简易单车",
            supportedMetricIds = setOf("heart", "cadence", "avg-heart"),
        )
        val c = WorkoutController(CoroutineScope(Dispatchers.Unconfined), limited)
        val realtime = c.uiState.candidatesIn(MetricCategory.REALTIME).map { it.id }
        assertEquals(listOf("heart", "cadence"), realtime)
        assertEquals(listOf("avg-heart"), c.uiState.candidatesIn(MetricCategory.AVERAGE).map { it.id })
        assertTrue("不支援的分類要是空的", c.uiState.candidatesIn(MetricCategory.AGGREGATE).isEmpty())
    }

    /** 「已在其他位置显示」只比同一族的資料位。 */
    @Test
    fun `used elsewhere only compares within the same layout family`() {
        val c = controller()
        // 豎屏預設：portrait-1 是心率
        assertTrue(c.uiState.isMetricUsedElsewhere("portrait-0", "heart"))
        assertFalse("自己那一格不算佔用", c.uiState.isMetricUsedElsewhere("portrait-1", "heart"))
        // 步數只出現在橫屏那一族（dock 第 7 格），豎屏的候選項不該標成已佔用。
        //
        // 反過來挑「只在豎屏有」的指標已經挑不出來了：側邊擴到 6 格之後，
        // 豎屏那 6 個預設值在橫屏那一族裡都能找到。
        assertTrue("dock 第 7 格是步數", c.uiState.isMetricUsedElsewhere("land-dock-0", "steps"))
        assertFalse("換到豎屏這一族就不算佔用", c.uiState.isMetricUsedElsewhere("portrait-0", "steps"))
    }

    /** 抽屜要能靠 key 找回資料位，否則開不出標題。 */
    @Test
    fun `editing slot resolves from the key`() {
        val c = controller()
        assertNull(c.uiState.editingSlot)
        c.beginEdit("land-dock-5")
        assertEquals("land-dock-5", c.uiState.editingSlot?.key)
    }

    // ---------- 滑動結束 ----------

    @Test
    fun `slide below the threshold does not end the session`() {
        val c = controller()
        val reached = c.updateSlideEnd(SLIDE_END_THRESHOLD - 0.05f)
        assertFalse(reached)
        c.releaseSlideEnd()
        assertEquals(WorkoutRuntimeState.ACTIVE, c.uiState.runtime)
        assertEquals("鬆手要彈回起點", 0f, c.uiState.slideEndProgress, 0.001f)
    }

    @Test
    fun `slide at the threshold ends the session`() {
        val c = controller()
        assertTrue(c.updateSlideEnd(SLIDE_END_THRESHOLD))
        c.releaseSlideEnd()
        assertEquals(WorkoutRuntimeState.ENDED, c.uiState.runtime)
    }

    // ---------- 版面 ----------

    @Test
    fun `assigning a metric overrides the default layout`() {
        val c = controller()
        assertEquals("resistance", c.uiState.metricAt("portrait-4")?.id)
        c.assignMetric("portrait-4", "cadence")
        assertEquals("cadence", c.uiState.metricAt("portrait-4")?.id)
        assertNull("指派後要關掉抽屜", c.uiState.editingSlotKey)
    }

    @Test
    fun `reset restores the default layout`() {
        val c = controller()
        c.assignMetric("portrait-0", "steps")
        c.resetLayout()
        assertTrue(c.uiState.layout.isEmpty())
        assertEquals("power", c.uiState.metricAt("portrait-0")?.id)
    }

    @Test
    fun `group index cycles between two groups`() {
        val c = controller()
        assertEquals(0, c.uiState.groupIndex)
        c.switchGroup()
        assertEquals(1, c.uiState.groupIndex)
        c.switchGroup()
        assertEquals(0, c.uiState.groupIndex)
    }

    @Test
    fun `switching group swaps the portrait and dock metrics`() {
        val c = controller()
        assertEquals("power", c.uiState.metricAt("portrait-0")?.id)
        assertEquals("distance", c.uiState.metricAt("land-dock-0")?.id)
        c.switchGroup()
        assertEquals("avg-speed", c.uiState.metricAt("portrait-0")?.id)
        assertEquals("avg-speed", c.uiState.metricAt("land-dock-0")?.id)
    }

    /** 橫屏側邊的大卡是這台裝置的主要讀數，切組時留在原位。 */
    @Test
    fun `switching group leaves the landscape side cards alone`() {
        val c = controller()
        c.switchGroup()
        assertEquals("duration", c.uiState.metricAt("land-side-0")?.id)
        assertEquals("heart", c.uiState.metricAt("land-side-1")?.id)
    }

    /** 在主組改過的格子不該把第 2 組的同一格也改掉。 */
    @Test
    fun `layout override is scoped to the group it was made in`() {
        val c = controller()
        c.assignMetric("portrait-0", "steps")
        assertEquals("steps", c.uiState.metricAt("portrait-0")?.id)
        c.switchGroup()
        assertEquals("avg-speed", c.uiState.metricAt("portrait-0")?.id)
        c.switchGroup()
        assertEquals("steps", c.uiState.metricAt("portrait-0")?.id)
    }

    /** 斷線時保留最後數值，不清零也不顯示佔位。 */
    @Test
    fun `values survive a disconnect`() {
        val c = controller()
        c.writeValue("resistance", 11f)
        c.disconnect()
        val metric = c.uiState.metricAt("portrait-4")!!
        assertEquals("11", c.uiState.valueOf(metric))
    }
}
