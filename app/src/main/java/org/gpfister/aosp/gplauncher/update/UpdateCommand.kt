package org.gpfister.aosp.gplauncher.update

import java.time.Instant

/** Instruction to install an update, as sent by the (future) WebSocket service. */
sealed interface UpdateCommand {
    /** When to start; null means immediately. */
    val installAt: Instant?

    /** Incremental A/B OTA payload applied through update_engine. */
    data class SystemUpdate(
        val url: String,
        val offset: Long = 0,
        val size: Long = 0,
        /** `KEY=VALUE` payload properties (file hash, metadata hash, ...). */
        val headerProperties: List<String> = emptyList(),
        override val installAt: Instant? = null,
    ) : UpdateCommand

    /** Silent APK install through PackageInstaller (device owner). */
    data class AppUpdate(
        val packageName: String,
        val url: String,
        val sha256: String,
        override val installAt: Instant? = null,
    ) : UpdateCommand
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Scheduled(val command: UpdateCommand) : UpdateState
    data class InProgress(val command: UpdateCommand, val progress: Float) : UpdateState
    data object Rebooting : UpdateState
    data class Failed(val command: UpdateCommand, val reason: String) : UpdateState
}
