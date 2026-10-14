/* Copyright 2026 FitDash contributors. */
package top.hasiy.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GlassSelectionComponentsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun slider_recomposition_usesLatestCallbacksAndRange() {
        val generation = mutableIntStateOf(0)
        var oldCalls = 0
        var newCalls = 0
        var finished = -1
        var emitted = 0f
        compose.setContent {
            val current = generation.intValue
            GlassSlider(
                value = if (current == 0) 0.5f else 15f,
                valueRange = if (current == 0) 0f..1f else 10f..20f,
                onValueChange = { emitted = it; if (current == 0) oldCalls++ else newCalls++ },
                onValueChangeFinished = { finished = current },
                modifier = Modifier.width(if (current == 0) 300.dp else 400.dp).testTag("slider"),
            )
        }
        compose.runOnIdle { generation.intValue = 1 }
        compose.onNodeWithTag("slider").performTouchInput {
            click(Offset(width * 0.75f, centerY))
        }
        compose.runOnIdle {
            assertEquals(0, oldCalls)
            assertTrue(newCalls > 0)
            assertTrue(emitted in 17f..18f)
            assertEquals(1, finished)
        }
    }

    @Test fun controls_semantics_exposeSelectionAndProgress() {
        val checked = mutableStateOf(false)
        val selected = mutableStateOf(false)
        val switched = mutableStateOf(false)
        val value = mutableFloatStateOf(0f)
        var finished = 0
        compose.setContent {
            Column {
                GlassCheckbox(checked.value, { checked.value = it }, Modifier.testTag("check"))
                GlassRadioButton(selected.value, { selected.value = true }, Modifier.testTag("radio"))
                GlassSwitch(switched.value, { switched.value = it }, Modifier.testTag("switch"))
                GlassSlider(value.floatValue, { value.floatValue = it }, Modifier.width(300.dp).testTag("slider"),
                    steps = 3, onValueChangeFinished = { finished++ })
                GlassVolumeSlider(0.5f, {}, Modifier.width(300.dp).testTag("disabled"), enabled = false)
            }
        }
        compose.onNodeWithTag("check").assertIsToggleable().performClick().assertIsOn()
        compose.onNodeWithTag("radio").performClick().assertIsSelected()
        compose.onNodeWithTag("switch").assertIsToggleable().performClick().assertIsOn()
        compose.onNodeWithTag("slider").performSemanticsAction(SemanticsActions.SetProgress) { it(0.6f) }
        compose.runOnIdle {
            assertEquals(0.5f, value.floatValue, 0.001f)
            assertEquals(1, finished)
        }
        compose.onNodeWithTag("disabled").assertIsNotEnabled()
    }

    @Test fun segmentedTab_rtlDrag_selectsLogicalFirstItemWithLatestCallback() {
        val generation = mutableIntStateOf(0)
        var selected = -1
        var callbackGeneration = -1
        compose.setContent {
            val current = generation.intValue
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                GlassSegmentedTabBar(listOf("first", "second", "third"), 1,
                    { selected = it; callbackGeneration = current }, Modifier.width(300.dp).testTag("tabs"))
            }
        }
        compose.runOnIdle { generation.intValue = 1 }
        compose.onNodeWithTag("tabs").performTouchInput {
            down(Offset(width / 2f, centerY))
            moveTo(Offset(width * 0.9f, centerY), delayMillis = 100)
            up()
        }
        compose.runOnIdle {
            assertEquals(0, selected)
            assertEquals(1, callbackGeneration)
        }
        compose.onNodeWithText("second").assertIsSelected()
    }

    @Test fun slider_disablingDuringDrag_doesNotSubmitCancelledGesture() {
        val enabled = mutableStateOf(true)
        val value = mutableFloatStateOf(0.5f)
        var finished = 0
        compose.setContent {
            GlassSlider(value.floatValue, { value.floatValue = it },
                Modifier.width(300.dp).testTag("slider"), enabled = enabled.value,
                onValueChangeFinished = { finished++ })
        }
        compose.onNodeWithTag("slider").performTouchInput {
            down(Offset(width / 2f, centerY))
            moveTo(Offset(width * 0.8f, centerY), delayMillis = 100)
        }
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithTag("slider").performTouchInput { up() }
        compose.runOnIdle { assertEquals(0, finished) }
    }
}
