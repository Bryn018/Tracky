package com.tracky.app.data.model

import com.tracky.app.data.local.entity.TransactionEntity
import java.util.Calendar

/**
 * Calculates current spending per category for a given month.
 *
 * All arithmetic is in integer shilling cents. The previous version summed
 * Doubles, so a category total could land a cent either side of a budget
 * limit purely from float representation.
 */
object BudgetCalculator {

    data class CategorySpending(
        val category: Category,
        val spentCents: Long,
        val budgetCents: Long?
    ) {
        val isOverBudget: Boolean
            get() = budgetCents != null && spentCents > budgetCents!!

        val remainingCents: Long?
            get() = budgetCents?.let { it - spentCents }

        val progressFraction: Float
            get() = if (budgetCents != null && budgetCents!! > 0) {
                (spentCents.toDouble() / budgetCents!!.toDouble()).toFloat().coerceIn(0f, 1.5f)
            } else 0f
    }

    /**
     * Spending per category for the current month, net of reversals and
     * refunds. A reversed purchase no longer counts against its budget.
     */
    fun calculateSpending(
        transactions: List<TransactionEntity>,
        budgets: List<CategorySpending>
    ): List<CategorySpending> {
        val spendingByCategory = currentMonthSpending(transactions)

        return budgets.map { budget ->
            val spent = spendingByCategory[budget.category] ?: 0L
            CategorySpending(
                category = budget.category,
                spentCents = spent,
                budgetCents = budget.budgetCents
            )
        }
    }

    /** Spending for every category present this month, including unbudgeted ones. */
    fun calculateAllSpending(transactions: List<TransactionEntity>): Map<Category, Long> =
        currentMonthSpending(transactions)

    private fun currentMonthSpending(transactions: List<TransactionEntity>): Map<Category, Long> {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        return transactions
            .filter { inMonth(it, currentMonth, currentYear) }
            .groupBy { Category.fromString(it.category) }
            .mapValues { (_, txs) ->
                txs.fold(0L) { acc, tx ->
                    val type = TransactionType.fromString(tx.type) ?: TransactionType.OUTGOING
                    acc + (tx.amountCents * type.spendingMultiplier)
                }
            }
            .filterValues { it != 0L }
    }

    private fun inMonth(tx: TransactionEntity, month: Int, year: Int): Boolean {
        val txCal = Calendar.getInstance()
        txCal.timeInMillis = tx.timestamp
        return txCal.get(Calendar.MONTH) == month && txCal.get(Calendar.YEAR) == year
    }
}
