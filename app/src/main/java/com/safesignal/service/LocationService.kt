package com.safesignal.service

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.safesignal.R
import com.safesignal.SafeSignalApp
import com.safesignal.util.FirebaseHelper
import com.safesignal.util.PrefManager

/**
 * Streams GPS location to Firebase every 30 seconds.
 * Automatically stops after 24 hours.
 */
class LocationService : Service() {

    private val prefs by lazy { PrefManager(this) }
    private lateinit var fusedClient: FusedLocationProviderClient
    private val handler = Handler(Looper.getMainLooper())

    private val stopRunnable = Runnable { stopSelf() }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            FirebaseHelper.pushLocation(prefs.pairCode, loc.latitude, loc.longitude)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(2, buildNotification())
        startLocationUpdates()
        // Auto-stop after 24 hours
        handler.postDelayed(stopRunnable, 24 * 60 * 60 * 1000L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        fusedClient.removeLocationUpdates(locationCallback)
        handler.removeCallbacks(stopRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 30_000L
        ).setMinUpdateIntervalMillis(15_000L).build()

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, SafeSignalApp.CHANNEL_PROTECTION)
            .setContentTitle("SafeSignal")
            .setContentText("Location sharing active")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
}
