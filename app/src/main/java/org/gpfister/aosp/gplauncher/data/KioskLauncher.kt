package org.gpfister.aosp.gplauncher.data

import android.content.Intent
import android.content.pm.PackageManager

const val KIOSK_PACKAGE = "org.gpfister.aosp.gpkiosk"

/** Resolves the intent that starts the kiosk app, or null when it is not installed. */
class KioskLauncher(private val packageManager: PackageManager) {
    fun launchIntent(): Intent? =
        packageManager.getLaunchIntentForPackage(KIOSK_PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
