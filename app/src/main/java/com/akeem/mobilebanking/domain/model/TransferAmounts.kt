package com.akeem.mobilebanking.domain.model

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
