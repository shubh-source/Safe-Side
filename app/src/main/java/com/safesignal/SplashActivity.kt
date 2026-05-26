package com.safesignal

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.safesignal.util.PrefManager

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = PrefManager(this)

        // After 2.5 seconds, navigate to the right screen
        Handler(Looper.getMainLooper()).postDelayed({
            if (prefs.setupDone) {
                // Already registered — go directly home
                val dest = if (prefs.role == "protected") ProtectedHomeActivity::class.java
                           else GuardianHomeActivity::class.java
                startActivity(Intent(this, dest))
            } else {
                // New user — go to role selection
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, 2500)
    }
}
