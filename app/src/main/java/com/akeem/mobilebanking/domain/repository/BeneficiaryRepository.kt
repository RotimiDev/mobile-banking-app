package com.akeem.mobilebanking.domain.repository

import com.akeem.mobilebanking.domain.model.ResolveResult

interface BeneficiaryRepository {
    /** Must never throw for expected failures; cancellation is propagated normally. */
    suspend fun resolve(bankCode: String, accountNumber: String): ResolveResult
}
