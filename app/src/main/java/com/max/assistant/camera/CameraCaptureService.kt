package com.max.assistant.camera

import android.app.*
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.IBinder
import android.os.Handler
import android.os.HandlerThread
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import com.max.assistant.R
import java.io.File
import java.io.FileOutputStream

class CameraCaptureService : Service() {
    private var camera: CameraDevice? = null
    private var reader: ImageReader? = null
    private lateinit var handler: Handler
    private lateinit var thread: HandlerThread
    override fun onCreate() { super.onCreate(); val channel = "max_camera"; getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel, "MAX camera", NotificationManager.IMPORTANCE_LOW)); startForeground(12, NotificationCompat.Builder(this, channel).setSmallIcon(R.drawable.ic_stat_max).setContentTitle("MAX camera").setContentText("Camera active by voice command").setOngoing(true).build()) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { if (intent?.action == ACTION_STOP) stopSelf(); else if (intent?.action == ACTION_PHOTO) capturePhoto(); else if (intent?.action == ACTION_VIDEO) captureVideo(); return START_NOT_STICKY }
    private fun captureVideo() { startActivity(Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); stopSelf() }
    private fun capturePhoto() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { stopSelf(); return }
        thread = HandlerThread("max-camera").also { it.start() }; handler = Handler(thread.looper)
        reader = ImageReader.newInstance(1920, 1080, android.graphics.ImageFormat.JPEG, 1).also { output ->
            output.setOnImageAvailableListener({ source -> source.acquireLatestImage()?.use { image ->
                val buffer = image.planes[0].buffer; val bytes = ByteArray(buffer.remaining()); buffer.get(bytes)
                val file = File(filesDir, "captures/max-${System.currentTimeMillis()}.jpg"); file.parentFile?.mkdirs(); FileOutputStream(file).use { it.write(bytes) }; stopSelf()
            } }, handler)
        }
        val manager = getSystemService(CameraManager::class.java); val cameraId = manager.cameraIdList.firstOrNull { manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK } ?: return
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) { camera = device; device.createCaptureSession(listOf(reader!!.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) { val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(reader!!.surface) }.build(); session.capture(request, null, handler) }
                override fun onConfigureFailed(session: CameraCaptureSession) { stopSelf() }
            }, handler) }
            override fun onDisconnected(device: CameraDevice) { device.close(); stopSelf() }
            override fun onError(device: CameraDevice, error: Int) { device.close(); stopSelf() }
        }, handler)
    }
    override fun onDestroy() { camera?.close(); reader?.close(); if (::thread.isInitialized) thread.quitSafely(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    companion object { const val ACTION_PHOTO = "com.max.assistant.PHOTO"; const val ACTION_VIDEO = "com.max.assistant.VIDEO"; const val ACTION_STOP = "com.max.assistant.STOP_CAMERA" }
}
