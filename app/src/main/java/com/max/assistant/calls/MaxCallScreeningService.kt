package com.max.assistant.calls

import android.telecom.Call
import android.telecom.CallScreeningService
import com.max.assistant.tts.PiperTts

class MaxCallScreeningService : CallScreeningService() {
    private lateinit var tts: PiperTts
    override fun onCreate() { super.onCreate(); tts = PiperTts(this) }
    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: "unknown number"
        tts.speak("इनकमिंग कॉल $number से है")
        respondToCall(callDetails, CallResponse.Builder().setSilenceCall(false).build())
    }
    override fun onDestroy() { tts.shutdown(); super.onDestroy() }
}
