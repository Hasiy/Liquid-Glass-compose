package top.hasiy.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** 以真正繪圖像素驗證座標；只看 semantics bounds 無法抓到鏡片副本被置中的回歸。 */
class GlassLensTabBarTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun lensClip_scalesContentAroundItsOwnCenter() {
        val selected = mutableIntStateOf(0)
        compose.setContent {
            Box(Modifier.background(Color.White)) {
                GlassLensTabBar(
                    items = List(4) { GlassLensTabItem("tab$it", "tab$it", Icons.Default.Home) },
                    selectedIndex = selected.intValue,
                    onSelect = { selected.intValue = it },
                    modifier = Modifier.width(400.dp).testTag("bar"),
                    contentColor = Color.Black,
                )
            }
        }
        val bar = compose.onNodeWithTag("bar")
        val before = bar.captureToImage()
        val width = before.width.toFloat()
        val sourceCenter = iconCenterX(before, 0f, width / 4f)
        assertEquals(width / 8f, sourceCenter, 2f)
        val untouched = iconCenterX(before, width / 2f, width * 3f / 4f)

        bar.performTouchInput {
            down(Offset(width / 8f, centerY))
            moveTo(Offset(width * 0.20f, centerY), delayMillis = 100)
        }
        compose.waitForIdle()
        val during = bar.captureToImage()
        val actual = iconCenterX(during, 0f, width / 4f)
        // 只有鏡片內的內容以鏡片中心 0.20w 放大；外部內容不重繪也不位移。
        val lensCenter = width * 0.20f
        val expected = lensCenter + (sourceCenter - lensCenter) * GlassLensTabBarDefaults.MAGNIFICATION
        assertEquals(expected, actual, 2f)
        assertTrue("Icon must not follow the lens center", actual < width * 0.15f)
        assertEquals(untouched, iconCenterX(during, width / 2f, width * 3f / 4f), 1f)
        compose.onNodeWithText("tab0").assertIsSelected()
        assertEquals(0, selected.intValue)

        bar.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(sourceCenter, iconCenterX(bar.captureToImage(), 0f, width / 4f), 2f)
    }

    @Test
    fun click_lastTab_centersMagnificationOnSelectedSlot() {
        val selected = mutableIntStateOf(0)
        compose.setContent {
            Box(Modifier.background(Color.White)) {
                GlassLensTabBar(
                    List(4) { GlassLensTabItem("tab$it", "tab$it", Icons.Default.Home) },
                    selected.intValue, { selected.intValue = it },
                    Modifier.width(400.dp).testTag("bar"),
                    contentColor = Color.Black,
                )
            }
        }
        compose.onNodeWithText("tab3").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("tab3").assertIsSelected()
        val bitmap = compose.onNodeWithTag("bar").captureToImage()
        assertEquals(bitmap.width * 7f / 8f,
            iconCenterX(bitmap, bitmap.width * 3f / 4f, bitmap.width.toFloat()), 2f)
    }

    @Test
    fun release_overLastItem_selectsDraggedToItem() {
        val selected = mutableIntStateOf(0)
        compose.setContent {
            GlassLensTabBar(
                List(4) { GlassLensTabItem("tab$it", "tab$it", Icons.Default.Home) },
                selected.intValue,
                { selected.intValue = it },
                Modifier.width(400.dp).testTag("bar"),
            )
        }

        val bar = compose.onNodeWithTag("bar")
        bar.performTouchInput {
            down(Offset(size.width / 8f, centerY))
            moveTo(Offset(size.width * 7f / 8f, centerY), delayMillis = 100)
            up()
        }
        compose.waitForIdle()

        assertEquals(3, selected.intValue)
        compose.onNodeWithText("tab3").assertIsSelected()
    }

    @Test
    fun centerAction_usesTheSameSelectedLensState() {
        val centerSelected = mutableStateOf(false)
        compose.setContent {
            GlassLensTabBar(
                items = List(4) { GlassLensTabItem("tab$it", "tab$it", Icons.Default.Home) },
                selectedIndex = 0,
                onSelect = { centerSelected.value = false },
                centerAction = { Icon(Icons.Default.Home, contentDescription = null) },
                centerActionSelected = centerSelected.value,
                onCenterActionClick = { centerSelected.value = true },
                centerActionDescription = "center action",
                modifier = Modifier.width(400.dp).testTag("bar"),
            )
        }

        compose.onNodeWithContentDescription("center action").performClick().assertIsSelected()
        assertTrue(centerSelected.value)
    }

    private fun iconCenterX(image: ImageBitmap, left: Float, right: Float): Float {
        val pixels = image.toPixelMap()
        var sum = 0f
        var count = 0
        // 上半部只有圖示；亮色鏡片邊框不會被計為黑色內容。
        for (y in 0 until image.height / 2) {
            for (x in left.toInt() until right.toInt()) {
                val pixel = pixels[x, y]
                if (pixel.alpha > 0.9f && pixel.red < 0.4f && pixel.green < 0.4f && pixel.blue < 0.4f) {
                    sum += x + 0.5f
                    count++
                }
            }
        }
        assertTrue("Expected visible icon pixels in the original slot", count > 0)
        return sum / count
    }
}
