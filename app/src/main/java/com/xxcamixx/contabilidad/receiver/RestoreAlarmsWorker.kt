package com.xxcamixx.contabilidad.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xxcamixx.contabilidad.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RestoreAlarmsWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val authPrefs = appContext.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
        val userId = authPrefs.getString("lastKnownUserId", null) ?: return Result.success()

        val db = AppDatabase.getDatabase(appContext, userId)
        val dao = db.financeDao()

        val reminders = dao.getBackupReminders()
        val fiadores = dao.getBackupFiadores()
        val now = System.currentTimeMillis()

        reminders.forEach { reminder ->
            if (reminder.targetDateInMillis > now) {
                val voiceText = if (reminder.amount > 0) "Debes pagar tu deuda de ${reminder.amount.toLong()} pesos a ${reminder.title}" else "Debes pagar a ${reminder.title}"
                val textMsg = if (reminder.amount > 0) "${reminder.title}: $${reminder.amount}" else reminder.title
                scheduleNotification(appContext, reminder.targetDateInMillis, "¡Hora de Pagar! ⏰", textMsg, reminder.id, "ALARM_TRIGGER", voiceText)
            }
        }

        fiadores.forEach { fiador ->
            if (fiador.targetDateInMillis > now && fiador.amount > fiador.paidAmount) {
                val remaining = fiador.amount - fiador.paidAmount
                val voiceText = if (fiador.isStore) {
                    val voiceReason = fiador.reason.replace("Uds ", " unidades de ").replace("Kg ", " kilos de ").replace("L ", " litros de ")
                    "${fiador.name} te debe ${remaining.toLong()} pesos, por la deuda de $voiceReason"
                } else {
                    "${fiador.name} te debe ${remaining.toLong()} pesos"
                }

                val reasonText = fiador.reason
                scheduleNotification(appContext, fiador.targetDateInMillis, "¡Cobrar a ${fiador.name}! 💰", "Monto: $${remaining} - $reasonText", fiador.id + 100000, "FIADOR_TRIGGER", voiceText)
            }
        }

        return Result.success()
    }

    private fun scheduleNotification(context: Context, timeInMillis: Long, notifTitle: String, notifText: String, id: Int, actionPrefix: String, voiceText: String? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return
            }
        }
        val isPersonal = actionPrefix == "ALARM_TRIGGER"
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.xxcamixx.contabilidad.${actionPrefix}_$id"
            putExtra("NOTIFICATION_TITLE", notifTitle)
            putExtra("NOTIFICATION_TEXT", notifText)
            putExtra("ID", id)
            putExtra("NOTIF_TYPE", if (isPersonal) "PERSONAL" else "STORE")
            if (voiceText != null) putExtra("VOICE_TEXT", voiceText)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        }
        val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } catch (_: Exception) {}
    }
}
