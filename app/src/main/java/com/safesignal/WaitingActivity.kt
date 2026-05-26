package com.safesignal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safesignal.databinding.ActivityWaitingBinding
import com.safesignal.util.FirebaseHelper
import com.safesignal.util.PrefManager

class WaitingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWaitingBinding
    private val prefs by lazy { PrefManager(this) }
    private var firebaseListener: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWaitingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvCodeLabel.text = prefs.pairCode

        // Listen ONLY for guardian to connect (not our own registration)
        firebaseListener = FirebaseHelper.listenForGuardianConnect(prefs.pairCode) { guardianNumber ->
            runOnUiThread {
                // Guardian connected! Go to protected home dashboard
                val intent = Intent(this, ProtectedHomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseListener?.let { FirebaseHelper.removeGuardianListener(prefs.pairCode, it) }
    }

    // Prevent going back to setup
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing — user must wait for guardian
    }
}
