package com.example.mobilebanking.testutil

import com.example.mobilebanking.domain.model.Account
import com.example.mobilebanking.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccountRepository(initial: List<Account> = listOf(SAVINGS, SALARY)) : AccountRepository {
    override val accounts = MutableStateFlow(initial)
}
