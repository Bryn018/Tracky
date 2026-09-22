package com.tracky.app.worker

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.tracky.app.MainActivity
import com.tracky.app.R
import com.tracky.app.TrackyApplication
import com.tracky.app.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * One-time worker that runs after the user grants SMS permission:
 * imports the last 90 days of mobile-money SMS history, then arms the
 * periodic catch-up scan that keeps data current from that point forward.
 */
@HiltWorker
class InitialBackfillWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Arm the periodic catch-up scan FIRST so it's always active,
            // even if the backfill below fails permanently (the mutex in the
            // repository prevents the two from double-importing).
            SmsScanWorker.schedulePeriodicScan(applicationContext)

            Log.d(TAG, "Starting 90-day history backfill")
            val imported = transactionRepository.scanExistingSms(applicationContext, days = 90)
            Log.d(TAG, "Backfill imported $imported transactions")

            if (imported > 0) {
                showBackfillNotification(imported)
            }
            Result.success(workDataOf(KEY_IMPORTED to imported))
        } catch (e: Exception) {
            Log.e(TAG, "Backfill failed", e)
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    private fun showBackfillNotification(imported: Int) {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return

        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = applicationContext.getString(R.string.backfill_notification_title)
        val body = applicationContext.getString(R.string.backfill_notification_body, imported)

        val notification = NotificationCompat.Builder(applicationContext, TrackyApplication.CHANNEL_DAILY_REPORT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "No notification permission: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "InitialBackfillWorker"
        private const val NOTIFICATION_ID = 2002
        private const val MAX_RETRIES = 3
        const val WORK_NAME = "initial_backfill_work"
        const val KEY_IMPORTED = "imported_count"

        /**
         * Enqueue the one-time backfill. Safe to call repeatedly —
         * [ExistingWorkPolicy.KEEP] means it only ever runs once.
         */
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<InitialBackfillWorker>()
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
