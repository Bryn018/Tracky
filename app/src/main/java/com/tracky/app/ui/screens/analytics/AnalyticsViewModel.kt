package com.tracky.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracky.app.data.local.entity.DailySummaryEntity
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.Category
import com.tracky.app.data.model.Money
import com.tracky.app.data.model.TransactionType
import com.tracky.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategoryAnalytics(
    val category: Category,
    val totalSpent: Money,
    val transactionCount: Int,
    val percentageOfTotal: Float
)

data class ContactAnalytics(
    val name: String,
    val totalSpent: Money,
    val transactionCount: Int
)

data class TrendPoint(
    val date: String,
    val amount: Money
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _dailySummary = MutableStateFlow<List<DailySummaryEntity>>(emptyList())
    val dailySummary: StateFlow<List<DailySummaryEntity>> = _dailySummary.asStateFlow()

    private val _weeklyTotal = MutableStateFlow(Money.ZERO)
    val weeklyTotal: StateFlow<Money> = _weeklyTotal.asStateFlow()

    private val _monthlyTotal = MutableStateFlow(Money.ZERO)
    val monthlyTotal: StateFlow<Money> = _monthlyTotal.asStateFlow()

    private val _topChannel = MutableStateFlow("")
    val topChannel: StateFlow<String> = _topChannel.asStateFlow()

    private val _averageDaily = MutableStateFlow(Money.ZERO)
    val averageDaily: StateFlow<Money> = _averageDaily.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<List<CategoryAnalytics>>(emptyList())
    val categoryBreakdown: StateFlow<List<CategoryAnalytics>> = _categoryBreakdown.asStateFlow()

    private val _topContacts = MutableStateFlow<List<ContactAnalytics>>(emptyList())
    val topContacts: StateFlow<List<ContactAnalytics>> = _topContacts.asStateFlow()

    private val _weeklyTrend = MutableStateFlow<List<TrendPoint>>(emptyList())
    val weeklyTrend: StateFlow<List<TrendPoint>> = _weeklyTrend.asStateFlow()

    private val _monthlyIncome = MutableStateFlow(Money.ZERO)
    val monthlyIncome: StateFlow<Money> = _monthlyIncome.asStateFlow()

    private val _totalBalance = MutableStateFlow(Money.ZERO)
    val totalBalance: StateFlow<Money> = _totalBalance.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        // Headline totals come from the SQL aggregation rather than from
        // summing a loaded transaction list in Kotlin.
        viewModelScope.launch {
            transactionRepository.getDailySummaries(30).collect { summaries ->
                _dailySummary.value = summaries

                val weekStart = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                // Summaries arrive as "yyyy-MM-dd"; compare on the date prefix
                // rather than re-parsing each into a Calendar.
                val weekDates = summaries.filter { it.date >= dateKey(weekStart) }
                _weeklyTotal.value = Money(weekDates.sumOf { it.totalOutgoingCents })

                val monthDates = summaries.filter { it.date >= dateKey(monthStart) }
                _monthlyTotal.value = Money(monthDates.sumOf { it.totalOutgoingCents })

                // Average over days that actually had activity, not over a
                // 30-day denominator that would understate a heavy week.
                _averageDaily.value = if (summaries.isNotEmpty()) {
                    Money(summaries.sumOf { it.totalOutgoingCents } / summaries.size)
                } else {
                    Money.ZERO
                }

                _topChannel.value = summaries.mapNotNull { it.topChannel }
                    .groupBy { it }
                    .maxByOrNull { it.value.size }?.key ?: "N/A"
            }
        }

        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { transactions ->
                calculateCategoryAnalytics(transactions)
                calculateTopContacts(transactions)
                calculateWeeklyTrend(transactions)
                calculateMonthlyIncome(transactions)
                calculateTotalBalance(transactions)
            }
        }
    }

    private fun calculateCategoryAnalytics(transactions: List<TransactionEntity>) {
        val monthOutgoing = currentMonthSpending(transactions)

        val totalSpent = monthOutgoing.sumOf { it.amountCents }
        val byCategory = monthOutgoing.groupBy { Category.fromString(it.category) }

        _categoryBreakdown.value = byCategory.map { (category, txs) ->
            val spent = txs.sumOf { it.amountCents }
            CategoryAnalytics(
                category = category,
                totalSpent = Money(spent.coerceAtLeast(0L)),
                transactionCount = txs.size,
                percentageOfTotal = if (totalSpent > 0) {
                    (spent.toDouble() / totalSpent.toDouble()).toFloat()
                } else 0f
            )
        }.sortedByDescending { it.totalSpent }
    }

    private fun calculateTopContacts(transactions: List<TransactionEntity>) {
        _topContacts.value = currentMonthSpending(transactions)
            .groupBy { it.contact ?: "Unknown" }
            .map { (name, txs) ->
                ContactAnalytics(
                    name = name,
                    totalSpent = Money(txs.sumOf { it.amountCents }.coerceAtLeast(0L)),
                    transactionCount = txs.size
                )
            }
            .sortedByDescending { it.totalSpent }
            .take(5)
    }

    private fun calculateWeeklyTrend(transactions: List<TransactionEntity>) {
        val trend = mutableListOf<TrendPoint>()

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            dayCal.set(Calendar.HOUR_OF_DAY, 0)
            dayCal.set(Calendar.MINUTE, 0)
            dayCal.set(Calendar.SECOND, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            val dayStart = dayCal.timeInMillis

            dayCal.set(Calendar.HOUR_OF_DAY, 23)
            dayCal.set(Calendar.MINUTE, 59)
            dayCal.set(Calendar.SECOND, 59)
            dayCal.set(Calendar.MILLISECOND, 999)
            val dayEnd = dayCal.timeInMillis

            val dayTotal = transactions
                .filter { it.timestamp in dayStart..dayEnd }
                .fold(0L) { acc, tx ->
                    acc + (tx.amountCents * typeOf(tx).spendingMultiplier)
                }

            val dateStr = "${dayCal.get(Calendar.MONTH) + 1}/${dayCal.get(Calendar.DAY_OF_MONTH)}"
            trend.add(TrendPoint(date = dateStr, amount = Money(dayTotal.coerceAtLeast(0L))))
        }

        _weeklyTrend.value = trend
    }

    private fun calculateMonthlyIncome(transactions: List<TransactionEntity>) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val income = transactions
            .filter { inMonth(it, currentMonth, currentYear) && typeOf(it) == TransactionType.INCOMING }
            .sumOf { it.amountCents }

        _monthlyIncome.value = Money(income)
    }

    /**
     * Net across all imported history. Reversals and refunds count as credits
     * here, which is what keeps a reversed payment from permanently
     * depressing the running balance.
     */
    private fun calculateTotalBalance(transactions: List<TransactionEntity>) {
        val balance = transactions.fold(0L) { acc, tx ->
            acc + (tx.amountCents * typeOf(tx).balanceMultiplier)
        }
        _totalBalance.value = Money(balance.coerceAtLeast(0L))
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun typeOf(tx: TransactionEntity): TransactionType =
        TransactionType.fromString(tx.type) ?: TransactionType.OUTGOING

    private fun inMonth(tx: TransactionEntity, month: Int, year: Int): Boolean {
        val txCal = Calendar.getInstance()
        txCal.timeInMillis = tx.timestamp
        return txCal.get(Calendar.MONTH) == month && txCal.get(Calendar.YEAR) == year
    }

    /**
     * This month's movements, carrying a signed amount so reversals and
     * refunds net against the spend that produced them.
     */
    private fun currentMonthSpending(transactions: List<TransactionEntity>): List<SignedTransaction> {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        return transactions
            .filter { inMonth(it, currentMonth, currentYear) }
            .mapNotNull { tx ->
                val type = typeOf(tx)
                if (type == TransactionType.INCOMING) return@mapNotNull null
                SignedTransaction(
                    category = tx.category,
                    contact = tx.contact,
                    amountCents = tx.amountCents * type.spendingMultiplier
                )
            }
    }

    private data class SignedTransaction(
        val category: String?,
        val contact: String?,
        val amountCents: Long
    )

    private fun dateKey(millis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
