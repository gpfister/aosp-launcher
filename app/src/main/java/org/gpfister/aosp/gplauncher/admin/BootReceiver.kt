package org.gpfister.aosp.gplauncher.admin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-applies the device-owner policy after every boot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) PolicyManager(context).apply()
    }
}
