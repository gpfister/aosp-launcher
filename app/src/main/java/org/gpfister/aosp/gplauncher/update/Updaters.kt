package org.gpfister.aosp.gplauncher.update

/** Applies a system payload. Throws on failure; reports progress in 0..1. */
interface SystemUpdater {
    suspend fun apply(command: UpdateCommand.SystemUpdate, onProgress: (Float) -> Unit)
}

/** Downloads, verifies and installs an APK. Throws on failure. */
interface AppUpdater {
    suspend fun install(command: UpdateCommand.AppUpdate, onProgress: (Float) -> Unit)
}
