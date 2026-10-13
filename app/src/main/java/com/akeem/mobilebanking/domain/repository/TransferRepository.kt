package com.akeem.mobilebanking.domain.repository

import com.akeem.mobilebanking.domain.model.TransferOutcome
import com.akeem.mobilebanking.domain.model.TransferRequest

interface TransferRepository {
    /**
     * Submits a transfer. [TransferRequest.reference] is the idempotency key: submitting the same
     * reference again must never move money twice.
     */
    suspend fun submit(request: TransferRequest): TransferOutcome

    /** Asks the backend for the status of an earlier request, by its original reference. */
    suspend fun checkStatus(reference: String): TransferOutcome
}
