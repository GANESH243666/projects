package com.max.assistant.reminders

import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat
import com.max.assistant.R

class ReminderReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) { val channel = "max_reminders"; context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(channel, "MAX reminders", NotificationManager.IMPORTANCE_HIGH)); context.getSystemService(NotificationManager::class.java).notify(intent.getIntExtra("id", 1), NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_stat_max).setContentTitle("MAX reminder").setContentText(intent.getStringExtra("text").orEmpty()).setAutoCancel(true).build()) } }
object ReminderScheduler { fun schedule(context: Context, id: Int, atMillis: Long, text: String) { val intent = Intent(context, ReminderReceiver::class.java).putExtra("id", id).putExtra("text", text); val pending = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); context.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending) } }
