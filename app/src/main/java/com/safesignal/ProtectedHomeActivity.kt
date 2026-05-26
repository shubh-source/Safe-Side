package com.safesignal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.safesignal.databinding.ActivityProtectedHomeBinding
import com.safesignal.service.PowerButtonService
import com.safesignal.util.PrefManager

import com.safesignal.util.FirebaseHelper
import android.graphics.Color

class ProtectedHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProtectedHomeBinding
    private val prefs by lazy { PrefManager(this) }
    private var firebaseListener: Any? = null

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) startProtectionService()
        else Toast.makeText(this, "Permissions needed for protection", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProtectedHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvPairCode.text = "Code: ${prefs.pairCode}"
        binding.tvGuardian.text = "Partner: ${prefs.partnerNumber}"

        requestPermissionsAndStart()

        binding.btnTestSOS.setOnClickListener {
            Toast.makeText(this, "Test: Press power button 3 times", Toast.LENGTH_LONG).show()
        }

        // Start listening to live connection status
        firebaseListener = FirebaseHelper.listenForPairingSuccess(prefs.pairCode) { partnerNum ->
            // Update UI dynamically when Guardian connects
            runOnUiThread {
                binding.ivStatusIcon.setImageResource(R.drawable.ic_check_vlynxly)
                binding.ivStatusIcon.setColorFilter(Color.parseColor("#6ECFA0"))
                binding.tvStatus.text = "Successfully Linked!"
                binding.tvStatus.setTextColor(Color.parseColor("#6ECFA0"))
                binding.tvGuardian.text = "Guardian: $partnerNum"
                binding.cardLinkStatus.strokeColor = Color.parseColor("#6ECFA0")
            }
        }
    }

    private fun requestPermissionsAndStart() {
        val needed = mutableListOf<String>()
        val required = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
        required.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                needed.add(it)
        }

        if (needed.isEmpty()) startProtectionService()
        else permLauncher.launch(needed.toTypedArray())

        // Request overlay permission for blink activity
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            )
        }

        // Request battery optimization exemption
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(android.os.PowerManager::class.java)
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            }
        }
    }

    private fun startProtectionService() {
        val intent = Intent(this, PowerButtonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure service is still running
        startProtectionService()
    }

    override fun onDestroy() {
        super.onDestroy()
        firebaseListener?.let { FirebaseHelper.removePairingListener(prefs.pairCode, it) }
    }
}
