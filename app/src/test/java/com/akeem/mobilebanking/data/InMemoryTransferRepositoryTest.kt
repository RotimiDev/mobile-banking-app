package com.akeem.mobilebanking.data

import com.akeem.mobilebanking.domain.model.FailureReason
import com.akeem.mobilebanking.domain.model.TransactionStatus
import com.akeem.mobilebanking.domain.model.TransferOutcome
import com.akeem.mobilebanking.domain.model.TransferRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryTransferRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-13T09:00:00Z"), ZoneOffset.UTC)
    private val store = InMemoryBankingStore(clock)
    private val simulation = SimulationSettings()
    private val repository = InMemoryTransferRepository(store, simulation)

    private fun request(reference: String = "REF1", amountKobo: Long = 2_500_000L) = TransferRequest(
        reference = reference,
        debitAccountId = "acc_savings",
        bankCode = "044",
        bankName = "Access Bank",
        accountNumber = "0123456789",
        beneficiaryName = "JOHN DOE",
        amountKobo = amountKobo,
        feeKobo = 2_500L,
        narration = "",
    )

    private fun balance() = store.accounts.value.first { it.id == "acc_savings" }.balanceKobo

    @Test fun `success debits amount plus fee once and records a successful debit transaction`() = runTest {
        val outcome = repository.submit(request())

        assertEquals(TransferOutcome.Successful("REF1"), outcome)
        assertEquals(15_000_000L - 2_502_500L, balance())
        val recorded = store.transactions.value.single()
        assertEquals(TransactionStatus.SUCCESSFUL, recorded.status)
        assertEquals("Transfer to John Doe", recorded.title)
        assertEquals(2_500_000L, recorded.amount)
    }

    @Test fun `replaying the same reference is idempotent and never debits twice`() = runTest {
        repository.submit(request())
        val balanceAfterFirst = balance()

        val replay = repository.submit(request())

        assertEquals(TransferOutcome.Successful("REF1"), replay)
        assertEquals(balanceAfterFirst, balance())
        assertEquals(1, store.transactions.value.size)
    }

    @Test fun `failed transfers are recorded as failed and do not debit`() = runTest {
        simulation.transfer = TransferSimulation.FAILED
        val outcome = repository.submit(request())

        assertEquals(TransferOutcome.Failed(FailureReason.REJECTED_BY_BANK), outcome)
        assertEquals(15_000_000L, balance())
        assertEquals(TransactionStatus.FAILED, store.transactions.value.single().status)
    }

    @Test fun `pending settles to successful on a later status check, with a single debit`() = runTest {
        simulation.transfer = TransferSimulation.PENDING
        assertEquals(TransferOutcome.Pending, repository.submit(request()))
        assertEquals(TransactionStatus.PENDING, store.transactions.value.single().status)

        assertEquals(TransferOutcome.Pending, repository.checkStatus("REF1"))
        assertEquals(TransferOutcome.Successful("REF1"), repository.checkStatus("REF1"))

        assertEquals(TransactionStatus.SUCCESSFUL, store.transactions.value.single().status)
        assertEquals(1, store.transactions.value.size)
        assertEquals(15_000_000L - 2_502_500L, balance())
    }

    @Test fun `timeout returns unknown, and the status check reveals the transfer actually went through`() = runTest {
        simulation.transfer = TransferSimulation.TIMEOUT
        assertEquals(TransferOutcome.Unknown, repository.submit(request()))
        assertTrue("client does not know yet", store.transactions.value.isEmpty())

        assertEquals(TransferOutcome.Successful("REF1"), repository.checkStatus("REF1"))
        assertEquals(1, store.transactions.value.size)
        assertEquals(15_000_000L - 2_502_500L, balance())

        repository.checkStatus("REF1") // repeated checks stay idempotent
        assertEquals(15_000_000L - 2_502_500L, balance())
    }

    @Test fun `the backend re-validates the balance independently of the client`() = runTest {
        val outcome = repository.submit(request(amountKobo = 20_000_000L)) // ₦200,000 > ₦150,000
        assertEquals(TransferOutcome.Failed(FailureReason.INSUFFICIENT_FUNDS), outcome)
        assertEquals(15_000_000L, balance())
    }

    @Test fun `status of a reference the backend never saw is reported as not processed`() = runTest {
        assertEquals(TransferOutcome.Failed(FailureReason.NOT_PROCESSED), repository.checkStatus("NOPE"))
    }
}
