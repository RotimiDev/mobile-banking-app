package com.akeem.mobilebanking.testutil

import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.model.Bank
import com.akeem.mobilebanking.domain.model.ResolveResult
import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.model.TransferOutcome
import com.akeem.mobilebanking.domain.model.TransferRequest
import com.akeem.mobilebanking.domain.repository.AccountRepository
import com.akeem.mobilebanking.domain.repository.BankRepository
import com.akeem.mobilebanking.domain.repository.BeneficiaryRepository
import com.akeem.mobilebanking.domain.repository.ReferenceGenerator
import com.akeem.mobilebanking.domain.repository.TransactionRepository
import com.akeem.mobilebanking.domain.repository.TransferRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

val SAVINGS = Account("acc_savings", "Savings Account", "4821", balanceKobo = 15_000_000L) // ₦150,000.00
val SALARY = Account("acc_salary", "Salary Account", "1096", balanceKobo = 842_050L)       // ₦8,420.50
val ACCESS = Bank("044", "Access Bank")
val GTB = Bank("058", "Guaranty Trust Bank")

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeAccountRepository(initial: List<Account> = listOf(SAVINGS, SALARY)) : AccountRepository {
    override val accounts = MutableStateFlow(initial)
}

class FakeBankRepository : BankRepository {
    override suspend fun getBanks() = listOf(ACCESS, GTB)
}

/** Each resolve() call is answered by the test, so tests control the ORDER in which answers arrive. */
class FakeBeneficiaryRepository : BeneficiaryRepository {
    class Call(val bankCode: String, val accountNumber: String) {
        val response = CompletableDeferred<ResolveResult>()
    }

    val calls = mutableListOf<Call>()

    override suspend fun resolve(bankCode: String, accountNumber: String): ResolveResult {
        val call = Call(bankCode, accountNumber)
        calls += call
        return call.response.await()
    }
}

class FakeTransferRepository : TransferRepository {
    var submitOutcome: TransferOutcome = TransferOutcome.Successful("ref")
    var statusOutcome: TransferOutcome = TransferOutcome.Successful("ref")
    var submitGate: CompletableDeferred<Unit>? = null
    var submitThrows: Throwable? = null

    val submitted = mutableListOf<TransferRequest>()
    val statusChecks = mutableListOf<String>()

    override suspend fun submit(request: TransferRequest): TransferOutcome {
        submitted += request
        submitGate?.await()
        submitThrows?.let { throw it }
        return submitOutcome
    }

    override suspend fun checkStatus(reference: String): TransferOutcome {
        statusChecks += reference
        return statusOutcome
    }
}

class FakeTransactionRepository(initial: List<Transaction> = emptyList()) : TransactionRepository {
    override val transactions: MutableStateFlow<List<Transaction>> = MutableStateFlow(initial)
    var refreshResult: Result<Unit> = Result.success(Unit)
    var refreshCalls = 0

    override suspend fun refresh(): Result<Unit> {
        refreshCalls++
        return refreshResult
    }
}

class SequentialReferenceGenerator : ReferenceGenerator {
    private var n = 0
    override fun next() = "REF${++n}"
}
