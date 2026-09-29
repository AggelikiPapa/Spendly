package com.spendly.ui.analytics

import com.spendly.domain.model.Category
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsCalculatorTest {
    private val september = YearMonth.of(2026, 9)
    private val zone = ZoneId.of("Europe/Athens")
    private val today = LocalDate.of(2026, 9, 10)
    private val categories = listOf(Category(1, "Groceries", true, false), Category(2, "Restaurants", true, true))

    @Test fun onlyConfirmedLocalMonthExpensesCountAndZeroDaysRemain() {
        val rows = listOf(
            transaction(1, 2_340, at = "2026-08-31T22:30:00Z"), // Sep 1 locally
            transaction(2, 7_210, at = "2026-09-02T10:00:00Z"),
            transaction(3, 80_000, type = TransactionType.INCOME),
            transaction(4, 30_000, type = TransactionType.TRANSFER),
            transaction(5, 4_000, status = ImportStatus.NEEDS_REVIEW),
            transaction(6, 5_000, status = ImportStatus.IGNORED),
            transaction(7, 1_000, at = "2026-08-30T10:00:00Z"), // Previous month
            transaction(8, 9_000, at = "2026-09-30T21:30:00Z"), // Oct 1 locally
        )
        val result = calculate(rows)
        assertEquals(Money(9_550, "EUR"), result.total)
        assertEquals(Money(1_000, "EUR"), result.comparison.previousTotal)
        assertEquals(30, result.daily.size)
        assertEquals(Money(2_340, "EUR"), result.daily[0].amount)
        assertEquals(Money(7_210, "EUR"), result.daily[1].amount)
        assertEquals(Money(0, "EUR"), result.daily[2].amount)
        assertEquals(listOf(2L, 1L), result.largest.map { it.transaction.id })
    }

    @Test fun categoriesIncludeInactiveUncategorizedAndMissingWithStableSorting() {
        val rows = listOf(
            transaction(1, 5_000, categoryId = 1),
            transaction(2, 3_000, categoryId = 2),
            transaction(3, 2_000, categoryId = 2),
            transaction(4, 4_000, categoryId = null),
            transaction(5, 1_000, categoryId = 999),
        )
        val result = calculate(rows)
        assertEquals(listOf("Groceries", "Restaurants", "Uncategorized", "Category unavailable"),
            result.categories.map { it.name })
        assertEquals(listOf(5_000L, 5_000L, 4_000L, 1_000L), result.categories.map { it.amount.amountMinor })
        val percentSum = result.categories.fold(BigDecimal.ZERO) { total, row -> total + row.percentage }
        assertTrue(percentSum.subtract(BigDecimal(100)).abs() <= BigDecimal("0.2"))
    }

    @Test fun largestExpensesLimitToFiveAndBreakAmountTiesByMostRecent() {
        val rows = (1L..7L).map { id -> transaction(id, 1_000 + id * 100) } +
            transaction(8, 1_700, at = "2026-09-09T10:00:00Z")
        val result = calculate(rows)
        assertEquals(listOf(8L, 7L, 6L, 5L, 4L), result.largest.map { it.transaction.id })
        assertEquals(5, result.largest.size)
    }

    @Test fun currentAverageUsesElapsedDaysAndRoundsHalfUp() {
        val result = calculate(listOf(transaction(1, 10_005)))
        assertEquals(10, result.averageDays)
        assertTrue(result.isCurrentMonth)
        assertEquals(Money(1_001, "EUR"), result.averageDaily)
    }

    @Test fun completedMonthAndLeapFebruaryUseAllCalendarDays() {
        val august = AnalyticsCalculator.calculate(YearMonth.of(2026, 8), today, zone,
            listOf(transaction(1, 9_300, at = "2026-08-20T10:00:00Z")), categories)
        assertEquals(31, august.averageDays)
        assertTrue(!august.isCurrentMonth)
        assertEquals(Money(300, "EUR"), august.averageDaily)
        val leap = AnalyticsCalculator.calculate(YearMonth.of(2024, 2), LocalDate.of(2024, 3, 1), zone,
            listOf(transaction(1, 2_900, at = "2024-02-29T10:00:00Z")), categories)
        assertEquals(29, leap.averageDays)
        assertEquals(Money(100, "EUR"), leap.averageDaily)
        assertEquals(29, leap.daily.size)
    }

    @Test fun comparisonHandlesIncreaseDecreaseAndZeroPrevious() {
        val increase = calculate(listOf(
            transaction(1, 80_000), transaction(2, 70_000, at = "2026-08-20T10:00:00Z"),
        )).comparison
        assertEquals(Money(10_000, "EUR"), increase.difference)
        assertEquals(0, BigDecimal("14.2857").compareTo(increase.percentageChange))
        val decrease = calculate(listOf(
            transaction(1, 60_000), transaction(2, 80_000, at = "2026-08-20T10:00:00Z"),
        )).comparison
        assertEquals(Money(-20_000, "EUR"), decrease.difference)
        assertEquals(0, BigDecimal("-25").compareTo(decrease.percentageChange))
        val noPrevious = calculate(listOf(transaction(1, 10_000))).comparison
        assertEquals(Money(10_000, "EUR"), noPrevious.difference)
        assertNull(noPrevious.percentageChange)
        assertEquals(BigDecimal.ZERO, calculate(emptyList()).comparison.percentageChange)
    }

    @Test fun emptySelectedMonthHasNoBreakdownOrLargestButKeepsPreviousComparison() {
        val result = calculate(listOf(transaction(1, 1_000, at = "2026-08-20T10:00:00Z")))
        assertTrue(result.isEmpty)
        assertTrue(result.categories.isEmpty())
        assertTrue(result.largest.isEmpty())
        assertEquals(Money(-1_000, "EUR"), result.comparison.difference)
        assertEquals(Money(0, "EUR"), result.averageDaily)
        assertTrue(!calculate(listOf(transaction(1, 0))).isEmpty)
    }

    private fun calculate(rows: List<Transaction>) = AnalyticsCalculator.calculate(september, today, zone, rows, categories)

    private fun transaction(
        id: Long,
        minor: Long,
        type: TransactionType = TransactionType.EXPENSE,
        status: ImportStatus = ImportStatus.CONFIRMED,
        at: String = "2026-09-05T10:00:00Z",
        categoryId: Long? = 1,
    ): Transaction {
        val instant = Instant.parse(at)
        return Transaction(id, Money(minor, "EUR"), type, "Shop", null, categoryId,
            instant, TransactionSource.MANUAL, status, null, null, null, instant, instant)
    }
}
