package org.gpfister.aosp.gplauncher

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.gpfister.aosp.gplauncher.admin.PowerController
import org.gpfister.aosp.gplauncher.data.DeviceInfoRepository
import org.gpfister.aosp.gplauncher.update.PackageInstallerAppUpdater
import org.gpfister.aosp.gplauncher.update.UpdateCoordinator
import org.gpfister.aosp.gplauncher.update.UpdateEngineSystemUpdater

/** Process-wide singletons. The future WebSocket client feeds [updateCoordinator]. */
class GPLauncherApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val deviceInfoRepository by lazy { DeviceInfoRepository(this) }
    val powerController by lazy { PowerController(this) }

    val updateCoordinator by lazy {
        UpdateCoordinator(
            scope = appScope,
            systemUpdater = UpdateEngineSystemUpdater(),
            appUpdater = PackageInstallerAppUpdater(this),
            reboot = powerController::reboot,
        )
    }
}
