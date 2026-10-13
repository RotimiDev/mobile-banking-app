package com.akeem.mobilebanking.testutil

import com.akeem.mobilebanking.domain.model.TransferOutcome
import com.akeem.mobilebanking.domain.model.TransferRequest
import com.akeem.mobilebanking.domain.repository.TransferRepository
import kotlinx.coroutines.CompletableDeferred

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
