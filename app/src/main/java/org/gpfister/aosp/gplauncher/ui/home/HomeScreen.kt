package org.gpfister.aosp.gplauncher.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.gpfister.aosp.gplauncher.R
import org.gpfister.aosp.gplauncher.data.DeviceInfo
import org.gpfister.aosp.gplauncher.update.UpdateCommand
import org.gpfister.aosp.gplauncher.update.UpdateState

private enum class PowerAction { Reboot, PowerOff }

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val unavailable = stringResource(R.string.error_app_unavailable)

    fun launch(intent: Intent?) {
        try {
            if (intent == null) throw ActivityNotFoundException()
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    HomeContent(
        state = state,
        onReboot = viewModel::reboot,
        onPowerOff = viewModel::powerOff,
        onOpenSettings = { launch(viewModel.settingsIntent()) },
        onOpenFiles = { launch(viewModel.filesIntent()) },
        onStartKiosk = viewModel::startKiosk,
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onReboot: () -> Unit,
    onPowerOff: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFiles: () -> Unit,
    onStartKiosk: () -> Unit,
) {
    var pending by rememberSaveable { mutableStateOf<PowerAction?>(null) }

    val controls = @Composable { modifier: Modifier ->
        ControlsCard(
            modifier = modifier,
            onReboot = { pending = PowerAction.Reboot },
            onPowerOff = { pending = PowerAction.PowerOff },
            onOpenSettings = onOpenSettings,
            onOpenFiles = onOpenFiles,
            showStartKiosk = state.info?.kioskVersion != null,
            onStartKiosk = onStartKiosk,
        )
    }

    Scaffold { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            if (maxWidth >= WIDE_LAYOUT_MIN_WIDTH) {
                // Ultra-wide, short screens (e.g. 32:9 bar displays): three columns, no scrolling.
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                        state.info?.let { DeviceInfoCard(it, Modifier.fillMaxWidth().weight(1f)) }
                    }
                    state.info?.let { ImagesCard(it, Modifier.weight(1f).fillMaxHeight()) }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UpdateCard(state.update, Modifier.fillMaxWidth())
                        controls(Modifier.fillMaxWidth().weight(1f))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 720.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
                        state.info?.let { DeviceInfoCard(it) }
                        state.info?.let { ImagesCard(it) }
                        UpdateCard(state.update)
                        controls(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }

    pending?.let { action ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = {
                Text(
                    stringResource(
                        if (action == PowerAction.Reboot) R.string.confirm_reboot_title
                        else R.string.confirm_power_off_title,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    if (action == PowerAction.Reboot) onReboot() else onPowerOff()
                }) { Text(stringResource(R.string.confirm_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.confirm_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(value ?: stringResource(R.string.value_not_available), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DeviceInfoCard(info: DeviceInfo, modifier: Modifier = Modifier) {
    SectionCard(stringResource(R.string.section_device_info), modifier) {
        InfoRow(stringResource(R.string.label_ethernet_mac), info.ethernetMac)
        InfoRow(stringResource(R.string.label_ethernet_ip), info.ethernetIp)
        InfoRow(stringResource(R.string.label_launcher_version), info.launcherVersion)
        InfoRow(stringResource(R.string.label_kiosk_version), info.kioskVersion)
    }
}

@Composable
private fun ImagesCard(info: DeviceInfo, modifier: Modifier = Modifier) {
    SectionCard(stringResource(R.string.section_images), modifier) {
        InfoRow(stringResource(R.string.label_active_slot), info.activeSlot)
        if (info.images.isEmpty()) {
            Text(stringResource(R.string.images_empty), style = MaterialTheme.typography.bodyMedium)
        }
        info.images.forEach { InfoRow(it.partition, it.version ?: it.fingerprint) }
    }
}

@Composable
private fun UpdateCard(update: UpdateState, modifier: Modifier = Modifier) {
    if (update is UpdateState.Idle) return
    fun UpdateCommand.target() = when (this) {
        is UpdateCommand.SystemUpdate -> null
        is UpdateCommand.AppUpdate -> packageName
    }
    val system = stringResource(R.string.update_target_system)
    val text = when (update) {
        UpdateState.Idle -> return
        is UpdateState.Scheduled -> stringResource(R.string.update_scheduled, update.command.target() ?: system)
        is UpdateState.InProgress -> stringResource(
            R.string.update_running, update.command.target() ?: system, (update.progress * 100).toInt(),
        )
        UpdateState.Rebooting -> stringResource(R.string.update_rebooting)
        is UpdateState.Failed -> stringResource(R.string.update_failed, update.command.target() ?: system, update.reason)
    }
    SectionCard(stringResource(R.string.section_updates), modifier) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ControlsCard(
    modifier: Modifier,
    onReboot: () -> Unit,
    onPowerOff: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFiles: () -> Unit,
    showStartKiosk: Boolean,
    onStartKiosk: () -> Unit,
) {
    SectionCard(stringResource(R.string.section_system), modifier) {
        val button = Modifier.heightIn(min = 56.dp)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onReboot, modifier = button.weight(1f)) { Text(stringResource(R.string.action_reboot)) }
                Button(onClick = onPowerOff, modifier = button.weight(1f)) { Text(stringResource(R.string.action_power_off)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onOpenSettings, modifier = button.weight(1f)) {
                    Text(stringResource(R.string.action_settings))
                }
                FilledTonalButton(onClick = onOpenFiles, modifier = button.weight(1f)) {
                    Text(stringResource(R.string.action_files))
                }
            }
            if (showStartKiosk) {
                Button(onClick = onStartKiosk, modifier = button.fillMaxWidth()) {
                    Text(stringResource(R.string.action_start_kiosk))
                }
            }
        }
    }
}

private val WIDE_LAYOUT_MIN_WIDTH = 840.dp
