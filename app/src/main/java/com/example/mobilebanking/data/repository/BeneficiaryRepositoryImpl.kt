package com.example.mobilebanking.data.repository

import com.example.mobilebanking.data.datasource.SampleData
import com.example.mobilebanking.domain.model.ResolveResult
import com.example.mobilebanking.domain.model.TransferPolicy
import com.example.mobilebanking.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class BeneficiaryRepositoryImpl @Inject constructor() : BeneficiaryRepository {
    override suspend fun resolve(bankCode: String, accountNumber: String): ResolveResult {
        val lastDigit = accountNumber.lastOrNull()?.digitToIntOrNull() ?: 0
        delay((600L + lastDigit * 250L).milliseconds)
        return when {
            accountNumber.length != TransferPolicy.ACCOUNT_NUMBER_LENGTH || !accountNumber.all { it.isDigit() } ->
                ResolveResult.NotFound

            accountNumber.startsWith("000") -> ResolveResult.NotFound
            accountNumber.endsWith("99") -> ResolveResult.ServiceError
            else -> {
                val index =
                    (accountNumber.sumOf { it.digitToInt() } + (bankCode.toIntOrNull() ?: 0)) %
                            SampleData.beneficiaryNames.size
                ResolveResult.Success(SampleData.beneficiaryNames[index])
            }
        }
    }
}
