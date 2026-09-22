package com.tracky.app.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracky.app.data.local.entity.TransactionEntity
import com.tracky.app.data.repository.SettingsRepository
import com.tracky.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _todaySpending = MutableStateFlow(0.0)
    val todaySpending: StateFlow<Double> = _todaySpending.asStateFlow()

    private val _todayIncome = MutableStateFlow(0.0)
    val todayIncome: StateFlow<Double> = _todayIncome.asStateFlow()

    private val _recentTransactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val recentTransactions: StateFlow<List<TransactionEntity>> = _recentTransactions.asStateFlow()

    private val _recentReceivedTransactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val receivedTransactions: StateFlow<List<TransactionEntity>> = _recentReceivedTransactions.asStateFlow()

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _scanResult = MutableStateFlow<Int?>(null)
    val scanResult: StateFlow<Int?> = _scanResult.asStateFlow()

    init {
        loadTodayData()
        loadRecentTransactions()
        loadRecentReceivedTransactions()
    }

    private fun loadTodayData() {
        viewModelScope.launch {
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

            transactionRepository.getTransactionsForPeriod(dayStart, dayEnd).collect { transactions ->
                _todaySpending.value = transactions
                    .filter { it.type == "OUTGOING" }
                    .sumOf { it.amount }
                _todayIncome.value = transactions
                    .filter { it.type == "INCOMING" }
                    .sumOf { it.amount }
            }
        }
    }

    private fun loadRecentTransactions() {
        viewModelScope.launch {
            transactionRepository.getRecentTransactions(5).collect { list ->
                _recentTransactions.value = list
            }
        }
    }

    private fun loadRecentReceivedTransactions() {
        viewModelScope.launch {
            transactionRepository.getTransactionsByType("INCOMING").collect { list ->
                _recentReceivedTransactions.value = list.take(5)
            }
        }
    }

    fun checkPermission(context: Context) {
        val result = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        )
        _permissionGranted.value = result == PackageManager.PERMISSION_GRANTED
    }

    fun requestScan(context: Context) {
        if (!_permissionGranted.value) return
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null
            try {
                val count = transactionRepository.scanExistingSms(context)
                _scanResult.value = count
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error during SMS scan"
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearScanResult() {
        _scanResult.value = null
    }
}