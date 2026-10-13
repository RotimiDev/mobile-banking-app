package com.akeem.mobilebanking.data

import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.model.TransactionStatus
import com.akeem.mobilebanking.domain.model.TransactionType
import com.akeem.mobilebanking.domain.model.TransferRequest
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The simulated "bank": one shared in-memory source of truth for balances and transactions.
 * The account / transaction / transfer repositories are thin views over this store, which is what
 * lets a completed transfer show up in the history immediately without restarting the app.
 */
@Singleton
class InMemoryBankingStore @Inject constructor(private val clock: Clock) {

    private val _accounts = MutableStateFlow(SeedData.accounts)
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    /** Transactions created in this session (newest first) and the "server-side" history. */
    private var created: List<Transaction> = emptyList()
    private var remote: List<Transaction> = emptyList()

    private val debitedReferences = mutableSetOf<String>()

    private val dateFormatter = DateTimeFormatter
        .ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        .withZone(ZoneId.of("Africa/Lagos"))

    @Synchronized
    fun setRemoteHistory(list: List<Transaction>) {
        remote = list
        publish()
    }

    /** Server-side balance check: client-side checks are never authoritative. */
    fun hasSufficientFunds(accountId: String, totalKobo: Long): Boolean =
        _accounts.value.firstOrNull { it.id == accountId }?.let { it.balanceKobo >= totalKobo } ?: false

    /**
     * Records (or updates) the history entry for a transfer and debits the account exactly once per
     * reference, no matter how many times it is called (idempotent).
     */
    @Synchronized
    fun recordTransfer(request: TransferRequest, status: TransactionStatus) {
        val id = "txn_${request.reference}"
        val existingDate = created.firstOrNull { it.id == id }?.date
        val transaction = Transaction(
            id = id,
            reference = request.reference,
            title = "Transfer to ${request.beneficiaryName.toTitleCase()}",
            amount = request.amountKobo,
            date = existingDate ?: dateFormatter.format(clock.instant()),
            type = TransactionType.DEBIT,
            status = status,
        )
        created = if (created.any { it.id == id }) {
            created.map { if (it.id == id) transaction else it }
        } else {
            listOf(transaction) + created
        }
        if (status != TransactionStatus.FAILED && debitedReferences.add(request.reference)) {
            _accounts.value = _accounts.value.map {
                if (it.id == request.debitAccountId) it.copy(balanceKobo = it.balanceKobo - request.totalDebitKobo) else it
            }
        }
        publish()
    }

    private fun publish() {
        _transactions.value = created + remote
    }

    private fun String.toTitleCase() = lowercase().split(" ")
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
}
