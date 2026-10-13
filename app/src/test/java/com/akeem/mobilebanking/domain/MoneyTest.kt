package com.akeem.mobilebanking.domain

import com.akeem.mobilebanking.domain.money.AmountParseResult
import com.akeem.mobilebanking.domain.money.MoneyFormatter
import com.akeem.mobilebanking.domain.money.MoneyParser
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test fun `formats kobo as naira with grouping and two decimals`() {
        assertEquals("\u20A625,000.00", MoneyFormatter.format(2_500_000L))
        assertEquals("\u20A60.05", MoneyFormatter.format(5L))
        assertEquals("\u20A62,350,500.75", MoneyFormatter.format(235_050_075L))
    }

    @Test fun `formats the largest long without overflow`() {
        assertEquals("\u20A692,233,720,368,547,758.07", MoneyFormatter.format(Long.MAX_VALUE))
    }

    @Test fun `signed format distinguishes credits and debits`() {
        assertEquals("+\u20A6450,000.00", MoneyFormatter.formatSigned(45_000_000L, isCredit = true))
        assertEquals("\u2212\u20A625,000.00", MoneyFormatter.formatSigned(2_500_000L, isCredit = false))
    }

    @Test fun `parses whole and decimal naira into exact kobo`() {
        assertEquals(AmountParseResult.Valid(2_500_000L), MoneyParser.parse("25000"))
        assertEquals(AmountParseResult.Valid(2_500_050L), MoneyParser.parse("25,000.50"))
        assertEquals(AmountParseResult.Valid(2_500_010L), MoneyParser.parse("25000.1"))
        assertEquals(AmountParseResult.Valid(2_500_000L), MoneyParser.parse("25000."))
        assertEquals(AmountParseResult.Valid(2_500_000L), MoneyParser.parse("\u20A6 25,000"))
    }

    @Test fun `no floating point drift on classic problem values`() {
        // 0.1 + 0.2 style values are exact in kobo.
        assertEquals(AmountParseResult.Valid(10L), MoneyParser.parse("0.10"))
        assertEquals(AmountParseResult.Valid(1_999_999L), MoneyParser.parse("19999.99"))
    }

    @Test fun `rejects empty and malformed input`() {
        assertEquals(AmountParseResult.Empty, MoneyParser.parse("   "))
        assertEquals(AmountParseResult.Invalid, MoneyParser.parse("12.345"))
        assertEquals(AmountParseResult.Invalid, MoneyParser.parse("abc"))
        assertEquals(AmountParseResult.Invalid, MoneyParser.parse("-5"))
        assertEquals(AmountParseResult.Invalid, MoneyParser.parse("1.2.3"))
        assertEquals(AmountParseResult.Invalid, MoneyParser.parse(".5"))
    }

    @Test fun `guards against overflow`() {
        assertEquals(AmountParseResult.TooLarge, MoneyParser.parse("99999999999999999999"))
        // Fits in a Long as naira, but x100 overflows.
        assertEquals(AmountParseResult.TooLarge, MoneyParser.parse("92233720368547758"))
    }

    @Test fun `leading zeros are fine and zero parses as zero`() {
        assertEquals(AmountParseResult.Valid(0L), MoneyParser.parse("0"))
        assertEquals(AmountParseResult.Valid(500L), MoneyParser.parse("005"))
    }
}
