package com.xxcamixx.contabilidad.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val workRequest = OneTimeWorkRequestBuilder<RestoreAlarmsWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
}
