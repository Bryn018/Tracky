package com.tracky.app.di

import android.content.Context
import com.tracky.app.data.local.TrackyDatabase
import com.tracky.app.data.local.dao.BudgetDao
import com.tracky.app.data.local.dao.DailySummaryDao
import com.tracky.app.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): TrackyDatabase {
        return TrackyDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTransactionDao(db: TrackyDatabase): TransactionDao {
        return db.transactionDao()
    }

    @Provides
    @Singleton
    fun provideDailySummaryDao(db: TrackyDatabase): DailySummaryDao {
        return db.dailySummaryDao()
    }

    @Provides
    @Singleton
    fun provideBudgetDao(db: TrackyDatabase): BudgetDao {
        return db.budgetDao()
    }
}
