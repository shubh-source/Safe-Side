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

import com.safesignal.util.FirebaseHelper
import android.graphics.Color

class GuardianHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuardianHomeBinding
    private val prefs by lazy { PrefManager(this) }
    private var firebaseListener: Any? = null

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

        binding.tvPairCode.text = "Code: ${prefs.pairCode}"
        binding.tvProtected.text = "Protecting: ${prefs.partnerNumber}"

        requestPermissionsAndStart()

        // Start listening to live connection status
        firebaseListener = FirebaseHelper.listenForPairingSuccess(prefs.pairCode) { partnerNum ->
            // Update UI dynamically when linked
            runOnUiThread {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_check_vlynxly)
                binding.ivStatusIcon.setColorFilter(Color.parseColor("#6ECFA0"))
                binding.tvStatus.text = "Successfully Linked!"
                binding.tvStatus.setTextColor(Color.parseColor("#6ECFA0"))
                binding.tvProtected.text = "Protecting: $partnerNum"
                binding.cardLinkStatus.strokeColor = Color.parseColor("#6ECFA0")
            }
        }
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
        binding.tvProtectionStatus.text = "Guardian Active — Listening"
    }

    override fun onResume() {
        super.onResume()
        startGuardianService()
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseListener?.let { FirebaseHelper.removePairingListener(prefs.pairCode, it) }
    }
}
