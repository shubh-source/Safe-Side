package com.safesignal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.safesignal.databinding.ActivitySetupCodeGuardianBinding
import com.safesignal.util.FirebaseHelper
import com.safesignal.util.PrefManager

class SetupCodeGuardianActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupCodeGuardianBinding
    private val prefs by lazy { PrefManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupCodeGuardianBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConnect.setOnClickListener {
            val code = binding.etPairCode.text.toString().trim()

            if (code.isBlank()) {
                Toast.makeText(this, "Please enter the pair code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save pair code and mark setup done
            prefs.pairCode = code
            prefs.setupDone = true

            // Register as Guardian in Firebase → this triggers GF's waiting screen!
            FirebaseHelper.registerAsGuardian(code, prefs.myNumber)

            // Go to Guardian Home
            val intent = Intent(this, GuardianHomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
