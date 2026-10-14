package com.example.mobilebanking.domain.model

data class ValidatedTransfer(
    val account: Account,
    val bank: Bank,
    val accountNumber: String,
    val beneficiaryName: String,
    val amounts: TransferAmounts,
)
