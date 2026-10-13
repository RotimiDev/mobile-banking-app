package com.akeem.mobilebanking.ui

import com.akeem.mobilebanking.domain.model.Transaction
import com.akeem.mobilebanking.domain.model.TransactionStatus
import com.akeem.mobilebanking.domain.model.TransactionType
import com.akeem.mobilebanking.domain.transactions.FilterTransactionsUseCase
import com.akeem.mobilebanking.domain.transactions.TransactionFilter
import com.akeem.mobilebanking.testutil.FakeTransactionRepository
import com.akeem.mobilebanking.testutil.MainDispatcherRule
import com.akeem.mobilebanking.ui.history.HistoryContent
import com.akeem.mobilebanking.ui.history.HistoryMessage
import com.akeem.mobilebanking.ui.history.HistoryViewModel
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule val mainRule = MainDispatcherRule()

    private fun tx(id: String, title: String, type: TransactionType, status: TransactionStatus = TransactionStatus.SUCCESSFUL) =
        Transaction(id, "REF$id", title, 100_000L, "12 Oct 2026, 09:00 AM", type, status)

    private val seed = listOf(
        tx("1", "Transfer to John Doe", TransactionType.DEBIT),
        tx("2", "Salary Payment", TransactionType.CREDIT),
        tx("3", "Transfer from Chinedu Okafor", TransactionType.CREDIT),
    )

    private val repository = FakeTransactionRepository(seed)

    private fun TestScope.newViewModel(): HistoryViewModel {
        val vm = HistoryViewModel(repository, FilterTransactionsUseCase())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        advanceUntilIdle()
        return vm
    }

    private fun HistoryViewModel.shownIds() =
        (uiState.value.content as HistoryContent.Items).transactions.map { it.id }

    @Test fun `shows loading and then the transactions`() = runTest {
        val vm = HistoryViewModel(repository, FilterTransactionsUseCase())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        assertEquals(HistoryContent.Loading, vm.uiState.value.content)
        advanceUntilIdle()
        assertEquals(listOf("1", "2", "3"), vm.shownIds())
    }

    @Test fun `search and type filter work together and never lose the source data`() = runTest {
        val vm = newViewModel()

        vm.onFilterSelected(TransactionFilter.DEBIT)
        vm.onQueryChanged("transfer")
        advanceUntilIdle()
        assertEquals(listOf("1"), vm.shownIds())

        vm.onFilterSelected(TransactionFilter.CREDIT)
        advanceUntilIdle()
        assertEquals(listOf("3"), vm.shownIds())

        vm.onFilterSelected(TransactionFilter.ALL)
        vm.onQueryChanged("")
        advanceUntilIdle()
        assertEquals(listOf("1", "2", "3"), vm.shownIds())
        assertEquals(3, repository.transactions.value.size)
    }

    @Test fun `no search results is distinct from an empty history`() = runTest {
        val vm = newViewModel()
        vm.onQueryChanged("does not exist")
        advanceUntilIdle()
        assertEquals(HistoryContent.NoResults, vm.uiState.value.content)

        repository.transactions.value = emptyList()
        advanceUntilIdle()
        assertEquals(HistoryContent.Empty, vm.uiState.value.content)
    }

    @Test fun `a failed load shows the error state and retry recovers`() = runTest {
        repository.refreshResult = Result.failure(IOException("boom"))
        val vm = newViewModel()
        assertEquals(HistoryContent.Error, vm.uiState.value.content)

        repository.refreshResult = Result.success(Unit)
        vm.load()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.content is HistoryContent.Items)
        assertEquals(2, repository.refreshCalls)
    }

    @Test fun `a transaction added to the repository appears immediately without a reload`() = runTest {
        val vm = newViewModel()
        repository.transactions.value = listOf(tx("new", "Transfer to Amina Bello", TransactionType.DEBIT)) + seed
        advanceUntilIdle()
        assertEquals(listOf("new", "1", "2", "3"), vm.shownIds())
        assertEquals(1, repository.refreshCalls) // no extra refresh needed
    }

    @Test fun `a failed pull-to-refresh keeps the list and emits a message`() = runTest {
        val vm = newViewModel()
        val messages = mutableListOf<HistoryMessage>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.messages.collect { messages += it } }

        repository.refreshResult = Result.failure(IOException("offline"))
        vm.refresh()
        advanceUntilIdle()

        assertEquals(listOf<HistoryMessage>(HistoryMessage.RefreshFailed), messages)
        assertEquals(listOf("1", "2", "3"), vm.shownIds())
        assertEquals(false, vm.uiState.value.isRefreshing)
    }
}
