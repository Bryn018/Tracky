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
import com.tracky.app.MainActivity
import com.tracky.app.R
import com.tracky.app.TrackyApplication
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.repository.TransactionRepository
import com.tracky.app.data.model.Money
import com.tracky.app.data.model.TransactionType
import com.tracky.app.data.sms.SmsParser
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

        // Note: SMS_RECEIVED is delivered to any app holding RECEIVE_SMS, even
        // when not the default SMS app. The periodic SmsScanWorker is the
        // safety net for anything missed here.

        // A single message can carry more than one movement (concatenated
        // confirmations), so all of them are saved, not just the first.
        val transactions = SmsParser.parseAll(fullBody, senderAddress, timestamp)
        if (transactions.isNotEmpty()) {
            // Use goAsync() to keep the receiver alive while we save
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withTimeout(10_000) { // 10 second timeout
                        val saved = transactionRepository.addTransactions(transactions)
                        Log.d(TAG, "Saved $saved/${transactions.size} transactions from SMS")
                        // Notify once per message, not once per movement, so a
                        // concatenated pair does not produce two alerts.
                        transactions.firstOrNull()?.let { showSilentNotification(context, it, saved) }
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

    private fun showSilentNotification(context: Context, transaction: TransactionEntity, count: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return
        }

        val verb = when (TransactionType.fromString(transaction.type)) {
            TransactionType.INCOMING -> "Received"
            TransactionType.OUTGOING -> "Sent"
            TransactionType.REVERSAL -> "Reversed"
            TransactionType.REFUND -> "Refunded"
            null -> "Processed"
        }
        val body = buildString {
            append(verb)
            append(" ")
            append(Money(transaction.amountCents).toDisplayString())
            if (count > 1) append(" (+${count - 1} more)")
            append(" via ")
            append(transaction.channel)
        }

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