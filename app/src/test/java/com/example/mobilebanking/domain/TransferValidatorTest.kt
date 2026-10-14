package com.example.mobilebanking.domain

import com.example.mobilebanking.domain.model.ResolutionFailure
import com.example.mobilebanking.domain.model.ResolutionKey
import com.example.mobilebanking.domain.model.ResolutionState
import com.example.mobilebanking.domain.model.TransferAmounts
import com.example.mobilebanking.domain.model.TransferPolicy
import com.example.mobilebanking.domain.transfer.FieldError
import com.example.mobilebanking.domain.transfer.TransferFormInput
import com.example.mobilebanking.domain.transfer.TransferValidator
import com.example.mobilebanking.testutil.ACCESS
import com.example.mobilebanking.testutil.GTB
import com.example.mobilebanking.testutil.SALARY
import com.example.mobilebanking.testutil.SAVINGS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferValidatorTest {

    private val validator = TransferValidator()
    private val number = "0123456789"
    private val resolvedForAccess =
        ResolutionState.Resolved(ResolutionKey(ACCESS.code, number), "JOHN DOE")

    private fun validInput() =
        TransferFormInput(SAVINGS, ACCESS, number, "25000", resolvedForAccess)

    @Test
    fun `a fully valid form passes and exposes the validated transfer`() {
        val result = validator.validate(validInput())
        assertTrue(result.isValid)
        val validated = requireNotNull(result.validated)
        assertEquals("JOHN DOE", validated.beneficiaryName)
        assertEquals(2_500_000L, validated.amounts.amountKobo)
    }

    @Test
    fun `requires a debit account`() {
        val result = validator.validate(validInput().copy(account = null))
        assertEquals(FieldError.ACCOUNT_REQUIRED, result.account)
        assertFalse(result.isValid)
    }

    @Test
    fun `requires a destination bank`() {
        val result = validator.validate(validInput().copy(bank = null))
        assertEquals(FieldError.BANK_REQUIRED, result.bank)
        assertFalse(result.isValid)
    }

    @Test
    fun `account number must be exactly ten digits`() {
        listOf("", "123456789", "01234567890", "01234abcde").forEach { bad ->
            val result = validator.validate(validInput().copy(accountNumber = bad))
            assertNotNull("'$bad' should be rejected", result.accountNumber)
            assertFalse(result.isValid)
        }
        assertEquals(
            FieldError.ACCOUNT_NUMBER_REQUIRED,
            validator.validate(validInput().copy(accountNumber = "")).accountNumber,
        )
        assertEquals(
            FieldError.ACCOUNT_NUMBER_INVALID,
            validator.validate(validInput().copy(accountNumber = "12345")).accountNumber,
        )
    }

    @Test
    fun `beneficiary must have been resolved`() {
        listOf(
            ResolutionState.Idle,
            ResolutionState.Failed(
                ResolutionKey(ACCESS.code, number),
                ResolutionFailure.SERVICE_ERROR
            )
        )
            .forEach { state ->
                val result = validator.validate(validInput().copy(resolution = state))
                assertEquals(FieldError.BENEFICIARY_NOT_RESOLVED, result.beneficiary)
                assertFalse(result.isValid)
            }
        val loading = validator.validate(
            validInput().copy(
                resolution = ResolutionState.Loading(
                    ResolutionKey(
                        ACCESS.code,
                        number
                    )
                )
            )
        )
        assertEquals(FieldError.BENEFICIARY_RESOLVING, loading.beneficiary)
    }

    @Test
    fun `beneficiary resolved for a different bank is rejected`() {
        val result =
            validator.validate(validInput().copy(bank = GTB))
        assertEquals(FieldError.BENEFICIARY_NOT_RESOLVED, result.beneficiary)
        assertFalse(result.isValid)
    }

    @Test
    fun `beneficiary resolved for a different account number is rejected`() {
        val result = validator.validate(validInput().copy(accountNumber = "0123456780"))
        assertEquals(FieldError.BENEFICIARY_NOT_RESOLVED, result.beneficiary)
        assertFalse(result.isValid)
    }

    @Test
    fun `amount must be present, valid and greater than zero`() {
        assertEquals(
            FieldError.AMOUNT_REQUIRED,
            validator.validate(validInput().copy(amountInput = "")).amount
        )
        assertEquals(
            FieldError.AMOUNT_INVALID,
            validator.validate(validInput().copy(amountInput = "12.345")).amount
        )
        assertEquals(
            FieldError.AMOUNT_INVALID,
            validator.validate(validInput().copy(amountInput = "abc")).amount
        )
        assertEquals(
            FieldError.AMOUNT_NOT_POSITIVE,
            validator.validate(validInput().copy(amountInput = "0")).amount
        )
        assertEquals(
            FieldError.AMOUNT_NOT_POSITIVE,
            validator.validate(validInput().copy(amountInput = "0.00")).amount
        )
    }

    @Test
    fun `absurdly large or overflowing amounts are rejected`() {
        assertEquals(
            FieldError.AMOUNT_TOO_LARGE,
            validator.validate(validInput().copy(amountInput = "99999999999999999999")).amount
        )
        assertEquals(
            FieldError.AMOUNT_TOO_LARGE,
            validator.validate(validInput().copy(amountInput = "2000000000000")).amount
        )
    }

    @Test
    fun `live validation hides required-field errors but keeps format errors`() {
        val untouched = validator.validate(
            TransferFormInput(null, null, "", "", ResolutionState.Idle),
        ).onlyLive()
        assertNull(untouched.account)
        assertNull(untouched.bank)
        assertNull(untouched.accountNumber)
        assertNull(untouched.amount)

        val typing = validator.validate(
            TransferFormInput(null, null, "123", "12.345", ResolutionState.Idle),
        ).onlyLive()
        assertEquals(FieldError.ACCOUNT_NUMBER_INVALID, typing.accountNumber)
        assertEquals(FieldError.AMOUNT_INVALID, typing.amount)
    }

    @Test
    fun `rejects a transfer when the balance cannot cover amount plus fee`() {
        val result = validator.validate(validInput().copy(account = SALARY, amountInput = "9000"))
        assertEquals(FieldError.INSUFFICIENT_FUNDS, result.amount)
        assertFalse(result.isValid)
    }

    @Test
    fun `balance exactly equal to amount plus fee is accepted, one kobo short is not`() {
        val exact = validator.validate(validInput().copy(account = SALARY, amountInput = "8395.50"))
        assertNull(exact.amount)
        assertTrue(exact.isValid)

        val oneKoboShort =
            validator.validate(validInput().copy(account = SALARY, amountInput = "8395.51"))
        assertEquals(FieldError.INSUFFICIENT_FUNDS, oneKoboShort.amount)
    }

    @Test
    fun `the fee is counted, so an amount equal to the balance is rejected`() {
        val result =
            validator.validate(validInput().copy(account = SALARY, amountInput = "8420.50"))
        assertEquals(FieldError.INSUFFICIENT_FUNDS, result.amount)
    }

    @Test
    fun `fee is a fixed 25 naira and total debit is amount plus fee`() {
        assertEquals(2_500L, TransferPolicy.FEE_KOBO)
        val amounts = TransferAmounts.of(2_500_000L)
        assertEquals(2_500_000L, amounts.amountKobo)
        assertEquals(2_500L, amounts.feeKobo)
        assertEquals(2_502_500L, amounts.totalDebitKobo)
    }

    @Test
    fun `validation result carries the computed amounts even when funds are insufficient`() {
        val result = validator.validate(validInput().copy(account = SALARY, amountInput = "9000"))
        assertEquals(900_000L + 2_500L, result.amounts?.totalDebitKobo)
    }

    @Test
    fun `total debit uses exact arithmetic and refuses to overflow`() {
        var threw = false
        try {
            TransferAmounts.of(Long.MAX_VALUE)
        } catch (e: ArithmeticException) {
            threw = true
        }
        assertTrue(threw)
    }
}
