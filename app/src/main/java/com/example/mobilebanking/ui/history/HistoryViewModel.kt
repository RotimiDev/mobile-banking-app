package com.example.mobilebanking.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebanking.domain.repository.TransactionRepository
import com.example.mobilebanking.domain.transactions.FilterTransactionsUseCase
import com.example.mobilebanking.domain.transactions.TransactionFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val filterTransactions: FilterTransactionsUseCase,
) : ViewModel() {

    private enum class LoadState { LOADING, LOADED, ERROR }

    private val loadState = MutableStateFlow(LoadState.LOADING)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TransactionFilter.ALL)
    private val isRefreshing = MutableStateFlow(false)

    private val _messages = Channel<HistoryMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.transactions, loadState, query, filter, isRefreshing,
    ) { all, load, q, f, refreshing ->
        val content = when (load) {
            LoadState.LOADING -> HistoryContent.Loading
            LoadState.ERROR -> HistoryContent.Error
            LoadState.LOADED -> {
                val visible = filterTransactions(all, q, f)
                when {
                    all.isEmpty() -> HistoryContent.Empty
                    visible.isEmpty() -> HistoryContent.NoResults
                    else -> HistoryContent.Items(visible)
                }
            }
        }
        HistoryUiState(content = content, query = q, filter = f, isRefreshing = refreshing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    init {
        load()
    }

    fun onQueryChanged(value: String) = query.update { value }

    fun onFilterSelected(value: TransactionFilter) = filter.update { value }

    fun load() {
        loadState.value = LoadState.LOADING
        viewModelScope.launch {
            loadState.value =
                if (repository.refresh().isSuccess) LoadState.LOADED else LoadState.ERROR
        }
    }

    fun refresh() {
        if (isRefreshing.value || loadState.value == LoadState.LOADING) return
        isRefreshing.value = true
        viewModelScope.launch {
            val result = repository.refresh()
            if (result.isSuccess) {
                loadState.value = LoadState.LOADED
            } else if (loadState.value == LoadState.LOADED) {
                _messages.trySend(HistoryMessage.RefreshFailed)
            } else {
                loadState.value = LoadState.ERROR
            }
            isRefreshing.value = false
        }
    }
}
