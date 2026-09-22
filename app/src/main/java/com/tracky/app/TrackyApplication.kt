package com.tracky.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tracky.app.worker.BudgetAlertScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TrackyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        BudgetAlertScheduler.scheduleBudgetAlerts(this)
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        val dailyReportChannel = NotificationChannel(
            CHANNEL_DAILY_REPORT,
            getString(R.string.daily_report_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.daily_report_channel_desc)
        }

        val smsMonitoringChannel = NotificationChannel(
            CHANNEL_SMS_MONITORING,
            getString(R.string.sms_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.sms_notification_channel_desc)
            setShowBadge(false)
        }

        notificationManager.createNotificationChannel(dailyReportChannel)
        notificationManager.createNotificationChannel(smsMonitoringChannel)
    }

    companion object {
        const val CHANNEL_DAILY_REPORT = "daily_report"
        const val CHANNEL_SMS_MONITORING = "sms_monitoring"
    }
}