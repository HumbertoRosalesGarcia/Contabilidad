package com.xxcamixx.contabilidad.util

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.xxcamixx.contabilidad.MainActivity
import com.xxcamixx.contabilidad.receiver.ReminderReceiver

fun showChatNotification(context: Context, title: String, text: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "chat_notifications"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Mensajes de Chat", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) }
        notificationManager.createNotificationChannel(channel)
    }
    val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification = NotificationCompat.Builder(context, channelId).setSmallIcon(android.R.drawable.ic_dialog_email).setContentTitle(title).setContentText(text).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pendingIntent).build()
    notificationManager.notify(System.currentTimeMillis().toInt(), notification)

    val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
    val userId = authPrefs.getString("lastKnownUserId", null)
    var useVoice = false
    if (userId != null) {
        val userPrefs = context.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)
        useVoice = userPrefs.getBoolean("voiceEnabled", false)
    }

    if (useVoice) {
        AppVoice.speak(context, "$title... $text")
    } else {
        AppSounds.play(context, "")
    }
}

@SuppressLint("ScheduleExactAlarm")
fun scheduleNextChatSync(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java).apply {
        action = "com.xxcamixx.contabilidad.CHAT_SYNC"
        addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
    }
    val pendingIntent = PendingIntent.getBroadcast(context, 1001, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val triggerTime = System.currentTimeMillis() + 60000L
    try {
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    } catch (_: Exception) {}
}

fun showPremiumToastMsg(context: Context) { Toast.makeText(context, "👑 Esta función es exclusiva para planes de pago.", Toast.LENGTH_SHORT).show() }