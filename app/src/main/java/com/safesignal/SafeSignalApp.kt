package com.safesignal

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SafeSignalApp : Application() {

    companion object {
        const val CHANNEL_PROTECTION = "protection_service"
        const val CHANNEL_GUARDIAN   = "guardian_service"
        const val CHANNEL_SOS_ALERT  = "sos_alert"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Silent channel for her background service
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PROTECTION,
                    "Protection Service",
                    NotificationManager.IMPORTANCE_MIN
                ).apply { description = "Runs silently in background" }
            )

            // Silent channel for his background listener
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_GUARDIAN,
                    "Guardian Service",
                    NotificationManager.IMPORTANCE_MIN
                ).apply { description = "Listening for SOS" }
            )

            // HIGH importance for SOS alert on his phone
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SOS_ALERT,
                    "SOS Alert",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Emergency SOS alerts"
                    enableVibration(true)
                }
            )
        }
    }
}
