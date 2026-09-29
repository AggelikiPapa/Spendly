package com.spendly.ui.analytics

import com.spendly.domain.MonthlySpendingCalculator
import com.spendly.domain.model.Category
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionVisibility
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CategorySpending(
    val categoryId: Long?,
    val name: String,
    val amount: Money,
    val percentage: BigDecimal,
)

data class DailySpending(val date: LocalDate, val amount: Money)

data class LargestExpense(val transaction: Transaction, val categoryName: String, val date: LocalDate)

data class MonthComparison(
    val previousMonth: YearMonth,
    val previousTotal: Money,
    /** Signed selected-month total minus previous-month total. */
    val difference: Money,
    /** Null when previous spending was zero and selected spending is positive. */
    val percentageChange: BigDecimal?,
)

data class AnalyticsSummary(
    val month: YearMonth,
    val isCurrentMonth: Boolean,
    val expenseCount: Int,
    val total: Money,
    val averageDaily: Money,
    val averageDays: Int,
    val categories: List<CategorySpending>,
    val daily: List<DailySpending>,
    val largest: List<LargestExpense>,
    val comparison: MonthComparison,
) {
    val isEmpty: Boolean get() = expenseCount == 0
}

/** Pure month calculations using the same confirmed-expense predicate as Dashboard. */
object AnalyticsCalculator {
    fun calculate(
        month: YearMonth,
        today: LocalDate,
        zone: ZoneId,
        transactions: List<Transaction>,
        categories: List<Category>,
    ): AnalyticsSummary {
        require(month <= YearMonth.from(today)) { "Cannot analyze a future month" }
        val previousMonth = month.minusMonths(1)
        val selectedExpenses = expensesInMonth(transactions, month, zone)
        val previousExpenses = expensesInMonth(transactions, previousMonth, zone)
        val total = MonthlySpendingCalculator.spent(selectedExpenses, month, zone, "EUR")
        val previousTotal = MonthlySpendingCalculator.spent(previousExpenses, previousMonth, zone, "EUR")
        val categoryNames = categories.associate { it.id to it.name } // Includes inactive historical categories.
        val byCategory = selectedExpenses.groupBy { it.categoryId }
        val categorySpending = byCategory.map { (id, items) ->
            val amount = sum(items)
            CategorySpending(
                categoryId = id,
                name = if (id == null) "Uncategorized" else categoryNames[id] ?: "Category unavailable",
                amount = amount,
                percentage = percent(amount.amountMinor, total.amountMinor) ?: BigDecimal.ZERO,
            )
        }.sortedWith(compareByDescending<CategorySpending> { it.amount.amountMinor }
            .thenBy { it.name }.thenBy { it.categoryId })

        val byDay = selectedExpenses.groupBy { it.occurredAt.atZone(zone).toLocalDate() }
        val daily = (1..month.lengthOfMonth()).map { day ->
            val date = month.atDay(day)
            DailySpending(date, sum(byDay[date].orEmpty()))
        }
        val averageDays = if (month == YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
        val averageMinor = BigDecimal.valueOf(total.amountMinor)
            .divide(BigDecimal.valueOf(averageDays.toLong()), 0, RoundingMode.HALF_UP).longValueExact()
        val largest = selectedExpenses.sortedWith(
            compareByDescending<Transaction> { it.amount.amountMinor }
                .thenByDescending { it.occurredAt }.thenByDescending { it.id },
        ).take(5).map { transaction ->
            LargestExpense(
                transaction,
                transaction.categoryId?.let { categoryNames[it] ?: "Category unavailable" } ?: "Uncategorized",
                transaction.occurredAt.atZone(zone).toLocalDate(),
            )
        }
        val difference = total - previousTotal
        val percentageChange = when {
            previousTotal.amountMinor == 0L && total.amountMinor == 0L -> BigDecimal.ZERO
            previousTotal.amountMinor == 0L -> null
            else -> BigDecimal.valueOf(difference.amountMinor).multiply(BigDecimal(100))
                .divide(BigDecimal.valueOf(previousTotal.amountMinor), 4, RoundingMode.HALF_UP)
        }
        return AnalyticsSummary(
            month = month,
            isCurrentMonth = month == YearMonth.from(today),
            expenseCount = selectedExpenses.size,
            total = total,
            averageDaily = Money(averageMinor, "EUR"),
            averageDays = averageDays,
            categories = categorySpending,
            daily = daily,
            largest = largest,
            comparison = MonthComparison(previousMonth, previousTotal, difference, percentageChange),
        )
    }

    private fun expensesInMonth(transactions: List<Transaction>, month: YearMonth, zone: ZoneId): List<Transaction> {
        val start = month.atDay(1).atStartOfDay(zone).toInstant()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        return transactions.filter {
            TransactionVisibility.inHistory(it) && it.type.countsTowardMonthlyBudget &&
                it.occurredAt >= start && it.occurredAt < end
        }
    }

    private fun sum(items: List<Transaction>): Money = items.fold(Money(0, "EUR")) { total, transaction ->
        require(transaction.amount.amountMinor >= 0L) { "Negative expense amount" }
        total + transaction.amount
    }

    private fun percent(part: Long, whole: Long): BigDecimal? = if (whole == 0L) null else
        BigDecimal.valueOf(part).multiply(BigDecimal(100))
            .divide(BigDecimal.valueOf(whole), 1, RoundingMode.HALF_UP)
}
