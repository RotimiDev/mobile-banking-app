package com.akeem.mobilebanking.data.repository

import com.akeem.mobilebanking.data.source.BankDataSource
import com.akeem.mobilebanking.domain.model.Account
import com.akeem.mobilebanking.domain.repository.AccountRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(store: BankDataSource) : AccountRepository {
    override val accounts: StateFlow<List<Account>> = store.accounts
}
