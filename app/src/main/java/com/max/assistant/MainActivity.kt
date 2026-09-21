package com.max.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.max.assistant.wakeword.WakeWordService

class MainActivity : Activity() {
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.SEND_SMS, Manifest.permission.POST_NOTIFICATIONS)
    override fun onCreate(state: Bundle?) { super.onCreate(state); render(); if (android.os.Build.VERSION.SDK_INT >= 23) requestPermissions(permissions, 40) }
    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 40, 32, 24); gravity = Gravity.CENTER_HORIZONTAL }
        root.addView(TextView(this).apply { text = "MAX\nआपका निजी voice assistant"; textSize = 28f; gravity = Gravity.CENTER; setPadding(0, 0, 0, 20) })
        root.addView(TextView(this).apply { text = "पहली बार सभी आवश्यक permissions दें। Accessibility से tap, scroll, back, home और screen पढ़ना चालू होगा। Notification access से SMS/WhatsApp पढ़े जाएंगे। Battery optimization exemption से Hey Max background में सुन सकेगा।"; textSize = 16f; setPadding(0, 0, 0, 18) })
        add(root, "माइक्रोफ़ोन permission", { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41) })
        add(root, "Camera और SMS permissions", { requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.SEND_SMS), 42) })
        add(root, "Notification access खोलें", { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
        add(root, "Accessibility खोलें", { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        add(root, "Battery optimization exemption", { startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))) })
        add(root, "Call screening चुनें", { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) })
        setContentView(root)
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == 0 && (android.os.Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == 0)) ContextCompat.startForegroundService(this, Intent(this, WakeWordService::class.java))
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, results); if (requestCode == 40 || requestCode == 41) render() }
    private fun add(root: LinearLayout, label: String, action: () -> Unit) { root.addView(Button(this).apply { text = label; setOnClickListener { action() } }, LinearLayout.LayoutParams(-1, -2)) }
}
