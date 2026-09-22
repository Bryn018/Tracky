package com.tracky.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // "yyyy-MM-dd"
    val totalIncoming: Double,
    val totalOutgoing: Double,
    val transactionCount: Int,
    val topChannel: String? = null,
    val primaryContact: String? = null
)
