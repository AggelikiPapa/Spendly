package com.spendly.ui.dashboard

import com.spendly.domain.model.Category
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MonthlyBudgetRepository
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val transactions = FakeTransactions()
    private val budgets = FakeBudgets()
    private val categories = FakeCategories()
    private val clock = Clock.fixed(Instant.parse("2026-09-22T09:00:00Z"), ZoneId.of("Europe/Athens"))
    private val month = YearMonth.of(2026, 9)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun noBudgetProducesReadyEmptyStateWithoutDefault() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        val state = vm.uiState.value as DashboardUiState.Ready
        assertEquals(month, state.month)
        assertNull(state.progress)
        assertTrue(state.recentTransactions.isEmpty())
        assertEquals(listOf(month), budgets.observedMonths)
    }

    @Test fun transactionAndBudgetFlowsRecalculateAutomatically() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        budgets.value.value = MonthlyBudget(month, Money(100_000, "EUR"))
        transactions.value.value = listOf(transaction(1, 10_000))
        advanceUntilIdle()
        assertEquals(Money(10_000, "EUR"), (vm.uiState.value as DashboardUiState.Ready).progress?.spent)
        assertEquals(SpendingPaceStatus.BELOW_PACE,
            (vm.uiState.value as DashboardUiState.Ready).progress?.spendingPace?.status)

        transactions.value.value = listOf(transaction(1, 20_000))
        advanceUntilIdle()
        assertEquals(Money(20_000, "EUR"), (vm.uiState.value as DashboardUiState.Ready).progress?.spent)
        assertEquals(Money(-53_333, "EUR"),
            (vm.uiState.value as DashboardUiState.Ready).progress?.spendingPace?.paceDifference)

        budgets.value.value = MonthlyBudget(month, Money(50_000, "EUR"))
        advanceUntilIdle()
        assertEquals(Money(30_000, "EUR"), (vm.uiState.value as DashboardUiState.Ready).progress?.remaining)
        assertEquals(Money(36_667, "EUR"),
            (vm.uiState.value as DashboardUiState.Ready).progress?.spendingPace?.expectedSpentByToday)
        transactions.value.value = emptyList()
        advanceUntilIdle()
        assertEquals(Money(0, "EUR"), (vm.uiState.value as DashboardUiState.Ready).progress?.spent)
    }

    @Test fun recentTransactionsAreNewestFirstLimitedToFiveAndResolveCategories() = runTest {
        transactions.value.value = (1L..7L).map { id -> transaction(id, 100, "2026-09-0${id}T10:00:00Z") }
        val vm = newViewModel()
        advanceUntilIdle()
        val recent = (vm.uiState.value as DashboardUiState.Ready).recentTransactions
        assertEquals(listOf(7L, 6L, 5L, 4L, 3L), recent.map { it.transaction.id })
        assertEquals(List(5) { "Groceries" }, recent.map { it.categoryName })
        assertEquals(0, transactions.getByIdCalls)
    }

    @Test fun reviewAndIgnoredWalletImportsStayOutOfRecentAndBudget() = runTest {
        budgets.value.value = MonthlyBudget(month, Money(10_000, "EUR"))
        val confirmed = transaction(1, 295).copy(source = TransactionSource.GOOGLE_WALLET)
        transactions.value.value = listOf(
            confirmed,
            transaction(2, 400).copy(source = TransactionSource.GOOGLE_WALLET, importStatus = ImportStatus.NEEDS_REVIEW),
            transaction(3, 600).copy(source = TransactionSource.GOOGLE_WALLET, importStatus = ImportStatus.IGNORED),
        )
        val vm = newViewModel()
        advanceUntilIdle()
        val state = vm.uiState.value as DashboardUiState.Ready
        assertEquals(listOf(1L), state.recentTransactions.map { it.transaction.id })
        assertEquals(Money(295, "EUR"), state.progress?.spent)
    }

    @Test fun incompatibleExpenseCurrencyShowsSafeError() = runTest {
        budgets.value.value = MonthlyBudget(month, Money(100_000, "EUR"))
        transactions.value.value = listOf(transaction(1, 100).copy(amount = Money(100, "USD")))
        val vm = newViewModel()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is DashboardUiState.Error)
        assertEquals("Dashboard could not be loaded.", (vm.uiState.value as DashboardUiState.Error).message)
    }

    private fun newViewModel() = DashboardViewModel(transactions, budgets, categories, clock)

    private fun transaction(id: Long, minor: Long, at: String = "2026-09-20T10:00:00Z"): Transaction {
        val instant = Instant.parse(at)
        return Transaction(id, Money(minor, "EUR"), TransactionType.EXPENSE, "Shop", null, 1,
            instant, TransactionSource.MANUAL, ImportStatus.CONFIRMED, null, null, null, instant, instant)
    }

    private class FakeTransactions : TransactionRepository {
        val value = MutableStateFlow<List<Transaction>>(emptyList())
        var getByIdCalls = 0
        override suspend fun insert(transaction: Transaction): Long = error("Unused")
        override suspend fun update(transaction: Transaction): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): Transaction? { getByIdCalls++; return null }
        override suspend fun getBySourceAndExternalReference(source: com.spendly.domain.model.TransactionSource, externalReference: String): Transaction? = error("Unused")
        override suspend fun getBySourceInTimeRange(source: com.spendly.domain.model.TransactionSource, startInclusive: Instant, endInclusive: Instant): List<Transaction> = error("Unused")
        override fun observeAll(): Flow<List<Transaction>> = value
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> = emptyFlow()
    }

    private class FakeBudgets : MonthlyBudgetRepository {
        val value = MutableStateFlow<MonthlyBudget?>(null)
        val observedMonths = mutableListOf<YearMonth>()
        override suspend fun upsert(budget: MonthlyBudget) = error("Unused")
        override suspend fun getByMonth(yearMonth: YearMonth): MonthlyBudget? = error("Unused")
        override fun observeByMonth(yearMonth: YearMonth): Flow<MonthlyBudget?> { observedMonths += yearMonth; return value }
    }

    private class FakeCategories : CategoryRepository {
        val value = MutableStateFlow(listOf(Category(1, "Groceries", true, true)))
        override suspend fun insert(category: Category): Long = error("Unused")
        override suspend fun update(category: Category): Int = error("Unused")
        override suspend fun getById(id: Long): Category? = error("Unused")
        override fun observeActive(): Flow<List<Category>> = value
        override fun observeAll(): Flow<List<Category>> = value
    }
}
