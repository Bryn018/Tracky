package com.tracky.app.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tracky.app.data.local.dao.BudgetDao
import com.tracky.app.data.local.dao.DailySummaryDao
import com.tracky.app.data.local.dao.TransactionDao
import com.tracky.app.data.local.entity.BudgetEntity
import com.tracky.app.data.local.entity.DailySummaryEntity
import com.tracky.app.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class, DailySummaryEntity::class, BudgetEntity::class],
    version = 3,
    exportSchema = true
)
abstract class TrackyDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun dailySummaryDao(): DailySummaryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: TrackyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS budgets (category TEXT NOT NULL PRIMARY KEY, monthlyLimit REAL NOT NULL, isEnabled INTEGER NOT NULL DEFAULT 1, createdAt INTEGER NOT NULL DEFAULT 0)")
            }
        }

        fun getDatabase(context: Context): TrackyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackyDatabase::class.java,
                    "tracky_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}