package com.example.mobilebanking.testutil

import com.example.mobilebanking.domain.model.ResolveResult
import com.example.mobilebanking.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class FakeBeneficiaryRepository : BeneficiaryRepository {
    class Call(val bankCode: String, val accountNumber: String) {
        val response = CompletableDeferred<ResolveResult>()
    }

    val calls = mutableListOf<Call>()
    var ignoreCancellation = false

    override suspend fun resolve(bankCode: String, accountNumber: String): ResolveResult {
        val call = Call(bankCode, accountNumber)
        calls += call
        return if (ignoreCancellation) withContext(NonCancellable) { call.response.await() } else call.response.await()
    }
}
