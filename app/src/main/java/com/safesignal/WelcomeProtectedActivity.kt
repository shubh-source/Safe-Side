package com.safesignal

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safesignal.databinding.ActivityWelcomeProtectedBinding

class WelcomeProtectedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeProtectedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeProtectedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnContinue.setOnClickListener {
            startActivity(Intent(this, SetupNumbersActivity::class.java))
            finish()
        }
    }
}
