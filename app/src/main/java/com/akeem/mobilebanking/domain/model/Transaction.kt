package com.akeem.mobilebanking.domain.model

data class Transaction(
    val id: String,
    val reference: String,
    val title: String,
    val amount: Long, // kobo
    val date: String,
    val type: TransactionType,
    val status: TransactionStatus,
)

enum class TransactionType { CREDIT, DEBIT }

enum class TransactionStatus { SUCCESSFUL, PENDING, FAILED }
