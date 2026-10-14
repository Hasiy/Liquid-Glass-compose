/* Copyright 2026 FitDash contributors. */
package top.hasiyliquidglassdemo.ui.workout

import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Exercise acknowledgement, rollback and cancellation with a virtual clock. */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutControllerAsyncTest {
    @Test fun writeValue_success_confirmsValue() = runTest {
        val controller = WorkoutController(this)
        controller.writeValue("resistance", 11f)
        advanceUntilIdle()
        controller.disconnect()
        assertEquals(11f, controller.uiState.overrides["resistance"])
        assertEquals(ControlWriteState.Idle, controller.uiState.controlWrite)
    }

    @Test fun writeValue_failureAndRejection_restoreAcknowledgedValue() = runTest {
        val controller = WorkoutController(this)
        controller.writeValue("resistance", 10f)
        advanceUntilIdle()
        listOf(WriteOutcome.FAILED, WriteOutcome.REJECTED).forEach { outcome ->
            controller.writeValue("resistance", 13f, outcome)
            runCurrent()
            advanceTimeBy(900)
            runCurrent()
            assertEquals(10f, controller.uiState.overrides["resistance"])
            assertTrue(controller.uiState.controlWrite is ControlWriteState.Failed ||
                controller.uiState.controlWrite is ControlWriteState.Rejected)
            advanceUntilIdle()
            assertEquals(ControlWriteState.Idle, controller.uiState.controlWrite)
        }
    }

    @Test fun writeValue_supersededPreview_doesNotBecomeRollbackBaseline() = runTest {
        val controller = WorkoutController(this)
        controller.writeValue("resistance", 10f)
        advanceUntilIdle()
        controller.writeValue("resistance", 12f)
        runCurrent()
        advanceTimeBy(100)
        controller.writeValue("resistance", 13f, WriteOutcome.FAILED)
        advanceUntilIdle()
        assertEquals(10f, controller.uiState.overrides["resistance"])
    }

    @Test fun writeValue_switchMetric_restoresCancelledMetric() = runTest {
        val controller = WorkoutController(this, WorkoutDeviceCapability.Treadmill)
        controller.writeValue("resistance", 12f)
        runCurrent()
        controller.writeValue("target-speed", 13f)
        advanceUntilIdle()
        assertTrue("Cancelled resistance preview must be removed", "resistance" !in controller.uiState.overrides)
        assertEquals(13f, controller.uiState.overrides["target-speed"])
    }

    @Test fun writeValue_runtimeChanges_cancelPendingWrite() = runTest {
        val changes: List<(WorkoutController) -> Unit> = listOf(
            { it.pause() }, { it.disconnect() }, { it.end() }, { it.useDevice(WorkoutDeviceCapability.ReadOnlyBike) },
        )
        changes.forEach { change ->
            val controller = WorkoutController(this)
            controller.writeValue("resistance", 12f)
            runCurrent()
            change(controller)
            advanceUntilIdle()
            assertTrue(controller.uiState.overrides.isEmpty())
            assertEquals(ControlWriteState.Idle, controller.uiState.controlWrite)
        }
    }

    @Test fun stepValue_germanLocale_usesRawValue() = runTest {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val controller = WorkoutController(this, WorkoutDeviceCapability.Treadmill)
            controller.writeValue("target-speed", 12.3f)
            advanceUntilIdle()
            val metric = MetricCatalog.find("target-speed")!!
            assertEquals("12,3", controller.uiState.valueOf(metric))
            controller.stepValue("target-speed", 1)
            advanceUntilIdle()
            assertEquals(12.4f, controller.uiState.numericValueOf(metric)!!, 0.001f)
            assertEquals("12,4", controller.uiState.valueOf(metric))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
