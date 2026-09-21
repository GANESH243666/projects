package com.max.assistant.messages

import android.annotation.SuppressLint
import android.telephony.SmsManager

object SmsReply {
    @SuppressLint("MissingPermission")
    fun send(number: String, message: String): Boolean = runCatching { SmsManager.getDefault().sendTextMessage(number, null, message, null, null) }.isSuccess
}
