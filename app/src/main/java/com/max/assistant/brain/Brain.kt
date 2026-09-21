package com.max.assistant.brain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.max.assistant.BuildConfig
import com.max.assistant.memory.MemoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class NetworkState(context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    fun online(): Boolean = manager.activeNetwork?.let { manager.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true } ?: false
}

class OnlineBrain {
    private val client = OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).build()
    suspend fun ask(prompt: String, memory: String): String = withContext(Dispatchers.IO) {
        if (BuildConfig.LLM_API_KEY.isBlank()) error("API key is not configured")
        val messages = JSONArray().put(JSONObject().put("role", "system").put("content", "You are MAX, a concise friendly bilingual Hindi-English assistant. Use these memories when useful:\n$memory"))
            .put(JSONObject().put("role", "user").put("content", prompt))
        val body = JSONObject().put("model", BuildConfig.LLM_MODEL).put("messages", messages).put("temperature", 0.4)
        val request = Request.Builder().url(BuildConfig.LLM_API_URL).addHeader("Authorization", "Bearer ${BuildConfig.LLM_API_KEY}").post(body.toString().toRequestBody("application/json".toMediaType())).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Online brain HTTP ${response.code}")
            val json = JSONObject(response.body?.string().orEmpty())
            json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        }
    }
}

class LlamaJniBridge {
    init { runCatching { System.loadLibrary("llama_jni") } }
    external fun generate(modelPath: String, prompt: String): String
    fun available(): Boolean = runCatching { generate("", "") }.isSuccess
}

class OfflineBrain(private val context: Context) {
    private val bridge = LlamaJniBridge()
    suspend fun ask(prompt: String, memory: String): String = withContext(Dispatchers.IO) {
        val model = context.filesDir.resolve("qwen2.5-1.5b-instruct-q4_k_m.gguf")
        if (!model.exists()) return@withContext "ऑफलाइन मॉडल फ़ाइल नहीं मिली। कृपया GGUF को app files में रखें।"
        runCatching { bridge.generate(model.absolutePath, "Memory:\n$memory\nUser: $prompt\nAssistant:") }.getOrElse { "ऑफलाइन इंजन उपलब्ध नहीं है।" }
    }
}

class BrainRouter(context: Context) {
    private val network = NetworkState(context)
    private val online = OnlineBrain()
    private val offline = OfflineBrain(context)
    private val memory = MemoryStore(context)
    suspend fun ask(prompt: String): Pair<String, String> {
        val contextText = memory.context()
        return if (network.online() && BuildConfig.LLM_API_KEY.isNotBlank()) runCatching { online.ask(prompt, contextText) }.map { it to "online" }.getOrElse { offline.ask(prompt, contextText) to "offline" }
        else offline.ask(prompt, contextText) to "offline"
    }
    suspend fun save(text: String) = memory.remember(text)
}
