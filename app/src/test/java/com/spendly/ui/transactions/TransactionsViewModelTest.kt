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
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
    private val clock = Clock.fixed(Instant.parse("2026-09-20T10:00:00Z"), ZoneId.of("Europe/Athens"))

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun emptyHistoryAndReactiveChanges() = runTest {
        val vm = TransactionsViewModel(transactions, categories, clock)
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
        val vm = TransactionsViewModel(transactions, categories, clock)
        advanceUntilIdle()
        categories.items.value = listOf(Category(1, "Food", true, true))
        advanceUntilIdle()
        assertEquals("Food", vm.uiState.value.rows.single().categoryName)
        assertEquals(0, transactions.getByIdCalls)
    }

    @Test fun confirmedHistoryExcludesReviewAndIgnoredWhileCountTracksWalletReview() = runTest {
        val manual = transaction(1, "2026-09-01T10:00:00Z", 1)
        val wallet = transaction(2, "2026-09-02T10:00:00Z", 1).copy(source = TransactionSource.GOOGLE_WALLET)
        transactions.items.value = listOf(
            manual, wallet,
            wallet.copy(id = 3, importStatus = ImportStatus.NEEDS_REVIEW),
            wallet.copy(id = 4, importStatus = ImportStatus.IGNORED),
            manual.copy(id = 5, importStatus = ImportStatus.NEEDS_REVIEW),
        )
        val vm = TransactionsViewModel(transactions, categories, clock)
        advanceUntilIdle()
        assertEquals(listOf(2L, 1L), vm.uiState.value.rows.map { it.transaction.id })
        assertEquals(1, vm.uiState.value.reviewCount)

        transactions.items.value = transactions.items.value.map {
            if (it.id == 3L) it.copy(importStatus = ImportStatus.CONFIRMED) else it
        }
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.reviewCount)
        assertEquals(listOf(3L, 2L, 1L), vm.uiState.value.rows.map { it.transaction.id })
    }

    @Test fun amountDisplayUsesTypePrefixAndMinorUnits() {
        val base = transaction(1, "2026-09-01T10:00:00Z", 1)
        assertEquals("-€12.40", MoneyDisplayFormatter.format(base, Locale.US))
        assertEquals("+€12.40", MoneyDisplayFormatter.format(base.copy(type = TransactionType.INCOME), Locale.US))
        assertEquals("€12.40", MoneyDisplayFormatter.format(base.copy(type = TransactionType.TRANSFER), Locale.US))
    }

    @Test fun searchFiltersAndClearingRemainReactiveWithoutNewMonthQueries() = runTest {
        val wallet = transaction(1, "2026-09-01T10:00:00Z", 1)
            .copy(merchant = "WOLT", source = TransactionSource.GOOGLE_WALLET)
        val manual = transaction(2, "2026-09-02T10:00:00Z", 1).copy(merchant = "Wolt")
        val other = transaction(3, "2026-09-03T10:00:00Z", 2).copy(merchant = "Shop")
        transactions.items.value = listOf(wallet, manual, other)
        val vm = TransactionsViewModel(transactions, categories, clock)
        advanceUntilIdle()
        val rangeCalls = transactions.ranges.size

        vm.setSearchQuery("  wOlT  ")
        vm.setCategoryFilter(CategoryFilter.Specific(1))
        vm.setSourceFilter(TransactionSource.GOOGLE_WALLET)
        vm.setTypeFilter(TransactionType.EXPENSE)
        advanceUntilIdle()
        assertEquals(listOf(1L), vm.uiState.value.rows.map { it.transaction.id })
        assertTrue(vm.uiState.value.filters.hasActiveFilters)
        assertEquals(rangeCalls, transactions.ranges.size)

        transactions.items.value = listOf(wallet.copy(merchant = "Other"), manual, other)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.rows.isEmpty())
        vm.clearFilters()
        advanceUntilIdle()
        assertEquals(listOf(3L, 2L, 1L), vm.uiState.value.rows.map { it.transaction.id })
        assertFalse(vm.uiState.value.filters.hasActiveFilters)
        assertEquals(YearMonth.of(2026, 9), vm.uiState.value.selectedMonth)
    }

    @Test fun historicalInactiveCategoryAndUncategorizedRemainFilterable() = runTest {
        categories.items.value = listOf(Category(1, "Old category", true, false))
        transactions.items.value = listOf(
            transaction(1, "2026-09-01T10:00:00Z", 1),
            transaction(2, "2026-09-02T10:00:00Z", 1).copy(categoryId = null),
        )
        val vm = TransactionsViewModel(transactions, categories, clock)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.availableCategories.any { it.id == 1L && it.name == "Old category" })
        vm.setCategoryFilter(CategoryFilter.Specific(1))
        advanceUntilIdle()
        assertEquals(listOf(1L), vm.uiState.value.rows.map { it.transaction.id })
        categories.items.value = listOf(Category(1, "Renamed", true, false))
        advanceUntilIdle()
        assertEquals("Renamed", vm.uiState.value.rows.single().categoryName)
        assertEquals("Renamed", vm.uiState.value.availableCategories.single().name)
        vm.setCategoryFilter(CategoryFilter.Uncategorized)
        advanceUntilIdle()
        assertEquals(listOf(2L), vm.uiState.value.rows.map { it.transaction.id })
    }

    @Test fun monthNavigationUsesLocalRangeAndStopsAtCurrentMonth() = runTest {
        transactions.items.value = listOf(
            transaction(1, "2026-09-01T10:00:00Z", 1),
            transaction(2, "2026-08-20T10:00:00Z", 1),
        )
        val vm = TransactionsViewModel(transactions, categories, clock)
        advanceUntilIdle()
        assertFalse(vm.canGoNext())
        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 9), vm.uiState.value.selectedMonth)
        vm.previousMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 8), vm.uiState.value.selectedMonth)
        assertEquals(listOf(2L), vm.uiState.value.rows.map { it.transaction.id })
        assertEquals(Instant.parse("2026-07-31T21:00:00Z") to Instant.parse("2026-08-31T21:00:00Z"),
            transactions.ranges.last())
        assertTrue(vm.canGoNext())
        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(listOf(1L), vm.uiState.value.rows.map { it.transaction.id })
    }

    @Test fun emptyStateDistinguishesNoHistoryMonthAndFilteredMiss() = runTest {
        val vm = TransactionsViewModel(transactions, categories, clock)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.hasAnyHistory)
        transactions.items.value = listOf(transaction(1, "2026-08-20T10:00:00Z", 1))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasAnyHistory)
        assertFalse(vm.uiState.value.hasMonthHistory)
        transactions.items.value += transaction(2, "2026-09-20T10:00:00Z", 1)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasMonthHistory)
        vm.setSearchQuery("missing")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.rows.isEmpty())
        assertTrue(vm.uiState.value.filters.hasActiveFilters)
    }

    private fun transaction(id: Long, at: String, category: Long) = Transaction(
        id, Money(1240, "EUR"), TransactionType.EXPENSE, "Shop", null, category,
        Instant.parse(at), TransactionSource.MANUAL, ImportStatus.CONFIRMED,
        null, null, null, Instant.parse(at), Instant.parse(at),
    )

    private class FakeTransactions : TransactionRepository {
        val items = MutableStateFlow<List<Transaction>>(emptyList())
        var getByIdCalls = 0
        val ranges = mutableListOf<Pair<Instant, Instant>>()
        override suspend fun insert(transaction: Transaction): Long = error("Unused")
        override suspend fun update(transaction: Transaction): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): Transaction? { getByIdCalls++; return null }
        override suspend fun getBySourceAndExternalReference(source: com.spendly.domain.model.TransactionSource, externalReference: String): Transaction? = error("Unused")
        override suspend fun getBySourceInTimeRange(source: com.spendly.domain.model.TransactionSource, startInclusive: Instant, endInclusive: Instant): List<Transaction> = error("Unused")
        override fun observeAll(): Flow<List<Transaction>> = items
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> {
            ranges += startInclusive to endExclusive
            return items.map { rows -> rows.filter { it.occurredAt >= startInclusive && it.occurredAt < endExclusive } }
        }
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
