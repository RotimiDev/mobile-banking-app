package com.akeem.mobilebanking.domain.model

object TransferPolicy {
    /** Fixed transfer fee: ₦25.00 */
    const val FEE_KOBO: Long = 2_500L
    const val ACCOUNT_NUMBER_LENGTH = 10
    const val NARRATION_MAX_LENGTH = 100

    /** Upper bound (₦1 trillion) so that amount + fee can never overflow a Long. */
    const val MAX_TRANSFER_KOBO: Long = 100_000_000_000_000L
}
