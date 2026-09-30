package com.tracky.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A monthly spending ceiling for one category, in integer shilling cents. */
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: String, // Category enum name (e.g. "FOOD", "TRANSPORT")
    val monthlyLimitCents: Long,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
