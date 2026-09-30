package com.tracky.app.ui.screens.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracky.app.data.local.entity.BudgetEntity
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.BudgetCalculator
import com.tracky.app.data.model.Category
import com.tracky.app.data.repository.TransactionRepository
import com.tracky.app.data.model.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _budgets = MutableStateFlow<List<BudgetEntity>>(emptyList())
    val budgets: StateFlow<List<BudgetEntity>> = _budgets.asStateFlow()

    private val _categorySpending = MutableStateFlow<List<BudgetCalculator.CategorySpending>>(emptyList())
    val categorySpending: StateFlow<List<BudgetCalculator.CategorySpending>> = _categorySpending.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _editingCategory = MutableStateFlow<Category?>(null)
    val editingCategory: StateFlow<Category?> = _editingCategory.asStateFlow()

    private val _editingLimit = MutableStateFlow("")
    val editingLimit: StateFlow<String> = _editingLimit.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadBudgets()
        observeSpending()
    }

    private fun loadBudgets() {
        viewModelScope.launch {
            transactionRepository.getAllBudgets().collect { budgetList ->
                _budgets.value = budgetList
            }
        }
    }

    private fun observeSpending() {
        viewModelScope.launch {
            combine(
                transactionRepository.getAllTransactions(),
                _budgets
            ) { transactions, budgets ->
                val budgetSpending = budgets.map {
                    BudgetCalculator.CategorySpending(
                        category = Category.fromString(it.category),
                        spentCents = 0L,
                        budgetCents = it.monthlyLimitCents
                    )
                }
                BudgetCalculator.calculateSpending(transactions, budgetSpending)
            }.collect { spending ->
                _categorySpending.value = spending
            }
        }
    }

    fun showAddDialog() {
        _editingCategory.value = null
        _editingLimit.value = ""
        _error.value = null
        _showAddDialog.value = true
    }

    fun showEditDialog(category: Category, currentLimitCents: Long) {
        _editingCategory.value = category
        // Edit field takes whole shillings, which is what a person types.
        _editingLimit.value = Money(currentLimitCents).wholeShillings.toString()
        _showAddDialog.value = true
    }

    fun dismissDialog() {
        _showAddDialog.value = false
    }

    fun setEditingLimit(limit: String) {
        _editingLimit.value = limit
    }

    fun saveBudget() {
        // Parse through Money so a malformed entry is rejected outright rather
        // than silently becoming a 0.00 budget.
        val limit = Money.parseOrNull(_editingLimit.value)
        if (limit == null || limit.isZero) {
            _error.value = "Enter a valid amount"
            return
        }
        val category = _editingCategory.value ?: Category.UNCATEGORIZED
        viewModelScope.launch {
            transactionRepository.setBudget(category, limit)
            _showAddDialog.value = false
            _error.value = null
        }
    }

    fun deleteBudget(category: Category) {
        viewModelScope.launch {
            transactionRepository.deleteBudget(category)
        }
    }
}