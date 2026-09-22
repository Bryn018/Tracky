package com.tracky.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // INCOMING or OUTGOING
    val amount: Double,
    val channel: String, // e.g. "M-Pesa", "Airtel Money"
    val contact: String? = null,
    val senderName: String? = null,
    val messageBody: String,
    val timestamp: Long, // epoch millis
    val balance: Double? = null,
    val notes: String? = null, // user notes (v2)
    val category: String? = null // user-defined category (v2)
)
