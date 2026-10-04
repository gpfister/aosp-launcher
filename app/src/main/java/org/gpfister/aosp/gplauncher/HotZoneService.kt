package org.gpfister.aosp.gplauncher

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import org.gpfister.aosp.gplauncher.admin.ForceStop
import org.gpfister.aosp.gplauncher.data.KIOSK_PACKAGE
import org.gpfister.aosp.gplauncher.data.MaintenanceMode

/**
 * Invisible touch zone in the top-left corner, drawn over the kiosk app.
 * Holding it for [HOLD_MILLIS] force-stops the kiosk and brings back the launcher's fallback screen.
 */
class HotZoneService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var zone: View? = null
    private val trigger = Runnable { exitKiosk() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (zone == null) addZone()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(trigger)
        removeZone()
        super.onDestroy()
    }

    private fun addZone() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "No overlay permission, hot zone disabled")
            return
        }
        val size = (ZONE_DP * resources.displayMetrics.density).toInt()
        val view = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> handler.postDelayed(trigger, HOLD_MILLIS)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(trigger)
                }
                true
            }
        }
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }
        getSystemService(WindowManager::class.java).addView(view, params)
        zone = view
    }

    private fun removeZone() {
        zone?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        zone = null
    }

    private fun exitKiosk() {
        MaintenanceMode.active = true
        runCatching { ForceStop.stop(this, KIOSK_PACKAGE) }
            .onFailure { Log.e(TAG, "Failed to stop kiosk", it) }
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        stopSelf()
    }

    companion object {
        private const val TAG = "GPHotZone"
        const val HOLD_MILLIS = 10_000L
        private const val ZONE_DP = 96

        fun start(context: Context) {
            context.startService(Intent(context, HotZoneService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HotZoneService::class.java))
        }
    }
}
