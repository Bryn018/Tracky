package com.tracky.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracky.app.data.backup.BackupManager
import com.tracky.app.data.backup.BackupMetadata
import com.tracky.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupState {
    object Idle : BackupState()
    object InProgress : BackupState()
    data class Success(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    private val _selectedBackupUri = MutableStateFlow<Uri?>(null)
    val selectedBackupUri: StateFlow<Uri?> = _selectedBackupUri.asStateFlow()

    private val _backupMetadata = MutableStateFlow<BackupMetadata?>(null)
    val backupMetadata: StateFlow<BackupMetadata?> = _backupMetadata.asStateFlow()

    private val _restoreState = MutableStateFlow<BackupState>(BackupState.Idle)
    val restoreState: StateFlow<BackupState> = _restoreState.asStateFlow()

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.InProgress
            try {
                val transactions = transactionRepository.getAllTransactions().first()
                val budgets = transactionRepository.getAllBudgets().first()

                val success = BackupManager.exportBackup(context, uri, transactions, budgets)
                _backupState.value = if (success) {
                    BackupState.Success("Backup exported successfully")
                } else {
                    BackupState.Error("Failed to export backup")
                }
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun selectBackupFile(uri: Uri) {
        _selectedBackupUri.value = uri
        viewModelScope.launch {
            _backupMetadata.value = BackupManager.peekBackupMetadata(context, uri)
        }
    }

    fun restoreBackup() {
        val uri = _selectedBackupUri.value ?: return
        viewModelScope.launch {
            _restoreState.value = BackupState.InProgress
            try {
                val backup = BackupManager.importBackup(context, uri)
                if (backup == null) {
                    _restoreState.value = BackupState.Error("Invalid backup file")
                    return@launch
                }

                // Clear existing data
                transactionRepository.clearAllData()

                // Restore transactions
                backup.transactions.forEach { tx ->
                    transactionRepository.addRawTransaction(tx)
                }

                // Restore budgets
                backup.budgets.forEach { budget ->
                    transactionRepository.updateBudget(budget)
                }

                _restoreState.value = BackupState.Success(
                    "Restored ${backup.transactions.size} transactions and ${backup.budgets.size} budgets"
                )
            } catch (e: Exception) {
                _restoreState.value = BackupState.Error(e.message ?: "Restore failed")
            }
        }
    }

    fun dismissBackupState() {
        _backupState.value = BackupState.Idle
    }

    fun dismissRestoreState() {
        _restoreState.value = BackupState.Idle
    }

    fun clearSelection() {
        _selectedBackupUri.value = null
        _backupMetadata.value = null
    }
}