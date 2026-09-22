package com.tracky.app.di

import android.content.Context
import com.tracky.app.data.local.TrackyDatabase
import com.tracky.app.data.repository.SettingsRepository
import com.tracky.app.data.repository.TransactionRepository
import com.tracky.app.data.sms.SmsReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTransactionRepository(db: TrackyDatabase): TransactionRepository {
        return TransactionRepository(db.transactionDao(), db.dailySummaryDao(), db.budgetDao())
    }

    @Provides
    @Singleton
    fun provideSmsReader(
        @ApplicationContext context: Context
    ): SmsReader {
        return SmsReader(context)
    }
}