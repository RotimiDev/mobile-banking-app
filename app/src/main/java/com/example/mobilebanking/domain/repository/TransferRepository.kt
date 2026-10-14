package com.example.mobilebanking.domain.repository

import com.example.mobilebanking.domain.model.TransferOutcome
import com.example.mobilebanking.domain.model.TransferRequest

interface TransferRepository {
    suspend fun submit(request: TransferRequest): TransferOutcome

    suspend fun checkStatus(reference: String): TransferOutcome
}
