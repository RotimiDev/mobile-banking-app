package com.akeem.mobilebanking.data.repository

import com.akeem.mobilebanking.data.source.SampleData
import com.akeem.mobilebanking.domain.model.Bank
import com.akeem.mobilebanking.domain.repository.BankRepository
import kotlinx.coroutines.delay
import javax.inject.Inject

class BankRepositoryImpl @Inject constructor() : BankRepository {
    override suspend fun getBanks(): List<Bank> {
        delay(150)
        return SampleData.banks
    }
}
