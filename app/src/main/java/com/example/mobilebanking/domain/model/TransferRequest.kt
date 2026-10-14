package com.example.mobilebanking.domain.model

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
