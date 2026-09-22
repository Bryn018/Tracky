package com.tracky.app.data.model

import com.tracky.app.data.local.entity.TransactionEntity
import java.util.Calendar

/**
 * Calculates current spending per category for a given month.
 */
object BudgetCalculator {

    data class CategorySpending(
        val category: Category,
        val spent: Double,
        val budget: Double?,
        val remaining: Double?
    ) {
        val isOverBudget: Boolean
            get() = budget != null && spent > budget

        val progressFraction: Float
            get() = if (budget != null && budget > 0) (spent / budget).toFloat().coerceIn(0f, 1.5f) else 0f
    }

    /**
     * Get spending summary for each category with a budget.
     */
    fun calculateSpending(
        transactions: List<TransactionEntity>,
        budgets: List<CategorySpending>
    ): List<CategorySpending> {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        // Filter to current month's OUTGOING transactions
        val monthTransactions = transactions.filter {
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = it.timestamp
            txCal.get(Calendar.MONTH) == currentMonth &&
                txCal.get(Calendar.YEAR) == currentYear &&
                it.type == "OUTGOING"
        }

        // Group by category
        val spendingByCategory = monthTransactions.groupBy { Category.fromString(it.category) }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        return budgets.map { budget ->
            val spent = spendingByCategory[budget.category] ?: 0.0
            CategorySpending(
                category = budget.category,
                spent = spent,
                budget = budget.budget,
                remaining = budget.budget?.let { it - spent }
            )
        }
    }

    /**
     * Get spending for all categories (even those without budgets).
     */
    fun calculateAllSpending(transactions: List<TransactionEntity>): Map<Category, Double> {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        return transactions.filter {
            val txCal = Calendar.getInstance()
            txCal.timeInMillis = it.timestamp
            txCal.get(Calendar.MONTH) == currentMonth &&
                txCal.get(Calendar.YEAR) == currentYear &&
                it.type == "OUTGOING"
        }
            .groupBy { Category.fromString(it.category) }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
    }
}