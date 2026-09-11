package top.hasiyliquidglassdemo.ui.workout

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 儀表環的角度換算。
 *
 * spec 3.4 給的是從 12 點鐘量的角度，Compose 從 3 點鐘量——這組測試把兩邊
 * 釘在一起，避免以後有人「順手」把 -90 拿掉。
 */
class WorkoutGaugeGeometryTest {

    /** spec：起始 218°，鋪滿 286°。 */
    @Test
    fun `arc starts at the spec angle converted to the canvas frame`() {
        assertEquals(218f - 90f, START_ANGLE_DEG, 0.001f)
        assertEquals(286f, SWEEP_DEG, 0.001f)
    }

    /**
     * 缺口落在正下方附近，但不是正中。
     *
     * 218° 起、286° 掃，缺口是 74°，中點落在 91°——比 6 點鐘偏 1°。要完全對稱
     * 起點得是 217°。這是參考稿自己的值，依「保留參考稿數值」的決定原樣照抄，
     * 不悄悄改成 217；這條測試把這 1° 記下來，免得日後被當成 bug 修掉。
     */
    @Test
    fun `the gap sits just off six o clock, exactly as the reference has it`() {
        val end = START_ANGLE_DEG + SWEEP_DEG
        // 畫布上 90° 就是 6 點鐘；缺口中點 = 終點與（起點 + 360）的中間
        val gapCentre = ((end + (START_ANGLE_DEG + 360f)) / 2f) % 360f
        assertEquals(91f, gapCentre, 0.001f)
        assertEquals("缺口總共 74°", 74f, 360f - SWEEP_DEG, 0.001f)
    }

    /** spec：主刻度 47.667°、小刻度 9.533°。 */
    @Test
    fun `tick steps match the spec`() {
        assertEquals(47.667f, MAJOR_STEP_DEG, 0.001f)
        assertEquals(9.533f, MINOR_STEP_DEG, 0.001f)
    }

    /** 最後一根小刻度要正好落在弧的終點。 */
    @Test
    fun `the last minor tick lands on the end of the arc`() {
        val last = START_ANGLE_DEG + MINOR_TICK_COUNT * MINOR_STEP_DEG
        assertEquals(START_ANGLE_DEG + SWEEP_DEG, last, 0.001f)
    }

    /** 主刻度要落在小刻度上，不能各畫各的。 */
    @Test
    fun `major ticks sit on minor tick positions`() {
        repeat(MAJOR_TICK_COUNT) { index ->
            val major = index * MAJOR_STEP_DEG
            val minor = index * 5 * MINOR_STEP_DEG
            assertEquals("第 $index 根主刻度", minor, major, 0.001f)
        }
    }

    @Test
    fun `speed maps onto the arc and clamps outside the range`() {
        assertEquals(0f, fractionOf(0f), 0.001f)
        assertEquals(0.5f, fractionOf(15f), 0.001f)
        assertEquals(1f, fractionOf(30f), 0.001f)
        assertEquals("低於量程要夾到起點", 0f, fractionOf(-4f), 0.001f)
        assertEquals("高於量程要夾到終點", 1f, fractionOf(99f), 0.001f)
    }

    @Test
    fun `gauge angle spans exactly the sweep across the range`() {
        assertEquals(START_ANGLE_DEG, gaugeAngleOf(SPEED_MIN), 0.001f)
        assertEquals(START_ANGLE_DEG + SWEEP_DEG, gaugeAngleOf(SPEED_MAX), 0.001f)
    }
}
