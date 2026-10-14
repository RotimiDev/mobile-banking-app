package com.example.mobilebanking.domain.model

data class TransferAmounts(
    val amountKobo: Long,
    val feeKobo: Long,
    val totalDebitKobo: Long,
) {
    companion object {
        fun of(amountKobo: Long, feeKobo: Long = TransferPolicy.FEE_KOBO): TransferAmounts =
            TransferAmounts(amountKobo, feeKobo, Math.addExact(amountKobo, feeKobo))
    }
}
