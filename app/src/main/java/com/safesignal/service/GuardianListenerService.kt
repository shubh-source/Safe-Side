package com.safesignal.service

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.safesignal.R
import com.safesignal.SafeSignalApp
import com.safesignal.SosAlertActivity
import com.safesignal.util.FirebaseHelper
import com.safesignal.util.PrefManager

/**
 * Runs on HIS phone.
 * Maintains a live Firebase RTDB listener. When SOS is detected:
 *   - Launches full-screen SosAlertActivity
 *   - Rings alarm
 *   - Auto-calls her number (handled by SosAlertActivity)
 */
class GuardianListenerService : Service() {

    private val prefs by lazy { PrefManager(this) }
    private var sosListener: Any? = null
    private var locListener: Any? = null
    private var sosAlreadyHandled = false

    override fun onCreate() {
        super.onCreate()
        startForeground(3, buildNotification())
        startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        val pairCode = prefs.pairCode
        sosListener?.let { FirebaseHelper.removeSOSListener(pairCode, it) }
        locListener?.let { FirebaseHelper.removeLocationListener(pairCode, it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startListening() {
        val pairCode = prefs.pairCode
        sosListener = FirebaseHelper.listenForSOS(pairCode) { partnerPhone, lat, lng ->
            if (!sosAlreadyHandled) {
                sosAlreadyHandled = true
                launchSOSAlert(partnerPhone, lat, lng, pairCode)
            }
        }
    }

    private fun launchSOSAlert(partnerPhone: String, lat: Double, lng: Double, pairCode: String) {
        val intent = Intent(this, SosAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("phone", partnerPhone)
            putExtra("lat", lat)
            putExtra("lng", lng)
            putExtra("pairCode", pairCode)
        }
        startActivity(intent)

        // Reset handler after 24 hours (allow next SOS)
        Handler(Looper.getMainLooper()).postDelayed({
            sosAlreadyHandled = false
        }, 24 * 60 * 60 * 1000L)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, SafeSignalApp.CHANNEL_GUARDIAN)
            .setContentTitle("SafeSignal Guardian")
            .setContentText("Listening for SOS")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
