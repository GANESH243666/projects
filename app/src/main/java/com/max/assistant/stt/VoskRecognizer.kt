package com.max.assistant.stt

import android.content.Context
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

class VoskRecognizer(private val context: Context) {
    private var hindi: Recognizer? = null
    private var english: Recognizer? = null
    fun load() {
        hindi = model("vosk-model-small-hi-0.22")?.let { Recognizer(Model(it), 16000f) }
        english = model("vosk-model-small-en-us-0.15")?.let { Recognizer(Model(it), 16000f) }
    }
    fun recognize(pcm: ByteArray): String {
        val candidates = listOfNotNull(hindi, english).mapNotNull { recognizer -> if (recognizer.acceptWaveForm(pcm, pcm.size)) runCatching { JSONObject(recognizer.result).optString("text") }.getOrNull() else null }
        return candidates.maxByOrNull { it.split(" ").size }.orEmpty()
    }
    private fun model(name: String): String? { val target = File(context.filesDir, "models/$name"); return target.takeIf { it.exists() }?.absolutePath }
    fun close() { hindi?.close(); english?.close() }
}
