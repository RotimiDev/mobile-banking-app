package com.akeem.mobilebanking.domain.transfer

import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.model.Bank
import com.akeem.mobilebanking.domain.model.ResolutionKey
import com.akeem.mobilebanking.domain.model.ResolutionState
import com.akeem.mobilebanking.domain.model.TransferAmounts
import com.akeem.mobilebanking.domain.model.TransferPolicy
import com.akeem.mobilebanking.domain.model.ValidatedTransfer
import com.akeem.mobilebanking.domain.money.AmountParseResult
import com.akeem.mobilebanking.domain.money.MoneyParser
import javax.inject.Inject

enum class FieldError {
    ACCOUNT_REQUIRED,
    BANK_REQUIRED,
    ACCOUNT_NUMBER_REQUIRED,
    ACCOUNT_NUMBER_INVALID,
    BENEFICIARY_RESOLVING,
    BENEFICIARY_NOT_RESOLVED,
    AMOUNT_REQUIRED,
    AMOUNT_INVALID,
    AMOUNT_NOT_POSITIVE,
    AMOUNT_TOO_LARGE,
    INSUFFICIENT_FUNDS,
}

data class TransferFormInput(
    val account: Account?,
    val bank: Bank?,
    val accountNumber: String,
    val amountInput: String,
    val resolution: ResolutionState,
)

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

/** Pure, UI-free transfer rules. */
class TransferValidator @Inject constructor() {

    fun validate(input: TransferFormInput): ValidationResult {
        val accountError = if (input.account == null) FieldError.ACCOUNT_REQUIRED else null
        val bankError = if (input.bank == null) FieldError.BANK_REQUIRED else null

        val numberValid = isValidAccountNumber(input.accountNumber)
        val numberError = when {
            input.accountNumber.isEmpty() -> FieldError.ACCOUNT_NUMBER_REQUIRED
            !numberValid -> FieldError.ACCOUNT_NUMBER_INVALID
            else -> null
        }

        val beneficiaryName = resolvedNameFor(input, numberValid)
        val beneficiaryError = when {
            beneficiaryName != null -> null
            input.resolution is ResolutionState.Loading -> FieldError.BENEFICIARY_RESOLVING
            else -> FieldError.BENEFICIARY_NOT_RESOLVED
        }

        val (amountError, amounts) = validateAmount(input)

        val validated = if (
            accountError == null && bankError == null && numberError == null &&
            beneficiaryError == null && amountError == null &&
            input.account != null && input.bank != null && beneficiaryName != null && amounts != null
        ) {
            ValidatedTransfer(input.account, input.bank, input.accountNumber, beneficiaryName, amounts)
        } else {
            null
        }

        return ValidationResult(
            account = accountError,
            bank = bankError,
            accountNumber = numberError,
            beneficiary = beneficiaryError,
            amount = amountError,
            amounts = amounts,
            validated = validated,
        )
    }

    private fun isValidAccountNumber(value: String) =
        value.length == TransferPolicy.ACCOUNT_NUMBER_LENGTH && value.all { it in '0'..'9' }

    /** A name only counts if it was resolved for the EXACT bank + account number currently entered. */
    private fun resolvedNameFor(input: TransferFormInput, numberValid: Boolean): String? {
        val resolved = input.resolution as? ResolutionState.Resolved ?: return null
        val bank = input.bank ?: return null
        if (!numberValid) return null
        return resolved.accountName.takeIf { resolved.key == ResolutionKey(bank.code, input.accountNumber) }
    }

    private data class AmountCheck(val error: FieldError?, val amounts: TransferAmounts?)

    private fun validateAmount(input: TransferFormInput): AmountCheck =
        when (val parsed = MoneyParser.parse(input.amountInput)) {
            AmountParseResult.Empty -> AmountCheck(FieldError.AMOUNT_REQUIRED, null)
            AmountParseResult.Invalid -> AmountCheck(FieldError.AMOUNT_INVALID, null)
            AmountParseResult.TooLarge -> AmountCheck(FieldError.AMOUNT_TOO_LARGE, null)
            is AmountParseResult.Valid -> when {
                parsed.kobo <= 0L -> AmountCheck(FieldError.AMOUNT_NOT_POSITIVE, null)
                parsed.kobo > TransferPolicy.MAX_TRANSFER_KOBO -> AmountCheck(FieldError.AMOUNT_TOO_LARGE, null)
                else -> {
                    val amounts = TransferAmounts.of(parsed.kobo)
                    val account = input.account
                    val insufficient = account != null && account.balanceKobo < amounts.totalDebitKobo
                    AmountCheck(if (insufficient) FieldError.INSUFFICIENT_FUNDS else null, amounts)
                }
            }
        }
}
