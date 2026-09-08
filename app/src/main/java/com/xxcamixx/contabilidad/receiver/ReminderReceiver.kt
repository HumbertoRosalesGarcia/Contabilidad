package com.xxcamixx.contabilidad.receiver

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.xxcamixx.contabilidad.MainActivity
import com.xxcamixx.contabilidad.network.RetrofitInstance
import com.xxcamixx.contabilidad.util.AppSounds
import com.xxcamixx.contabilidad.util.AppVoice
import com.xxcamixx.contabilidad.util.scheduleNextChatSync
import com.xxcamixx.contabilidad.util.showChatNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

@SuppressLint("ObsoleteSdkInt")
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // Bloque WakeLock Seguro (Ahora dura 8 segundos para darle tiempo a la voz de terminar)
        var wakeLock: PowerManager.WakeLock? = null
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiBilletera::AlarmaWakeLock")
            wakeLock?.acquire(8000)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            if (intent.action == "com.xxcamixx.contabilidad.CHAT_SYNC") {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
                        val userId = authPrefs.getString("lastKnownUserId", null)
                        val userRole = authPrefs.getString("lastKnownRole", "INVITADO")

                        if (userId != null) {
                            val isSuperAdmin = userId.lowercase(Locale.getDefault()) == "zonacami77777@gmail.com"
                            val effectiveRole = if (isSuperAdmin) "ADMIN" else userRole
                            var lastNotified = authPrefs.getLong("lastNotified_$userId", System.currentTimeMillis())
                            var newestTime = lastNotified
                            var hasNewMsg = false; var notificationMsg: String? = null; var notificationSender: String? = null

                            if (effectiveRole == "ADMIN") {
                                val chats = RetrofitInstance.api.getAllChats()
                                chats.forEach { (email, msgs) ->
                                    msgs.forEach { msg ->
                                        if (msg.sender != "zonacami77777@gmail.com" && msg.timestamp > lastNotified) {
                                            notificationMsg = msg.text; notificationSender = email.substringBefore("@")
                                            if (msg.timestamp > newestTime) newestTime = msg.timestamp
                                            hasNewMsg = true
                                        }
                                    }
                                }
                            } else {
                                val msgs = RetrofitInstance.api.getChat(userId)
                                msgs.forEach { msg ->
                                    if (msg.sender == "zonacami77777@gmail.com" && msg.timestamp > lastNotified) {
                                        notificationMsg = msg.text; notificationSender = "Soporte (Admin)"
                                        if (msg.timestamp > newestTime) newestTime = msg.timestamp
                                        hasNewMsg = true
                                    }
                                }
                            }

                            if (hasNewMsg && notificationMsg != null) {
                                val userPrefs = context.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)
                                val useVoice = userPrefs.getBoolean("voiceEnabled", false)

                                if (useVoice) {
                                    AppVoice.speak(context, "Nuevo mensaje de $notificationSender. $notificationMsg")
                                } else {
                                    AppSounds.init()
                                }

                                showChatNotification(context, "Nuevo mensaje de $notificationSender", notificationMsg!!)
                                authPrefs.edit().putLong("lastNotified_$userId", newestTime).apply()
                            }
                        }
                    } catch (_: Exception) {
                    } finally {
                        scheduleNextChatSync(context)
                        pendingResult.finish()
                    }
                }
                return
            }

            if (intent.action?.startsWith("com.xxcamixx.contabilidad.") != true) return

            val notifTitle = intent.getStringExtra("NOTIFICATION_TITLE") ?: "¡Alerta! ⏰"
            val notifText = intent.getStringExtra("NOTIFICATION_TEXT") ?: "Tienes una alerta pendiente"

            // --- NUEVO: Extraemos el texto fantasma si existe ---
            val voiceText = intent.getStringExtra("VOICE_TEXT")
            val id = intent.getIntExtra("ID", 0)

            val authPrefs = context.getSharedPreferences("GlobalAuthPrefs", Context.MODE_PRIVATE)
            val userId = authPrefs.getString("lastKnownUserId", null)
            var useVoice = false
            var customSound = ""

            if (userId != null) {
                val userPrefs = context.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)
                useVoice = userPrefs.getBoolean("voiceEnabled", false)
                customSound = userPrefs.getString("customSoundUri", "") ?: ""
            }

            if (useVoice) {
                // Selecciona el texto fantasma (fluido) o el normal (si es otra notificación)
                val textToSpeak = voiceText ?: "$notifTitle. $notifText"
                // Limpieza rápida extra para que no diga "punto" en notificaciones normales
                val cleanText = textToSpeak.replace(Regex("\\.(?=\\s|$)"), ",")
                AppVoice.speak(context, cleanText)
            } else {
                AppSounds.init()
                AppSounds.play(context, customSound)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager != null) {
                val channelId = "finance_alarms_v6"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(channelId, "Recordatorios de App", NotificationManager.IMPORTANCE_HIGH).apply {
                        description = "Notificaciones para recordar pagos y cobros"
                        enableVibration(true)
                        setSound(null, null)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
                val tapIntent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
                val tapPendingIntent = PendingIntent.getActivity(context, id, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(notifTitle) // Muestra el título con emojis
                    .setContentText(notifText) // Muestra el texto con los signos $
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS)
                    .setContentIntent(tapPendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(id, notification)
            }
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}