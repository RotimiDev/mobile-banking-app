package com.example.mobilebanking.ui.history

import com.example.mobilebanking.domain.model.Transaction
import com.example.mobilebanking.domain.transactions.TransactionFilter

sealed interface HistoryContent {
    data object Loading : HistoryContent
    data object Error : HistoryContent
    data object Empty : HistoryContent
    data object NoResults : HistoryContent
    data class Items(val transactions: List<Transaction>) : HistoryContent
}

data class HistoryUiState(
    val content: HistoryContent = HistoryContent.Loading,
    val query: String = "",
    val filter: TransactionFilter = TransactionFilter.ALL,
    val isRefreshing: Boolean = false,
)

sealed interface HistoryMessage {
    data object RefreshFailed : HistoryMessage
}
