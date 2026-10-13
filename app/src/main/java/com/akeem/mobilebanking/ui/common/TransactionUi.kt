package com.akeem.mobilebanking.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.akeem.mobilebanking.R
import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.model.TransactionStatus
import com.akeem.mobilebanking.domain.model.TransactionType
import com.akeem.mobilebanking.domain.money.MoneyFormatter

@StringRes
fun TransactionStatus.labelRes(): Int = when (this) {
    TransactionStatus.SUCCESSFUL -> R.string.status_successful
    TransactionStatus.PENDING -> R.string.status_pending
    TransactionStatus.FAILED -> R.string.status_failed
}

@ColorRes
fun TransactionStatus.foregroundRes(): Int = when (this) {
    TransactionStatus.SUCCESSFUL -> R.color.success
    TransactionStatus.PENDING -> R.color.pending
    TransactionStatus.FAILED -> R.color.failed
}

@ColorRes
fun TransactionStatus.backgroundRes(): Int = when (this) {
    TransactionStatus.SUCCESSFUL -> R.color.success_container
    TransactionStatus.PENDING -> R.color.pending_container
    TransactionStatus.FAILED -> R.color.failed_container
}

@StringRes
fun TransactionType.labelRes(): Int = when (this) {
    TransactionType.CREDIT -> R.string.type_credit
    TransactionType.DEBIT -> R.string.type_debit
}

@ColorRes
fun TransactionType.colorRes(): Int = when (this) {
    TransactionType.CREDIT -> R.color.credit
    TransactionType.DEBIT -> R.color.debit
}

@DrawableRes
fun TransactionType.iconRes(): Int = when (this) {
    TransactionType.CREDIT -> R.drawable.ic_arrow_down
    TransactionType.DEBIT -> R.drawable.ic_arrow_up
}

fun Transaction.formattedAmount(): String =
    MoneyFormatter.formatSigned(amount, isCredit = type == TransactionType.CREDIT)

/** Styles a TextView as a coloured status pill (colour is never the only signal: the label is always shown). */
fun TextView.bindStatusBadge(status: TransactionStatus) {
    text = context.getString(status.labelRes())
    setTextColor(ContextCompat.getColor(context, status.foregroundRes()))
    backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, status.backgroundRes()))
}
