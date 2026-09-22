package com.tracky.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tracky.app.MainActivity
import com.tracky.app.R
import com.tracky.app.TrackyApplication
import com.tracky.app.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val summary = transactionRepository.getTodaySummary().first()

        if (summary == null || summary.transactionCount == 0) {
            showNoTransactionNotification()
            return Result.success()
        }

        val spentAmount = "Ksh ${String.format("%.0f", summary.totalOutgoing)}"
        val count = summary.transactionCount

        showReportNotification(spentAmount, count)

        return Result.success()
    }

    private fun showReportNotification(amount: String, count: Int) {
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            TrackyApplication.CHANNEL_DAILY_REPORT
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.daily_report_title))
            .setContentText(
                applicationContext.getString(R.string.daily_report_content, amount, count)
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    applicationContext.getString(R.string.daily_report_content, amount, count)
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(REPORT_NOTIFICATION_ID, notification)
    }

    private fun showNoTransactionNotification() {
        val notification = NotificationCompat.Builder(
            applicationContext,
            TrackyApplication.CHANNEL_DAILY_REPORT
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.daily_report_title))
            .setContentText("No transactions recorded today")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(REPORT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "DailyReportWorker"
        private const val REPORT_NOTIFICATION_ID = 1002
    }
}
