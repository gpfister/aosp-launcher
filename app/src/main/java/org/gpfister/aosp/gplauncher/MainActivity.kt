package org.gpfister.aosp.gplauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import org.gpfister.aosp.gplauncher.data.KioskLauncher
import org.gpfister.aosp.gplauncher.data.MaintenanceMode
import org.gpfister.aosp.gplauncher.ui.home.HomeScreen
import org.gpfister.aosp.gplauncher.ui.home.HomeViewModel
import org.gpfister.aosp.gplauncher.ui.theme.GPLauncherTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GPLauncherTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        // Start the kiosk whenever the launcher gets focus; fall back to the home screen if absent
        // or while maintenance mode (entered via the hot zone) is active.
        val kiosk = if (MaintenanceMode.active) null else KioskLauncher(packageManager).launchIntent()
        if (kiosk != null) {
            HotZoneService.start(this)
            startActivity(kiosk)
        } else {
            HotZoneService.stop(this)
        }
    }
}
