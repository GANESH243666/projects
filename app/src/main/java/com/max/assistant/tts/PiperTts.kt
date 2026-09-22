package com.max.assistant.tts

import android.content.Context
import android.net.ConnectivityManager
import com.max.assistant.BuildConfig
import com.max.assistant.stt.AssetInstaller
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class PiperTts(private val context: Context) {
    private val prefs = context.getSharedPreferences("max_voice_settings", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()
    private val cloud = CloudTts()

    init { AssetInstaller.installPiper(context) }

    fun speak(text: String, hindi: Boolean = containsHindi(text)) {
        if (text.isBlank()) return
        executor.execute {
            val usedCloud = BuildConfig.CLOUD_TTS_API_KEY.isNotBlank() && isOnline() && runCatching { cloud.speak(text) }.getOrDefault(false)
            if (!usedCloud) speakWithPiper(text, hindi)
        }
    }

    fun setRate(rate: Float) = prefs.edit().putFloat(KEY_RATE, rate.coerceIn(0.5f, 2f)).apply()
    fun rate(): Float = prefs.getFloat(KEY_RATE, 0.95f)

    private fun speakWithPiper(text: String, hindi: Boolean) {
        val voice = if (hindi) "hi-IN-medium" else "en-US-medium"
        val model = File(context.filesDir, "piper/$voice.onnx")
        val config = File(context.filesDir, "piper/$voice.onnx.json")
        runCatching {
            System.loadLibrary("piper_jni")
            nativeSpeak(text, model.absolutePath, config.absolutePath, rate())
        }
    }

    fun shutdown() { executor.shutdownNow() }
    private fun isOnline(): Boolean = context.getSystemService(ConnectivityManager::class.java).activeNetwork != null
    private external fun nativeSpeak(text: String, modelPath: String, configPath: String, rate: Float)
    companion object {
        private const val KEY_RATE = "speaking_rate"
        fun containsHindi(text: String) = text.any { it in '\u0900'..'\u097F' }
    }
}

private class CloudTts {
    private val client = OkHttpClient()

    fun speak(text: String): Boolean {
        val body = JSONObject().put("model", BuildConfig.CLOUD_TTS_MODEL).put("voice", BuildConfig.CLOUD_TTS_VOICE).put("input", text).put("response_format", "mp3").toString()
        val request = Request.Builder().url(BuildConfig.CLOUD_TTS_API_URL)
            .addHeader("Authorization", "Bearer ${BuildConfig.CLOUD_TTS_API_KEY}")
            .post(body.toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
            val audio = response.body?.bytes() ?: return false
            val file = File.createTempFile("max-tts-", ".mp3")
            FileOutputStream(file).use { it.write(audio) }
            val player = MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start() }
            player.setOnCompletionListener { player.release(); file.delete() }
            return true
        }
    }
}
