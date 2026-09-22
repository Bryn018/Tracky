package com.tracky.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tracky.app.MainActivity
import com.tracky.app.R
import com.tracky.app.TrackyApplication
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.repository.TransactionRepository
import com.tracky.app.data.sms.SmsParser
import com.tracky.app.util.SystemUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val messageBody = StringBuilder()
        var sender: String? = null
        var timestamp: Long = System.currentTimeMillis()

        for (msg in messages) {
            messageBody.append(msg.messageBody ?: "")
            if (sender == null) {
                sender = msg.originatingAddress
            }
            if (msg.timestampMillis > 0) {
                timestamp = msg.timestampMillis
            }
        }

        val fullBody = messageBody.toString()
        val senderAddress = sender ?: ""
Log.d(TAG, "SMS received from $senderAddress: ${fullBody.take(30)}...")

        // On Android 14+, only process if we are the default SMS app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!SystemUtils.isDefaultSmsApp(context)) {
                Log.d(TAG, "Not default SMS app on Android 14+, scheduling fallback scan")
                SmsScanWorker.enqueueFallbackScan(context)
                return
            }
        }

        val transaction = SmsParser.parseSms(fullBody, senderAddress, timestamp)
        if (transaction != null) {
            // Use goAsync() to keep the receiver alive while we save
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withTimeout(10_000) { // 10 second timeout
                        transactionRepository.addTransaction(transaction)
                        Log.d(TAG, "Transaction saved: ${transaction.amount} ${transaction.type}")
                        showSilentNotification(context, transaction)
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Timeout saving transaction", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save transaction", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            Log.d(TAG, "SMS is not a transaction, ignored")
        }
    }

    private fun showSilentNotification(context: Context, transaction: TransactionEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return
        }

        val type = if (transaction.type == "INCOMING") "Received" else "Sent"
        val body = "$type ${transaction.amount} via ${transaction.channel}"

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, TrackyApplication.CHANNEL_SMS_MONITORING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Transaction Detected")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(SMS_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "No notification permission, skipping notification: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "SmsReceiver"
        private const val SMS_NOTIFICATION_ID = 2001
    }
}
