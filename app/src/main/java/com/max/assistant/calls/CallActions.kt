package com.max.assistant.calls

import android.annotation.SuppressLint
import android.content.Context
import android.telecom.TelecomManager

object CallActions {
    @SuppressLint("MissingPermission") fun accept(context: Context) { context.getSystemService(TelecomManager::class.java).acceptRingingCall() }
    @SuppressLint("MissingPermission") fun reject(context: Context) { context.getSystemService(TelecomManager::class.java).endCall() }
}
