package com.akeem.mobilebanking.domain.money

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

const val NAIRA_SYMBOL = "\u20A6"
private const val MINUS = "\u2212"

/** Formats integer kobo as Naira without ever touching floating point. */
object MoneyFormatter {

    /** 2_500_000 -> "₦25,000.00" (negative values get a leading minus). */
    fun format(kobo: Long): String {
        val value = BigDecimal.valueOf(kobo, 2)
        val formatted = newFormat().format(value.abs())
        return (if (kobo < 0) MINUS else "") + NAIRA_SYMBOL + formatted
    }

    /** Signed, as used in the transaction list: "+₦450,000.00" / "−₦25,000.00". */
    fun formatSigned(kobo: Long, isCredit: Boolean): String =
        (if (isCredit) "+" else MINUS) + format(kobo)

    private fun newFormat() = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
}

sealed interface AmountParseResult {
    data object Empty : AmountParseResult
    data object Invalid : AmountParseResult
    data object TooLarge : AmountParseResult
    data class Valid(val kobo: Long) : AmountParseResult
}

/** Parses user-entered naira text ("25000", "25,000.50") into exact kobo. */
object MoneyParser {
    private val PATTERN = Regex("""^(\d+)(?:\.(\d{0,2}))?$""")

    fun parse(input: String): AmountParseResult {
        val cleaned = input.trim()
            .replace(",", "")
            .replace(NAIRA_SYMBOL, "")
            .replace(" ", "")
        if (cleaned.isEmpty()) return AmountParseResult.Empty

        val match = PATTERN.matchEntire(cleaned) ?: return AmountParseResult.Invalid
        val wholePart = match.groupValues[1].trimStart('0').ifEmpty { "0" }
        val fractionPart = match.groupValues[2].padEnd(2, '0')

        val whole = wholePart.toLongOrNull() ?: return AmountParseResult.TooLarge
        val kobo = try {
            Math.addExact(Math.multiplyExact(whole, 100L), fractionPart.toLong())
        } catch (e: ArithmeticException) {
            return AmountParseResult.TooLarge
        }
        return AmountParseResult.Valid(kobo)
    }
}
