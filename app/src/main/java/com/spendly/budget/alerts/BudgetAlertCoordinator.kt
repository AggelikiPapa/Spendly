package com.spendly.budget.alerts

import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.repository.MonthlyBudgetRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
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
    private var suppressedPaceDate: LocalDate? = null

    fun start(scope: CoroutineScope) = scope.launch {
        dateFlow().flatMapLatest { today ->
            val month = YearMonth.from(today)
            combine(transactions.observeAll(), budgets.observeByMonth(month), store.enabled, store.paceEnabled) { all, budget, _, _ ->
                Snapshot(month, today, all, budget)
            }
        }.collect { snapshot ->
            try {
                evaluate(snapshot.month, snapshot.transactions, snapshot.budget, snapshot.today)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Alert failures must not affect transaction saves or terminate observation.
            }
        }
    }

    suspend fun evaluate(
        month: YearMonth,
        allTransactions: List<Transaction>,
        budget: MonthlyBudget?,
        today: LocalDate = LocalDate.now(clock),
    ) = mutex.withLock {
        if (!store.isEnabled() || !permission.canPost()) return@withLock
        val threshold = BudgetThresholdEvaluator.evaluate(month, budget, allTransactions, store.delivered(month), clock.zone)
        if (threshold != null) {
            // Persist every newly crossed threshold before posting only the highest one.
            if (!store.markDelivered(month, threshold.newlyCrossed)) return@withLock
            // A threshold owns this day when pace is also above target. Later changes cannot add a second alert.
            val alsoAbovePace = runCatching {
                store.isPaceEnabled() &&
                    SpendingPaceAlertEvaluator.evaluate(month, today, budget, allTransactions, clock.zone) != null
            }.getOrDefault(false)
            if (alsoAbovePace) {
                suppressedPaceDate = today
                runCatching { store.markPaceAlertDate(today) }
            }
            sender.send(month, threshold)
            return@withLock
        }
        if (!store.isPaceEnabled() || suppressedPaceDate == today || store.lastPaceAlertDate() == today) return@withLock
        val pace = SpendingPaceAlertEvaluator.evaluate(month, today, budget, allTransactions, clock.zone) ?: return@withLock
        if (!store.markPaceAlertDate(today)) return@withLock
        suppressedPaceDate = today
        sender.sendPace(pace)
    }

    private fun dateFlow() = flow {
        while (true) {
            val today = LocalDate.now(clock)
            emit(today)
            val tomorrow = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
            delay(Duration.between(clock.instant(), tomorrow).toMillis().coerceAtLeast(1L))
        }
    }

    private data class Snapshot(
        val month: YearMonth,
        val today: LocalDate,
        val transactions: List<Transaction>,
        val budget: MonthlyBudget?,
    )
}
