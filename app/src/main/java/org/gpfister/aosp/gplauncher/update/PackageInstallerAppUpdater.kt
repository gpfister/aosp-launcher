package org.gpfister.aosp.gplauncher.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PackageInstallerAppUpdater(private val context: Context) : AppUpdater {

    override suspend fun install(command: UpdateCommand.AppUpdate, onProgress: (Float) -> Unit) {
        val apk = withContext(Dispatchers.IO) { download(command, onProgress) }
        try {
            commit(command.packageName, apk)
        } finally {
            apk.delete()
        }
    }

    private fun download(command: UpdateCommand.AppUpdate, onProgress: (Float) -> Unit): File {
        val target = File(context.cacheDir, "update-${UUID.randomUUID()}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = URL(command.url).openConnection()
        val total = connection.contentLengthLong
        connection.getInputStream().use { input ->
            target.outputStream().use { out ->
                val buffer = ByteArray(BUFFER_SIZE)
                var read = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    digest.update(buffer, 0, n)
                    read += n
                    if (total > 0) onProgress(read.toFloat() / total)
                }
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(command.sha256, ignoreCase = true)) {
            target.delete()
            throw IllegalStateException("SHA-256 mismatch for ${command.packageName}")
        }
        return target
    }

    private suspend fun commit(packageName: String, apk: File) {
        val installer = context.packageManager.packageInstaller
        val action = "$ACTION_PREFIX.${UUID.randomUUID()}"
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        try {
            suspendCancellableCoroutine<Unit> { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context, intent: Intent) {
                        runCatching { context.unregisterReceiver(this) }
                        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                        if (status == PackageInstaller.STATUS_SUCCESS) {
                            cont.resume(Unit)
                        } else {
                            val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                            cont.resumeWithException(IllegalStateException("Install failed ($status): $msg"))
                        }
                    }
                }
                ContextCompat.registerReceiver(
                    context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                cont.invokeOnCancellation {
                    runCatching { context.unregisterReceiver(receiver) }
                    runCatching { installer.abandonSession(sessionId) }
                }
                installer.openSession(sessionId).use { session ->
                    apk.inputStream().use { input ->
                        session.openWrite("base.apk", 0, apk.length()).use { out ->
                            input.copyTo(out)
                            session.fsync(out)
                        }
                    }
                    val pending = PendingIntent.getBroadcast(
                        context, sessionId,
                        Intent(action).setPackage(context.packageName),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                    )
                    session.commit(pending.intentSender)
                }
            }
        } catch (e: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            throw e
        }
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        const val ACTION_PREFIX = "org.gpfister.aosp.gplauncher.INSTALL_RESULT"
    }
}
