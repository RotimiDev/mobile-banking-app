package com.example.mobilebanking.domain.model

object TransferPolicy {
    const val FEE_KOBO: Long = 2_500L
    const val ACCOUNT_NUMBER_LENGTH = 10
    const val NARRATION_MAX_LENGTH = 100
    const val MAX_TRANSFER_KOBO: Long = 100_000_000_000_000L
}
