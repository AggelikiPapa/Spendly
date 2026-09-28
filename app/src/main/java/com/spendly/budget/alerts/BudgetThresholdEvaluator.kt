package com.spendly.budget.alerts

import com.spendly.domain.MonthlySpendingCalculator
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.ZoneId

data class BudgetAlertDecision(
    val highest: BudgetThreshold,
    val newlyCrossed: Set<BudgetThreshold>,
    val spent: Money,
    val limit: Money,
    val percentageUsed: BigDecimal?,
)

object BudgetThresholdEvaluator {
    fun evaluate(
        month: YearMonth,
        budget: MonthlyBudget?,
        transactions: List<Transaction>,
        delivered: Set<BudgetThreshold>,
        zone: ZoneId,
    ): BudgetAlertDecision? {
        if (budget == null) return null
        require(budget.yearMonth == month) { "Budget month does not match" }
        val spent = MonthlySpendingCalculator.spent(transactions, month, zone, budget.limit.currencyCode)
        val spentMinor = BigDecimal.valueOf(spent.amountMinor)
        val limitMinor = BigDecimal.valueOf(budget.limit.amountMinor)
        val reached = when {
            budget.limit.amountMinor == 0L && spent.amountMinor == 0L -> emptySet()
            budget.limit.amountMinor == 0L -> setOf(BudgetThreshold.ONE_HUNDRED)
            else -> BudgetThreshold.entries.filterTo(mutableSetOf()) { threshold ->
                spentMinor.multiply(BigDecimal.valueOf(100)) >= limitMinor.multiply(BigDecimal.valueOf(threshold.percent.toLong()))
            }
        }
        val newlyCrossed = reached - delivered
        val highest = newlyCrossed.maxByOrNull { it.percent } ?: return null
        val percentage = if (budget.limit.amountMinor == 0L) null else spentMinor.multiply(BigDecimal.valueOf(100))
            .divide(limitMinor, 0, RoundingMode.HALF_UP)
        return BudgetAlertDecision(highest, newlyCrossed, spent, budget.limit, percentage)
    }
}
