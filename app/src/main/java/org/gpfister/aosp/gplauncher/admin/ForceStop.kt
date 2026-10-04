package org.gpfister.aosp.gplauncher.admin

import android.app.ActivityManager
import android.content.Context

/** `ActivityManager.forceStopPackage` is a hidden API; needs FORCE_STOP_PACKAGES (platform-signed). */
object ForceStop {
    fun stop(context: Context, packageName: String) {
        val am = context.getSystemService(ActivityManager::class.java)
        ActivityManager::class.java
            .getMethod("forceStopPackage", String::class.java)
            .invoke(am, packageName)
    }
}
