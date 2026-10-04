package org.gpfister.aosp.gplauncher.admin

import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import org.gpfister.aosp.gplauncher.MainActivity
import org.gpfister.aosp.gplauncher.data.KIOSK_PACKAGE

/** Device-owner policy: the launcher, not the OS, decides when updates happen. */
class PolicyManager(private val context: Context) {
    private val dpm = context.getSystemService(DevicePolicyManager::class.java)
    private val admin = ComponentName(context, GPDeviceAdminReceiver::class.java)

    fun apply() {
        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w(TAG, "Not device owner, skipping policy")
            return
        }
        runPolicy("system update policy") {
            dpm.setSystemUpdatePolicy(admin, SystemUpdatePolicy.createPostponeInstallPolicy())
        }
        runPolicy("persistent HOME") {
            val home = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            dpm.addPersistentPreferredActivity(admin, home, ComponentName(context, MainActivity::class.java))
        }
        runPolicy("kiosk protection") {
            dpm.setUserControlDisabledPackages(admin, listOf(KIOSK_PACKAGE))
        }
    }

    private fun runPolicy(name: String, block: () -> Unit) {
        try {
            block()
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to apply $name", e)
        }
    }

    private companion object {
        const val TAG = "GPPolicyManager"
    }
}
