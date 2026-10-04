package org.gpfister.aosp.gplauncher.admin

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.PowerManager

class PowerController(private val context: Context) {
    private val admin = ComponentName(context, GPDeviceAdminReceiver::class.java)

    fun reboot() {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        if (dpm.isDeviceOwnerApp(context.packageName)) {
            dpm.reboot(admin)
        } else {
            context.getSystemService(PowerManager::class.java).reboot(null)
        }
    }

    /** `PowerManager.shutdown` is a hidden API, available to the platform-signed app with SHUTDOWN. */
    fun powerOff() {
        val pm = context.getSystemService(PowerManager::class.java)
        PowerManager::class.java
            .getMethod("shutdown", Boolean::class.javaPrimitiveType, String::class.java, Boolean::class.javaPrimitiveType)
            .invoke(pm, false, null, false)
    }
}
