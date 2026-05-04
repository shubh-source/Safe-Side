package com.safesignal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.safesignal.service.GuardianListenerService
import com.safesignal.service.PowerButtonService
import com.safesignal.util.PrefManager

/** Restarts the correct background service after phone reboot */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = PrefManager(context)
        if (!prefs.setupDone) return

        val serviceClass = if (prefs.role == "protected") PowerButtonService::class.java
                           else GuardianListenerService::class.java

        val svcIntent = Intent(context, serviceClass)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svcIntent)
        } else {
            context.startService(svcIntent)
        }
    }
}
