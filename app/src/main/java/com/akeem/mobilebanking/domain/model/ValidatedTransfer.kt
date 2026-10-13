package com.akeem.mobilebanking.domain.model

/** A fully validated transfer, ready to be reviewed on the confirmation screen. */
data class ValidatedTransfer(
    val account: Account,
    val bank: Bank,
    val accountNumber: String,
    val beneficiaryName: String,
    val amounts: TransferAmounts,
)
