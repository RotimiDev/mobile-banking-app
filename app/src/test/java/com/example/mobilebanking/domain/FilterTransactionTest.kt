package com.example.mobilebanking.domain

import com.example.mobilebanking.domain.model.Transaction
import com.example.mobilebanking.domain.model.TransactionStatus
import com.example.mobilebanking.domain.model.TransactionType
import com.example.mobilebanking.domain.transactions.FilterTransactionsUseCase
import com.example.mobilebanking.domain.transactions.TransactionFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterTransactionsTest {

    private val filter = FilterTransactionsUseCase()

    private fun tx(id: String, title: String, type: TransactionType) =
        Transaction(id, "REF$id", title, 100_000L, "12 Oct 2026, 09:00 AM", type, TransactionStatus.SUCCESSFUL)

    private val all = listOf(
        tx("1", "Transfer to John Doe", TransactionType.DEBIT),
        tx("2", "Salary Payment", TransactionType.CREDIT),
        tx("3", "Transfer from Chinedu Okafor", TransactionType.CREDIT),
        tx("4", "Electricity Bill", TransactionType.DEBIT),
        tx("5", "transfer to jane smith", TransactionType.DEBIT),
    )

    private fun ids(list: List<Transaction>) = list.map { it.id }

    @Test fun `ALL with an empty query returns everything`() {
        assertEquals(listOf("1", "2", "3", "4", "5"), ids(filter(all, "", TransactionFilter.ALL)))
    }

    @Test fun `credit and debit filters select by type`() {
        assertEquals(listOf("2", "3"), ids(filter(all, "", TransactionFilter.CREDIT)))
        assertEquals(listOf("1", "4", "5"), ids(filter(all, "", TransactionFilter.DEBIT)))
    }

    @Test fun `search is case-insensitive and matches the description`() {
        assertEquals(listOf("1", "3", "5"), ids(filter(all, "TRANSFER", TransactionFilter.ALL)))
        assertEquals(listOf("2"), ids(filter(all, "salary", TransactionFilter.ALL)))
    }

    @Test fun `search and filter combine, as in the Transfer + Debit example`() {
        assertEquals(listOf("1", "5"), ids(filter(all, "Transfer", TransactionFilter.DEBIT)))
        assertEquals(listOf("3"), ids(filter(all, "Transfer", TransactionFilter.CREDIT)))
    }

    @Test fun `surrounding whitespace in the query is ignored`() {
        assertEquals(listOf("2"), ids(filter(all, "  Salary  ", TransactionFilter.ALL)))
    }

    @Test
    fun `no match yields an empty list`() {
        assertEquals(emptyList<Transaction>(), filter(all, "zzz", TransactionFilter.ALL))
    }

    @Test fun `filtering never modifies the source list`() {
        val snapshot = all.toList()
        filter(all, "Transfer", TransactionFilter.DEBIT)
        assertEquals(snapshot, all)
    }
}
