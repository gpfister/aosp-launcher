package org.gpfister.aosp.gplauncher.update

import android.os.UpdateEngine
import android.os.UpdateEngine.ErrorCodeConstants
import android.os.UpdateEngine.UpdateStatusConstants
import android.os.UpdateEngineCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Applies an A/B payload (full or incremental) with update_engine, which streams it from the URL.
 * Needs the platform-signed priv-app privileges; `android.os.UpdateEngine` is a system API.
 */
class UpdateEngineSystemUpdater : SystemUpdater {

    override suspend fun apply(command: UpdateCommand.SystemUpdate, onProgress: (Float) -> Unit) {
        val engine = UpdateEngine()
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                engine.bind(object : UpdateEngineCallback() {
                    override fun onStatusUpdate(status: Int, percent: Float) {
                        if (status == UpdateStatusConstants.DOWNLOADING ||
                            status == UpdateStatusConstants.FINALIZING
                        ) {
                            onProgress(percent)
                        }
                    }

                    override fun onPayloadApplicationComplete(errorCode: Int) {
                        if (!cont.isActive) return
                        if (errorCode == ErrorCodeConstants.SUCCESS ||
                            errorCode == ErrorCodeConstants.UPDATED_BUT_NOT_ACTIVE
                        ) {
                            cont.resume(Unit)
                        } else {
                            cont.resumeWithException(IllegalStateException("update_engine error $errorCode"))
                        }
                    }
                })
                cont.invokeOnCancellation { engine.cancel() }
                engine.applyPayload(command.url, command.offset, command.size, command.headerProperties.toTypedArray())
            }
        } finally {
            engine.unbind()
        }
    }
}
