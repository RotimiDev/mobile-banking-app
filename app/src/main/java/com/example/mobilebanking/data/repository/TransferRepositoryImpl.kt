package com.example.mobilebanking.data.repository

import com.example.mobilebanking.data.datasource.BankDataSource
import com.example.mobilebanking.data.simulation.SimulationSettings
import com.example.mobilebanking.data.simulation.TransferSimulation
import com.example.mobilebanking.domain.model.FailureReason
import com.example.mobilebanking.domain.model.TransactionStatus
import com.example.mobilebanking.domain.model.TransferOutcome
import com.example.mobilebanking.domain.model.TransferRequest
import com.example.mobilebanking.domain.repository.TransferRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val store: BankDataSource,
    private val simulation: SimulationSettings,
) : TransferRepository {

    private class LedgerEntry(
        val request: TransferRequest,
        var status: TransactionStatus,
        var pollsWhilePending: Int = 0,
    )

    private val ledger = mutableMapOf<String, LedgerEntry>()

    override suspend fun submit(request: TransferRequest): TransferOutcome {
        ledger[request.reference]?.let { return outcomeOf(it) }

        delay(1_800.milliseconds)
        if (!store.hasSufficientFunds(request.debitAccountId, request.totalDebitKobo)) {
            return TransferOutcome.Failed(FailureReason.INSUFFICIENT_FUNDS)
        }

        return when (simulation.transfer) {
            TransferSimulation.SUCCESS -> {
                ledger[request.reference] = LedgerEntry(request, TransactionStatus.SUCCESSFUL)
                store.recordTransfer(request, TransactionStatus.SUCCESSFUL)
                TransferOutcome.Successful(request.reference)
            }

            TransferSimulation.FAILED -> {
                ledger[request.reference] = LedgerEntry(request, TransactionStatus.FAILED)
                store.recordTransfer(request, TransactionStatus.FAILED)
                TransferOutcome.Failed(FailureReason.REJECTED_BY_BANK)
            }

            TransferSimulation.PENDING -> {
                ledger[request.reference] = LedgerEntry(request, TransactionStatus.PENDING)
                store.recordTransfer(request, TransactionStatus.PENDING)
                TransferOutcome.Pending
            }

            TransferSimulation.TIMEOUT -> {
                ledger[request.reference] = LedgerEntry(request, TransactionStatus.SUCCESSFUL)
                delay(4_000.milliseconds)
                TransferOutcome.Unknown
            }
        }
    }

    override suspend fun checkStatus(reference: String): TransferOutcome {
        delay(1_200.milliseconds)
        val entry = ledger[reference]
            ?: return TransferOutcome.Failed(FailureReason.NOT_PROCESSED)

        if (entry.status == TransactionStatus.PENDING) {
            entry.pollsWhilePending++
            if (entry.pollsWhilePending >= 2) entry.status = TransactionStatus.SUCCESSFUL
        }
        if (entry.status != TransactionStatus.PENDING) {
            store.recordTransfer(entry.request, entry.status)
        }
        return outcomeOf(entry)
    }

    private fun outcomeOf(entry: LedgerEntry): TransferOutcome = when (entry.status) {
        TransactionStatus.SUCCESSFUL -> TransferOutcome.Successful(entry.request.reference)
        TransactionStatus.PENDING -> TransferOutcome.Pending
        TransactionStatus.FAILED -> TransferOutcome.Failed(FailureReason.REJECTED_BY_BANK)
    }
}
