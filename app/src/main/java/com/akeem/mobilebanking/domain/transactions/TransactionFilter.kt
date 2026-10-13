package com.akeem.mobilebanking.domain.transactions

import com.akeem.mobilebanking.domain.model.TransactionType

enum class TransactionFilter {
    ALL, CREDIT, DEBIT;

    fun matches(type: TransactionType): Boolean = when (this) {
        ALL -> true
        CREDIT -> type == TransactionType.CREDIT
        DEBIT -> type == TransactionType.DEBIT
    }
}
