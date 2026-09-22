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
import com.tracky.app.data.local.entity.BudgetEntity
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.BudgetCalculator
import com.tracky.app.data.model.Category
import com.tracky.app.data.repository.TransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

@HiltWorker
class BudgetAlertWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionRepository: TransactionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val budgets = transactionRepository.getAllBudgets().first()
            val transactions = transactionRepository.getAllTransactions().first()

            val budgetSpending = budgets.map {
                BudgetCalculator.CategorySpending(
                    category = Category.fromString(it.category),
                    spent = 0.0,
                    budget = it.monthlyLimit,
                    remaining = it.monthlyLimit
                )
            }

            val spending = BudgetCalculator.calculateSpending(transactions, budgetSpending)

            for (item in spending) {
                if (item.isOverBudget) {
                    showBudgetAlert(item.category.displayName, item.spent, item.budget ?: 0.0)
                } else if (item.progressFraction > 0.8f && item.budget != null) {
                    showBudgetWarning(item.category.displayName, item.spent, item.budget)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showBudgetAlert(category: String, spent: Double, budget: Double) {
        showNotification(
            "Budget Exceeded!",
            "You've spent Ksh ${String.format("%.0f", spent)} of your Ksh ${String.format("%.0f", budget)} $category budget"
        )
    }

    private fun showBudgetWarning(category: String, spent: Double, budget: Double) {
        showNotification(
            "Budget Warning",
            "You've used ${String.format("%.0f", (spent / budget) * 100)}% of your $category budget"
        )
    }

    private fun showNotification(title: String, content: String) {
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(applicationContext, TrackyApplication.CHANNEL_DAILY_REPORT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(BUDGET_ALERT_NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // No notification permission
        }
    }

    companion object {
        private const val TAG = "BudgetAlertWorker"
        private const val BUDGET_ALERT_NOTIFICATION_ID = 1003
    }
}