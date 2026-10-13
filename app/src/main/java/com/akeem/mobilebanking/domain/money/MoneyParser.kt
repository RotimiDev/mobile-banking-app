package com.akeem.mobilebanking.domain.money

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
