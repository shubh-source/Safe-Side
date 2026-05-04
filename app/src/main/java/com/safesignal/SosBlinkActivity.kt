package com.safesignal

import android.graphics.Color
import android.os.*
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.view.View

/**
 * Shown on HER phone after SOS is triggered.
 * Screen flashes white 4 times, then auto-finishes.
 * Completely silent — no sound, no vibration.
 */
class SosBlinkActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on and show above lock screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        val root = View(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)

        blink(root, 0)
    }

    /**
     * Blinks the screen white/black 4 times (200ms each state).
     * Finishes after the 4th blink.
     */
    private fun blink(view: View, count: Int) {
        if (count >= 8) {          // 4 blinks = 8 state changes
            finish()
            return
        }
        val isWhite = count % 2 == 0
        view.setBackgroundColor(if (isWhite) Color.WHITE else Color.BLACK)
        handler.postDelayed({ blink(view, count + 1) }, 200L)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
