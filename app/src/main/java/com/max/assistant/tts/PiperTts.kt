package com.max.assistant.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class PiperTts(private val context: Context) {
    private var fallback: TextToSpeech? = null
    private var ready = false
    init { fallback = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS; if (ready) fallback?.language = Locale("hi", "IN") } }
    fun speak(text: String, hindi: Boolean = containsHindi(text)) {
        val voice = if (hindi) "hi-IN-female" else "en-US-female"
        if (ready) fallback?.language = if (hindi) Locale("hi", "IN") else Locale.US
        runCatching { speakWithPiper(text, voice) }.onFailure { if (ready) fallback?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "max") }
    }
    private fun speakWithPiper(text: String, voice: String) {
        System.loadLibrary("piper_jni")
        nativeSpeak(text, voice)
    }
    private external fun nativeSpeak(text: String, voice: String)
    fun shutdown() { fallback?.shutdown() }
    companion object { fun containsHindi(text: String) = text.any { it in '\u0900'..'\u097F' } }
}
