package com.tracky.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracky.app.data.local.entity.DailySummaryEntity
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.Category
import com.tracky.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategoryAnalytics(
    val category: Category,
    val totalSpent: Double,
    val transactionCount: Int,
    val percentageOfTotal: Float
)

data class ContactAnalytics(
    val name: String,
    val totalSpent: Double,
    val transactionCount: Int
)

data class TrendPoint(
    val date: String,
    val amount: Double
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _dailySummary = MutableStateFlow<List<DailySummaryEntity>>(emptyList())
    val dailySummary: StateFlow<List<DailySummaryEntity>> = _dailySummary.asStateFlow()

    private val _weeklyTotal = MutableStateFlow(0.0)
    val weeklyTotal: StateFlow<Double> = _weeklyTotal.asStateFlow()

    private val _monthlyTotal = MutableStateFlow(0.0)
    val monthlyTotal: StateFlow<Double> = _monthlyTotal.asStateFlow()

    private val _topChannel = MutableStateFlow("")
    val topChannel: StateFlow<String> = _topChannel.asStateFlow()

    private val _averageDaily = MutableStateFlow(0.0)
    val averageDaily: StateFlow<Double> = _averageDaily.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<List<CategoryAnalytics>>(emptyList())
    val categoryBreakdown: StateFlow<List<CategoryAnalytics>> = _categoryBreakdown.asStateFlow()

    private val _topContacts = MutableStateFlow<List<ContactAnalytics>>(emptyList())
    val topContacts: StateFlow<List<ContactAnalytics>> = _topContacts.asStateFlow()

    private val _weeklyTrend = MutableStateFlow<List<TrendPoint>>(emptyList())
    val weeklyTrend: StateFlow<List<TrendPoint>> = _weeklyTrend.asStateFlow()

    private val _monthlyIncome = MutableStateFlow(0.0)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    private val _totalBalance = MutableStateFlow(0.0)
    val totalBalance: StateFlow<Double> = _totalBalance.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            transactionRepository.getDailySummaries(30).collect { summaries ->
                _dailySummary.value = summaries

                // Weekly total
                val weekStart = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                _weeklyTotal.value = summaries.filter {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = it.date.replace("-", "").take(8).let { d ->
                        // Parse date string to millis for comparison
                        val parts = it.date.split("-")
                        Calendar.getInstance().apply {
                            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    }
                    cal.timeInMillis >= weekStart
                }.sumOf { it.totalOutgoing }

                // Monthly total
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                _monthlyTotal.value = summaries.filter {
                    val parts = it.date.split("-")
                    val cal = Calendar.getInstance().apply {
                        set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    cal.timeInMillis >= monthStart
                }.sumOf { it.totalOutgoing }

                // Average daily
                _averageDaily.value = if (summaries.isNotEmpty()) {
                    summaries.sumOf { it.totalOutgoing } / summaries.size
                } else 0.0

                // Top channel
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
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val monthOutgoing = transactions.filter {
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = it.timestamp
            txCal.get(Calendar.MONTH) == currentMonth &&
                txCal.get(Calendar.YEAR) == currentYear &&
                it.type == "OUTGOING"
        }

        val totalSpent = monthOutgoing.sumOf { it.amount }
        val byCategory = monthOutgoing.groupBy { Category.fromString(it.category) }

        _categoryBreakdown.value = byCategory.map { (category, txs) ->
            CategoryAnalytics(
                category = category,
                totalSpent = txs.sumOf { it.amount },
                transactionCount = txs.size,
                percentageOfTotal = if (totalSpent > 0) (txs.sumOf { it.amount } / totalSpent).toFloat() else 0f
            )
        }.sortedByDescending { it.totalSpent }
    }

    private fun calculateTopContacts(transactions: List<TransactionEntity>) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        val monthOutgoing = transactions.filter {
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = it.timestamp
            txCal.get(Calendar.MONTH) == currentMonth &&
                txCal.get(Calendar.YEAR) == currentYear &&
                it.type == "OUTGOING"
        }

        _topContacts.value = monthOutgoing
            .groupBy { it.contact ?: "Unknown" }
            .map { (name, txs) ->
                ContactAnalytics(
                    name = name,
                    totalSpent = txs.sumOf { it.amount },
                    transactionCount = txs.size
                )
            }
            .sortedByDescending { it.totalSpent }
            .take(5)
    }

    private fun calculateWeeklyTrend(transactions: List<TransactionEntity>) {
        val cal = Calendar.getInstance()
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

            val dayTotal = transactions.filter {
                it.timestamp in dayStart..dayEnd && it.type == "OUTGOING"
            }.sumOf { it.amount }

            val dateStr = "${dayCal.get(Calendar.MONTH) + 1}/${dayCal.get(Calendar.DAY_OF_MONTH)}"
            trend.add(TrendPoint(date = dateStr, amount = dayTotal))
        }

        _weeklyTrend.value = trend
    }

    private fun calculateMonthlyIncome(transactions: List<TransactionEntity>) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        _monthlyIncome.value = transactions.filter {
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = it.timestamp
            txCal.get(Calendar.MONTH) == currentMonth &&
                txCal.get(Calendar.YEAR) == currentYear &&
                it.type == "INCOMING"
        }.sumOf { it.amount }
    }

    private fun calculateTotalBalance(transactions: List<TransactionEntity>) {
        val totalIncoming = transactions.filter { it.type == "INCOMING" }.sumOf { it.amount }
        val totalOutgoing = transactions.filter { it.type == "OUTGOING" }.sumOf { it.amount }
        _totalBalance.value = totalIncoming - totalOutgoing
    }
}