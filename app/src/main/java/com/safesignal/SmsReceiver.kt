package com.safesignal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Intercepts incoming SMS alerts offline.
 * Extracts GPS coordinates and triggers the loud SosAlertActivity.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.messageBody ?: continue
                if (body.contains("[SafeSignal-SOS]")) {
                    var lat = 0.0
                    var lng = 0.0
                    try {
                        val queryStart = body.indexOf("?q=")
                        if (queryStart != -1) {
                            val coordsStr = body.substring(queryStart + 3).trim()
                            val parts = coordsStr.split(",")
                            if (parts.size >= 2) {
                                lat = parts[0].toDoubleOrNull() ?: 0.0
                                lng = parts[1].toDoubleOrNull() ?: 0.0
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val partnerPhone = sms.originatingAddress ?: ""

                    // Start the full-screen SOS alert activity
                    val alertIntent = Intent(context, SosAlertActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("phone", partnerPhone)
                        putExtra("lat", lat)
                        putExtra("lng", lng)
                        putExtra("pairCode", "SMS_BACKUP")
                    }
                    context.startActivity(alertIntent)
                }
            }
        }
    }
}
