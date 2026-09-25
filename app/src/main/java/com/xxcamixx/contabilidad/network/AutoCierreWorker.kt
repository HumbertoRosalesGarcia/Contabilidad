package com.xxcamixx.contabilidad.network

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.xxcamixx.contabilidad.data.AppDatabase
import com.xxcamixx.contabilidad.model.CierreSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AutoCierreWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val userId = inputData.getString("USER_ID") ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)

        val isEnabled = prefs.getBoolean("autoCierreEnabled", false)
        if (!isEnabled) return Result.success()

        val frequency = prefs.getString("autoCierreFrequency", "DIARIO") ?: "DIARIO"
        val mode = prefs.getString("autoCierreMode", "TODOS") ?: "TODOS"
        val lastClosureTime = prefs.getLong("lastAutoCierreTimestamp", 0L)
        val country = prefs.getString("selectedCountry", "Colombia") ?: "Colombia"

        val now = System.currentTimeMillis()

        // Check elapsed time to prevent duplicate triggers
        if (lastClosureTime > 0L) {
            val diffMillis = now - lastClosureTime
            val diffHours = diffMillis / (1000 * 60 * 60)
            val shouldRun = when (frequency) {
                "DIARIO" -> diffHours >= 18
                "SEMANAL" -> diffHours >= (24 * 6)
                "MENSUAL" -> diffHours >= (24 * 27)
                else -> diffHours >= 18
            }
            if (!shouldRun) return Result.success()
        }

        return try {
            val dao = AppDatabase.getDatabase(applicationContext, userId).financeDao()
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            val modesToClose = if (mode == "TODOS") listOf("PERSONAL", "TIENDA", "PEDIDOS") else listOf(mode)

            for (m in modesToClose) {
                val sessionName = "Cierre Automático $m - $dateStr"
                val session = CierreSession(
                    mode = m,
                    name = sessionName,
                    totalIncomes = 0.0,
                    totalExpenses = 0.0,
                    timestamp = now,
                    country = country
                )
                val sessionId = dao.insertCierreSession(session).toInt()

                when (m) {
                    "PERSONAL" -> {
                        dao.updatePersonalTransactionsWithCierre(country, sessionId)
                        dao.updateFiadoresWithCierre(country, sessionId, "PERSONAL")
                    }
                    "TIENDA" -> {
                        dao.updateTiendaTransactionsWithCierre(country, sessionId)
                        dao.updateFiadoresWithCierre(country, sessionId, "TIENDA")
                    }
                    "PEDIDOS" -> {
                        dao.updateComercioMovementsWithCierre(country, sessionId)
                    }
                }
            }

            prefs.edit().putLong("lastAutoCierreTimestamp", now).apply()

            // Trigger immediate cloud sync for the new closures
            try {
                val syncRequest = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .setInputData(workDataOf("USER_ID" to userId))
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                    "OfflineAutoSync_$userId",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
            } catch (_: Exception) {}

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
