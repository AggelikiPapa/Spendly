package com.spendly.widget

import android.app.Application
import android.content.Intent
import com.spendly.MainActivity
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.MonthlyBudgetRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BudgetWidgetDataProviderTest {
    private val month = YearMonth.of(2026, 9)
    private val clock = Clock.fixed(Instant.parse("2026-09-20T10:00:00Z"), ZoneId.of("Europe/Athens"))
    private val transactions = FakeTransactions()
    private val budgets = FakeBudgets()
    private val provider get() = BudgetWidgetDataProvider(transactions, budgets, clock, Locale.US)

    @Test fun confirmedExpensesMatchDashboardAndBothLayoutsHaveTheirValues() = runBlocking {
        budgets.budget = MonthlyBudget(month, Money(100_000, "EUR"))
        transactions.items = listOf(
            transaction(1, 68_400),
            transaction(2, 9_000, status = ImportStatus.NEEDS_REVIEW),
            transaction(3, 8_000, status = ImportStatus.IGNORED),
            transaction(4, 7_000, type = TransactionType.INCOME),
            transaction(5, 6_000, type = TransactionType.TRANSFER),
            transaction(6, 5_000, at = "2026-08-30T10:00:00Z"),
        )
        val state = provider.load() as BudgetWidgetState.Ready
        assertEquals(month, budgets.requestedMonth)
        assertEquals(Money(68_400, "EUR"), state.spent)
        assertEquals(Money(31_600, "EUR"), state.remaining)
        assertEquals("68%", state.percentageLabel)
        assertEquals("€684.00 / €1,000.00", state.spentLimitLabel)
        assertEquals("€316.00 remaining", state.balanceLabel)
        assertEquals("€316.00 left", state.smallBalanceLabel)
        assertEquals(0.684f, state.visualProgress)
        assertFalse(state.isOverBudget)
    }

    @Test fun overBudgetKeepsActualPercentageAndFormatsPositiveOverage() = runBlocking {
        budgets.budget = MonthlyBudget(month, Money(100_000, "EUR"))
        transactions.items = listOf(transaction(1, 125_000))
        val state = provider.load() as BudgetWidgetState.Ready
        assertEquals(Money(-25_000, "EUR"), state.remaining)
        assertEquals("125%", state.percentageLabel)
        assertTrue(requireNotNull(state.percentageUsed) > java.math.BigDecimal(100))
        assertEquals("€250.00 over", state.balanceLabel)
        assertEquals("€250.00 over", state.smallBalanceLabel)
        assertEquals(1f, state.visualProgress)
        assertTrue(state.isOverBudget)
    }

    @Test fun noBudgetProducesNoBudgetState() = runBlocking {
        assertEquals(BudgetWidgetState.NoBudget(month), provider.load())
    }

    @Test fun zeroBudgetHandlesZeroAndPositiveSpending() = runBlocking {
        budgets.budget = MonthlyBudget(month, Money(0, "EUR"))
        val zero = provider.load() as BudgetWidgetState.Ready
        assertEquals("0%", zero.percentageLabel)
        assertEquals("€0.00 remaining", zero.balanceLabel)
        transactions.items = listOf(transaction(1, 100))
        val over = provider.load() as BudgetWidgetState.Ready
        assertNull(over.percentageUsed)
        assertEquals("Limit exceeded", over.percentageLabel)
        assertEquals("€1.00 over", over.balanceLabel)
        assertEquals(1f, over.visualProgress)
    }

    @Test fun localMonthBoundaryAndEachLoadUsesCurrentMonth() = runBlocking {
        budgets.budget = MonthlyBudget(month, Money(100_000, "EUR"))
        transactions.items = listOf(transaction(1, 1_000, at = "2026-08-31T22:30:00Z"))
        assertEquals(Money(1_000, "EUR"), (provider.load() as BudgetWidgetState.Ready).spent)
        val octoberClock = Clock.fixed(Instant.parse("2026-09-30T22:30:00Z"), clock.zone)
        val october = BudgetWidgetDataProvider(transactions, budgets, octoberClock, Locale.US).load()
        assertEquals(BudgetWidgetState.NoBudget(YearMonth.of(2026, 10)), october)
    }

    @Test fun repositoryFailureBecomesSafeErrorState() = runBlocking {
        budgets.failure = true
        assertEquals(BudgetWidgetState.Error(month), provider.load())
    }

    @Test fun clickIntentOpensMainActivityWithCleanTask() {
        val context = RuntimeEnvironment.getApplication()
        val intent = BudgetWidgetActions.dashboardIntent(context)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
    }

    private fun transaction(
        id: Long,
        minor: Long,
        type: TransactionType = TransactionType.EXPENSE,
        status: ImportStatus = ImportStatus.CONFIRMED,
        at: String = "2026-09-20T10:00:00Z",
    ): Transaction {
        val instant = Instant.parse(at)
        return Transaction(id, Money(minor, "EUR"), type, null, null, null, instant,
            TransactionSource.MANUAL, status, null, null, null, instant, instant)
    }

    private class FakeBudgets : MonthlyBudgetRepository {
        var budget: MonthlyBudget? = null
        var requestedMonth: YearMonth? = null
        var failure = false
        override suspend fun upsert(budget: MonthlyBudget) = error("Unused")
        override suspend fun getByMonth(yearMonth: YearMonth): MonthlyBudget? {
            if (failure) error("Database unavailable")
            requestedMonth = yearMonth
            return budget?.takeIf { it.yearMonth == yearMonth }
        }
        override fun observeByMonth(yearMonth: YearMonth): Flow<MonthlyBudget?> = error("Unused")
    }

    private class FakeTransactions : TransactionRepository {
        var items: List<Transaction> = emptyList()
        override suspend fun insert(transaction: Transaction): Long = error("Unused")
        override suspend fun update(transaction: Transaction): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): Transaction? = error("Unused")
        override suspend fun getBySourceAndExternalReference(source: TransactionSource, externalReference: String): Transaction? = error("Unused")
        override suspend fun getBySourceInTimeRange(source: TransactionSource, startInclusive: Instant, endInclusive: Instant): List<Transaction> = error("Unused")
        override fun observeAll(): Flow<List<Transaction>> = flowOf(items)
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> = error("Unused")
    }
}
