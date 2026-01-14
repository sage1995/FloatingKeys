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
import android.widget.Toast
import androidx.core.app.NotificationCompat

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private var touchZone: View? = null
    private var bar: View? = null
    private var taps = 0
    private var lastTap = 0L

    override fun onCreate() {
        super.onCreate()
        // 1. Show message to prove app is trying to start
        Toast.makeText(this, "Service Created", Toast.LENGTH_SHORT).show()

        // 2. Start Foreground Notification IMMEDIATELY
        startForeground(101, createNotification())

        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        try {
            createTouchZone()
            createBar()
            Toast.makeText(this, "Overlay Active - Tap Top Left", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun createTouchZone() {
        touchZone = View(this)
        touchZone?.setBackgroundColor(0x00000000) // Transparent
        
        val params = WindowManager.LayoutParams(
            150, 150, // 150px size to make sure you can hit it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        
        touchZone?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val now = System.currentTimeMillis()
                if (now - lastTap > 500) taps = 0
                taps++
                lastTap = now
                if (taps == 2) {
                    toggleBar()
                    taps = 0
                }
            }
            true // Return true to consume the touch
        }
        
        wm.addView(touchZone, params)
    }

    private fun createBar() {
        // We build the layout in CODE so it cannot crash due to missing XML
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.setBackgroundColor(Color.DKGRAY)
        layout.setPadding(20, 20, 20, 20)

        val btnUp = Button(this).apply { text = "+"; setOnClickListener { runRoot("input keyevent 24") } }
        val btnDown = Button(this).apply { text = "-"; setOnClickListener { runRoot("input keyevent 25") } }
        val btnLock = Button(this).apply { text = "OFF"; setOnClickListener { runRoot("input keyevent 26") } }

        layout.addView(btnUp, LinearLayout.LayoutParams(150, 150))
        layout.addView(btnDown, LinearLayout.LayoutParams(150, 150))
        layout.addView(btnLock, LinearLayout.LayoutParams(150, 150))

        bar = layout
        bar?.visibility = View.GONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.CENTER
        
        wm.addView(bar, params)
    }

    private fun toggleBar() {
        bar?.let {
            it.visibility = if (it.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun runRoot(cmd: String) {
        try {
            Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
        } catch (e: Exception) {
            Toast.makeText(this, "Root Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotification(): Notification {
        val channelId = "floating_keys_id"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Floating Keys", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        
        // Ensure we use a built-in icon to avoid crashes
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Floating Keys Running")
            .setSmallIcon(android.R.drawable.ic_menu_compass) 
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (touchZone != null) wm.removeView(touchZone)
        if (bar != null) wm.removeView(bar)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
