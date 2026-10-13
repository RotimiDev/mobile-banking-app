package com.akeem.mobilebanking.domain.model

/** A customer account. All monetary values are integer kobo (1 naira = 100 kobo). */
data class Account(
    val id: String,
    val productName: String,
    val last4: String,
    val balanceKobo: Long,
)
