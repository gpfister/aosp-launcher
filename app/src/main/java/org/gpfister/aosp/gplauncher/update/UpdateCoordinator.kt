package org.gpfister.aosp.gplauncher.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Runs one update at a time. A new command replaces a scheduled or running one.
 * A successful system update is followed by a reboot to switch A/B slots.
 */
class UpdateCoordinator(
    private val scope: CoroutineScope,
    private val systemUpdater: SystemUpdater,
    private val appUpdater: AppUpdater,
    private val reboot: () -> Unit,
    private val now: () -> Instant = Instant::now,
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var job: Job? = null

    fun submit(command: UpdateCommand) {
        job?.cancel()
        _state.value = UpdateState.Scheduled(command)
        job = scope.launch { run(command) }
    }

    fun cancel() {
        job?.cancel()
        _state.value = UpdateState.Idle
    }

    private suspend fun run(command: UpdateCommand) {
        try {
            command.installAt?.let { at ->
                val wait = at.toEpochMilli() - now().toEpochMilli()
                if (wait > 0) delay(wait)
            }
            val progress = { p: Float -> _state.value = UpdateState.InProgress(command, p) }
            progress(0f)
            when (command) {
                is UpdateCommand.SystemUpdate -> {
                    systemUpdater.apply(command, progress)
                    _state.value = UpdateState.Rebooting
                    reboot()
                }
                is UpdateCommand.AppUpdate -> {
                    appUpdater.install(command, progress)
                    _state.value = UpdateState.Idle
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(command, e.message ?: e.javaClass.simpleName)
        }
    }
}
