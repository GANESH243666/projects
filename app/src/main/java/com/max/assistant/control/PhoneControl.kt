package com.max.assistant.control

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.provider.Settings

object PhoneControl {
    fun torch(context: Context, enabled: Boolean) { val manager = context.getSystemService(CameraManager::class.java); manager.cameraIdList.firstOrNull()?.let { manager.setTorchMode(it, enabled) } }
    fun settings(context: Context) { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    fun openApp(context: Context, packageName: String): Boolean = context.packageManager.getLaunchIntentForPackage(packageName)?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true } ?: false
}
