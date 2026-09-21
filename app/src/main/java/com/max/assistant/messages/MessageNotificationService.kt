package com.max.assistant.messages

import android.app.RemoteInput
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.max.assistant.tts.PiperTts

class MessageNotificationService : NotificationListenerService() {
    private lateinit var tts: PiperTts
    override fun onCreate() { super.onCreate(); tts = PiperTts(this) }
    override fun onNotificationPosted(sbn: StatusBarNotification) { val packageName = sbn.packageName; if (packageName == "com.whatsapp" || packageName == "com.google.android.apps.messaging" || packageName == "com.android.mms") { val extras = sbn.notification.extras; val title = extras.getString("android.title").orEmpty(); val text = extras.getCharSequence("android.text")?.toString().orEmpty(); if (text.isNotBlank()) tts.speak("$title ने कहा: $text") } }
    fun reply(sbn: StatusBarNotification, reply: String): Boolean { val action = sbn.notification.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true } ?: return false; val input = action.remoteInputs!!.first(); val intent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); RemoteInput.addResultsToIntent(arrayOf(input), intent, android.os.Bundle().apply { putCharSequence(input.resultKey, reply) }); action.actionIntent.send(this, 0, intent); return true }
    override fun onDestroy() { tts.shutdown(); super.onDestroy() }
}
