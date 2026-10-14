package com.example.mobilebanking.ui.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebanking.domain.model.ResolutionFailure
import com.example.mobilebanking.domain.model.ResolutionKey
import com.example.mobilebanking.domain.model.ResolutionState
import com.example.mobilebanking.domain.model.ResolveResult
import com.example.mobilebanking.domain.model.TransferDraft
import com.example.mobilebanking.domain.model.TransferOutcome
import com.example.mobilebanking.domain.model.TransferPolicy
import com.example.mobilebanking.domain.repository.AccountRepository
import com.example.mobilebanking.domain.repository.BankRepository
import com.example.mobilebanking.domain.repository.BeneficiaryRepository
import com.example.mobilebanking.domain.repository.ReferenceGenerator
import com.example.mobilebanking.domain.repository.TransferRepository
import com.example.mobilebanking.domain.transfer.TransferFormInput
import com.example.mobilebanking.domain.transfer.TransferValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val bankRepository: BankRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val transferRepository: TransferRepository,
    private val validator: TransferValidator,
    private val referenceGenerator: ReferenceGenerator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    private val _events = Channel<TransferEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var resolutionJob: Job? = null
    private var latestResolutionRequestId = 0L

    init {
        viewModelScope.launch {
            accountRepository.accounts.collect { accounts ->
                _uiState.update { it.copy(accounts = accounts).recomputed() }
            }
        }
        viewModelScope.launch {
            val banks = bankRepository.getBanks()
            _uiState.update { it.copy(banks = banks).recomputed() }
        }
    }

    fun onAccountSelected(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId).recomputed() }
    }

    fun onBankSelected(bankCode: String) {
        if (_uiState.value.selectedBankCode == bankCode) return
        _uiState.update { it.copy(selectedBankCode = bankCode).recomputed() }
        restartResolution()
    }

    fun onAccountNumberChanged(raw: String) {
        val digits = raw.filter { it in '0'..'9' }.take(TransferPolicy.ACCOUNT_NUMBER_LENGTH)
        if (digits == _uiState.value.accountNumber) return
        _uiState.update { it.copy(accountNumber = digits).recomputed() }
        restartResolution()
    }

    fun onAmountChanged(value: String) {
        if (value == _uiState.value.amountInput) return
        _uiState.update { it.copy(amountInput = value).recomputed() }
    }

    fun onNarrationChanged(value: String) {
        if (value == _uiState.value.narration) return
        _uiState.update { it.copy(narration = value.take(TransferPolicy.NARRATION_MAX_LENGTH)) }
    }

    fun onRetryResolution() {
        if (_uiState.value.resolution is ResolutionState.Failed) restartResolution()
    }

    private fun restartResolution() {
        resolutionJob?.cancel()
        val requestId = ++latestResolutionRequestId

        val state = _uiState.value
        val bank = state.selectedBank
        if (bank == null || state.accountNumber.length != TransferPolicy.ACCOUNT_NUMBER_LENGTH) {
            setResolution(ResolutionState.Idle)
            return
        }

        val key = ResolutionKey(bank.code, state.accountNumber)
        setResolution(ResolutionState.Loading(key))
        resolutionJob = viewModelScope.launch {
            val result = beneficiaryRepository.resolve(key.bankCode, key.accountNumber)
            applyResolution(requestId, key, result)
        }
    }

    private fun applyResolution(requestId: Long, key: ResolutionKey, result: ResolveResult) {
        if (requestId != latestResolutionRequestId) return
        val resolution = when (result) {
            is ResolveResult.Success -> ResolutionState.Resolved(key, result.accountName)
            ResolveResult.NotFound -> ResolutionState.Failed(
                key,
                ResolutionFailure.ACCOUNT_NOT_FOUND
            )

            ResolveResult.ServiceError -> ResolutionState.Failed(
                key,
                ResolutionFailure.SERVICE_ERROR
            )
        }
        _uiState.update { state ->
            val currentBank = state.selectedBank
            val stillCurrent = currentBank != null &&
                    ResolutionKey(currentBank.code, state.accountNumber) == key
            if (stillCurrent) state.copy(resolution = resolution).recomputed() else state
        }
    }

    private fun setResolution(resolution: ResolutionState) {
        _uiState.update { it.copy(resolution = resolution).recomputed() }
    }

    fun onContinue() {
        val state = _uiState.value
        if (state.isBusy) return
        if (state.unresolved != null) {
            _events.trySend(TransferEvent.PreviousTransferUnresolved)
            return
        }

        val validated = validator.validate(state.toInput()).validated
        if (validated == null) {
            _uiState.update { it.copy(showAllErrors = true).recomputed() }
            return
        }

        val draft = TransferDraft(
            reference = referenceGenerator.next(),
            debitAccount = validated.account,
            bank = validated.bank,
            accountNumber = validated.accountNumber,
            beneficiaryName = validated.beneficiaryName,
            amounts = validated.amounts,
            narration = state.narration.trim(),
        )
        _uiState.update { it.copy(draft = draft, process = TransferProcessState.Idle) }
        _events.trySend(TransferEvent.NavigateToConfirmation)
    }

    fun confirmTransfer() {
        var draft: TransferDraft? = null
        _uiState.update { state ->
            if (state.process == TransferProcessState.Idle && state.draft != null) {
                draft = state.draft
                state.copy(process = TransferProcessState.Submitting)
            } else {
                draft = null
                state
            }
        }
        val accepted = draft ?: return

        viewModelScope.launch {
            val outcome = try {
                transferRepository.submit(accepted.toRequest())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                TransferOutcome.Unknown
            }
            applyOutcome(outcome)
        }
    }

    fun checkStatus() {
        var reference: String? = null
        _uiState.update { state ->
            val unresolved = state.unresolved
            if (unresolved != null && !state.isBusy) {
                reference = unresolved.draft.reference
                state.copy(process = TransferProcessState.CheckingStatus)
            } else {
                reference = null
                state
            }
        }
        val ref = reference ?: return

        viewModelScope.launch {
            val outcome = try {
                transferRepository.checkStatus(ref)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                TransferOutcome.Unknown
            }
            applyOutcome(outcome)
        }
    }

    private fun applyOutcome(outcome: TransferOutcome) {
        _uiState.update { state ->
            val unresolved = when (outcome) {
                TransferOutcome.Pending, TransferOutcome.Unknown ->
                    state.draft?.let { UnresolvedTransfer(it, outcome) }

                else -> null
            }
            state.copy(process = TransferProcessState.Finished(outcome), unresolved = unresolved)
        }
    }

    fun onResultDismissed() {
        _uiState.update { state ->
            val process = state.process as? TransferProcessState.Finished ?: return@update state
            when (process.outcome) {
                is TransferOutcome.Successful -> {
                    latestResolutionRequestId++
                    resolutionJob?.cancel()
                    TransferUiState(accounts = state.accounts, banks = state.banks)
                }

                is TransferOutcome.Failed -> state.copy(
                    draft = null,
                    process = TransferProcessState.Idle
                ).recomputed()

                TransferOutcome.Pending, TransferOutcome.Unknown -> state.copy(process = TransferProcessState.Idle)
            }
        }
    }

    private fun TransferUiState.toInput() = TransferFormInput(
        account = selectedAccount,
        bank = selectedBank,
        accountNumber = accountNumber,
        amountInput = amountInput,
        resolution = resolution,
    )

    private fun TransferUiState.recomputed(): TransferUiState {
        val full = validator.validate(toInput())
        return copy(validation = if (showAllErrors) full else full.onlyLive())
    }
}
