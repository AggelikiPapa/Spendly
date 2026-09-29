package com.spendly.budget.alerts

import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.ui.dashboard.BudgetProgressCalculator
import com.spendly.ui.dashboard.SpendingPaceStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class SpendingPaceAlertDecision(val aheadOfTarget: Money)

/** Delegates to the same pace calculation used by Dashboard. */
object SpendingPaceAlertEvaluator {
    fun evaluate(
        month: YearMonth,
        today: LocalDate,
        budget: MonthlyBudget?,
        transactions: List<Transaction>,
        zone: ZoneId,
    ): SpendingPaceAlertDecision? {
        if (budget == null || budget.limit.amountMinor == 0L) return null
        val pace = BudgetProgressCalculator.calculate(budget, transactions, month, today, zone).spendingPace
        return pace.takeIf { it.status == SpendingPaceStatus.ABOVE_PACE }
            ?.let { SpendingPaceAlertDecision(it.paceDifference) }
    }
}
