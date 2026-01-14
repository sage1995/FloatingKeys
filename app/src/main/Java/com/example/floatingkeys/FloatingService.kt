package com.example.floatingkeys

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var touchZone: View? = null
    private var bar: View? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // IMMEDIATE NOTIFICATION (Must be the very first thing called)
        startForeground(101, createNotification())
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Use a Try-Catch to ensure the service stays alive even if views fail
        try {
            setupViews()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupViews() {
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        // 1. CREATE TOUCH ZONE (150x150 in top left)
        touchZone = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            val params = WindowManager.LayoutParams(
                150, 150, overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START }
            
            var taps = 0
            var lastTap = 0L
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val now = System.currentTimeMillis()
                    if (now - lastTap > 600) taps = 0
                    taps++
                    lastTap = now
                    if (taps == 2) {
                        bar?.visibility = if (bar?.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        taps = 0
                    }
                }
                true
            }
            wm.addView(this, params)
        }

        // 2. CREATE BAR (Built in code to avoid XML errors)
        bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#88000000"))
            setPadding(10, 10, 10, 10)
            visibility = View.GONE

            val btnParams = LinearLayout.LayoutParams(120, 120).apply { setMargins(5, 5, 5, 5) }
            
            addView(Button(context).apply { text = "+"; setOnClickListener { runRoot("input keyevent 24") } }, btnParams)
            addView(Button(context).apply { text = "-"; setOnClickListener { runRoot("input keyevent 25") } }, btnParams)
            addView(Button(context).apply { text = "L"; setOnClickListener { runRoot("input keyevent 26") } }, btnParams)

            val barParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.CENTER }
            
            wm.addView(this, barParams)
        }
    }

    private fun runRoot(cmd: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        } catch (e: Exception) {}
    }

    private fun createNotification(): Notification {
        val channelId = "floating_keys"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Active", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Floating Keys")
            .setContentText("Double tap top-left to show")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onBind(intent: Intent?) = null
    
    override fun onDestroy() {
        super.onDestroy()
        touchZone?.let { wm.removeView(it) }
        bar?.let { wm.removeView(it) }
    }
}
