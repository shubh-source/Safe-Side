package com.safesignal

import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.safesignal.databinding.ActivitySosAlertBinding
import com.safesignal.util.FirebaseHelper
import com.safesignal.util.PrefManager

/**
 * Full-screen SOS alert shown on HIS phone.
 * - Rings alarm at max volume
 * - Listens for live location updates
 * - Auto-calls her after 3 seconds
 */
class SosAlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySosAlertBinding
    private var mediaPlayer: MediaPlayer? = null
    private var locListener: Any? = null
    private val handler = Handler(Looper.getMainLooper())
    private var partnerPhone = ""
    private var pairCode = ""
    private var lat = 0.0
    private var lng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySosAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show over lock screen at full brightness
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        window.attributes.screenBrightness = 1f

        partnerPhone = intent.getStringExtra("phone") ?: PrefManager(this).partnerNumber
        pairCode     = intent.getStringExtra("pairCode") ?: PrefManager(this).pairCode
        lat          = intent.getDoubleExtra("lat", 0.0)
        lng          = intent.getDoubleExtra("lng", 0.0)

        startAlarm()
        updateLocationUI(lat, lng)
        listenLiveLocation()

        // Auto-call her after 3 seconds
        handler.postDelayed({ callHer() }, 3000L)

        binding.btnCallNow.setOnClickListener { callHer() }
        binding.btnOpenMap.setOnClickListener { openMap(lat, lng) }
        binding.btnDismiss.setOnClickListener { dismiss() }
    }

    // ── Alarm ────────────────────────────────────────────────────────────────

    private fun startAlarm() {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@SosAlertActivity, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Call ─────────────────────────────────────────────────────────────────

    private fun callHer() {
        if (partnerPhone.isBlank()) return
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$partnerPhone")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try { startActivity(callIntent) } catch (e: SecurityException) { e.printStackTrace() }
    }

    // ── Location ─────────────────────────────────────────────────────────────

    private fun listenLiveLocation() {
        locListener = FirebaseHelper.listenForLocation(pairCode) { newLat, newLng ->
            lat = newLat; lng = newLng
            updateLocationUI(newLat, newLng)
        }
    }

    private fun updateLocationUI(lat: Double, lng: Double) {
        val hasLoc = lat != 0.0 || lng != 0.0
        binding.tvLocation.text = if (hasLoc)
            "📍 %.5f, %.5f".format(lat, lng)
        else
            "📍 Waiting for location…"
        binding.btnOpenMap.isEnabled = hasLoc
    }

    private fun openMap(lat: Double, lng: Double) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Her+Location)")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Fallback to browser
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/?q=$lat,$lng")))
        }
    }

    // ── Dismiss ──────────────────────────────────────────────────────────────

    private fun dismiss() {
        mediaPlayer?.stop()
        handler.removeCallbacksAndMessages(null)
        locListener?.let { FirebaseHelper.removeLocationListener(pairCode, it) }
        FirebaseHelper.deactivateSOS(pairCode)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
        locListener?.let { FirebaseHelper.removeLocationListener(pairCode, it) }
    }

    // Prevent back button from dismissing accidentally
    @Deprecated("Deprecated")
    override fun onBackPressed() { /* do nothing */ }
}
