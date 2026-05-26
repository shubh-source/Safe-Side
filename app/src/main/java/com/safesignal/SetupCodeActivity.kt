package com.safesignal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safesignal.databinding.ActivitySetupCodeBinding
import com.safesignal.util.FirebaseHelper
import com.safesignal.util.PrefManager

class SetupCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupCodeBinding
    private val prefs by lazy { PrefManager(this) }

    companion object {
        const val PAIR_CODE = "Christine0109"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show the fixed pair code
        binding.tvCodeDisplay.text = PAIR_CODE

        binding.btnConnect.setOnClickListener {
            // Save pair code
            prefs.pairCode = PAIR_CODE
            prefs.setupDone = true

            // Register GF under this pair code in Firebase
            FirebaseHelper.registerAsProtected(PAIR_CODE, prefs.myNumber)

            // Go to waiting screen
            startActivity(Intent(this, WaitingActivity::class.java))
            finish()
        }
    }
}
