package com.example.floatingkeys

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.view.*
import android.os.IBinder
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager

class FloatingService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var bar: View
    private var taps = 0
    private var lastTap = 0L

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createTouchZone()
        createBar()
    }

    private fun createTouchZone() {
        val v = View(this)
        val p = WindowManager.LayoutParams(
            1,1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        v.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                val now = System.currentTimeMillis()
                if (now - lastTap > 800) taps = 0
                taps++
                lastTap = now
                if (taps == 3) {
                    bar.visibility =
                        if (bar.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    taps = 0
                }
            }
            true
        }
        wm.addView(v, p)
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
        wm.addView(bar, p)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
