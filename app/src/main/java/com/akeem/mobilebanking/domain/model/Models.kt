package com.akeem.mobilebanking.domain.model

/** A customer account. All monetary values are integer kobo (1 naira = 100 kobo). */
data class Account(
    val id: String,
    val productName: String,
    val last4: String,
    val balanceKobo: Long,
)

data class Bank(val code: String, val name: String)

data class Transaction(
    val id: String,
    val reference: String,
    val title: String,
    val amount: Long, // kobo
    val date: String,
    val type: TransactionType,
    val status: TransactionStatus,
)

enum class TransactionType { CREDIT, DEBIT }

enum class TransactionStatus { SUCCESSFUL, PENDING, FAILED }

/** Identifies the exact bank + account number a beneficiary lookup was made for. */
data class ResolutionKey(val bankCode: String, val accountNumber: String)

enum class ResolutionFailure { ACCOUNT_NOT_FOUND, SERVICE_ERROR }

sealed interface ResolutionState {
    data object Idle : ResolutionState
    data class Loading(val key: ResolutionKey) : ResolutionState
    data class Resolved(val key: ResolutionKey, val accountName: String) : ResolutionState
    data class Failed(val key: ResolutionKey, val reason: ResolutionFailure) : ResolutionState
}

/** Result of the (simulated) account-resolution call. */
sealed interface ResolveResult {
    data class Success(val accountName: String) : ResolveResult
    data object NotFound : ResolveResult
    data object ServiceError : ResolveResult
}
