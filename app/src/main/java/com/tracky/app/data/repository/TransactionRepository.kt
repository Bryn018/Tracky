package com.tracky.app.data.repository

import android.content.Context
import android.net.Uri
import com.tracky.app.data.ExportHelper
import com.tracky.app.data.local.dao.BudgetDao
import com.tracky.app.data.local.dao.DailySummaryDao
import com.tracky.app.data.local.dao.TransactionDao
import com.tracky.app.data.local.entity.BudgetEntity
import com.tracky.app.data.local.entity.DailySummaryEntity
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.AutoCategoryManager
import com.tracky.app.data.model.Category
import com.tracky.app.data.model.Money
import com.tracky.app.data.model.TransactionType
import com.tracky.app.data.sms.SmsReader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    @Suppress("unused") private val dailySummaryDao: DailySummaryDao,
    private val budgetDao: BudgetDao
) {
    /**
     * Serialises the dedup-check-then-insert sequences. Without it the
     * real-time receiver, the initial backfill and the periodic scan can
     * interleave and import the same message twice.
     *
     * Note what this mutex no longer has to guard: the previous version held
     * it across a full-table read and linear scan on every incoming message,
     * so the cost of the fix grew with the size of the user's history. Dedup
     * is now an indexed key lookup, so the critical section is small and
     * roughly constant no matter how many rows exist.
     */
    private val writeMutex = Mutex()

    /**
     * Insert [transactions], skipping any whose source SMS has already been
     * imported. Returns the number actually inserted.
     *
     * Dedup keys on SMS identity (sender + timestamp + body) rather than on
     * amount/contact proximity. The old rule could not tell a re-delivered
     * message from two genuine identical purchases, and silently deleted the
     * real one.
     */
    suspend fun addTransactions(transactions: List<TransactionEntity>): Int = writeMutex.withLock {
        if (transactions.isEmpty()) return@withLock 0

        val enriched = transactions.map { tx ->
            val category = AutoCategoryManager.categorize(tx.messageBody, tx.channel, tx.contact)
            tx.copy(category = category.name)
        }

        // Dedup within the batch as well as against the database: a re-scan
        // can hand us the same message twice in a single pass.
        val seenKeys = mutableSetOf<String>()
        val unique = enriched.filter { seenKeys.add(it.smsKey) }

        val candidateKeys = unique.map { it.smsKey }.distinct()
        val existing = transactionDao.findExistingKeys(candidateKeys).toHashSet()
        val fresh = unique.filter { it.smsKey !in existing }
        if (fresh.isEmpty()) return@withLock 0

        // The unique index on smsKey is the backstop: even if two callers
        // somehow raced past the check, the second insert is ignored.
        val inserted = transactionDao.insertAll(fresh)
        inserted.count { it != -1L }
    }

    /** Single-transmission convenience wrapper. */
    suspend fun addTransaction(transaction: TransactionEntity): Boolean =
        addTransactions(listOf(transaction)) > 0

    /**
     * Insert without dedup, for backup restore where rows come from a
     * known-good export and must be preserved verbatim.
     */
    suspend fun addRawTransaction(transaction: TransactionEntity) {
        writeMutex.withLock { transactionDao.insert(transaction) }
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

    suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    fun getTransactionByIdFlow(id: Long): Flow<TransactionEntity?> =
        transactionDao.getTransactionByIdFlow(id)

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteById(id)
    }

    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByType(type.name)

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    /**
     * Re-read recent SMS and import anything not already held. Uses the same
     * [addTransactions] path, so this is idempotent by construction rather
     * than by a proximity heuristic.
     */
    suspend fun scanExistingSms(context: Context, days: Int = 90): Int {
        val smsReader = SmsReader(context)
        return addTransactions(smsReader.readExistingSms(days))
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

    // ── Summaries (aggregated in SQL over integer cents) ───────────────

    fun getTodaySummary(): Flow<DailySummaryEntity?> {
        val (start, end) = dayBounds()
        return transactionDao.getSummaryForPeriod(start, end, todayString())
    }

    fun getWeeklySummary(): Flow<DailySummaryEntity?> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -7)
        startOfDayMillis(cal)
        return transactionDao.getSummaryForPeriod(cal.timeInMillis, System.currentTimeMillis(), todayString())
    }

    fun getSummaryFor(startMillis: Long, endMillis: Long, label: String = todayString()): Flow<DailySummaryEntity?> =
        transactionDao.getSummaryForPeriod(startMillis, endMillis, label)

    fun getTodaySpending(): Flow<Money> =
        getTodaySummary().map { Money((it?.totalOutgoingCents ?: 0L).coerceAtLeast(0L)) }

    fun getTodayIncome(): Flow<Money> =
        getTodaySummary().map { Money((it?.totalIncomingCents ?: 0L).coerceAtLeast(0L)) }

    /**
     * Per-day totals for the last [days] days.
     *
     * This previously grouped the entire transaction table in Kotlin on every
     * emission, re-running as new messages arrived. It is now a SQL GROUP BY
     * over the timestamp index, so cost tracks the window requested rather
     * than the total history held.
     */
    fun getDailySummaries(days: Int): Flow<List<DailySummaryEntity>> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        startOfDayMillis(cal)
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()

        return transactionDao.getDailyTotals(start, end).map { rows ->
            rows.map { row ->
                DailySummaryEntity(
                    date = row.date,
                    totalIncomingCents = row.totalIncomingCents.coerceAtLeast(0L),
                    totalOutgoingCents = row.totalOutgoingCents.coerceAtLeast(0L),
                    transactionCount = row.transactionCount
                )
            }
        }
    }

    fun getTransactionsForPeriod(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(startMillis, endMillis)

    fun getTransactionsByChannel(channel: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByChannel(channel)

    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(start, end)

    /** Spend in one category over a window, net of reversals and refunds. */
    fun getCategorySpend(category: Category, startMillis: Long, endMillis: Long): Flow<Money> =
        transactionDao.getCategorySpendFlow(category.name, startMillis, endMillis)
            .map { Money(it.coerceAtLeast(0L)) }

    // ── Budgets ────────────────────────────────────────────────────────

    fun getAllBudgets(): Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun setBudget(category: Category, limit: Money) {
        budgetDao.insert(BudgetEntity(category = category.name, monthlyLimitCents = limit.cents))
    }

    /** Insert-or-replace — used by backup restore where rows may not exist yet. */
    suspend fun upsertBudget(budget: BudgetEntity) {
        budgetDao.insert(budget)
    }

    suspend fun updateBudget(budget: BudgetEntity) {
        budgetDao.update(budget)
    }

    suspend fun deleteBudget(category: Category) {
        budgetDao.deleteBudget(category.name)
    }

    suspend fun getBudget(category: Category): BudgetEntity? =
        budgetDao.getBudget(category.name)

    fun getBudgetFlow(category: Category): Flow<BudgetEntity?> =
        budgetDao.getBudgetFlow(category.name)

    // ── Date helpers ───────────────────────────────────────────────────

    internal fun todayString(): String {
        val cal = Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun startOfDayMillis(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun dayBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val start = startOfDayMillis(cal)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.add(Calendar.MILLISECOND, -1)
        return start to cal.timeInMillis
    }

    fun monthBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val start = startOfDayMillis(cal)
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        return start to cal.timeInMillis
    }

    // ── Entity <-> Money accessors ─────────────────────────────────────
    //
    // The database stores integer cents; everything above this line works in
    // [Money]. Keeping the conversion in one place is what stops a raw cents
    // value from leaking into formatting or a budget comparison.

    fun TransactionEntity.money(): Money = Money(amountCents)

    fun TransactionEntity.balance(): Money? = balanceCents?.let { Money(it) }

    fun TransactionEntity.transactionType(): TransactionType =
        TransactionType.fromString(type) ?: TransactionType.OUTGOING

    fun DailySummaryEntity.totalIncoming(): Money = Money(totalIncomingCents.coerceAtLeast(0L))

    fun DailySummaryEntity.totalOutgoing(): Money = Money(totalOutgoingCents.coerceAtLeast(0L))

    fun BudgetEntity.limit(): Money = Money(monthlyLimitCents)

    fun transactionFromCents(
        type: TransactionType,
        amountCents: Long,
        channel: String,
        contact: String?,
        messageBody: String,
        timestamp: Long,
        balanceCents: Long?,
        category: String?,
        smsKey: String
    ): TransactionEntity = TransactionEntity(
        type = type.name,
        amountCents = amountCents,
        channel = channel,
        contact = contact,
        senderName = contact,
        messageBody = messageBody,
        timestamp = timestamp,
        balanceCents = balanceCents,
        category = category,
        smsKey = smsKey
    )
}