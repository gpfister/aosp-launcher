package org.gpfister.aosp.gplauncher.data

import android.content.Intent
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KioskLauncherTest {
    @Test
    fun `no intent when kiosk is absent`() {
        val pm = mockk<PackageManager> { every { getLaunchIntentForPackage(KIOSK_PACKAGE) } returns null }
        assertNull(KioskLauncher(pm).launchIntent())
    }

    @Test
    fun `intent when kiosk is present`() {
        val pm = mockk<PackageManager> {
            every { getLaunchIntentForPackage(KIOSK_PACKAGE) } returns mockk<Intent>(relaxed = true)
        }
        assertNotNull(KioskLauncher(pm).launchIntent())
    }
}
