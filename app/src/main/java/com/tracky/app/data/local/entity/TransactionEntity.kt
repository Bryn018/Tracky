package com.tracky.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single financial movement recovered from an SMS.
 *
 * Money columns are integer counts of Kenyan shilling cents, and are named
 * `…Cents` so the unit is impossible to miss at a call site. Floating point
 * is never used: the previous REAL columns drifted on every running total.
 * [com.tracky.app.data.model.Money] is the type used above this layer; the
 * conversion happens in the repository.
 *
 * [type] carries both direction and kind — a reversal is not "outgoing",
 * because booking one as spending is what made totals permanently wrong.
 *
 * [smsKey] identifies the source message. Dedup keys on this rather than on
 * amount and contact, because no amount-based rule can distinguish a
 * re-delivered message from two genuine identical purchases.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["smsKey"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** INCOMING, OUTGOING, REVERSAL or REFUND. */
    val type: String,
    val amountCents: Long,
    val channel: String, // e.g. "M-Pesa", "Airtel Money", "Bank"
    val contact: String? = null,
    val senderName: String? = null,
    val messageBody: String,
    val timestamp: Long, // epoch millis
    val balanceCents: Long? = null,
    val notes: String? = null, // user notes (v2)
    val category: String? = null, // user-defined category (v2)
    /** Sender + timestamp + body hash. Unique — enforces dedup at the DB. */
    val smsKey: String = ""
)
