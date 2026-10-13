package com.akeem.mobilebanking.domain.repository

import com.akeem.mobilebanking.domain.model.Transaction
import kotlinx.coroutines.flow.StateFlow

interface TransactionRepository {
    /** Single source of truth for the history screen; emits again when a transfer is recorded. */
    val transactions: StateFlow<List<Transaction>>

    suspend fun refresh(): Result<Unit>
}
