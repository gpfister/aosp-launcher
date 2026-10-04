package org.gpfister.aosp.gplauncher.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class GPDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        PolicyManager(context).apply()
    }
}
