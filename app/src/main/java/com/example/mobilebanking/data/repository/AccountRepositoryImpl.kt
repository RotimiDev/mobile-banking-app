package com.example.mobilebanking.data.repository

import com.example.mobilebanking.data.datasource.BankDataSource
import com.example.mobilebanking.domain.model.Account
import com.example.mobilebanking.domain.repository.AccountRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(store: BankDataSource) :
    AccountRepository {
    override val accounts: StateFlow<List<Account>> = store.accounts
}
