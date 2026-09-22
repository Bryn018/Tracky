package com.tracky.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class ReportAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm received, scheduling DailyReportWorker")

        val workRequest = OneTimeWorkRequestBuilder<DailyReportWorker>()
            .addTag(WORK_TAG_DAILY_REPORT)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                WORK_NAME_DAILY_REPORT,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    companion object {
        private const val TAG = "ReportAlarmReceiver"
        const val WORK_NAME_DAILY_REPORT = "daily_report_work"
        const val WORK_TAG_DAILY_REPORT = "daily_report"
    }
}
