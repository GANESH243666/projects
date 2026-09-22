package com.max.assistant.wakeword

import android.app.*
import android.content.Intent
import android.media.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.max.assistant.R
import com.max.assistant.brain.BrainRouter
import com.max.assistant.stt.VoskRecognizer
import com.max.assistant.stt.AssetInstaller
import com.max.assistant.tts.PiperTts
import com.max.assistant.voice.CommandParser
import com.max.assistant.voice.MaxCommand
import com.max.assistant.calls.CallActions
import com.max.assistant.camera.CameraCaptureService
import com.max.assistant.control.MaxAccessibilityService
import com.max.assistant.control.PhoneControl
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class WakeWordService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var tts: PiperTts
    private lateinit var recognizer: VoskRecognizer
    private lateinit var kws: KwsRecognizer
    override fun onCreate() { super.onCreate(); tts = PiperTts(this); AssetInstaller.installModels(this); recognizer = VoskRecognizer(this); recognizer.load(); kws = KwsRecognizer(this); startForeground(9, notification()); listen() }
    private fun listen() { scope.launch {
        val recorder = AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 3200)
        val buffer = ByteArray(3200); recorder.startRecording(); var awake = false
        while (isActive) { val count = recorder.read(buffer, 0, buffer.size); if (count <= 0) continue
            if (!awake && kws.accept(buffer, count)) { awake = true; withContext(Dispatchers.Main) { tts.speak("जी, मैं सुन रहा हूँ") } }
            else if (awake) { val phrase = recognizer.recognize(buffer).lowercase(); if (phrase.isNotBlank()) { handle(phrase); awake = false } }
        }
        recorder.stop(); recorder.release()
    } }
    private suspend fun handle(text: String) {
        when (val command = CommandParser.parse(text)) {
            MaxCommand.AcceptCall -> CallActions.accept(this)
            MaxCommand.RejectCall -> CallActions.reject(this)
            MaxCommand.TakePhoto -> ContextCompat.startForegroundService(this, Intent(this, CameraCaptureService::class.java).setAction(CameraCaptureService.ACTION_PHOTO))
            MaxCommand.RecordVideo -> ContextCompat.startForegroundService(this, Intent(this, CameraCaptureService::class.java).setAction(CameraCaptureService.ACTION_VIDEO))
            MaxCommand.TorchOn -> PhoneControl.torch(this, true)
            MaxCommand.TorchOff -> PhoneControl.torch(this, false)
            MaxCommand.ReadScreen -> {
                val screen = MaxAccessibilityService.instance?.readScreen().orEmpty()
                val result = BrainRouter(this).ask("Describe this screen in Hindi: $screen")
                withContext(Dispatchers.Main) { tts.speak(result.first) }
            }
            is MaxCommand.Ask -> {
                val result = BrainRouter(this).ask(command.text)
                withContext(Dispatchers.Main) { tts.speak("${result.second}: ${result.first}") }
            }
            else -> Unit
        }
    }
    private fun notification(): Notification { val channel = "max_wake"; getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel, "MAX wake word", NotificationManager.IMPORTANCE_LOW)); return NotificationCompat.Builder(this, channel).setSmallIcon(R.drawable.ic_stat_max).setContentTitle("MAX सक्रिय है").setContentText("Hey Max सुन रहा है").setOngoing(true).build() }
    override fun onDestroy() { scope.cancel(); recognizer.close(); kws.close(); tts.shutdown(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
