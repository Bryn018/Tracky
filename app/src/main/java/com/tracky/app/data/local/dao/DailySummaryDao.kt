package com.tracky.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tracky.app.data.local.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {

    @Upsert
    suspend fun upsert(summary: DailySummaryEntity)

    @Query("SELECT * FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSummariesBetween(startDate: String, endDate: String): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    suspend fun getSummaryByDate(date: String): DailySummaryEntity?
}
