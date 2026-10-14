package com.example.mobilebanking.domain.repository

import com.example.mobilebanking.domain.model.Account
import kotlinx.coroutines.flow.StateFlow

interface AccountRepository {
    val accounts: StateFlow<List<Account>>
}
