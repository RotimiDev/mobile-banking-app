package com.akeem.mobilebanking.domain.repository

import com.akeem.mobilebanking.domain.model.Account
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository contracts. The UI and ViewModels only know these interfaces, so the in-memory
 * implementations in `data/` can be swapped for Retrofit-backed ones without touching them.
 */
interface AccountRepository {
    val accounts: StateFlow<List<Account>>
}
