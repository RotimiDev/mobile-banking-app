package com.example.mobilebanking.domain.repository

import com.example.mobilebanking.domain.model.ResolveResult

interface BeneficiaryRepository {
    suspend fun resolve(bankCode: String, accountNumber: String): ResolveResult
}
