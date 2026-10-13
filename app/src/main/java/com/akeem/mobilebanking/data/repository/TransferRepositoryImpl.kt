package com.akeem.mobilebanking.data.repository

import com.akeem.mobilebanking.data.demo.DemoSettings
import com.akeem.mobilebanking.data.demo.DemoTransferResult
import com.akeem.mobilebanking.data.source.BankDataSource
import com.akeem.mobilebanking.domain.model.FailureReason
import com.akeem.mobilebanking.domain.model.TransactionStatus
import com.akeem.mobilebanking.domain.model.TransferOutcome
import com.akeem.mobilebanking.domain.model.TransferRequest
import com.akeem.mobilebanking.domain.repository.TransferRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simulated transfer backend. It keeps a ledger keyed by the request reference, which makes
 * [submit] idempotent (a replay returns the original result instead of moving money twice) and
 * lets [checkStatus] answer for the ORIGINAL transaction.
 *
 *  SUCCESS  -> debits, records SUCCESSFUL
 *  FAILED   -> records FAILED, no debit
 *  PENDING  -> records PENDING (funds on hold); settles to SUCCESSFUL on the 2nd status check
 *  TIMEOUT  -> the backend DID process it, but the response is "lost"; the client receives Unknown
 *              and only learns the truth by checking status (history updates at that point)
 */
@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val store: BankDataSource,
    private val demoSettings: DemoSettings,
) : TransferRepository {

    private class LedgerEntry(
        val request: TransferRequest,
        var status: TransactionStatus,
        var pollsWhilePending: Int = 0,
    )

    private val ledger = mutableMapOf<String, LedgerEntry>()

    override suspend fun submit(request: TransferRequest): TransferOutcome {
        ledger[request.reference]?.let { return outcomeOf(it) } // idempotent replay

        delay(1_800)
        if (!store.hasSufficientFunds(request.debitAccountId, request.totalDebitKobo)) {
            return TransferOutcome.Failed(FailureReason.INSUFFICIENT_FUNDS)
        }

        return when (demoSettings.transfer) {
            DemoTransferResult.SUCCESS -> {
                ledger[request.reference] = LedgerEntry(request, TransactionStatus.SUCCESSFUL)
                store.recordTransfer(request, TransactionStatus.SUCCESSFUL)
                TransferOutcome.Successful(request.reference)
            }
            DemoTransferResult.FAILED -> {
                ledger[request.reference] = LedgerEntry(request, TransactionStatus.FAILED)
                store.recordTransfer(request, TransactionStatus.FAILED)
                TransferOutcome.Failed(FailureReason.REJECTED_BY_BANK)
            }
            DemoTransferResult.PENDING -> {
                ledger[request.reference] = LedgerEntry(request, TransactionStatus.PENDING)
                store.recordTransfer(request, TransactionStatus.PENDING)
                TransferOutcome.Pending
            }
            DemoTransferResult.TIMEOUT -> {
                ledger[request.reference] = LedgerEntry(request, TransactionStatus.SUCCESSFUL)
                delay(4_000) // the client waits, then gives up
                TransferOutcome.Unknown
            }
        }
    }

    override suspend fun checkStatus(reference: String): TransferOutcome {
        delay(1_200)
        val entry = ledger[reference]
            ?: return TransferOutcome.Failed(FailureReason.NOT_PROCESSED)

        if (entry.status == TransactionStatus.PENDING) {
            entry.pollsWhilePending++
            if (entry.pollsWhilePending >= 2) entry.status = TransactionStatus.SUCCESSFUL
        }
        if (entry.status != TransactionStatus.PENDING) {
            store.recordTransfer(entry.request, entry.status) // reconcile history (idempotent)
        }
        return outcomeOf(entry)
    }

    private fun outcomeOf(entry: LedgerEntry): TransferOutcome = when (entry.status) {
        TransactionStatus.SUCCESSFUL -> TransferOutcome.Successful(entry.request.reference)
        TransactionStatus.PENDING -> TransferOutcome.Pending
        TransactionStatus.FAILED -> TransferOutcome.Failed(FailureReason.REJECTED_BY_BANK)
    }
}
