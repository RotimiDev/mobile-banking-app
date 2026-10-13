package com.akeem.mobilebanking.domain.model

object TransferPolicy {
    /** Fixed transfer fee: ₦25.00 */
    const val FEE_KOBO: Long = 2_500L
    const val ACCOUNT_NUMBER_LENGTH = 10
    const val NARRATION_MAX_LENGTH = 100

    /** Upper bound (₦1 trillion) so that amount + fee can never overflow a Long. */
    const val MAX_TRANSFER_KOBO: Long = 100_000_000_000_000L
}

data class TransferAmounts(
    val amountKobo: Long,
    val feeKobo: Long,
    val totalDebitKobo: Long,
) {
    companion object {
        /** Total debit = transfer amount + fee. Uses exact arithmetic so overflow can never go unnoticed. */
        fun of(amountKobo: Long, feeKobo: Long = TransferPolicy.FEE_KOBO): TransferAmounts =
            TransferAmounts(amountKobo, feeKobo, Math.addExact(amountKobo, feeKobo))
    }
}

/** A fully validated transfer, ready to be reviewed on the confirmation screen. */
data class ValidatedTransfer(
    val account: Account,
    val bank: Bank,
    val accountNumber: String,
    val beneficiaryName: String,
    val amounts: TransferAmounts,
)

/** What the customer reviews on the confirmation screen. The [reference] identifies this attempt for its whole life. */
data class TransferDraft(
    val reference: String,
    val debitAccount: Account,
    val bank: Bank,
    val accountNumber: String,
    val beneficiaryName: String,
    val amounts: TransferAmounts,
    val narration: String,
) {
    fun toRequest() = TransferRequest(
        reference = reference,
        debitAccountId = debitAccount.id,
        bankCode = bank.code,
        bankName = bank.name,
        accountNumber = accountNumber,
        beneficiaryName = beneficiaryName,
        amountKobo = amounts.amountKobo,
        feeKobo = amounts.feeKobo,
        narration = narration,
    )
}

/** Payload sent to the repository / backend. [reference] doubles as the idempotency key. */
data class TransferRequest(
    val reference: String,
    val debitAccountId: String,
    val bankCode: String,
    val bankName: String,
    val accountNumber: String,
    val beneficiaryName: String,
    val amountKobo: Long,
    val feeKobo: Long,
    val narration: String,
) {
    val totalDebitKobo: Long get() = Math.addExact(amountKobo, feeKobo)
}

enum class FailureReason { INSUFFICIENT_FUNDS, REJECTED_BY_BANK, NOT_PROCESSED }

sealed interface TransferOutcome {
    data class Successful(val reference: String) : TransferOutcome
    data class Failed(val reason: FailureReason) : TransferOutcome
    /** Accepted but not yet settled. */
    data object Pending : TransferOutcome
    /** Timeout / network loss / unexpected error: the transfer MAY have been processed. */
    data object Unknown : TransferOutcome
}
