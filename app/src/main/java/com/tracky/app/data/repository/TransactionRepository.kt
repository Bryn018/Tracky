package com.tracky.app.data.repository

import android.content.Context
import android.net.Uri
import com.tracky.app.data.ExportHelper
import com.tracky.app.data.local.dao.BudgetDao
import com.tracky.app.data.local.dao.DailySummaryDao
import com.tracky.app.data.local.dao.TransactionDao
import com.tracky.app.data.local.entity.BudgetEntity
import com.tracky.app.data.local.entity.DailySummaryEntity
import com.tracky.app.data.local.entity.DuplicateDetector
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.AutoCategoryManager
import com.tracky.app.data.model.Category
import com.tracky.app.data.sms.SmsReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val dailySummaryDao: DailySummaryDao,
    private val budgetDao: BudgetDao
) {
    suspend fun addTransaction(transaction: TransactionEntity): Boolean {
        val category = AutoCategoryManager.categorize(transaction.messageBody, transaction.channel, transaction.contact)
        val enriched = transaction.copy(category = category.name)

        // Check for duplicates before inserting
        val existing = transactionDao.getAllTransactionsList()
        if (DuplicateDetector.isDuplicate(enriched, existing)) {
            return false // Duplicate detected, not inserted
        }

        transactionDao.insert(enriched)
        return true
    }

    suspend fun addRawTransaction(transaction: TransactionEntity) {
        transactionDao.insert(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.update(transaction)
    }

    suspend fun updateNotes(id: Long, notes: String?) {
        transactionDao.updateNotes(id, notes)
    }

    suspend fun updateCategory(id: Long, category: Category) {
        transactionDao.updateCategory(id, category.name)
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionByIdFlow(id: Long): Flow<TransactionEntity?> {
        return transactionDao.getTransactionByIdFlow(id)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteById(id)
    }

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByType(type)

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    suspend fun scanExistingSms(context: Context): Int {
        val smsReader = SmsReader(context)
        val transactions = smsReader.readExistingSms()
        if (transactions.isNotEmpty()) {
            val existing = transactionDao.getAllTransactionsList()
            val newTransactions = transactions.filter { newTx ->
                !DuplicateDetector.isDuplicate(newTx, existing)
            }
            if (newTransactions.isNotEmpty()) {
                transactionDao.insertAll(newTransactions)
            }
            return newTransactions.size
        }
        return 0
    }

    suspend fun exportToCsv(context: Context, uri: Uri): Int {
        val transactions = transactionDao.getAllTransactionsList()
        if (transactions.isEmpty()) return 0
        return ExportHelper.exportTransactionsToCsv(context, transactions, uri)
    }

    suspend fun clearAllData() {
        transactionDao.deleteAll()
        budgetDao.deleteAll()
    }

    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions().map { list -> list.take(limit) }

    fun getTodaySummary(): Flow<DailySummaryEntity?> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val dayEnd = cal.timeInMillis
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        return transactionDao.getTodaySummary(dayStart, dayEnd, dateString)
    }

    fun getTransactionsForPeriod(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(startMillis, endMillis)

    fun getTransactionsByChannel(channel: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByChannel(channel)

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(start, end)

    fun getWeeklySummary(): Flow<DailySummaryEntity?> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val weekStart = cal.timeInMillis
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateString = dateFormat.format(Date())
        return transactionDao.getTodaySummary(weekStart, now, dateString)
    }

    fun getTodaySpending(): Flow<Double> =
        getTodaySummary().map { it?.totalOutgoing ?: 0.0 }

    fun getTodayIncome(): Flow<Double> =
        getTodaySummary().map { it?.totalIncoming ?: 0.0 }

    fun getDailySummaries(days: Int): Flow<List<DailySummaryEntity>> {
        return transactionDao.getAllTransactions().map { transactions ->
            val grouped = transactions.groupBy {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.timestamp
                "%04d-%02d-%02d".format(
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)
                )
            }
            grouped.entries
                .map { (date, txs) ->
                    DailySummaryEntity(
                        date = date,
                        totalIncoming = txs.filter { it.type == "INCOMING" }.sumOf { it.amount },
                        totalOutgoing = txs.filter { it.type == "OUTGOING" }.sumOf { it.amount },
                        transactionCount = txs.size,
                        topChannel = txs.groupBy { it.channel }.maxByOrNull { it.value.size }?.key,
                        primaryContact = txs.mapNotNull { it.contact }
                            .groupBy { it }.maxByOrNull { it.value.size }?.key
                    )
                }
                .sortedByDescending { it.date }
                .take(days)
        }
    }

    // Budget management
    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun setBudget(category: Category, limit: Double) {
        budgetDao.insert(BudgetEntity(category = category.name, monthlyLimit = limit))
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.update(budget)
    }

    suspend fun deleteBudget(category: Category) {
        budgetDao.deleteBudget(category.name)
    }

    suspend fun getBudget(category: Category): BudgetEntity? {
        return budgetDao.getBudget(category.name)
    }

    fun getBudgetFlow(category: Category): Flow<BudgetEntity?> {
        return budgetDao.getBudgetFlow(category.name)
    }
}