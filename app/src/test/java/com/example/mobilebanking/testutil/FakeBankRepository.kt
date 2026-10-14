package com.example.mobilebanking.testutil

import com.example.mobilebanking.domain.repository.BankRepository

class FakeBankRepository : BankRepository {
    override suspend fun getBanks() = listOf(ACCESS, GTB)
}
