package com.akeem.mobilebanking.ui.history

import com.akeem.mobilebanking.domain.model.Transaction

sealed interface HistoryContent {
    data object Loading : HistoryContent
    data object Error : HistoryContent
    /** The customer has no transactions at all. */
    data object Empty : HistoryContent
    /** There are transactions, but none match the current search + filter. */
    data object NoResults : HistoryContent
    data class Items(val transactions: List<Transaction>) : HistoryContent
}
