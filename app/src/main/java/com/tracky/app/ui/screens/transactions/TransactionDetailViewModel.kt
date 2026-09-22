package com.tracky.app.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.model.Category
import com.tracky.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _transaction = MutableStateFlow<TransactionEntity?>(null)
    val transaction: StateFlow<TransactionEntity?> = _transaction.asStateFlow()

    private val _selectedCategory = MutableStateFlow(Category.UNCATEGORIZED)
    val selectedCategory: StateFlow<Category> = _selectedCategory.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            val tx = transactionRepository.getTransactionById(id)
            _transaction.value = tx
            if (tx != null) {
                _selectedCategory.value = Category.fromString(tx.category)
                _notes.value = tx.notes ?: ""
            }
        }
    }

    fun setCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun setNotes(text: String) {
        _notes.value = text
    }

    fun save() {
        val tx = _transaction.value ?: return
        viewModelScope.launch {
            transactionRepository.updateTransaction(
                tx.copy(
                    category = _selectedCategory.value.name,
                    notes = _notes.value.ifBlank { null }
                )
            )
            _saved.value = true
        }
    }

    fun delete() {
        val tx = _transaction.value ?: return
        viewModelScope.launch {
            transactionRepository.deleteTransactionById(tx.id)
            _deleted.value = true
        }
    }
}