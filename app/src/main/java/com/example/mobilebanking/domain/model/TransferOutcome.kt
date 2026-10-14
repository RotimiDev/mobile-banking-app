package com.example.mobilebanking.domain.model

enum class FailureReason { INSUFFICIENT_FUNDS, REJECTED_BY_BANK, NOT_PROCESSED }

sealed interface TransferOutcome {
    data class Successful(val reference: String) : TransferOutcome
    data class Failed(val reason: FailureReason) : TransferOutcome
    data object Pending : TransferOutcome
    data object Unknown : TransferOutcome
}
