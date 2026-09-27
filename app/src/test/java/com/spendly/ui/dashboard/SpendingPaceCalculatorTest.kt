package com.spendly.ui.dashboard

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SpendingPaceCalculatorTest {
    private val zone = ZoneId.of("Europe/Athens")

    @Test fun elapsedDaysIncludeTodayAndRoundExpectedToCentsHalfUp() {
        val pace = progress(YearMonth.of(2026, 9), 10, 100_000, 60_000).spendingPace
        assertEquals(Money(33_333, "EUR"), pace.expectedSpentByToday)
        assertEquals(Money(26_667, "EUR"), pace.paceDifference)
        assertEquals(SpendingPaceStatus.ABOVE_PACE, pace.status)
    }

    @Test fun belowPaceRetainsNegativeDifference() {
        val pace = progress(YearMonth.of(2026, 9), 10, 100_000, 25_000).spendingPace
        assertEquals(Money(-8_333, "EUR"), pace.paceDifference)
        assertEquals(SpendingPaceStatus.BELOW_PACE, pace.status)
    }

    @Test fun fivePercentOrFiveEuroToleranceIsInclusive() {
        // Day 10 of September: expected €333.33, so 5% is €16.6665.
        val month = YearMonth.of(2026, 9)
        assertEquals(SpendingPaceStatus.ON_PACE, progress(month, 10, 100_000, 34_999).spendingPace.status)
        assertEquals(SpendingPaceStatus.ABOVE_PACE, progress(month, 10, 100_000, 35_000).spendingPace.status)
        assertEquals(SpendingPaceStatus.ON_PACE, progress(month, 10, 100_000, 31_667).spendingPace.status)
        assertEquals(SpendingPaceStatus.BELOW_PACE, progress(month, 10, 100_000, 31_666).spendingPace.status)

        // Expected €10 on day 3 of a 30-day month, so the €5 floor applies.
        assertEquals(SpendingPaceStatus.ON_PACE, progress(month, 3, 10_000, 1_500).spendingPace.status)
        assertEquals(SpendingPaceStatus.ABOVE_PACE, progress(month, 3, 10_000, 1_501).spendingPace.status)
    }

    @Test fun februaryAndLeapYearFebruaryUseTheirActualLengths() {
        assertEquals(
            Money(14_000, "EUR"),
            progress(YearMonth.of(2025, 2), 14, 28_000, 0).spendingPace.expectedSpentByToday,
        )
        assertEquals(
            Money(14_000, "EUR"),
            progress(YearMonth.of(2024, 2), 14, 29_000, 0).spendingPace.expectedSpentByToday,
        )
    }

    @Test fun firstAndLastDayUseCorrectElapsedDays() {
        val month = YearMonth.of(2026, 9)
        assertEquals(Money(3_333, "EUR"), progress(month, 1, 100_000, 0).spendingPace.expectedSpentByToday)
        assertEquals(Money(100_000, "EUR"), progress(month, 30, 100_000, 0).spendingPace.expectedSpentByToday)
    }

    @Test fun exactHalfCentRoundsUp() {
        val expected = progress(YearMonth.of(2026, 9), 15, 1, 0).spendingPace.expectedSpentByToday
        assertEquals(Money(1, "EUR"), expected)
    }

    @Test fun zeroBudgetHasNeutralZeroStateAndPositiveSpendingIsAbovePace() {
        val month = YearMonth.of(2026, 9)
        val zero = progress(month, 10, 0, 0).spendingPace
        assertEquals(Money(0, "EUR"), zero.expectedSpentByToday)
        assertEquals(SpendingPaceStatus.ON_PACE, zero.status)
        val positive = progress(month, 10, 0, 1).spendingPace
        assertEquals(Money(1, "EUR"), positive.paceDifference)
        assertEquals(SpendingPaceStatus.ABOVE_PACE, positive.status)
    }

    @Test fun overTotalBudgetStillShowsUncappedAbovePace() {
        val pace = progress(YearMonth.of(2026, 9), 24, 100_000, 110_000).spendingPace
        assertEquals(Money(80_000, "EUR"), pace.expectedSpentByToday)
        assertEquals(Money(30_000, "EUR"), pace.paceDifference)
        assertEquals(SpendingPaceStatus.ABOVE_PACE, pace.status)
    }

    @Test fun incomeAndTransferDoNotAffectActualPace() {
        val month = YearMonth.of(2026, 9)
        val today = LocalDate.of(2026, 9, 10)
        val instant = today.atTime(12, 0).atZone(zone).toInstant()
        val transactions = listOf(
            transaction(1, 33_333, TransactionType.EXPENSE, instant),
            transaction(2, 200_000, TransactionType.INCOME, instant),
            transaction(3, 40_000, TransactionType.TRANSFER, instant),
        )
        val result = BudgetProgressCalculator.calculate(
            MonthlyBudget(month, Money(100_000, "EUR")), transactions, month, today, zone,
        )
        assertEquals(Money(33_333, "EUR"), result.spent)
        assertEquals(Money(0, "EUR"), result.spendingPace.paceDifference)
        assertEquals(SpendingPaceStatus.ON_PACE, result.spendingPace.status)
    }

    private fun progress(month: YearMonth, day: Int, limit: Long, spent: Long): BudgetProgress {
        val today = month.atDay(day)
        val transactions = if (spent == 0L) emptyList() else listOf(
            transaction(1, spent, TransactionType.EXPENSE, today.atTime(12, 0).atZone(zone).toInstant()),
        )
        return BudgetProgressCalculator.calculate(
            MonthlyBudget(month, Money(limit, "EUR")), transactions, month, today, zone,
        )
    }

    private fun transaction(id: Long, minor: Long, type: TransactionType, at: Instant) = Transaction(
        id, Money(minor, "EUR"), type, "Shop", null, 1, at,
        TransactionSource.MANUAL, ImportStatus.CONFIRMED, null, null, null, at, at,
    )
}
