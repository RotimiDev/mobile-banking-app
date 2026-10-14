package com.example.mobilebanking.domain.model

sealed interface ResolveResult {
    data class Success(val accountName: String) : ResolveResult
    data object NotFound : ResolveResult
    data object ServiceError : ResolveResult
}
