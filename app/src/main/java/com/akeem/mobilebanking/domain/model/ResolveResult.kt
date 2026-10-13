package com.akeem.mobilebanking.domain.model

/** Result of the (simulated) account-resolution call. */
sealed interface ResolveResult {
    data class Success(val accountName: String) : ResolveResult
    data object NotFound : ResolveResult
    data object ServiceError : ResolveResult
}
