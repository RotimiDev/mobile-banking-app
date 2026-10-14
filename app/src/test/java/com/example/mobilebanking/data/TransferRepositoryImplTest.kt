package com.example.mobilebanking.data

import com.example.mobilebanking.data.repository.TransferRepositoryImpl
import com.example.mobilebanking.data.datasource.BankDataSource
import com.example.mobilebanking.data.simulation.SimulationSettings
import com.example.mobilebanking.data.simulation.TransferSimulation
import com.example.mobilebanking.domain.model.FailureReason
import com.example.mobilebanking.domain.model.TransactionStatus
import com.example.mobilebanking.domain.model.TransferOutcome
import com.example.mobilebanking.domain.model.TransferRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TransferRepositoryImplTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-13T09:00:00Z"), ZoneOffset.UTC)
    private val store = BankDataSource(clock)
    private val simulationSettings = SimulationSettings()
    private val repository = TransferRepositoryImpl(store, simulationSettings)

    private fun request(reference: String = "REF1", amountKobo: Long = 2_500_000L) =
        TransferRequest(
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

    @Test
    fun `success debits amount plus fee once and records a successful debit transaction`() =
        runTest {
            val outcome = repository.submit(request())
            assertEquals(TransferOutcome.Successful("REF1"), outcome)
            assertEquals(15_000_000L - 2_502_500L, balance())
            val recorded = store.transactions.value.single()
            assertEquals(TransactionStatus.SUCCESSFUL, recorded.status)
            assertEquals("Transfer to John Doe", recorded.title)
            assertEquals(2_500_000L, recorded.amount)
        }

    @Test
    fun `replaying the same reference is idempotent and never debits twice`() = runTest {
        repository.submit(request())
        val balanceAfterFirst = balance()
        val replay = repository.submit(request())
        assertEquals(TransferOutcome.Successful("REF1"), replay)
        assertEquals(balanceAfterFirst, balance())
        assertEquals(1, store.transactions.value.size)
    }

    @Test
    fun `failed transfers are recorded as failed and do not debit`() = runTest {
        simulationSettings.transfer = TransferSimulation.FAILED
        val outcome = repository.submit(request())
        assertEquals(TransferOutcome.Failed(FailureReason.REJECTED_BY_BANK), outcome)
        assertEquals(15_000_000L, balance())
        assertEquals(TransactionStatus.FAILED, store.transactions.value.single().status)
    }

    @Test
    fun `pending settles to successful on a later status check, with a single debit`() = runTest {
        simulationSettings.transfer = TransferSimulation.PENDING
        assertEquals(TransferOutcome.Pending, repository.submit(request()))
        assertEquals(TransactionStatus.PENDING, store.transactions.value.single().status)
        assertEquals(TransferOutcome.Pending, repository.checkStatus("REF1"))
        assertEquals(TransferOutcome.Successful("REF1"), repository.checkStatus("REF1"))
        assertEquals(TransactionStatus.SUCCESSFUL, store.transactions.value.single().status)
        assertEquals(1, store.transactions.value.size)
        assertEquals(15_000_000L - 2_502_500L, balance())
    }

    @Test
    fun `timeout returns unknown, and the status check reveals the transfer actually went through`() =
        runTest {
            simulationSettings.transfer = TransferSimulation.TIMEOUT
            assertEquals(TransferOutcome.Unknown, repository.submit(request()))
            assertTrue("client does not know yet", store.transactions.value.isEmpty())

            assertEquals(TransferOutcome.Successful("REF1"), repository.checkStatus("REF1"))
            assertEquals(1, store.transactions.value.size)
            assertEquals(15_000_000L - 2_502_500L, balance())

            repository.checkStatus("REF1")
            assertEquals(15_000_000L - 2_502_500L, balance())
        }

    @Test
    fun `the backend re-validates the balance independently of the client`() = runTest {
        val outcome = repository.submit(request(amountKobo = 20_000_000L))
        assertEquals(TransferOutcome.Failed(FailureReason.INSUFFICIENT_FUNDS), outcome)
        assertEquals(15_000_000L, balance())
    }

    @Test
    fun `status of a reference the backend never saw is reported as not processed`() = runTest {
        assertEquals(
            TransferOutcome.Failed(FailureReason.NOT_PROCESSED),
            repository.checkStatus("NOPE")
        )
    }
}
