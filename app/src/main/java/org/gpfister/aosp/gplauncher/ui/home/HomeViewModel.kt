package org.gpfister.aosp.gplauncher.ui.home

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.gpfister.aosp.gplauncher.GPLauncherApplication
import org.gpfister.aosp.gplauncher.HotZoneService
import org.gpfister.aosp.gplauncher.data.DeviceInfo
import org.gpfister.aosp.gplauncher.data.KioskLauncher
import org.gpfister.aosp.gplauncher.data.MaintenanceMode
import org.gpfister.aosp.gplauncher.update.UpdateState

data class HomeUiState(
    val info: DeviceInfo? = null,
    val update: UpdateState = UpdateState.Idle,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as GPLauncherApplication
    private val info = MutableStateFlow<DeviceInfo?>(null)

    val state: StateFlow<HomeUiState> =
        combine(info, app.updateCoordinator.state) { i, u -> HomeUiState(i, u) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() {
        viewModelScope.launch {
            info.value = withContext(Dispatchers.IO) { app.deviceInfoRepository.load() }
        }
    }

    /** Leaves maintenance mode and starts the kiosk (with its exit hot zone). */
    fun startKiosk() {
        val intent = KioskLauncher(app.packageManager).launchIntent() ?: return
        MaintenanceMode.active = false
        HotZoneService.start(app)
        app.startActivity(intent)
    }

    fun reboot() = app.powerController.reboot()

    fun powerOff() = app.powerController.powerOff()

    /** Intent for Android Settings. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Intent for the platform Files app (DocumentsUI), or null if it is missing. */
    fun filesIntent(): Intent? = app.packageManager
        .getLaunchIntentForPackage(FILES_PACKAGE)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private companion object {
        const val FILES_PACKAGE = "com.android.documentsui"
    }
}
