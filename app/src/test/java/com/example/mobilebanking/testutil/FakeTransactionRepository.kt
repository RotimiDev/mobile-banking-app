package com.example.mobilebanking.testutil

import com.example.mobilebanking.domain.model.Transaction
import com.example.mobilebanking.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTransactionRepository(initial: List<Transaction> = emptyList()) : TransactionRepository {
    override val transactions: MutableStateFlow<List<Transaction>> = MutableStateFlow(initial)
    var refreshResult: Result<Unit> = Result.success(Unit)
    var refreshCalls = 0

    override suspend fun refresh(): Result<Unit> {
        refreshCalls++
        return refreshResult
    }
}
