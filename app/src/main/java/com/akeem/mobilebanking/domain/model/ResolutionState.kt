package com.akeem.mobilebanking.domain.model

/** Identifies the exact bank + account number a beneficiary lookup was made for. */
data class ResolutionKey(val bankCode: String, val accountNumber: String)

enum class ResolutionFailure { ACCOUNT_NOT_FOUND, SERVICE_ERROR }

sealed interface ResolutionState {
    data object Idle : ResolutionState
    data class Loading(val key: ResolutionKey) : ResolutionState
    data class Resolved(val key: ResolutionKey, val accountName: String) : ResolutionState
    data class Failed(val key: ResolutionKey, val reason: ResolutionFailure) : ResolutionState
}
