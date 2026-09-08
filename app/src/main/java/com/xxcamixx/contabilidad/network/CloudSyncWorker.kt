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
import java.util.UUID

class CloudSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val userId = inputData.getString("USER_ID") ?: return Result.failure()
        val db = AppDatabase.getDatabase(applicationContext, userId).financeDao()
        return try {
            val transactions = db.getBackupTransactions()
            val reminders = db.getBackupReminders()
            val fiadores = db.getBackupFiadores()
            val products = db.getBackupProducts()

            val newData = BackupData(transactions, reminders, fiadores, products)
            val timeString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val newRecord = BackupRecord(UUID.randomUUID().toString(), "Automático - $timeString", System.currentTimeMillis(), newData)

            val remotePayload = RetrofitInstance.api.getBackup(userId)
            val existingBackups = mutableListOf<BackupRecord>()

            if (remotePayload != null) {
                if (remotePayload.backups != null) { existingBackups.addAll(remotePayload.backups) }
                else if (remotePayload.transactions != null) { existingBackups.add(BackupRecord("old", "Respaldo Antiguo", 0L, BackupData(remotePayload.transactions, remotePayload.reminders ?: emptyList(), remotePayload.fiadores ?: emptyList(), remotePayload.products ?: emptyList()))) }
            }

            existingBackups.add(0, newRecord)
            if (existingBackups.size > 15) { existingBackups.removeAt(existingBackups.size - 1) }

            RetrofitInstance.api.uploadBackup(userId, CloudPayload(backups = existingBackups))
            applicationContext.getSharedPreferences("FinancePrefs_$userId", Context.MODE_PRIVATE).edit().putLong("lastSync", System.currentTimeMillis()).apply()
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}
