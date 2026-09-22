package com.tracky.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracky.app.R
import com.tracky.app.data.repository.SettingsRepository
import com.tracky.app.data.repository.TransactionRepository
import com.tracky.app.worker.DailyReportScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class ExportState {
    object Idle : ExportState()
    object InProgress : ExportState()
    data class Success(val filePath: String) : ExportState()
    data class Error(val message: String) : ExportState()
}

sealed class DataState {
    object Idle : DataState()
    object Confirming : DataState()
    object InProgress : DataState()
    object Done : DataState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val reportEnabled: StateFlow<Boolean> = settingsRepository.isDailyReportEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val reportHour: StateFlow<Int> = settingsRepository.getReportHour()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    val reportMinute: StateFlow<Int> = settingsRepository.getReportMinute()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _clearDataState = MutableStateFlow<DataState>(DataState.Idle)
    val clearDataState: StateFlow<DataState> = _clearDataState.asStateFlow()

    private val _defaultSmsStatus = MutableStateFlow("")
    val defaultSmsStatus: StateFlow<String> = _defaultSmsStatus.asStateFlow()

    private val _batteryOptimizationStatus = MutableStateFlow("")
    val batteryOptimizationStatus: StateFlow<String> = _batteryOptimizationStatus.asStateFlow()

    private val _exportedFileForShare = MutableStateFlow<File?>(null)
    val exportedFileForShare: StateFlow<File?> = _exportedFileForShare.asStateFlow()

    init {
        viewModelScope.launch {
            if (reportEnabled.value) {
                scheduleReport()
            }
        }
        refreshSystemStatus()
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun refreshSystemStatus() {
        viewModelScope.launch {
            _defaultSmsStatus.value = if (com.tracky.app.util.SystemUtils.isDefaultSmsApp(context)) {
                context.getString(R.string.default_sms_already_set)
            } else {
                context.getString(R.string.default_sms_not_set)
            }
            _batteryOptimizationStatus.value = if (com.tracky.app.util.SystemUtils.isIgnoringBatteryOptimizations(context)) {
                context.getString(R.string.battery_optimization_already_excluded)
            } else {
                "Optimized"
            }
        }
    }

    fun openDefaultSmsSettings() {
        com.tracky.app.util.SystemUtils.openDefaultSmsSettings(context)
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            refreshSystemStatus()
        }
    }

    fun requestBatteryOptimizationExemption() {
        com.tracky.app.util.SystemUtils.requestIgnoreBatteryOptimizations(context)
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            refreshSystemStatus()
        }
    }

    fun setReportEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDailyReportEnabled(enabled)
            if (enabled) {
                scheduleReport()
            } else {
                cancelReport()
            }
        }
    }

    fun setReportTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setReportTime(hour, minute)
            if (reportEnabled.value) {
                scheduleReport()
            }
        }
    }

    private fun scheduleReport() {
        DailyReportScheduler.scheduleReport(context, reportHour.value, reportMinute.value)
    }

    private fun cancelReport() {
        DailyReportScheduler.cancelReport(context)
    }

    fun exportTransactions() {
        viewModelScope.launch {
            _exportState.value = ExportState.InProgress
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                if (transactions.isEmpty()) {
                    _exportState.value = ExportState.Error("No transactions to export")
                    return@launch
                }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
                val fileName = "tracky_transactions_${dateFormat.format(Date())}.csv"
                val file = File(context.cacheDir, fileName)
                file.outputStream().bufferedWriter().use { writer ->
                    writer.write("ID,Type,Amount,Channel,Contact,Sender,Timestamp,Balance,Notes,Category,MessageBody\n")
                    for (tx in transactions) {
                        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(tx.timestamp))
                        val line = buildString {
                            append(tx.id); append(',')
                            append(escapeCsv(tx.type)); append(',')
                            append(tx.amount); append(',')
                            append(escapeCsv(tx.channel)); append(',')
                            append(escapeCsv(tx.contact ?: "")); append(',')
                            append(escapeCsv(tx.senderName ?: "")); append(',')
                            append(escapeCsv(ts)); append(',')
                            append(tx.balance?.toString() ?: ""); append(',')
                            append(escapeCsv(tx.notes ?: "")); append(',')
                            append(escapeCsv(tx.category ?: "")); append(',')
                            append(escapeCsv(tx.messageBody))
                        }
                        writer.write(line)
                        writer.newLine()
                    }
                }
                _exportedFileForShare.value = file
                _exportState.value = ExportState.Success(file.absolutePath)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun exportToCsv(uri: Uri) {
        viewModelScope.launch {
            _exportState.value = ExportState.InProgress
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                if (transactions.isEmpty()) {
                    _exportState.value = ExportState.Error("No transactions to export")
                    return@launch
                }
                val count = transactionRepository.exportToCsv(context, uri)
                _exportState.value = ExportState.Success("Exported $count transactions")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun shareExportedFile(launcher: (Intent) -> Unit) {
        val file = _exportedFileForShare.value
        if (file == null) {
            _exportState.value = ExportState.Error("Export a file first before sharing")
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launcher(intent)
    }

    fun requestClearData() {
        _clearDataState.value = DataState.Confirming
    }

    fun confirmClearData() {
        viewModelScope.launch {
            _clearDataState.value = DataState.InProgress
            try {
                transactionRepository.clearAllData()
                _clearDataState.value = DataState.Done
            } catch (e: Exception) {
                _clearDataState.value = DataState.Idle
                _exportState.value = ExportState.Error("Failed to clear data: ${e.message}")
            }
        }
    }

    fun cancelClearData() {
        _clearDataState.value = DataState.Idle
    }

    fun dismissExportState() {
        _exportState.value = ExportState.Idle
    }

    fun dismissClearDataState() {
        _clearDataState.value = DataState.Idle
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}