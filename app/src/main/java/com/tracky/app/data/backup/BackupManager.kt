package com.tracky.app.data.backup

import android.content.Context
import android.net.Uri
import com.tracky.app.data.local.entity.BudgetEntity
import com.tracky.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles export and import of backup data in JSON format.
 */
object BackupManager {

    private const val BACKUP_VERSION = 1

    /**
     * Export all data to a JSON file at the given URI.
     * Returns true if successful.
     */
    suspend fun exportBackup(
        context: Context,
        uri: Uri,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = BackupData(
                version = BACKUP_VERSION,
                exportedAt = System.currentTimeMillis(),
                transactions = transactions,
                budgets = budgets
            )

            val json = buildJsonBackup(backup)

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray())
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Import data from a JSON file at the given URI.
     * Returns BackupData if successful, null otherwise.
     */
    suspend fun importBackup(
        context: Context,
        uri: Uri
    ): BackupData? = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use(BufferedReader::readText)
                ?: return@withContext null

            parseJsonBackup(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildJsonBackup(backup: BackupData): String {
        val root = JSONObject()
        root.put("version", backup.version)
        root.put("exportedAt", backup.exportedAt)

        val transactionsArray = JSONArray()
        backup.transactions.forEach { tx ->
            val txObj = JSONObject()
            txObj.put("id", tx.id)
            txObj.put("type", tx.type)
            txObj.put("amount", tx.amount)
            txObj.put("channel", tx.channel)
            txObj.put("contact", tx.contact ?: "")
            txObj.put("senderName", tx.senderName ?: "")
            txObj.put("messageBody", tx.messageBody)
            txObj.put("timestamp", tx.timestamp)
            txObj.put("balance", tx.balance ?: 0.0)
            txObj.put("notes", tx.notes ?: "")
            txObj.put("category", tx.category ?: "UNCATEGORIZED")
            transactionsArray.put(txObj)
        }
        root.put("transactions", transactionsArray)

        val budgetsArray = JSONArray()
        backup.budgets.forEach { budget ->
            val budgetObj = JSONObject()
            budgetObj.put("category", budget.category)
            budgetObj.put("monthlyLimit", budget.monthlyLimit)
            budgetObj.put("isEnabled", budget.isEnabled)
            budgetObj.put("createdAt", budget.createdAt)
            budgetsArray.put(budgetObj)
        }
        root.put("budgets", budgetsArray)

        return root.toString(2)
    }

    private fun parseJsonBackup(json: String): BackupData? {
        return try {
            val root = JSONObject(json)
            val version = root.getInt("version")
            val exportedAt = root.getLong("exportedAt")

            val transactionsArray = root.getJSONArray("transactions")
            val transactions = mutableListOf<TransactionEntity>()
            for (i in 0 until transactionsArray.length()) {
                val txObj = transactionsArray.getJSONObject(i)
                transactions.add(
                    TransactionEntity(
                        id = txObj.getLong("id"),
                        type = txObj.getString("type"),
                        amount = txObj.getDouble("amount"),
                        channel = txObj.getString("channel"),
                        contact = txObj.optString("contact").ifBlank { null },
                        senderName = txObj.optString("senderName").ifBlank { null },
                        messageBody = txObj.getString("messageBody"),
                        timestamp = txObj.getLong("timestamp"),
                        balance = txObj.optDouble("balance").let { if (it > 0) it else null },
                        notes = txObj.optString("notes").ifBlank { null },
                        category = txObj.optString("category")
                    )
                )
            }

            val budgetsArray = root.optJSONArray("budgets")
            val budgets = mutableListOf<BudgetEntity>()
            if (budgetsArray != null) {
                for (i in 0 until budgetsArray.length()) {
                    val budgetObj = budgetsArray.getJSONObject(i)
                    budgets.add(
                        BudgetEntity(
                            category = budgetObj.getString("category"),
                            monthlyLimit = budgetObj.getDouble("monthlyLimit"),
                            isEnabled = budgetObj.optBoolean("isEnabled", true),
                            createdAt = budgetObj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            BackupData(
                version = version,
                exportedAt = exportedAt,
                transactions = transactions,
                budgets = budgets
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get metadata from a backup file without importing.
     */
    suspend fun peekBackupMetadata(
        context: Context,
        uri: Uri
    ): BackupMetadata? = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use(BufferedReader::readText)
                ?: return@withContext null

            val root = JSONObject(json)
            val transactionsArray = root.getJSONArray("transactions")
            val budgetsArray = root.optJSONArray("budgets")

            BackupMetadata(
                version = root.getInt("version"),
                exportedAt = root.getLong("exportedAt"),
                transactionCount = transactionsArray.length(),
                budgetCount = budgetsArray?.length() ?: 0
            )
        } catch (e: Exception) {
            null
        }
    }
}