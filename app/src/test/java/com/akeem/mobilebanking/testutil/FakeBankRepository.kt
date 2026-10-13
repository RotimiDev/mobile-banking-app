package com.akeem.mobilebanking.testutil

import com.akeem.mobilebanking.domain.repository.BankRepository

class FakeBankRepository : BankRepository {
    override suspend fun getBanks() = listOf(ACCESS, GTB)
}
