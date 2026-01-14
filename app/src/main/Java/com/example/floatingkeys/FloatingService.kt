package com.example.floatingkeys

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import androidx.core.app.NotificationCompat

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var bar: View
    private lateinit var touchZone: View
    private var taps = 0
    private var lastTap = 0L

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 1. Mandatory Foreground Notification for Android 8+
        startForeground(1, createNotification())

        createTouchZone()
        createBar()
    }

    private fun createTouchZone() {
        touchZone = View(this)
        // Set height to 100px so you can actually find/tap it!
        val p = WindowManager.LayoutParams(
            100, 100,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        touchZone.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                val now = System.currentTimeMillis()
                if (now - lastTap > 500) taps = 0
                taps++
                lastTap = now
                if (taps == 2) { // Changed to 2 taps as requested
                    bar.visibility = if (bar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    taps = 0
                }
            }
            true
        }
        wm.addView(touchZone, p)
    }

    private fun createBar() {
        bar = LayoutInflater.from(this).inflate(R.layout.floating_bar, null)
        bar.visibility = View.GONE

        val p = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Root commands for buttons
        bar.findViewById<Button>(R.id.btnVolUp).setOnClickListener { runRoot("input keyevent 24") }
        bar.findViewById<Button>(R.id.btnVolDown).setOnClickListener { runRoot("input keyevent 25") }
        bar.findViewById<Button>(R.id.btnLock).setOnClickListener { runRoot("input keyevent 26") }

        wm.addView(bar, p)
    }

    private fun runRoot(cmd: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "floating_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Floating Keys Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Keys Active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
