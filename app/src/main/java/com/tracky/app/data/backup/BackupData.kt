package com.tracky.app.data.backup

import com.tracky.app.data.local.entity.BudgetEntity
import com.tracky.app.data.local.entity.TransactionEntity

/**
 * Represents a full backup of user data.
 */
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList()
)

/**
 * Backup metadata for display in restore UI.
 */
data class BackupMetadata(
    val version: Int,
    val exportedAt: Long,
    val transactionCount: Int,
    val budgetCount: Int
)