package com.example.mobilebanking.data.repository

import com.example.mobilebanking.data.simulation.HistorySimulation
import com.example.mobilebanking.data.simulation.SimulationSettings
import com.example.mobilebanking.data.datasource.BankDataSource
import com.example.mobilebanking.data.datasource.SampleData
import com.example.mobilebanking.domain.model.Transaction
import com.example.mobilebanking.domain.repository.TransactionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class TransactionRepositoryImpl @Inject constructor(
    private val store: BankDataSource,
    private val simulation: SimulationSettings,
) : TransactionRepository {

    override val transactions: StateFlow<List<Transaction>> = store.transactions

    override suspend fun refresh(): Result<Unit> {
        delay(900.milliseconds)
        return when (simulation.history) {
            HistorySimulation.ERROR -> Result.failure(IOException("Simulated history service failure"))
            HistorySimulation.EMPTY -> {
                store.setRemoteHistory(emptyList())
                Result.success(Unit)
            }

            HistorySimulation.NORMAL -> {
                store.setRemoteHistory(SampleData.transactions)
                Result.success(Unit)
            }
        }
    }
}
