package com.tracky.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tracky.app.data.local.entity.DailySummaryEntity
import com.tracky.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /**
     * IGNORE, not REPLACE. The unique index on smsKey makes this the
     * authoritative dedup check: a duplicate insert is a no-op rather than a
     * row replacement, so a re-scan can never destroy a row the user has
     * since annotated or re-categorised.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String?)

    @Query("UPDATE transactions SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String?)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE channel = :channel ORDER BY timestamp DESC")
    fun getTransactionsByChannel(channel: String): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE " +
        "contact LIKE '%' || :query || '%' OR " +
        "senderName LIKE '%' || :query || '%' OR " +
        "messageBody LIKE '%' || :query || '%' " +
        "ORDER BY timestamp DESC"
    )
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    /**
     * Which of these keys already exist. Lets the repository detect
     * duplicates with an indexed lookup instead of loading the entire
     * transaction table on every incoming message.
     */
    @Query("SELECT smsKey FROM transactions WHERE smsKey IN (:keys)")
    suspend fun findExistingKeys(keys: List<String>): List<String>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionByIdFlow(id: Long): Flow<TransactionEntity?>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Period totals computed in SQL over integer cents, so no rounding error
     * can accumulate. Reversals and refunds subtract from spending, which is
     * what stops a reversed purchase from continuing to inflate what the user
     * is shown as having spent.
     */
    @Query(
        """
        SELECT
            :dateString AS id,
            :dateString AS date,
            COALESCE(SUM(CASE WHEN type = 'INCOMING' THEN amountCents ELSE 0 END), 0) AS totalIncomingCents,
            COALESCE(SUM(
                CASE type
                    WHEN 'OUTGOING' THEN amountCents
                    WHEN 'REVERSAL' THEN -amountCents
                    WHEN 'REFUND' THEN -amountCents
                    ELSE 0
                END
            ), 0) AS totalOutgoingCents,
            COUNT(*) AS transactionCount,
            NULL AS topChannel,
            NULL AS primaryContact
        FROM transactions
        WHERE timestamp BETWEEN :dateStart AND :dateEnd
        """
    )
    fun getSummaryForPeriod(
        dateStart: Long,
        dateEnd: Long,
        dateString: String
    ): Flow<DailySummaryEntity?>

    /**
     * Per-day totals across a window, grouped in SQL. Reversals and refunds
     * subtract from spending, matching [getSummaryForPeriod].
     */
    @Query(
        """
        SELECT
            strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS date,
            COALESCE(SUM(CASE WHEN type = 'INCOMING' THEN amountCents ELSE 0 END), 0) AS totalIncomingCents,
            COALESCE(SUM(
                CASE type
                    WHEN 'OUTGOING' THEN amountCents
                    WHEN 'REVERSAL' THEN -amountCents
                    WHEN 'REFUND' THEN -amountCents
                    ELSE 0
                END
            ), 0) AS totalOutgoingCents,
            COUNT(*) AS transactionCount
        FROM transactions
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY date
        ORDER BY date DESC
        """
    )
    fun getDailyTotals(start: Long, end: Long): Flow<List<DailyTotalRow>>

    /** Net spend in one category over a window, excluding reversals/refunds. */
    @Query(
        """
        SELECT COALESCE(SUM(
            CASE type
                WHEN 'OUTGOING' THEN amountCents
                WHEN 'REVERSAL' THEN -amountCents
                WHEN 'REFUND' THEN -amountCents
                ELSE 0
            END
        ), 0)
        FROM transactions
        WHERE category = :category AND timestamp BETWEEN :start AND :end
        """
    )
    fun getCategorySpendFlow(category: String, start: Long, end: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
