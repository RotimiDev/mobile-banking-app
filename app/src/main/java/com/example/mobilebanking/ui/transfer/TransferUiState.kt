package com.example.mobilebanking.ui.transfer

import com.example.mobilebanking.domain.model.Account
import com.example.mobilebanking.domain.model.Bank
import com.example.mobilebanking.domain.model.ResolutionState
import com.example.mobilebanking.domain.model.TransferDraft
import com.example.mobilebanking.domain.model.TransferOutcome
import com.example.mobilebanking.domain.transfer.ValidationResult

sealed interface TransferProcessState {
    data object Idle : TransferProcessState
    data object Submitting : TransferProcessState
    data object CheckingStatus : TransferProcessState
    data class Finished(val outcome: TransferOutcome) : TransferProcessState
}

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
