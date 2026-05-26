package com.safesignal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.safesignal.databinding.ActivitySetupNumbersBinding
import com.safesignal.util.PrefManager

class SetupNumbersActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupNumbersBinding
    private val prefs by lazy { PrefManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupNumbersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Customize labels based on role
        val isProtected = prefs.role == "protected"
        if (!isProtected) {
            // Guardian label tweaks
            binding.etPartnerNumber.hint = "Protected person's number"
        }

        binding.btnNext.setOnClickListener {
            val myNumber = binding.etMyNumber.text.toString().trim()
            val partnerNumber = binding.etPartnerNumber.text.toString().trim()

            if (myNumber.isBlank() || partnerNumber.isBlank()) {
                Toast.makeText(this, "Please enter both phone numbers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save numbers
            prefs.myNumber = myNumber
            prefs.partnerNumber = partnerNumber

            // Route based on role
            if (isProtected) {
                startActivity(Intent(this, SetupCodeActivity::class.java))
            } else {
                startActivity(Intent(this, SetupCodeGuardianActivity::class.java))
            }
        }
    }
}
