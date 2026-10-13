package com.akeem.mobilebanking.data.repository

import com.akeem.mobilebanking.data.source.SampleData
import com.akeem.mobilebanking.domain.model.ResolveResult
import com.akeem.mobilebanking.domain.model.TransferPolicy
import com.akeem.mobilebanking.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Simulated NIP name enquiry. Behaviour is deterministic so each state can be demonstrated:
 *  - account numbers starting with "000"  -> account not found
 *  - account numbers ending with "99"     -> service error (retryable)
 *  - anything else                        -> success; the name depends on bank + digits
 * Latency grows with the last digit (0.6s … 2.85s) so an older request can legitimately be
 * slower than a newer one, which is exactly the race the ViewModel must survive.
 */
class BeneficiaryRepositoryImpl @Inject constructor() : BeneficiaryRepository {
    override suspend fun resolve(bankCode: String, accountNumber: String): ResolveResult {
        val lastDigit = accountNumber.lastOrNull()?.digitToIntOrNull() ?: 0
        delay(600L + lastDigit * 250L)
        return when {
            accountNumber.length != TransferPolicy.ACCOUNT_NUMBER_LENGTH || !accountNumber.all { it.isDigit() } ->
                ResolveResult.NotFound
            accountNumber.startsWith("000") -> ResolveResult.NotFound
            accountNumber.endsWith("99") -> ResolveResult.ServiceError
            else -> {
                val index = (accountNumber.sumOf { it.digitToInt() } + (bankCode.toIntOrNull() ?: 0)) %
                    SampleData.beneficiaryNames.size
                ResolveResult.Success(SampleData.beneficiaryNames[index])
            }
        }
    }
}
