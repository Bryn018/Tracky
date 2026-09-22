package com.tracky.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tracky.app.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Update
    suspend fun update(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getBudget(category: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    fun getBudgetFlow(category: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets ORDER BY category ASC")
    suspend fun getAllBudgetsList(): List<BudgetEntity>

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudget(category: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}