package com.example.mobilebanking.domain.repository

import com.example.mobilebanking.domain.model.Bank

interface BankRepository {
    suspend fun getBanks(): List<Bank>
}
