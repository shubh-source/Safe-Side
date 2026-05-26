package com.safesignal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.safesignal.databinding.ActivitySetupBinding
import com.safesignal.util.FirebaseHelper
import com.safesignal.util.PrefManager
import java.util.Locale

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private lateinit var prefs: PrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefManager(this)

        val isProtected = prefs.role == "protected"

        // Show the correct label
        binding.tvRoleLabel.text = if (isProtected) "You are being Protected" else "You are the Guardian"
        binding.tvPairCodeHint.text = if (isProtected)
            "Share this code with your guardian"
        else
            "Enter the code from the protected person's phone"

        // Generate a random pair code if protected role
        if (isProtected) {
            val code = generateCode()
            prefs.pairCode = code
            binding.etPairCode.setText(code)
            binding.etPairCode.isEnabled = false   // they share this code, not type it
        }

        binding.btnSave.setOnClickListener {
            val myNumber      = binding.etMyNumber.text.toString().trim()
            val partnerNumber = binding.etPartnerNumber.text.toString().trim()
            val pairCode      = if (isProtected) prefs.pairCode
                                else binding.etPairCode.text.toString().trim()

            if (myNumber.isBlank() || partnerNumber.isBlank() || pairCode.isBlank()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.myNumber      = myNumber
            prefs.partnerNumber = partnerNumber
            prefs.pairCode      = pairCode
            prefs.setupDone     = true

            // Register pair code → partner number in Firebase
            FirebaseHelper.registerPairCode(pairCode, partnerNumber)

            val dest = if (isProtected) ProtectedHomeActivity::class.java
                       else GuardianHomeActivity::class.java
            startActivity(Intent(this, dest))
            finish()
        }
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
