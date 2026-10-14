package com.example.mobilebanking.domain.transactions

import com.example.mobilebanking.domain.model.Transaction
import javax.inject.Inject

class FilterTransactionsUseCase @Inject constructor() {
    operator fun invoke(
        transactions: List<Transaction>,
        query: String,
        filter: TransactionFilter,
    ): List<Transaction> {
        val needle = query.trim()
        return transactions.filter { tx ->
            filter.matches(tx.type) && (needle.isEmpty() || tx.title.contains(
                needle,
                ignoreCase = true
            ))
        }
    }
}
