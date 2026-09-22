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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

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
    suspend fun getAllTransactionsList(): List<TransactionEntity> // For export

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionByIdFlow(id: Long): Flow<TransactionEntity?>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "SELECT " +
        ":dateStart AS id, " +
        ":dateString AS date, " +
        "COALESCE(SUM(CASE WHEN type = 'INCOMING' THEN amount ELSE 0 END), 0.0) AS totalIncoming, " +
        "COALESCE(SUM(CASE WHEN type = 'OUTGOING' THEN amount ELSE 0 END), 0.0) AS totalOutgoing, " +
        "COUNT(*) AS transactionCount, " +
        "NULL AS topChannel, " +
        "NULL AS primaryContact " +
        "FROM transactions WHERE timestamp BETWEEN :dateStart AND :dateEnd"
    )
    fun getTodaySummary(
        dateStart: Long,
        dateEnd: Long,
        dateString: String
    ): Flow<DailySummaryEntity?>

    @Query("SELECT COUNT(*) FROM transactions")
    fun getTransactionCount(): Flow<Int>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}