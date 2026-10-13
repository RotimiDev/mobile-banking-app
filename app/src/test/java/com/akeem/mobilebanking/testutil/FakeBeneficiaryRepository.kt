package com.akeem.mobilebanking.testutil

import com.akeem.mobilebanking.domain.model.ResolveResult
import com.akeem.mobilebanking.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Each resolve() call is answered by the test, so tests control the ORDER in which answers arrive. */
class FakeBeneficiaryRepository : BeneficiaryRepository {
    class Call(val bankCode: String, val accountNumber: String) {
        val response = CompletableDeferred<ResolveResult>()
    }

    val calls = mutableListOf<Call>()

    /** Simulates a data source that does NOT honour cancellation, to prove the ViewModel's own guards. */
    var ignoreCancellation = false

    override suspend fun resolve(bankCode: String, accountNumber: String): ResolveResult {
        val call = Call(bankCode, accountNumber)
        calls += call
        return if (ignoreCancellation) withContext(NonCancellable) { call.response.await() } else call.response.await()
    }
}
