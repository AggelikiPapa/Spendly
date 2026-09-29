package com.spendly.widget

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.MonthlyBudgetRepository
import com.spendly.domain.repository.TransactionRepository
import com.spendly.ui.theme.AccentColor
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetWidgetUpdateCoordinatorTest {
    private val september = YearMonth.of(2026, 9)
    private val months = MutableStateFlow(september)
    private val clock = Clock.fixed(Instant.parse("2026-09-20T10:00:00Z"), ZoneId.of("Europe/Athens"))
    private val transactions = FakeTransactions()
    private val budgets = FakeBudgets()
    private val accents = MutableStateFlow(AccentColor.Teal)
    private val updater = FakeUpdater()
    private val coordinator get() = BudgetWidgetUpdateCoordinator(transactions, budgets, updater, clock, months, accents)

    @Test fun accentChangeRefreshesWidgetWithoutDataChange() = runTest {
        coordinator.start(backgroundScope)
        settle()
        assertEquals(1, updater.count)
        accents.value = AccentColor.Purple
        settle()
        assertEquals(2, updater.count)
        accents.value = AccentColor.Purple
        settle()
        assertEquals(2, updater.count)
    }

    @Test fun manualInsertUpdateDeleteAndBudgetSaveEachRefresh() = runTest {
        coordinator.start(backgroundScope)
        settle()
        assertEquals(1, updater.count) // Startup refresh, including the no-budget state.

        val expense = transaction(1, 2_000)
        transactions.items.value = listOf(expense)
        settle()
        assertEquals(2, updater.count)

        transactions.items.value = listOf(expense.copy(amount = Money(3_000, "EUR")))
        settle()
        assertEquals(3, updater.count)

        transactions.items.value = emptyList()
        settle()
        assertEquals(4, updater.count)

        budgets.forMonth(september).value = MonthlyBudget(september, Money(100_000, "EUR"))
        settle()
        assertEquals(5, updater.count)
        budgets.forMonth(september).value = MonthlyBudget(september, Money(120_000, "EUR"))
        settle()
        assertEquals(6, updater.count)
    }

    @Test fun walletImportReviewConfirmAndIgnoreRefreshThroughRepositoryFlow() = runTest {
        coordinator.start(backgroundScope)
        settle()
        val wallet = transaction(2, 2_000, source = TransactionSource.GOOGLE_WALLET)
        transactions.items.value = listOf(wallet)
        settle()
        assertEquals(2, updater.count)

        val review = transaction(3, 1_000, source = TransactionSource.GOOGLE_WALLET, status = ImportStatus.NEEDS_REVIEW)
        transactions.items.value += review
        settle()
        transactions.items.value = listOf(wallet, review.copy(importStatus = ImportStatus.CONFIRMED))
        settle()
        transactions.items.value = listOf(wallet, review.copy(importStatus = ImportStatus.IGNORED))
        settle()
        assertEquals(5, updater.count)
    }

    @Test fun rapidChangesCoalesceAndMerchantOnlyEditDoesNotRefresh() = runTest {
        coordinator.start(backgroundScope)
        settle()
        val expense = transaction(1, 1_000)
        transactions.items.value = listOf(expense)
        runCurrent()
        transactions.items.value = listOf(expense.copy(amount = Money(2_000, "EUR")))
        runCurrent()
        transactions.items.value = listOf(expense.copy(amount = Money(3_000, "EUR")))
        settle()
        assertEquals(2, updater.count)
        transactions.items.value = listOf(expense.copy(amount = Money(3_000, "EUR"), merchant = "Renamed"))
        settle()
        assertEquals(2, updater.count)
    }

    @Test fun startIsIdempotentAndUpdateFailureDoesNotStopObservation() = runTest {
        val sync = coordinator
        assertSame(sync.start(backgroundScope), sync.start(backgroundScope))
        updater.failNext = true
        settle()
        assertEquals(1, updater.attempts)
        transactions.items.value = listOf(transaction(1, 1_000))
        settle()
        assertEquals(2, updater.attempts)
        assertEquals(1, updater.count)
    }

    @Test fun monthTransitionRebindsBudgetAndRefreshesEvenWithoutTransactions() = runTest {
        coordinator.start(backgroundScope)
        settle()
        months.value = september.plusMonths(1)
        settle()
        assertEquals(listOf(september, september.plusMonths(1)), budgets.observed)
        assertEquals(2, updater.count)
        budgets.forMonth(september.plusMonths(1)).value = MonthlyBudget(september.plusMonths(1), Money(50_000, "EUR"))
        settle()
        assertEquals(3, updater.count)
    }

    @Test fun localMonthBoundaryFiltersOldTransactionsButDetectsMovesIntoMonth() = runTest {
        coordinator.start(backgroundScope)
        settle()
        val old = transaction(1, 1_000, at = "2026-08-30T10:00:00Z")
        transactions.items.value = listOf(old)
        settle()
        assertEquals(1, updater.count)
        transactions.items.value = listOf(old.copy(occurredAt = Instant.parse("2026-08-31T22:30:00Z")))
        settle()
        assertEquals(2, updater.count) // September 1 in Athens.
    }

    @Test fun noWidgetUpdaterAndNewProcessCoordinatorAreSafe() = runTest {
        val noWidgets = BudgetWidgetUpdater { }
        val oldJob = BudgetWidgetUpdateCoordinator(transactions, budgets, noWidgets, clock, months).start(backgroundScope)
        settle()
        transactions.items.value = listOf(transaction(1, 1_000))
        settle()
        // A fresh coordinator in a restarted process performs its own initial refresh.
        oldJob.cancel()
        BudgetWidgetUpdateCoordinator(transactions, budgets, updater, clock, months).start(backgroundScope)
        settle()
        assertEquals(1, updater.count)
    }

    @Test fun monthClockEmitsAtLocalMonthBoundary() = runTest {
        val movingClock = MovingClock(Instant.parse("2026-09-30T20:59:59Z"), ZoneId.of("Europe/Athens"))
        val observed = mutableListOf<YearMonth>()
        backgroundScope.launch { currentMonthFlow(movingClock).take(2).toList(observed) }
        runCurrent()
        assertEquals(listOf(september), observed)
        advanceTimeBy(999)
        movingClock.now = Instant.parse("2026-09-30T21:00:00Z")
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(september, september.plusMonths(1)), observed)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.settle() {
        runCurrent()
        advanceTimeBy(BudgetWidgetUpdateCoordinator.COALESCE_MILLIS)
        runCurrent()
    }

    private fun transaction(
        id: Long,
        minor: Long,
        source: TransactionSource = TransactionSource.MANUAL,
        status: ImportStatus = ImportStatus.CONFIRMED,
        at: String = "2026-09-20T10:00:00Z",
    ): Transaction {
        val instant = Instant.parse(at)
        return Transaction(id, Money(minor, "EUR"), TransactionType.EXPENSE, "Shop", null, null,
            instant, source, status, null, null, null, instant, instant)
    }

    private class FakeUpdater : BudgetWidgetUpdater {
        var attempts = 0
        var count = 0
        var failNext = false
        override suspend fun update() {
            attempts++
            if (failNext) { failNext = false; error("Widget host unavailable") }
            count++
        }
    }

    private class MovingClock(var now: Instant, private val timeZone: ZoneId) : Clock() {
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = timeZone
        override fun withZone(zone: ZoneId): Clock = MovingClock(now, zone)
    }

    private class FakeBudgets : MonthlyBudgetRepository {
        private val values = mutableMapOf<YearMonth, MutableStateFlow<MonthlyBudget?>>()
        val observed = mutableListOf<YearMonth>()
        fun forMonth(month: YearMonth) = values.getOrPut(month) { MutableStateFlow(null) }
        override suspend fun upsert(budget: MonthlyBudget) = error("Unused")
        override suspend fun getByMonth(yearMonth: YearMonth): MonthlyBudget? = error("Unused")
        override fun observeByMonth(yearMonth: YearMonth): Flow<MonthlyBudget?> {
            observed += yearMonth
            return forMonth(yearMonth)
        }
    }

    private class FakeTransactions : TransactionRepository {
        val items = MutableStateFlow<List<Transaction>>(emptyList())
        override suspend fun insert(transaction: Transaction): Long = error("Unused")
        override suspend fun update(transaction: Transaction): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): Transaction? = error("Unused")
        override suspend fun getBySourceAndExternalReference(source: TransactionSource, externalReference: String): Transaction? = error("Unused")
        override suspend fun getBySourceInTimeRange(source: TransactionSource, startInclusive: Instant, endInclusive: Instant): List<Transaction> = error("Unused")
        override fun observeAll(): Flow<List<Transaction>> = items
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> = emptyFlow()
    }
}
