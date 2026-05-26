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
import android.net.Uri
import android.provider.Settings
import android.view.View

class GuardianHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuardianHomeBinding
    private val prefs by lazy { PrefManager(this) }
    private var firebaseListener: Any? = null
    private var sosStatusListener: Any? = null
    private var liveLocListener: Any? = null

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

        // Listen to SOS status in real-time on dashboard
        sosStatusListener = FirebaseHelper.listenForSosStatus(prefs.pairCode) { active, partnerPhone, initialLat, initialLng ->
            runOnUiThread {
                if (active) {
                    binding.cardSosAlert.visibility = View.VISIBLE
                    updateSosLocationUI(initialLat, initialLng)
                    
                    binding.btnSosOpenMap.setOnClickListener {
                        openPartnerMap(initialLat, initialLng)
                    }
                    binding.btnSosCall.setOnClickListener {
                        callPartner(partnerPhone)
                    }
                    binding.btnSosDismiss.setOnClickListener {
                        FirebaseHelper.deactivateSOS(prefs.pairCode)
                    }

                    // Start live location listener if not already active
                    if (liveLocListener == null) {
                        liveLocListener = FirebaseHelper.listenForLocation(prefs.pairCode) { liveLat, liveLng ->
                            runOnUiThread {
                                updateSosLocationUI(liveLat, liveLng)
                                binding.btnSosOpenMap.setOnClickListener {
                                    openPartnerMap(liveLat, liveLng)
                                }
                            }
                        }
                    }
                } else {
                    binding.cardSosAlert.visibility = View.GONE
                    liveLocListener?.let {
                        FirebaseHelper.removeLocationListener(prefs.pairCode, it)
                        liveLocListener = null
                    }
                }
            }
        }
    }

    private fun updateSosLocationUI(lat: Double, lng: Double) {
        val hasLoc = lat != 0.0 || lng != 0.0
        binding.tvSosLocation.text = if (hasLoc)
            "📍 %.5f, %.5f".format(lat, lng)
        else
            "📍 Waiting for GPS location…"
        binding.btnSosOpenMap.isEnabled = hasLoc
    }

    private fun openPartnerMap(lat: Double, lng: Double) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Her+Location)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$lat,$lng")))
        }
    }

    private fun callPartner(phone: String) {
        val partner = if (phone.isNotBlank()) phone else prefs.partnerNumber
        if (partner.isBlank()) return
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$partner"))
        try {
            startActivity(callIntent)
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission denied to make call", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestPermissionsAndStart() {
        val needed = mutableListOf<String>()
        val required = listOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.RECEIVE_SMS
        )
        required.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                needed.add(it)
        }
        if (needed.isEmpty()) startGuardianService()
        else permLauncher.launch(needed.toTypedArray())

        // Request overlay permission for SOS alarm popup from background
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            )
        }
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
        sosStatusListener?.let { FirebaseHelper.removeSOSListener(prefs.pairCode, it) }
        liveLocListener?.let { FirebaseHelper.removeLocationListener(prefs.pairCode, it) }
    }
}
