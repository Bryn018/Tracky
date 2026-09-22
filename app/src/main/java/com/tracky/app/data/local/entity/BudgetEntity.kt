package com.tracky.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val category: String, // Category enum name (e.g. "FOOD", "TRANSPORT")
    val monthlyLimit: Double,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)