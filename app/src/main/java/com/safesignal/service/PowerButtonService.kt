package com.safesignal.service

import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import com.safesignal.R
import com.safesignal.SafeSignalApp
import com.safesignal.SosBlinkActivity
import com.safesignal.service.LocationService
import com.safesignal.util.FirebaseHelper
import com.safesignal.util.PrefManager

/**
 * Runs on HER phone in the background.
 * Detects 4 power button presses within 2.5 seconds → triggers SOS.
 */
class PowerButtonService : Service() {

    private val prefs by lazy { PrefManager(this) }
    private val pressTimes = mutableListOf<Long>()
    private val PRESS_WINDOW_MS = 4000L
    private val REQUIRED_PRESSES = 3

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF || intent.action == Intent.ACTION_SCREEN_ON) {
                val now = System.currentTimeMillis()
                pressTimes.add(now)
                // Remove stale presses outside the window
                pressTimes.removeAll { now - it > PRESS_WINDOW_MS }

                if (pressTimes.size >= REQUIRED_PRESSES) {
                    pressTimes.clear()
                    onSOSDetected()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY   // restart automatically if killed

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── SOS trigger ──────────────────────────────────────────────────────────

    private fun onSOSDetected() {
        // 1. Show blink activity (4 white flashes as confirmation)
        val blinkIntent = Intent(this, SosBlinkActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(blinkIntent)

        // 2. Write SOS to Firebase
        val pairCode = prefs.pairCode
        val partnerNumber = prefs.partnerNumber
        FirebaseHelper.triggerSOS(pairCode, partnerNumber)

        // 3. Start location streaming
        val locIntent = Intent(this, LocationService::class.java)
        startForegroundService(locIntent)
    }

    // ── Foreground notification (minimal / silent) ────────────────────────────

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, SafeSignalApp.CHANNEL_PROTECTION)
            .setContentTitle("SafeSignal")
            .setContentText("Protection active")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
