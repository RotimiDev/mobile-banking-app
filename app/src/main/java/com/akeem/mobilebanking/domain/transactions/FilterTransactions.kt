package com.akeem.mobilebanking.domain.transactions

import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.model.TransactionType
import javax.inject.Inject

enum class TransactionFilter {
    ALL, CREDIT, DEBIT;

    fun matches(type: TransactionType): Boolean = when (this) {
        ALL -> true
        CREDIT -> type == TransactionType.CREDIT
        DEBIT -> type == TransactionType.DEBIT
    }
}

/**
 * Derives the displayed list from the source list. Never mutates the source:
 * search (case-insensitive, on description) and type filter are combined with AND.
 */
class FilterTransactionsUseCase @Inject constructor() {
    operator fun invoke(
        transactions: List<Transaction>,
        query: String,
        filter: TransactionFilter,
    ): List<Transaction> {
        val needle = query.trim()
        return transactions.filter { tx ->
            filter.matches(tx.type) && (needle.isEmpty() || tx.title.contains(needle, ignoreCase = true))
        }
    }
}
