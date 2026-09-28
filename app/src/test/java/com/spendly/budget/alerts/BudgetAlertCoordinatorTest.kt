package com.spendly.budget.alerts

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
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetAlertCoordinatorTest {
    private val month = YearMonth.of(2026, 9)
    private val budget = MonthlyBudget(month, Money(10_000, "EUR"))
    private val store = FakeStore()
    private val permission = FakePermission()
    private val sender = FakeSender()
    private val transactions = UnusedTransactions()
    private val budgets = UnusedBudgets()
    private val coordinator = BudgetAlertCoordinator(transactions, budgets, store, permission, sender,
        Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC))

    @Test fun oneLargeExpensePostsHighestAndPersistsAllCrossedThresholds() = runTest {
        coordinator.evaluate(month, listOf(expense(9_200)), budget)
        assertEquals(listOf(BudgetThreshold.NINETY), sender.sent.map { it.highest })
        assertEquals(setOf(BudgetThreshold.SEVENTY, BudgetThreshold.EIGHTY, BudgetThreshold.NINETY), store.delivered(month))
        coordinator.evaluate(month, listOf(expense(9_200)), budget)
        assertEquals(1, sender.sent.size)
    }

    @Test fun laterMonthCanSendSameThresholdAgain() = runTest {
        coordinator.evaluate(month, listOf(expense(7_100)), budget)
        val october = month.plusMonths(1)
        coordinator.evaluate(october, listOf(expense(7_100, Instant.parse("2026-10-15T12:00:00Z"))),
            MonthlyBudget(october, Money(10_000, "EUR")))
        assertEquals(listOf(BudgetThreshold.SEVENTY, BudgetThreshold.SEVENTY), sender.sent.map { it.highest })
    }

    @Test fun disabledPermissionMissingOrPersistenceFailureSendsNothing() = runTest {
        store.enabledState.value = false
        coordinator.evaluate(month, listOf(expense(7_100)), budget)
        assertTrue(sender.sent.isEmpty())
        store.enabledState.value = true
        permission.granted = false
        coordinator.evaluate(month, listOf(expense(7_100)), budget)
        assertTrue(sender.sent.isEmpty())
        permission.granted = true
        store.persist = false
        coordinator.evaluate(month, listOf(expense(7_100)), budget)
        assertTrue(sender.sent.isEmpty())
        assertTrue(store.delivered(month).isEmpty())
    }

    @Test fun concurrentEvaluationsCannotDuplicateNotification() = runTest {
        (1..20).map { async { coordinator.evaluate(month, listOf(expense(7_100)), budget) } }.awaitAll()
        assertEquals(1, sender.sent.size)
        assertEquals(setOf(BudgetThreshold.SEVENTY), store.delivered(month))
    }

    @Test fun deletionDoesNotResetDeliveredAndBudgetEditCanSendNextThreshold() = runTest {
        coordinator.evaluate(month, listOf(expense(7_100)), budget)
        coordinator.evaluate(month, emptyList(), budget)
        coordinator.evaluate(month, listOf(expense(7_100)), budget)
        assertEquals(1, sender.sent.size)
        coordinator.evaluate(month, listOf(expense(7_100)), MonthlyBudget(month, Money(8_000, "EUR")))
        assertEquals(listOf(BudgetThreshold.SEVENTY, BudgetThreshold.EIGHTY), sender.sent.map { it.highest })
    }

    @Test fun repositoryChangesFromManualWalletReviewAndBudgetAreObserved() = runTest {
        coordinator.start(backgroundScope)
        runCurrent()
        transactions.items.value = listOf(expense(7_100))
        runCurrent()
        assertTrue(sender.sent.isEmpty()) // No budget yet.
        budgets.current.value = budget
        runCurrent()
        assertEquals(listOf(BudgetThreshold.SEVENTY), sender.sent.map { it.highest })
        val wallet = expense(1_100).copy(source = TransactionSource.GOOGLE_WALLET)
        transactions.items.value += wallet
        runCurrent()
        assertEquals(BudgetThreshold.EIGHTY, sender.sent.last().highest)
        transactions.items.value += expense(1_000).copy(source = TransactionSource.GOOGLE_WALLET,
            importStatus = ImportStatus.NEEDS_REVIEW)
        runCurrent()
        assertEquals(2, sender.sent.size)
        transactions.items.value = transactions.items.value.map {
            if (it.importStatus == ImportStatus.NEEDS_REVIEW) it.copy(importStatus = ImportStatus.CONFIRMED) else it
        }
        runCurrent()
        assertEquals(BudgetThreshold.NINETY, sender.sent.last().highest)
    }

    private fun expense(amount: Long, at: Instant = Instant.parse("2026-09-15T12:00:00Z")) =
        Transaction(0, Money(amount, "EUR"), TransactionType.EXPENSE, "Shop", null, null, at,
            TransactionSource.MANUAL, ImportStatus.CONFIRMED, null, null, null, at, at)

    private class FakeStore : BudgetAlertStore {
        val enabledState = MutableStateFlow(true)
        override val enabled: Flow<Boolean> = enabledState
        val byMonth = mutableMapOf<YearMonth, Set<BudgetThreshold>>()
        var persist = true
        override fun isEnabled() = enabledState.value
        override fun setEnabled(enabled: Boolean): Boolean { enabledState.value = enabled; return true }
        override fun delivered(month: YearMonth) = byMonth[month].orEmpty()
        override fun markDelivered(month: YearMonth, thresholds: Set<BudgetThreshold>): Boolean {
            if (!persist) return false
            byMonth[month] = delivered(month) + thresholds
            return true
        }
    }

    private class FakePermission : BudgetAlertPermission {
        var granted = true
        override fun canPost() = granted
    }

    private class FakeSender : BudgetAlertSender {
        val sent = mutableListOf<BudgetAlertDecision>()
        override fun send(month: YearMonth, decision: BudgetAlertDecision) { sent += decision }
    }

    private class UnusedTransactions : TransactionRepository {
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

    private class UnusedBudgets : MonthlyBudgetRepository {
        val current = MutableStateFlow<MonthlyBudget?>(null)
        override suspend fun upsert(budget: MonthlyBudget) = error("Unused")
        override suspend fun getByMonth(yearMonth: YearMonth): MonthlyBudget? = error("Unused")
        override fun observeByMonth(yearMonth: YearMonth): Flow<MonthlyBudget?> = current
    }
}
