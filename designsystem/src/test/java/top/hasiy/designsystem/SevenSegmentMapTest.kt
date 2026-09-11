package top.hasiy.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 七段映射表。
 *
 * 这张表是逐字抄参考稿 `sevenSegmentMap` 的，抄错一个段在屏幕上就是一个畸形数字，
 * 而且只有那一个数字出现时才看得到——用测试钉住比靠眼睛靠谱。
 */
class SevenSegmentMapTest {

    /** 每个数字点亮的段数，是七段管的常识值。 */
    @Test
    fun `each digit lights the expected number of segments`() {
        val expected = mapOf(
            '0' to 6, '1' to 2, '2' to 5, '3' to 5, '4' to 4,
            '5' to 5, '6' to 6, '7' to 3, '8' to 7, '9' to 6,
        )
        expected.forEach { (digit, count) ->
            assertEquals("$digit 的段数", count, sevenSegmentsFor(digit)!!.length)
        }
    }

    /** 1 只亮右侧两段，8 全亮——两端的极值最容易暴露抄错。 */
    @Test
    fun `boundary digits map to the right segments`() {
        assertEquals("bc", sevenSegmentsFor('1'))
        assertEquals("abcdefg".toSet(), sevenSegmentsFor('8')!!.toSet())
        assertEquals("g", sevenSegmentsFor('-'))
    }

    /** 只能出现 a–g，抄错成别的字母不会报错，只会静默少画一段。 */
    @Test
    fun `every entry only references segments a through g`() {
        ('0'..'9').forEach { digit ->
            val segments = sevenSegmentsFor(digit)!!
            assertTrue("$digit 含非法段：$segments", segments.all { it in 'a'..'g' })
            assertEquals("$digit 有重复段：$segments", segments.length, segments.toSet().size)
        }
    }

    /** 标点与单位不走七段，交给普通文本。 */
    @Test
    fun `punctuation and letters are not digits`() {
        listOf('.', ':', ',', ' ', 'k', '级').forEach {
            assertNull("$it 不该有段", sevenSegmentsFor(it))
        }
    }
}
