package com.akeem.mobilebanking.ui.transfer

import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.model.Bank
import com.akeem.mobilebanking.domain.model.ResolutionState
import com.akeem.mobilebanking.domain.model.TransferDraft
import com.akeem.mobilebanking.domain.model.TransferOutcome
import com.akeem.mobilebanking.domain.transfer.ValidationResult

sealed interface TransferProcessState {
    data object Idle : TransferProcessState
    data object Submitting : TransferProcessState
    data object CheckingStatus : TransferProcessState
    data class Finished(val outcome: TransferOutcome) : TransferProcessState
}

/** A transfer whose outcome is Pending/Unknown. It blocks new transfers until its status is settled. */
data class UnresolvedTransfer(val draft: TransferDraft, val outcome: TransferOutcome)

data class TransferUiState(
    val accounts: List<Account> = emptyList(),
    val banks: List<Bank> = emptyList(),
    val selectedAccountId: String? = null,
    val selectedBankCode: String? = null,
    val accountNumber: String = "",
    val amountInput: String = "",
    val narration: String = "",
    val resolution: ResolutionState = ResolutionState.Idle,
    val showAllErrors: Boolean = false,
    /** Errors currently visible to the customer (live errors, or all of them after a Continue tap). */
    val validation: ValidationResult = ValidationResult(),
    val draft: TransferDraft? = null,
    val process: TransferProcessState = TransferProcessState.Idle,
    val unresolved: UnresolvedTransfer? = null,
) {
    val selectedAccount: Account? get() = accounts.firstOrNull { it.id == selectedAccountId }
    val selectedBank: Bank? get() = banks.firstOrNull { it.code == selectedBankCode }
    val isBusy: Boolean
        get() = process is TransferProcessState.Submitting || process is TransferProcessState.CheckingStatus
}

sealed interface TransferEvent {
    data object NavigateToConfirmation : TransferEvent
    data object PreviousTransferUnresolved : TransferEvent
}
