package com.spendly.ui.analytics

import com.spendly.domain.model.Category
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
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
class AnalyticsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"), ZoneId.of("Europe/Athens"))
    private val transactions = FakeTransactions()
    private val categories = FakeCategories()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun fixedClockStartsAtCurrentMonthAndNavigationNeverEntersFuture() = runTest {
        val vm = AnalyticsViewModel(transactions, categories, clock)
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 9), vm.selectedMonth.value)
        assertFalse(vm.canGoNext())
        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 9), vm.selectedMonth.value)
        vm.previousMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 8), vm.selectedMonth.value)
        assertTrue(vm.canGoNext())
        assertEquals(Instant.parse("2026-06-30T21:00:00Z"), transactions.ranges.last().first)
        assertEquals(Instant.parse("2026-08-31T21:00:00Z"), transactions.ranges.last().second)
        vm.nextMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 9), vm.selectedMonth.value)
    }

    @Test fun transactionAndCategoryChangesRecalculateWithoutRefresh() = runTest {
        transactions.items.value = listOf(transaction(1, 1_000))
        val vm = AnalyticsViewModel(transactions, categories, clock)
        advanceUntilIdle()
        assertEquals(Money(1_000, "EUR"), (vm.uiState.value as AnalyticsUiState.Ready).summary.total)
        assertEquals("Groceries", (vm.uiState.value as AnalyticsUiState.Ready).summary.categories.single().name)

        transactions.items.value += transaction(2, 2_000)
        advanceUntilIdle()
        assertEquals(Money(3_000, "EUR"), (vm.uiState.value as AnalyticsUiState.Ready).summary.total)
        categories.items.value = listOf(Category(1, "Food", true, false))
        advanceUntilIdle()
        assertEquals("Food", (vm.uiState.value as AnalyticsUiState.Ready).summary.categories.single().name)

        transactions.items.value = listOf(transaction(2, 2_000).copy(importStatus = ImportStatus.IGNORED))
        advanceUntilIdle()
        assertTrue(vm.uiState.value is AnalyticsUiState.Empty)
    }

    @Test fun repositoryFailureShowsErrorAndMonthNavigationRecovers() = runTest {
        transactions.fail = true
        val vm = AnalyticsViewModel(transactions, categories, clock)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is AnalyticsUiState.Error)
        transactions.fail = false
        vm.previousMonth()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is AnalyticsUiState.Empty)
    }

    private fun transaction(id: Long, minor: Long): Transaction {
        val instant = Instant.parse("2026-09-05T10:00:00Z")
        return Transaction(id, Money(minor, "EUR"), TransactionType.EXPENSE, "Shop", null, 1,
            instant, TransactionSource.MANUAL, ImportStatus.CONFIRMED, null, null, null, instant, instant)
    }

    private class FakeCategories : CategoryRepository {
        val items = MutableStateFlow(listOf(Category(1, "Groceries", true, true)))
        override suspend fun insert(category: Category): Long = error("Unused")
        override suspend fun update(category: Category): Int = error("Unused")
        override suspend fun getById(id: Long): Category? = error("Unused")
        override fun observeActive(): Flow<List<Category>> = error("Unused")
        override fun observeAll(): Flow<List<Category>> = items
    }

    private class FakeTransactions : TransactionRepository {
        val items = MutableStateFlow<List<Transaction>>(emptyList())
        val ranges = mutableListOf<Pair<Instant, Instant>>()
        var fail = false
        override suspend fun insert(transaction: Transaction): Long = error("Unused")
        override suspend fun update(transaction: Transaction): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): Transaction? = error("Unused")
        override suspend fun getBySourceAndExternalReference(source: TransactionSource, externalReference: String): Transaction? = error("Unused")
        override suspend fun getBySourceInTimeRange(source: TransactionSource, startInclusive: Instant, endInclusive: Instant): List<Transaction> = error("Unused")
        override fun observeAll(): Flow<List<Transaction>> = emptyFlow()
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> {
            ranges += startInclusive to endExclusive
            return flow {
                if (fail) error("Database unavailable")
                emitAll(items.map { rows -> rows.filter { it.occurredAt >= startInclusive && it.occurredAt < endExclusive } })
            }
        }
    }
}
