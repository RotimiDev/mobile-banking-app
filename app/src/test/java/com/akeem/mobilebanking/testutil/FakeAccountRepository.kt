package com.akeem.mobilebanking.testutil

import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccountRepository(initial: List<Account> = listOf(SAVINGS, SALARY)) : AccountRepository {
    override val accounts = MutableStateFlow(initial)
}
