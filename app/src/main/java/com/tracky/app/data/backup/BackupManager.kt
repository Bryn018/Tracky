package com.tracky.app.data.backup

import android.content.Context
import android.net.Uri
import com.tracky.app.data.local.entity.BudgetEntity
import com.tracky.app.data.local.entity.SmsIdentity
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

    /**
     * v2 stores money as integer cents under explicit `…Cents` keys.
     *
     * v1 wrote shillings as a JSON double under `amount` / `balance` /
     * `monthlyLimit`. Import still reads v1, converting through
     * [toCents], so a backup taken by a previous install restores to the same
     * value rather than being off by a factor of 100 or rejected outright.
     */
    private const val BACKUP_VERSION = 2

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
            txObj.put("amountCents", tx.amountCents)
            txObj.put("channel", tx.channel)
            txObj.put("contact", tx.contact ?: "")
            txObj.put("senderName", tx.senderName ?: "")
            txObj.put("messageBody", tx.messageBody)
            txObj.put("timestamp", tx.timestamp)
            // Absent rather than 0 when unknown: "balance unknown" and
            // "balance is zero" are different facts.
            tx.balanceCents?.let { txObj.put("balanceCents", it) }
            txObj.put("notes", tx.notes ?: "")
            txObj.put("category", tx.category ?: "UNCATEGORIZED")
            txObj.put("smsKey", tx.smsKey)
            transactionsArray.put(txObj)
        }
        root.put("transactions", transactionsArray)

        val budgetsArray = JSONArray()
        backup.budgets.forEach { budget ->
            val budgetObj = JSONObject()
            budgetObj.put("category", budget.category)
            budgetObj.put("monthlyLimitCents", budget.monthlyLimitCents)
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
                        type = txObj.optString("type", "OUTGOING"),
                        amountCents = readCents(txObj, "amountCents", "amount"),
                        channel = txObj.getString("channel"),
                        contact = txObj.optString("contact").ifBlank { null },
                        senderName = txObj.optString("senderName").ifBlank { null },
                        messageBody = txObj.getString("messageBody"),
                        timestamp = txObj.getLong("timestamp"),
                        balanceCents = readOptionalCents(txObj, "balanceCents", "balance"),
                        notes = txObj.optString("notes").ifBlank { null },
                        category = txObj.optString("category"),
                        // v1 backups predate smsKey. Deriving it from the row's
                        // own content keeps the unique index satisfiable and
                        // stops the restored row colliding with a later
                        // re-import of the same message.
                        smsKey = txObj.optString("smsKey").ifBlank {
                            SmsIdentity.key(
                                txObj.optString("senderName", txObj.optString("contact", "")),
                                txObj.getLong("timestamp"),
                                txObj.getString("messageBody")
                            )
                        }
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
                            monthlyLimitCents = readCents(budgetObj, "monthlyLimitCents", "monthlyLimit"),
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

    /**
     * Read a money field in either format: `centsKey` holds integer cents
     * (v2), `legacyKey` holds shillings as a double (v1).
     */
    private fun readCents(obj: JSONObject, centsKey: String, legacyKey: String): Long {
        if (obj.has(centsKey)) {
            return obj.optLong(centsKey, 0L).coerceAtLeast(0L)
        }
        if (obj.has(legacyKey)) {
            return toCents(obj.optDouble(legacyKey, 0.0))
        }
        return 0L
    }

    /** As [readCents], but absent stays absent rather than becoming zero. */
    private fun readOptionalCents(obj: JSONObject, centsKey: String, legacyKey: String): Long? {
        if (obj.has(centsKey)) {
            return obj.optLong(centsKey, 0L).takeIf { it > 0L }
        }
        if (obj.has(legacyKey)) {
            return toCents(obj.optDouble(legacyKey, 0.0)).takeIf { it > 0L }
        }
        return null
    }

    private fun toCents(shillings: Double): Long {
        if (shillings.isNaN() || shillings.isInfinite()) return 0L
        return Math.round(shillings * 100.0).coerceAtLeast(0L)
    }
}
