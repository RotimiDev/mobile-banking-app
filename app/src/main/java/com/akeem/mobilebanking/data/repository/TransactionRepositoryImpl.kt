package com.akeem.mobilebanking.data.repository

import com.akeem.mobilebanking.data.demo.DemoHistoryResult
import com.akeem.mobilebanking.data.demo.DemoSettings
import com.akeem.mobilebanking.data.source.BankDataSource
import com.akeem.mobilebanking.data.source.SampleData
import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.repository.TransactionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val store: BankDataSource,
    private val demoSettings: DemoSettings,
) : TransactionRepository {

    override val transactions: StateFlow<List<Transaction>> = store.transactions

    override suspend fun refresh(): Result<Unit> {
        delay(900)
        return when (demoSettings.history) {
            DemoHistoryResult.ERROR -> Result.failure(IOException("Simulated history service failure"))
            DemoHistoryResult.EMPTY -> {
                store.setRemoteHistory(emptyList())
                Result.success(Unit)
            }
            DemoHistoryResult.NORMAL -> {
                store.setRemoteHistory(SampleData.transactions)
                Result.success(Unit)
            }
        }
    }
}
