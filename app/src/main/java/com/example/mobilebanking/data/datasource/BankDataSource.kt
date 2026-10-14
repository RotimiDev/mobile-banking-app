package com.example.mobilebanking.data.datasource

import com.example.mobilebanking.domain.model.Account
import com.example.mobilebanking.domain.model.Transaction
import com.example.mobilebanking.domain.model.TransactionStatus
import com.example.mobilebanking.domain.model.TransactionType
import com.example.mobilebanking.domain.model.TransferRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankDataSource @Inject constructor(private val clock: Clock) {

    private val _accounts = MutableStateFlow(SampleData.accounts)
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

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

    fun hasSufficientFunds(accountId: String, totalKobo: Long): Boolean =
        _accounts.value.firstOrNull { it.id == accountId }?.let { it.balanceKobo >= totalKobo } ?: false

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
