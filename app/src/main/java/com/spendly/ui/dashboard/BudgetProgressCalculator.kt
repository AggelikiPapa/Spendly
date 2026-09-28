package com.spendly.ui.dashboard

import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionVisibility
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class BudgetProgress(
    val budgetLimit: Money,
    val spent: Money,
    val remaining: Money,
    /** Null when a zero limit has been exceeded, because no finite percentage exists. */
    val percentageUsed: BigDecimal?,
    val daysRemaining: Int,
    val recommendedDailySpend: Money,
    val spendingPace: SpendingPace,
)

enum class SpendingPaceStatus { BELOW_PACE, ON_PACE, ABOVE_PACE }

data class SpendingPace(
    val expectedSpentByToday: Money,
    /** Actual spending minus expected spending; retains its sign. */
    val paceDifference: Money,
    val status: SpendingPaceStatus,
)

object BudgetProgressCalculator {
    fun calculate(
        budget: MonthlyBudget,
        transactions: List<Transaction>,
        month: YearMonth,
        today: LocalDate,
        zone: ZoneId,
    ): BudgetProgress {
        require(budget.yearMonth == month) { "Budget month does not match the dashboard month" }
        require(today.year == month.year && today.month == month.month) { "Date is outside dashboard month" }
        require(budget.limit.currencyCode == "EUR") { "Unsupported budget currency" }

        val start: Instant = month.atDay(1).atStartOfDay(zone).toInstant()
        val end: Instant = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        val spent = transactions.asSequence()
            .filter { TransactionVisibility.inHistory(it) && it.type.countsTowardMonthlyBudget && it.occurredAt >= start && it.occurredAt < end }
            .fold(Money(0, budget.limit.currencyCode)) { total, transaction ->
                require(transaction.amount.amountMinor >= 0) { "Negative expense amount" }
                total + transaction.amount
            }
        val remaining = budget.limit - spent
        val percentage = when {
            budget.limit.amountMinor == 0L && spent.amountMinor == 0L -> BigDecimal.ZERO
            budget.limit.amountMinor == 0L -> null
            else -> BigDecimal.valueOf(spent.amountMinor).multiply(BigDecimal(100))
                .divide(BigDecimal.valueOf(budget.limit.amountMinor), 6, RoundingMode.HALF_UP)
        }
        val days = ChronoUnit.DAYS.between(today, month.atEndOfMonth()).toInt() + 1
        val dailyMinor = if (remaining.amountMinor <= 0L) 0L else BigDecimal.valueOf(remaining.amountMinor)
            .divide(BigDecimal.valueOf(days.toLong()), 0, RoundingMode.HALF_UP).longValueExact()

        val expectedMinor = BigDecimal.valueOf(budget.limit.amountMinor)
            .multiply(BigDecimal.valueOf(today.dayOfMonth.toLong()))
            .divide(BigDecimal.valueOf(month.lengthOfMonth().toLong()), 0, RoundingMode.HALF_UP)
            .longValueExact()
        val expected = Money(expectedMinor, budget.limit.currencyCode)
        val difference = spent - expected
        // Compare whole-cent differences against the exact threshold; 5% need not be a whole cent.
        val toleranceMinor = BigDecimal.valueOf(expectedMinor).multiply(BigDecimal("0.05"))
            .max(BigDecimal(500))
        val status = when {
            budget.limit.amountMinor == 0L && spent.amountMinor > 0L -> SpendingPaceStatus.ABOVE_PACE
            BigDecimal.valueOf(difference.amountMinor) > toleranceMinor -> SpendingPaceStatus.ABOVE_PACE
            BigDecimal.valueOf(difference.amountMinor) < toleranceMinor.negate() -> SpendingPaceStatus.BELOW_PACE
            else -> SpendingPaceStatus.ON_PACE
        }

        return BudgetProgress(
            budgetLimit = budget.limit,
            spent = spent,
            remaining = remaining,
            percentageUsed = percentage,
            daysRemaining = days,
            recommendedDailySpend = Money(dailyMinor, budget.limit.currencyCode),
            spendingPace = SpendingPace(expected, difference, status),
        )
    }
}
