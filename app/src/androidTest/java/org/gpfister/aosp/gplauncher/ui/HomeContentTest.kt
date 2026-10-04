package org.gpfister.aosp.gplauncher.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.gpfister.aosp.gplauncher.data.DeviceInfo
import org.gpfister.aosp.gplauncher.ui.home.HomeContent
import org.gpfister.aosp.gplauncher.ui.home.HomeUiState
import org.gpfister.aosp.gplauncher.ui.theme.GPLauncherTheme
import org.junit.Rule
import org.junit.Test

class HomeContentTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(dark: Boolean) = rule.setContent {
        GPLauncherTheme(darkTheme = dark) {
            HomeContent(
                state = HomeUiState(
                    DeviceInfo(null, null, "0.1.0", null, "a", emptyList()),
                ),
                onReboot = {}, onPowerOff = {}, onOpenSettings = {}, onOpenFiles = {}, onStartKiosk = {},
            )
        }
    }

    @Test
    fun showsControlsInLightTheme() {
        show(dark = false)
        rule.onNodeWithText("Reboot").assertExists()
        rule.onNodeWithText("Android Settings").assertExists()
        rule.onNodeWithText("N/A").assertExists()
    }

    @Test
    fun showsControlsInDarkTheme() {
        show(dark = true)
        rule.onNodeWithText("Android Files").assertExists()
    }
}
