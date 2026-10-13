package com.akeem.mobilebanking.ui

import com.akeem.mobilebanking.domain.model.FailureReason
import com.akeem.mobilebanking.domain.model.ResolutionFailure
import com.akeem.mobilebanking.domain.model.ResolutionState
import com.akeem.mobilebanking.domain.model.ResolveResult
import com.akeem.mobilebanking.domain.model.TransferOutcome
import com.akeem.mobilebanking.domain.transfer.FieldError
import com.akeem.mobilebanking.domain.transfer.TransferValidator
import com.akeem.mobilebanking.testutil.ACCESS
import com.akeem.mobilebanking.testutil.FakeAccountRepository
import com.akeem.mobilebanking.testutil.FakeBankRepository
import com.akeem.mobilebanking.testutil.FakeBeneficiaryRepository
import com.akeem.mobilebanking.testutil.FakeTransferRepository
import com.akeem.mobilebanking.testutil.GTB
import com.akeem.mobilebanking.testutil.MainDispatcherRule
import com.akeem.mobilebanking.testutil.SALARY
import com.akeem.mobilebanking.testutil.SAVINGS
import com.akeem.mobilebanking.testutil.SequentialReferenceGenerator
import com.akeem.mobilebanking.ui.transfer.TransferEvent
import com.akeem.mobilebanking.ui.transfer.TransferProcessState
import com.akeem.mobilebanking.ui.transfer.TransferViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private val accounts = FakeAccountRepository()
    private val beneficiaries = FakeBeneficiaryRepository()
    private val transfers = FakeTransferRepository()

    private fun TestScope.newViewModel(): TransferViewModel {
        val vm = TransferViewModel(
            accounts, FakeBankRepository(), beneficiaries, transfers, TransferValidator(), SequentialReferenceGenerator(),
        )
        advanceUntilIdle() // let accounts + banks load
        return vm
    }

    /** Fills a valid form and answers the resolution request with [name]. */
    private fun TestScope.fillValidForm(
        vm: TransferViewModel,
        number: String = "0123456789",
        amount: String = "25000",
        name: String = "JOHN DOE",
    ) {
        vm.onAccountSelected(SAVINGS.id)
        vm.onBankSelected(ACCESS.code)
        vm.onAccountNumberChanged(number)
        vm.onAmountChanged(amount)
        advanceUntilIdle()
        beneficiaries.calls.last().response.complete(ResolveResult.Success(name))
        advanceUntilIdle()
    }

    private fun TestScope.collectEvents(vm: TransferViewModel): List<TransferEvent> {
        val received = mutableListOf<TransferEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.collect { received += it } }
        return received
    }

    // ───────────── Account resolution ─────────────

    @Test fun `resolution starts only once bank and ten digits are present, shows loading, then the name`() = runTest {
        val vm = newViewModel()
        vm.onAccountNumberChanged("0123456789")
        advanceUntilIdle()
        assertEquals(ResolutionState.Idle, vm.uiState.value.resolution) // no bank yet
        assertTrue(beneficiaries.calls.isEmpty())

        vm.onBankSelected(ACCESS.code)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.resolution is ResolutionState.Loading)

        beneficiaries.calls.single().response.complete(ResolveResult.Success("JOHN DOE"))
        advanceUntilIdle()
        assertEquals("JOHN DOE", (vm.uiState.value.resolution as ResolutionState.Resolved).accountName)
    }

    @Test fun `failures are surfaced and retryable`() = runTest {
        val vm = newViewModel()
        vm.onBankSelected(ACCESS.code)
        vm.onAccountNumberChanged("0123456789")
        advanceUntilIdle()
        beneficiaries.calls.last().response.complete(ResolveResult.ServiceError)
        advanceUntilIdle()
        val failed = vm.uiState.value.resolution as ResolutionState.Failed
        assertEquals(ResolutionFailure.SERVICE_ERROR, failed.reason)

        vm.onRetryResolution()
        advanceUntilIdle()
        assertEquals(2, beneficiaries.calls.size)
        assertTrue(vm.uiState.value.resolution is ResolutionState.Loading)
    }

    @Test fun `an older response arriving after a newer one never overwrites it (cancellation honoured)`() = runTest {
        val vm = newViewModel()
        vm.onBankSelected(ACCESS.code)
        vm.onAccountNumberChanged("0000000001")
        advanceUntilIdle()
        vm.onAccountNumberChanged("0000000002")
        advanceUntilIdle()
        assertEquals(2, beneficiaries.calls.size)

        beneficiaries.calls[1].response.complete(ResolveResult.Success("NAME TWO"))
        advanceUntilIdle()
        beneficiaries.calls[0].response.complete(ResolveResult.Success("NAME ONE"))
        advanceUntilIdle()

        assertEquals("NAME TWO", (vm.uiState.value.resolution as ResolutionState.Resolved).accountName)
    }

    @Test fun `stale responses are ignored even when the data source ignores cancellation`() = runTest {
        beneficiaries.ignoreCancellation = true
        val vm = newViewModel()
        vm.onBankSelected(ACCESS.code)
        vm.onAccountNumberChanged("0000000001")
        advanceUntilIdle()
        vm.onAccountNumberChanged("0000000002")
        advanceUntilIdle()

        // The OLD request answers first: it must not be displayed.
        beneficiaries.calls[0].response.complete(ResolveResult.Success("NAME ONE"))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.resolution is ResolutionState.Loading)

        beneficiaries.calls[1].response.complete(ResolveResult.Success("NAME TWO"))
        advanceUntilIdle()
        assertEquals("NAME TWO", (vm.uiState.value.resolution as ResolutionState.Resolved).accountName)
    }

    @Test fun `changing the bank clears the old beneficiary and re-resolves`() = runTest {
        val vm = newViewModel()
        fillValidForm(vm)
        assertTrue(vm.uiState.value.resolution is ResolutionState.Resolved)

        vm.onBankSelected(GTB.code)
        advanceUntilIdle()

        val resolution = vm.uiState.value.resolution
        assertTrue("old name must be gone", resolution is ResolutionState.Loading)
        assertEquals(GTB.code, (resolution as ResolutionState.Loading).key.bankCode)
    }

    @Test fun `editing the account number clears the beneficiary immediately`() = runTest {
        val vm = newViewModel()
        fillValidForm(vm)
        vm.onAccountNumberChanged("012345678")
        advanceUntilIdle()
        assertEquals(ResolutionState.Idle, vm.uiState.value.resolution)
    }

    @Test fun `non-digits are stripped and input is capped at ten digits`() = runTest {
        val vm = newViewModel()
        vm.onAccountNumberChanged("01a2-3456 78901234")
        assertEquals("0123456789", vm.uiState.value.accountNumber)
    }

    // ───────────── Continue / validation / confirmation ─────────────

    @Test fun `continue with an incomplete form shows all errors and does not create a draft`() = runTest {
        val vm = newViewModel()
        val events = collectEvents(vm)
        vm.onContinue()
        advanceUntilIdle()

        val validation = vm.uiState.value.validation
        assertEquals(FieldError.ACCOUNT_REQUIRED, validation.account)
        assertEquals(FieldError.BANK_REQUIRED, validation.bank)
        assertEquals(FieldError.ACCOUNT_NUMBER_REQUIRED, validation.accountNumber)
        assertEquals(FieldError.AMOUNT_REQUIRED, validation.amount)
        assertNull(vm.uiState.value.draft)
        assertTrue(events.isEmpty())
    }

    @Test fun `insufficient balance blocks continue`() = runTest {
        val vm = newViewModel()
        vm.onAccountSelected(SALARY.id) // ₦8,420.50
        vm.onBankSelected(ACCESS.code)
        vm.onAccountNumberChanged("0123456789")
        vm.onAmountChanged("9000")
        advanceUntilIdle()
        beneficiaries.calls.last().response.complete(ResolveResult.Success("JOHN DOE"))
        advanceUntilIdle()

        vm.onContinue()
        advanceUntilIdle()
        assertEquals(FieldError.INSUFFICIENT_FUNDS, vm.uiState.value.validation.amount)
        assertNull(vm.uiState.value.draft)
    }

    @Test fun `continue is blocked while the beneficiary is still being resolved`() = runTest {
        val vm = newViewModel()
        vm.onAccountSelected(SAVINGS.id)
        vm.onBankSelected(ACCESS.code)
        vm.onAccountNumberChanged("0123456789")
        vm.onAmountChanged("25000")
        advanceUntilIdle() // resolution still in flight
        vm.onContinue()
        advanceUntilIdle()
        assertEquals(FieldError.BENEFICIARY_RESOLVING, vm.uiState.value.validation.beneficiary)
        assertNull(vm.uiState.value.draft)
    }

    @Test fun `valid form creates a draft with fee and total and asks to navigate to confirmation`() = runTest {
        val vm = newViewModel()
        val events = collectEvents(vm)
        fillValidForm(vm)
        vm.onNarrationChanged("  Monthly allowance ")
        vm.onContinue()
        advanceUntilIdle()

        val draft = requireNotNull(vm.uiState.value.draft)
        assertEquals("REF1", draft.reference)
        assertEquals("JOHN DOE", draft.beneficiaryName)
        assertEquals(2_500_000L, draft.amounts.amountKobo)
        assertEquals(2_500L, draft.amounts.feeKobo)
        assertEquals(2_502_500L, draft.amounts.totalDebitKobo)
        assertEquals("Monthly allowance", draft.narration)
        assertEquals(listOf<TransferEvent>(TransferEvent.NavigateToConfirmation), events)
    }

    @Test fun `going back from confirmation keeps everything the customer entered`() = runTest {
        val vm = newViewModel()
        fillValidForm(vm)
        vm.onContinue() // → confirmation; "Cancel" is just popping the back stack, no VM reset
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("0123456789", state.accountNumber)
        assertEquals("25000", state.amountInput)
        assertEquals(SAVINGS.id, state.selectedAccountId)
        assertEquals(ACCESS.code, state.selectedBankCode)
        assertTrue(state.resolution is ResolutionState.Resolved)
        assertEquals(TransferProcessState.Idle, state.process)
        assertEquals(0, transfers.submitted.size) // nothing is submitted without explicit confirmation
    }

    // ───────────── Submission ─────────────

    private fun TestScope.vmAtConfirmation(): TransferViewModel {
        val vm = newViewModel()
        fillValidForm(vm)
        vm.onContinue()
        advanceUntilIdle()
        return vm
    }

    @Test fun `rapid repeated confirm taps submit exactly once`() = runTest {
        val gate = CompletableDeferred<Unit>()
        transfers.submitGate = gate
        val vm = vmAtConfirmation()

        repeat(5) { vm.confirmTransfer() }
        advanceUntilIdle()

        assertEquals(1, transfers.submitted.size)
        assertEquals(TransferProcessState.Submitting, vm.uiState.value.process)

        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, transfers.submitted.size)

        // Even after completion, confirming again must not resubmit.
        vm.confirmTransfer()
        advanceUntilIdle()
        assertEquals(1, transfers.submitted.size)
    }

    @Test fun `submitted request carries the draft's reference, amounts and beneficiary`() = runTest {
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()

        val request = transfers.submitted.single()
        assertEquals("REF1", request.reference)
        assertEquals(2_500_000L, request.amountKobo)
        assertEquals(2_500L, request.feeKobo)
        assertEquals("JOHN DOE", request.beneficiaryName)
        assertEquals(ACCESS.code, request.bankCode)
    }

    @Test fun `successful outcome is surfaced and leaves nothing unresolved`() = runTest {
        transfers.submitOutcome = TransferOutcome.Successful("REF1")
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()

        assertEquals(TransferProcessState.Finished(TransferOutcome.Successful("REF1")), vm.uiState.value.process)
        assertNull(vm.uiState.value.unresolved)
    }

    @Test fun `failed outcome is surfaced and leaves nothing unresolved`() = runTest {
        transfers.submitOutcome = TransferOutcome.Failed(FailureReason.REJECTED_BY_BANK)
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()

        assertEquals(
            TransferProcessState.Finished(TransferOutcome.Failed(FailureReason.REJECTED_BY_BANK)),
            vm.uiState.value.process,
        )
        assertNull(vm.uiState.value.unresolved)
    }

    @Test fun `pending outcome is surfaced and keeps the original reference unresolved`() = runTest {
        transfers.submitOutcome = TransferOutcome.Pending
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()

        assertEquals(TransferProcessState.Finished(TransferOutcome.Pending), vm.uiState.value.process)
        assertEquals("REF1", vm.uiState.value.unresolved?.draft?.reference)
    }

    @Test fun `unknown outcome is surfaced and keeps the original reference unresolved`() = runTest {
        transfers.submitOutcome = TransferOutcome.Unknown
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()

        assertEquals(TransferProcessState.Finished(TransferOutcome.Unknown), vm.uiState.value.process)
        assertEquals("REF1", vm.uiState.value.unresolved?.draft?.reference)
    }

    @Test fun `an unexpected exception is treated as unknown, never as failed`() = runTest {
        transfers.submitThrows = java.io.IOException("socket timeout")
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()

        assertEquals(TransferProcessState.Finished(TransferOutcome.Unknown), vm.uiState.value.process)
        assertNotNull(vm.uiState.value.unresolved)
    }

    // ───────────── Uncertain outcomes ─────────────

    @Test fun `after an unknown outcome a new transfer is blocked until the original is checked`() = runTest {
        transfers.submitOutcome = TransferOutcome.Unknown
        val vm = vmAtConfirmation()
        val events = collectEvents(vm)
        vm.confirmTransfer()
        advanceUntilIdle()
        vm.onResultDismissed() // customer taps "Done"

        vm.onContinue()
        advanceUntilIdle()

        assertEquals(listOf<TransferEvent>(TransferEvent.PreviousTransferUnresolved), events)
        assertEquals("REF1", vm.uiState.value.draft?.reference) // no new draft / reference
        assertEquals(1, transfers.submitted.size)
    }

    @Test fun `checking status queries the ORIGINAL reference and never submits again`() = runTest {
        transfers.submitOutcome = TransferOutcome.Unknown
        transfers.statusOutcome = TransferOutcome.Successful("REF1")
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()

        vm.checkStatus()
        advanceUntilIdle()

        assertEquals(listOf("REF1"), transfers.statusChecks)
        assertEquals(1, transfers.submitted.size)
        assertEquals(TransferProcessState.Finished(TransferOutcome.Successful("REF1")), vm.uiState.value.process)
        assertNull(vm.uiState.value.unresolved) // settled
    }

    @Test fun `a status check that is still pending keeps the transfer unresolved`() = runTest {
        transfers.submitOutcome = TransferOutcome.Pending
        transfers.statusOutcome = TransferOutcome.Pending
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()
        vm.checkStatus()
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.unresolved)
    }

    @Test fun `a status check that finds nothing resolves as failed so the customer can retry safely`() = runTest {
        transfers.submitOutcome = TransferOutcome.Unknown
        transfers.statusOutcome = TransferOutcome.Failed(FailureReason.NOT_PROCESSED)
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()
        vm.checkStatus()
        advanceUntilIdle()
        assertNull(vm.uiState.value.unresolved)
        vm.onResultDismissed()
        assertEquals(TransferProcessState.Idle, vm.uiState.value.process)
    }

    @Test fun `status checks cannot be started while one is already running`() = runTest {
        transfers.submitOutcome = TransferOutcome.Unknown
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()

        vm.checkStatus()
        vm.checkStatus()
        vm.checkStatus()
        advanceUntilIdle()
        assertEquals(1, transfers.statusChecks.size)
    }

    // ───────────── Leaving the result screen ─────────────

    @Test fun `after success the form is reset for the next transfer`() = runTest {
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()
        vm.onResultDismissed()

        val state = vm.uiState.value
        assertEquals("", state.accountNumber)
        assertEquals("", state.amountInput)
        assertNull(state.selectedAccountId)
        assertNull(state.draft)
        assertEquals(TransferProcessState.Idle, state.process)
        assertFalse(state.accounts.isEmpty()) // lookups preserved
    }

    @Test fun `after a failure the entered details are kept and a retry gets a NEW reference`() = runTest {
        transfers.submitOutcome = TransferOutcome.Failed(FailureReason.REJECTED_BY_BANK)
        val vm = vmAtConfirmation()
        vm.confirmTransfer()
        advanceUntilIdle()
        vm.onResultDismissed()

        assertEquals("0123456789", vm.uiState.value.accountNumber)
        assertNull(vm.uiState.value.draft)

        transfers.submitOutcome = TransferOutcome.Successful("REF2")
        vm.onContinue()
        advanceUntilIdle()
        assertEquals("REF2", vm.uiState.value.draft?.reference)
    }
}
