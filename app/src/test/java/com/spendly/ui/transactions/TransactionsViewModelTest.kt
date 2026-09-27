package com.spendly.ui.transactions

import com.spendly.domain.model.Category
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val transactions = FakeTransactions()
    private val categories = FakeCategories()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun emptyHistoryAndReactiveChanges() = runTest {
        val vm = TransactionsViewModel(transactions, categories)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.rows.isEmpty())

        val first = transaction(1, "2026-09-01T10:00:00Z", 1)
        val second = transaction(2, "2026-09-02T10:00:00Z", 2)
        transactions.items.value = listOf(first, second)
        advanceUntilIdle()
        assertEquals(listOf(2L, 1L), vm.uiState.value.rows.map { it.transaction.id })
        assertEquals(listOf("Travel", "Groceries"), vm.uiState.value.rows.map { it.categoryName })

        transactions.items.value = listOf(first.copy(merchant = "Edited"))
        advanceUntilIdle()
        assertEquals("Edited", vm.uiState.value.rows.single().transaction.merchant)
        transactions.items.value = emptyList()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.rows.isEmpty())
        assertEquals(0, transactions.getByIdCalls)
    }

    @Test fun categoryChangesUpdateExistingRowsWithoutPerRowLookup() = runTest {
        transactions.items.value = listOf(transaction(1, "2026-09-01T10:00:00Z", 1))
        val vm = TransactionsViewModel(transactions, categories)
        advanceUntilIdle()
        categories.items.value = listOf(Category(1, "Food", true, true))
        advanceUntilIdle()
        assertEquals("Food", vm.uiState.value.rows.single().categoryName)
        assertEquals(0, transactions.getByIdCalls)
    }

    @Test fun amountDisplayUsesTypePrefixAndMinorUnits() {
        val base = transaction(1, "2026-09-01T10:00:00Z", 1)
        assertEquals("-€12.40", MoneyDisplayFormatter.format(base, Locale.US))
        assertEquals("+€12.40", MoneyDisplayFormatter.format(base.copy(type = TransactionType.INCOME), Locale.US))
        assertEquals("€12.40", MoneyDisplayFormatter.format(base.copy(type = TransactionType.TRANSFER), Locale.US))
    }

    private fun transaction(id: Long, at: String, category: Long) = Transaction(
        id, Money(1240, "EUR"), TransactionType.EXPENSE, "Shop", null, category,
        Instant.parse(at), TransactionSource.MANUAL, ImportStatus.CONFIRMED,
        null, null, null, Instant.parse(at), Instant.parse(at),
    )

    private class FakeTransactions : TransactionRepository {
        val items = MutableStateFlow<List<Transaction>>(emptyList())
        var getByIdCalls = 0
        override suspend fun insert(transaction: Transaction): Long = error("Unused")
        override suspend fun update(transaction: Transaction): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): Transaction? { getByIdCalls++; return null }
        override fun observeAll(): Flow<List<Transaction>> = items
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> = error("Unused")
    }

    private class FakeCategories : CategoryRepository {
        val items = MutableStateFlow(listOf(Category(1, "Groceries", true, true), Category(2, "Travel", true, true)))
        override suspend fun insert(category: Category): Long = error("Unused")
        override suspend fun update(category: Category): Int = error("Unused")
        override suspend fun getById(id: Long): Category? = error("Unused")
        override fun observeActive(): Flow<List<Category>> = items
        override fun observeAll(): Flow<List<Category>> = items
    }
}
