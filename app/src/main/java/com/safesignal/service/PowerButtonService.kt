package com.safesignal.service

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
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
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
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

        // 4. Send Offline Backup SMS Alert
        sendOfflineSmsAlert(partnerNumber)
    }

    private fun sendOfflineSmsAlert(partnerNumber: String) {
        if (partnerNumber.isBlank()) return

        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                fusedClient.lastLocation.addOnSuccessListener { loc ->
                    val lat = loc?.latitude ?: 0.0
                    val lng = loc?.longitude ?: 0.0
                    sendSms(partnerNumber, lat, lng)
                }.addOnFailureListener {
                    sendSms(partnerNumber, 0.0, 0.0)
                }
            } catch (e: SecurityException) {
                sendSms(partnerNumber, 0.0, 0.0)
            }
        } else {
            sendSms(partnerNumber, 0.0, 0.0)
        }
    }

    private fun sendSms(partnerNumber: String, lat: Double, lng: Double) {
        try {
            val message = "[SafeSignal-SOS] EMERGENCY ALERT! I need help immediately. My location: https://maps.google.com/?q=$lat,$lng"
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(partnerNumber, null, message, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
