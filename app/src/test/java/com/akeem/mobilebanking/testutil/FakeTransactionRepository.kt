package com.akeem.mobilebanking.testutil

import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.repository.TransactionRepository
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
