package com.spendly.widget

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.MonthlyBudgetRepository
import com.spendly.domain.repository.TransactionRepository
import com.spendly.ui.theme.AccentColor
import java.time.Clock
import java.time.Duration
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch

fun interface BudgetWidgetUpdater {
    suspend fun update()
}

/** One process-level observer covers manual edits, Wallet imports, Review, and budget saves. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class BudgetWidgetUpdateCoordinator(
    private val transactions: TransactionRepository,
    private val budgets: MonthlyBudgetRepository,
    private val updater: BudgetWidgetUpdater,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val months: Flow<YearMonth> = currentMonthFlow(clock),
    private val accents: Flow<AccentColor> = flowOf(AccentColor.Teal),
) {
    private var observationJob: Job? = null

    @Synchronized
    fun start(scope: CoroutineScope): Job {
        observationJob?.let { return it }
        return scope.launch {
            months.flatMapLatest { month ->
                val start = month.atDay(1).atStartOfDay(clock.zone).toInstant()
                val end = month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toInstant()
                combine(transactions.observeAll(), budgets.observeByMonth(month), accents) { all, budget, accent ->
                    WidgetSnapshot(
                        month,
                        budget?.limit,
                        all.asSequence()
                            .filter { it.occurredAt >= start && it.occurredAt < end }
                            .map { it.widgetKey() }
                            .sortedBy { it.id }
                            .toList(),
                        accent,
                    )
                }
            }.distinctUntilChanged()
                .debounce(COALESCE_MILLIS)
                .retryWhen { error, attempt ->
                    if (error is CancellationException) throw error
                    delay((1_000L * (attempt + 1).coerceAtMost(30)).coerceAtMost(30_000L))
                    true
                }
                .collect {
                    try {
                        updater.update()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Rendering failures must not terminate observation or affect saved data.
                    }
                }
        }.also { observationJob = it }
    }

    private data class WidgetSnapshot(
        val month: YearMonth,
        val limit: Money?,
        val transactions: List<TransactionKey>,
        val accent: AccentColor,
    )

    /** Excludes merchant/category edits, but retains Review status changes and month movement. */
    private data class TransactionKey(
        val id: Long,
        val amount: Money,
        val type: TransactionType,
        val status: ImportStatus,
        val occurredAt: java.time.Instant,
    )

    private fun Transaction.widgetKey() = TransactionKey(id, amount, type, importStatus, occurredAt)

    companion object {
        internal const val COALESCE_MILLIS = 250L
    }
}

/** Rebinds the budget flow when the device-local month changes, without polling Room. */
internal fun currentMonthFlow(clock: Clock): Flow<YearMonth> = flow {
    while (true) {
        val month = YearMonth.now(clock)
        emit(month)
        val nextMonth = month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toInstant()
        delay(Duration.between(clock.instant(), nextMonth).toMillis().coerceAtLeast(1L))
    }
}
