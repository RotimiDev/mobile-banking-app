package com.akeem.mobilebanking.domain.repository

import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.model.Bank
import com.akeem.mobilebanking.domain.model.ResolveResult
import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.model.TransferOutcome
import com.akeem.mobilebanking.domain.model.TransferRequest
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository contracts. The UI and ViewModels only know these interfaces, so the in-memory
 * implementations in `data/` can be swapped for Retrofit-backed ones without touching them.
 */
interface AccountRepository {
    val accounts: StateFlow<List<Account>>
}

interface BankRepository {
    suspend fun getBanks(): List<Bank>
}

interface BeneficiaryRepository {
    /** Must never throw for expected failures; cancellation is propagated normally. */
    suspend fun resolve(bankCode: String, accountNumber: String): ResolveResult
}

interface TransferRepository {
    /**
     * Submits a transfer. [TransferRequest.reference] is the idempotency key: submitting the same
     * reference again must never move money twice.
     */
    suspend fun submit(request: TransferRequest): TransferOutcome

    /** Asks the backend for the status of an earlier request, by its original reference. */
    suspend fun checkStatus(reference: String): TransferOutcome
}

interface TransactionRepository {
    /** Single source of truth for the history screen; emits again when a transfer is recorded. */
    val transactions: StateFlow<List<Transaction>>

    suspend fun refresh(): Result<Unit>
}

fun interface ReferenceGenerator {
    fun next(): String
}
