package org.gpfister.aosp.gplauncher.data

/**
 * While active, the launcher does not start the kiosk app, so the fallback screen stays reachable.
 * In-memory on purpose: it resets when the launcher process restarts or the device reboots.
 */
object MaintenanceMode {
    @Volatile
    var active: Boolean = false
}
