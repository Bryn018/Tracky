package com.tracky.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object BudgetAlertScheduler {

    const val WORK_NAME = "budget_alert_check"

    fun scheduleBudgetAlerts(context: Context) {
        val request = PeriodicWorkRequestBuilder<BudgetAlertWorker>(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelBudgetAlerts(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}