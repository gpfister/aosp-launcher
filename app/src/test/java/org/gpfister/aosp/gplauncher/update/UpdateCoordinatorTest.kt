package org.gpfister.aosp.gplauncher.update

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateCoordinatorTest {
    private class FakeSystem(var failure: Exception? = null) : SystemUpdater {
        var applied = 0
        override suspend fun apply(command: UpdateCommand.SystemUpdate, onProgress: (Float) -> Unit) {
            failure?.let { throw it }
            onProgress(0.5f)
            applied++
        }
    }

    private class FakeApps : AppUpdater {
        val installed = mutableListOf<String>()
        override suspend fun install(command: UpdateCommand.AppUpdate, onProgress: (Float) -> Unit) {
            installed += command.packageName
        }
    }

    private fun TestScope.coordinator(system: SystemUpdater, apps: AppUpdater, onReboot: () -> Unit) =
        UpdateCoordinator(
            scope = this,
            systemUpdater = system,
            appUpdater = apps,
            reboot = onReboot,
            now = { Instant.ofEpochMilli(currentTime) },
        )

    @Test
    fun `system update reboots after apply`() = runTest(StandardTestDispatcher()) {
        val system = FakeSystem()
        var rebooted = 0
        val c = coordinator(system, FakeApps()) { rebooted++ }
        c.submit(UpdateCommand.SystemUpdate(url = "http://x/payload.bin"))
        advanceUntilIdle()
        assertEquals(1, system.applied)
        assertEquals(1, rebooted)
        assertEquals(UpdateState.Rebooting, c.state.value)
    }

    @Test
    fun `scheduled update waits for installAt`() = runTest(StandardTestDispatcher()) {
        val apps = FakeApps()
        val c = coordinator(FakeSystem(), apps) {}
        c.submit(UpdateCommand.AppUpdate("a.b", "http://x/a.apk", "00", Instant.ofEpochMilli(10_000)))
        advanceUntilIdle()
        assertEquals(10_000L, currentTime)
        assertEquals(listOf("a.b"), apps.installed)
        assertEquals(UpdateState.Idle, c.state.value)
    }

    @Test
    fun `failure is reported and does not reboot`() = runTest(StandardTestDispatcher()) {
        var rebooted = 0
        val c = coordinator(FakeSystem(IllegalStateException("boom")), FakeApps()) { rebooted++ }
        c.submit(UpdateCommand.SystemUpdate(url = "http://x/payload.bin"))
        advanceUntilIdle()
        val state = c.state.value
        assertTrue(state is UpdateState.Failed && state.reason == "boom")
        assertEquals(0, rebooted)
    }
}
