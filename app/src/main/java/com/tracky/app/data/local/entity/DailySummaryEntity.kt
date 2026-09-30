package com.tracky.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pre-aggregated per-day totals. Money columns are integer shilling cents. */
@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "yyyy-MM-dd"
    val totalIncomingCents: Long,
    val totalOutgoingCents: Long,
    val transactionCount: Int,
    val topChannel: String? = null,
    val primaryContact: String? = null
)
