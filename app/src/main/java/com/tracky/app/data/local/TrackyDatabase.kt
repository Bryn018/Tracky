package com.tracky.app.data.local

import android.content.Context
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
    version = 4,
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

        /**
         * 3 -> 4: money becomes integer cents, and transaction type becomes a
         * four-value enum.
         *
         * Every shilling-denominated REAL is multiplied by 100 and rounded to
         * the nearest cent, so existing history keeps its exact value. NULL
         * balances stay NULL rather than becoming zero, since "balance
         * unknown" and "balance is zero" are different facts.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transactions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        amountCents INTEGER NOT NULL,
                        channel TEXT NOT NULL,
                        contact TEXT,
                        senderName TEXT,
                        messageBody TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        balanceCents INTEGER,
                        notes TEXT,
                        category TEXT,
                        smsKey TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // Legacy rows predate smsKey, so each gets a stable synthetic
                // key. This keeps the new unique index satisfiable without
                // inventing collisions between distinct rows.
                db.execSQL(
                    """
                    INSERT INTO transactions_new
                        (id, type, amountCents, channel, contact, senderName, messageBody,
                         timestamp, balanceCents, notes, category, smsKey)
                    SELECT
                        id,
                        type,
                        CAST(ROUND(COALESCE(amount, 0) * 100) AS INTEGER),
                        channel,
                        contact,
                        senderName,
                        messageBody,
                        timestamp,
                        CASE WHEN balance IS NULL THEN NULL
                             ELSE CAST(ROUND(COALESCE(balance, 0) * 100) AS INTEGER) END,
                        notes,
                        category,
                        'legacy-' || id
                    FROM transactions
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_smsKey ON transactions (smsKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions (type)")

                // daily_summaries: REAL totals to integer cents.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_summaries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        totalIncomingCents INTEGER NOT NULL,
                        totalOutgoingCents INTEGER NOT NULL,
                        transactionCount INTEGER NOT NULL,
                        topChannel TEXT,
                        primaryContact TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO daily_summaries_new
                        (id, date, totalIncomingCents, totalOutgoingCents, transactionCount, topChannel, primaryContact)
                    SELECT
                        id,
                        date,
                        CAST(ROUND(COALESCE(totalIncoming, 0) * 100) AS INTEGER),
                        CAST(ROUND(COALESCE(totalOutgoing, 0) * 100) AS INTEGER),
                        transactionCount,
                        topChannel,
                        primaryContact
                    FROM daily_summaries
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE daily_summaries")
                db.execSQL("ALTER TABLE daily_summaries_new RENAME TO daily_summaries")

                // budgets: REAL limit to integer cents.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS budgets_new (
                        category TEXT NOT NULL PRIMARY KEY,
                        monthlyLimitCents INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO budgets_new (category, monthlyLimitCents, isEnabled, createdAt)
                    SELECT
                        category,
                        CAST(ROUND(COALESCE(monthlyLimit, 0) * 100) AS INTEGER),
                        isEnabled,
                        createdAt
                    FROM budgets
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE budgets")
                db.execSQL("ALTER TABLE budgets_new RENAME TO budgets")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)

        fun getDatabase(context: Context): TrackyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackyDatabase::class.java,
                    "tracky_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    // Deliberately no fallbackToDestructiveMigration. This
                    // database holds a user's financial history; silently
                    // wiping it because a schema edit was wrong is worse than
                    // a migration failure the user can report.
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}