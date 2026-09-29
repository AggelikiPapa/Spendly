package com.spendly.widget

import com.spendly.domain.repository.MonthlyBudgetRepository
import com.spendly.domain.repository.TransactionRepository
import com.spendly.ui.dashboard.BudgetProgressCalculator
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class BudgetWidgetDataProvider(
    private val transactions: TransactionRepository,
    private val budgets: MonthlyBudgetRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val locale: Locale = Locale.getDefault(),
) {
    suspend fun load(): BudgetWidgetState {
        val month = YearMonth.now(clock)
        return try {
            val budget = budgets.getByMonth(month) ?: return BudgetWidgetState.NoBudget(month)
            val allTransactions = transactions.observeAll().first()
            val progress = BudgetProgressCalculator.calculate(
                budget, allTransactions, month, LocalDate.now(clock), clock.zone,
            )
            BudgetWidgetStateFactory.ready(month, progress, locale)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            BudgetWidgetState.Error(month)
        }
    }
}
