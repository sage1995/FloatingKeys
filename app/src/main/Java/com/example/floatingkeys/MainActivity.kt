package com.example.floatingkeys

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simple button to start manually
        val btn = Button(this)
        btn.text = "Start Floating Keys"
        btn.setOnClickListener { checkAndStart() }
        setContentView(btn)

        checkAndStart()
    }

    private fun checkAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please Grant Overlay Permission", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            startService()
        }
    }

    private fun startService() {
        val intent = Intent(this, FloatingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
