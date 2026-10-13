package com.akeem.mobilebanking.ui.history

sealed interface HistoryMessage {
    data object RefreshFailed : HistoryMessage
}
