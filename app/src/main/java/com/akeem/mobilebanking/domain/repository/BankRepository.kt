package com.akeem.mobilebanking.domain.repository

import com.akeem.mobilebanking.domain.model.Bank

interface BankRepository {
    suspend fun getBanks(): List<Bank>
}
