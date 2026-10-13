package com.akeem.mobilebanking.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: TransactionRepository,
) : ViewModel() {

    private val transactionId: String = checkNotNull(savedStateHandle["transactionId"])

    /** Observes the shared repository so a status change (e.g. pending -> successful) is reflected live. */
    val transaction: StateFlow<Transaction?> = repository.transactions
        .map { list -> list.firstOrNull { it.id == transactionId } }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            repository.transactions.value.firstOrNull { it.id == transactionId },
        )
}
