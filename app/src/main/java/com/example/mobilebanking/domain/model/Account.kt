package com.example.mobilebanking.domain.model

data class Account(
    val id: String,
    val productName: String,
    val last4: String,
    val balanceKobo: Long,
)
