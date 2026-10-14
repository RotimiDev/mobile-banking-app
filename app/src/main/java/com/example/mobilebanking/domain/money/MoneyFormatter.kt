package com.example.mobilebanking.domain.money

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

const val NAIRA_SYMBOL = "\u20A6"

private const val MINUS = "\u2212"

object MoneyFormatter {

    fun format(kobo: Long): String {
        val value = BigDecimal.valueOf(kobo, 2)
        val formatted = newFormat().format(value.abs())
        return (if (kobo < 0) MINUS else "") + NAIRA_SYMBOL + formatted
    }

    fun formatSigned(kobo: Long, isCredit: Boolean): String =
        (if (isCredit) "+" else MINUS) + format(kobo)

    private fun newFormat() = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
}
