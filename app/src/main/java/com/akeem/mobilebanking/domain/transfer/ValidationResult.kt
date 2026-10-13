package com.akeem.mobilebanking.domain.transfer

import com.akeem.mobilebanking.domain.model.TransferAmounts
import com.akeem.mobilebanking.domain.model.ValidatedTransfer

data class ValidationResult(
    val account: FieldError? = null,
    val bank: FieldError? = null,
    val accountNumber: FieldError? = null,
    val beneficiary: FieldError? = null,
    val amount: FieldError? = null,
    /** Present whenever the typed amount is a positive, parseable value (even if funds are insufficient). */
    val amounts: TransferAmounts? = null,
    /** Non-null only when every rule passes. */
    val validated: ValidatedTransfer? = null,
) {
    val isValid: Boolean get() = validated != null

    /**
     * Errors that are useful to show while the customer is still typing: format problems,
     * not "you haven't filled this in yet" problems.
     */
    fun onlyLive(): ValidationResult = copy(
        account = null,
        bank = null,
        accountNumber = accountNumber.takeIf { it == FieldError.ACCOUNT_NUMBER_INVALID },
        beneficiary = null,
        amount = amount.takeIf { it != FieldError.AMOUNT_REQUIRED },
    )
}
