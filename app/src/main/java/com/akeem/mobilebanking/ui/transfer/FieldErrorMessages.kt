package com.akeem.mobilebanking.ui.transfer

import android.content.Context
import com.akeem.mobilebanking.R
import com.akeem.mobilebanking.domain.model.TransferPolicy
import com.akeem.mobilebanking.domain.money.MoneyFormatter
import com.akeem.mobilebanking.domain.transfer.FieldError

fun FieldError.toMessage(context: Context): String = when (this) {
    FieldError.ACCOUNT_REQUIRED -> context.getString(R.string.error_account_required)
    FieldError.BANK_REQUIRED -> context.getString(R.string.error_bank_required)
    FieldError.ACCOUNT_NUMBER_REQUIRED -> context.getString(R.string.error_account_number_required)
    FieldError.ACCOUNT_NUMBER_INVALID -> context.getString(R.string.error_account_number_invalid)
    FieldError.BENEFICIARY_RESOLVING -> context.getString(R.string.error_beneficiary_resolving)
    FieldError.BENEFICIARY_NOT_RESOLVED -> context.getString(R.string.error_beneficiary_not_resolved)
    FieldError.AMOUNT_REQUIRED -> context.getString(R.string.error_amount_required)
    FieldError.AMOUNT_INVALID -> context.getString(R.string.error_amount_invalid)
    FieldError.AMOUNT_NOT_POSITIVE -> context.getString(R.string.error_amount_not_positive)
    FieldError.AMOUNT_TOO_LARGE -> context.getString(R.string.error_amount_too_large)
    FieldError.INSUFFICIENT_FUNDS ->
        context.getString(R.string.error_insufficient_funds, MoneyFormatter.format(TransferPolicy.FEE_KOBO))
}
