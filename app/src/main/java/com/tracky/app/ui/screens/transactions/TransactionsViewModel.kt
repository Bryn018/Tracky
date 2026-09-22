package com.tracky.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.Category
import com.tracky.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _filterType = MutableStateFlow<String?>(null)
    val filterType: StateFlow<String?> = _filterType.asStateFlow()

    private val _filterCategory = MutableStateFlow<Category?>(null)
    val filterCategory: StateFlow<Category?> = _filterCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                combine(
                    transactionRepository.getAllTransactions(),
                    _filterType,
                    _filterCategory,
                    _searchQuery
                ) { allTransactions, filter, category, query ->
                    var result = allTransactions

                    if (filter != null) {
                        result = result.filter { it.type == filter }
                    }

                    if (category != null) {
                        result = result.filter { Category.fromString(it.category) == category }
                    }

                    if (query.isNotBlank()) {
                        val q = query.lowercase()
                        result = result.filter {
                            (it.contact?.lowercase()?.contains(q) == true) ||
                                (it.senderName?.lowercase()?.contains(q) == true) ||
                                it.channel.lowercase().contains(q) ||
                                it.messageBody.lowercase().contains(q)
                        }
                    }

                    result
                }.collect { filtered ->
                    _transactions.value = filtered
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
                _isLoading.value = false
            }
        }
    }

    fun setFilter(type: String?) {
        _filterType.value = type
    }

    fun setCategoryFilter(category: Category?) {
        _filterCategory.value = category
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
    }

    fun refresh() {
        observeTransactions()
    }

    fun dismissError() {
        _error.value = null
    }
}