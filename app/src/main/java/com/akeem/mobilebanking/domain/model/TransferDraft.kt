package com.akeem.mobilebanking.domain.model

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
