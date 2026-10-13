package com.akeem.mobilebanking.ui.history

import com.akeem.mobilebanking.domain.transactions.TransactionFilter

data class HistoryUiState(
    val content: HistoryContent = HistoryContent.Loading,
    val query: String = "",
    val filter: TransactionFilter = TransactionFilter.ALL,
    val isRefreshing: Boolean = false,
)
