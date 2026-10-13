package com.akeem.mobilebanking.domain.model

enum class FailureReason { INSUFFICIENT_FUNDS, REJECTED_BY_BANK, NOT_PROCESSED }

sealed interface TransferOutcome {
    data class Successful(val reference: String) : TransferOutcome
    data class Failed(val reason: FailureReason) : TransferOutcome
    /** Accepted but not yet settled. */
    data object Pending : TransferOutcome
    /** Timeout / network loss / unexpected error: the transfer MAY have been processed. */
    data object Unknown : TransferOutcome
}
