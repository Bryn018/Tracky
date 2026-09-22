package com.tracky.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.tracky.app.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic safety net: re-scans recent SMS so transactions missed by the
 * real-time receiver (e.g. receiver killed, phone off, delivery race) are
 * still captured. The [DuplicateDetector] makes repeated scans idempotent.
 */
@HiltWorker
class SmsScanWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val count = transactionRepository.scanExistingSms(applicationContext, days = 90)
            Log.d(TAG, "Periodic scan imported $count new transactions")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Periodic scan failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sms_scan_work"
        const val TAG = "SmsScanWorker"

        /** Schedule the recurring catch-up scan (every 6 hours). */
        fun schedulePeriodicScan(context: Context) {
            val request = PeriodicWorkRequestBuilder<SmsScanWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /**
         * One-off immediate catch-up scan (used by the real-time receiver
         * when it cannot process a message directly, e.g. no notification
         * access on some OEM ROMs).
         */
        fun enqueueFallbackScan(context: Context) {
            schedulePeriodicScan(context)
        }
    }
}
