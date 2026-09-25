package com.xxcamixx.contabilidad.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xxcamixx.contabilidad.model.BackupData
import com.xxcamixx.contabilidad.model.BackupRecord
import com.xxcamixx.contabilidad.model.CloudPayload
import com.xxcamixx.contabilidad.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CloudSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val userId = inputData.getString("USER_ID") ?: return Result.failure()
        val db = AppDatabase.getDatabase(applicationContext, userId).financeDao()
        return try {
            val transactions = db.getBackupTransactions().map { it.copy(imageUri = null) }
            val reminders = db.getBackupReminders()
            val fiadores = db.getBackupFiadores()
            val products = db.getBackupProducts().map { it.copy(imageUri = null) }
            val comercioProducts = db.getBackupComercioProducts().map { it.copy(imageUri = null) }
            val comercioPedidos = db.getBackupComercioPedidos().map { it.copy(imageUri = null) }
            val comercioMovements = db.getBackupComercioMovements()
            val cierreSessions = db.getBackupCierreSessions()

            val currentData = BackupData(
                transactions = transactions,
                reminders = reminders,
                fiadores = fiadores,
                products = products,
                comercioProducts = comercioProducts,
                comercioPedidos = comercioPedidos,
                comercioMovements = comercioMovements,
                cierreSessions = cierreSessions
            )
            val timeString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val autoRecord = BackupRecord(
                id = "auto_sync_latest",
                name = "Sincronización Automática ($timeString)",
                timestamp = System.currentTimeMillis(),
                data = currentData
            )

            val remotePayload = try { RetrofitInstance.api.getBackup(userId) } catch (_: Exception) { null }
            val existingBackups = mutableListOf<BackupRecord>()

            if (remotePayload != null) {
                if (remotePayload.backups != null) {
                    existingBackups.addAll(remotePayload.backups.filter { it.id != "auto_sync_latest" })
                } else if (remotePayload.transactions != null) {
                    existingBackups.add(
                        BackupRecord(
                            "old",
                            "Respaldo Antiguo",
                            0L,
                            BackupData(
                                remotePayload.transactions,
                                remotePayload.reminders ?: emptyList(),
                                remotePayload.fiadores ?: emptyList(),
                                remotePayload.products ?: emptyList()
                            )
                        )
                    )
                }
            }

            existingBackups.add(0, autoRecord)
            if (existingBackups.size > 20) {
                existingBackups.removeAt(existingBackups.size - 1)
            }

            val payload = CloudPayload(
                backups = existingBackups,
                transactions = transactions,
                reminders = reminders,
                fiadores = fiadores,
                products = products,
                comercioProducts = comercioProducts,
                comercioPedidos = comercioPedidos,
                comercioMovements = comercioMovements,
                cierreSessions = cierreSessions
            )

            RetrofitInstance.api.uploadBackup(userId, payload)
            applicationContext.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE)
                .edit().putLong("lastSync", System.currentTimeMillis()).apply()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
