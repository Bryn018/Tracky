package com.tracky.app.data.local.dao

import androidx.room.ColumnInfo

/**
 * One row of the per-day GROUP BY in [TransactionDao.getDailyTotals].
 *
 * A projection, not a table. Money columns are integer shilling cents.
 */
data class DailyTotalRow(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "totalIncomingCents") val totalIncomingCents: Long,
    @ColumnInfo(name = "totalOutgoingCents") val totalOutgoingCents: Long,
    @ColumnInfo(name = "transactionCount") val transactionCount: Int
)
