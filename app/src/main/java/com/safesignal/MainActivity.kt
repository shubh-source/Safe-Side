package com.safesignal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safesignal.databinding.ActivityMainBinding
import com.safesignal.util.PrefManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefManager(this)

        // If already set up, skip straight to the correct home screen
        if (prefs.setupDone) {
            navigateToHome()
            return
        }

        binding.btnProtected.setOnClickListener {
            prefs.role = "protected"
            startActivity(Intent(this, WelcomeProtectedActivity::class.java))
            finish()
        }

        binding.btnGuardian.setOnClickListener {
            prefs.role = "guardian"
            startActivity(Intent(this, SetupNumbersActivity::class.java))
            finish()
        }
    }

    private fun navigateToHome() {
        val dest = if (prefs.role == "protected") ProtectedHomeActivity::class.java
                   else GuardianHomeActivity::class.java
        startActivity(Intent(this, dest))
        finish()
    }
}
