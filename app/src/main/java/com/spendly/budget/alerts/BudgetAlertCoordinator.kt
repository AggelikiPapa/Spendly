package com.spendly.budget.alerts

import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.repository.MonthlyBudgetRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Duration
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Observes persisted spending and budgets once for every transaction source. */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetAlertCoordinator(
    private val transactions: TransactionRepository,
    private val budgets: MonthlyBudgetRepository,
    private val store: BudgetAlertStore,
    private val permission: BudgetAlertPermission,
    private val sender: BudgetAlertSender,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val mutex = Mutex()

    fun start(scope: CoroutineScope) = scope.launch {
        monthFlow().flatMapLatest { month ->
            combine(transactions.observeAll(), budgets.observeByMonth(month), store.enabled) { all, budget, _ ->
                Snapshot(month, all, budget)
            }
        }.collect { snapshot ->
            try {
                evaluate(snapshot.month, snapshot.transactions, snapshot.budget)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Alert failures must not affect transaction saves or terminate observation.
            }
        }
    }

    suspend fun evaluate(month: YearMonth, allTransactions: List<Transaction>, budget: MonthlyBudget?) = mutex.withLock {
        if (!store.isEnabled() || !permission.canPost()) return@withLock
        val decision = BudgetThresholdEvaluator.evaluate(month, budget, allTransactions, store.delivered(month), clock.zone)
            ?: return@withLock
        // Persist every newly crossed threshold before posting only the highest one.
        if (!store.markDelivered(month, decision.newlyCrossed)) return@withLock
        sender.send(month, decision)
    }

    private fun monthFlow() = flow {
        while (true) {
            val month = YearMonth.now(clock)
            emit(month)
            val nextMonth = month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toInstant()
            delay(Duration.between(clock.instant(), nextMonth).toMillis().coerceAtLeast(1L))
        }
    }

    private data class Snapshot(val month: YearMonth, val transactions: List<Transaction>, val budget: MonthlyBudget?)
}
