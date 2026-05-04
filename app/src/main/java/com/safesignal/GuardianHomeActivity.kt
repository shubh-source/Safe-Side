package com.safesignal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.safesignal.databinding.ActivityGuardianHomeBinding
import com.safesignal.service.GuardianListenerService
import com.safesignal.util.PrefManager

class GuardianHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuardianHomeBinding
    private val prefs by lazy { PrefManager(this) }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) startGuardianService()
        else Toast.makeText(this, "Call permission needed to auto-call", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuardianHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvPairCode.text = "Pair Code: ${prefs.pairCode}"
        binding.tvProtected.text = "Protecting: ${prefs.partnerNumber}"

        requestPermissionsAndStart()
    }

    private fun requestPermissionsAndStart() {
        val needed = mutableListOf<String>()
        val required = listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.POST_NOTIFICATIONS
        )
        required.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                needed.add(it)
        }
        if (needed.isEmpty()) startGuardianService()
        else permLauncher.launch(needed.toTypedArray())
    }

    private fun startGuardianService() {
        val intent = Intent(this, GuardianListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        binding.tvStatus.text = "🛡️ Guardian Active — Listening"
    }

    override fun onResume() {
        super.onResume()
        startGuardianService()
    }
}
