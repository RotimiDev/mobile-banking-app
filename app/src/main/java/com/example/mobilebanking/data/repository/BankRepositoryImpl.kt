package com.example.mobilebanking.data.repository

import com.example.mobilebanking.data.datasource.SampleData
import com.example.mobilebanking.domain.repository.BankRepository
import com.example.mobilebanking.domain.model.Bank
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class BankRepositoryImpl @Inject constructor() : BankRepository {
    override suspend fun getBanks(): List<Bank> {
        delay(150.milliseconds)
        return SampleData.banks
    }
}
