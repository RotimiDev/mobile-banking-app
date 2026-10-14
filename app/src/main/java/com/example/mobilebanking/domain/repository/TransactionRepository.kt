package com.example.mobilebanking.domain.repository

import com.example.mobilebanking.domain.model.Transaction
import kotlinx.coroutines.flow.StateFlow

interface TransactionRepository {
    val transactions: StateFlow<List<Transaction>>

    suspend fun refresh(): Result<Unit>
}
