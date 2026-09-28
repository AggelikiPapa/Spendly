package com.spendly.ui.dashboard

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
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
import org.junit.Assert.assertThrows
import org.junit.Test

class BudgetProgressCalculatorTest {
    private val month = YearMonth.of(2026, 9)
    private val zone = ZoneId.of("Europe/Athens")
    private val today = LocalDate.of(2026, 9, 22)

    @Test fun onlyLocalMonthExpensesCountAndIncomeTransfersDoNot() {
        val transactions = listOf(
            transaction(1, 10_000, TransactionType.EXPENSE, "2026-08-31T22:30:00Z"), // Sep 1 locally
            transaction(2, 5_000, TransactionType.EXPENSE, "2026-09-20T10:00:00Z"),
            transaction(3, 200_000, TransactionType.INCOME, "2026-09-20T10:00:00Z"),
            transaction(4, 40_000, TransactionType.TRANSFER, "2026-09-20T10:00:00Z"),
            transaction(5, 9_000, TransactionType.EXPENSE, "2026-09-30T21:30:00Z"), // Oct 1 locally
            transaction(6, 8_000, TransactionType.EXPENSE, "2026-08-30T10:00:00Z"),
        )
        val result = calculate(100_000, transactions)
        assertEquals(Money(15_000, "EUR"), result.spent)
        assertEquals(Money(85_000, "EUR"), result.remaining)
    }

    @Test fun onlyConfirmedWalletExpensesConsumeBudget() {
        val confirmed = transaction(1, 295, TransactionType.EXPENSE)
            .copy(source = TransactionSource.GOOGLE_WALLET)
        val review = transaction(2, 400, TransactionType.EXPENSE)
            .copy(source = TransactionSource.GOOGLE_WALLET, importStatus = ImportStatus.NEEDS_REVIEW)
        val ignored = transaction(3, 600, TransactionType.EXPENSE)
            .copy(source = TransactionSource.GOOGLE_WALLET, importStatus = ImportStatus.IGNORED)
        assertEquals(Money(295, "EUR"), calculate(10_000, listOf(confirmed, review, ignored)).spent)
    }

    @Test fun remainingPercentageAndDailyAllowanceUseMinorUnits() {
        val result = calculate(100_000, listOf(transaction(1, 68_420, TransactionType.EXPENSE)))
        assertEquals(Money(31_580, "EUR"), result.remaining)
        assertEquals(0, BigDecimal("68.42").compareTo(requireNotNull(result.percentageUsed)))
        assertEquals(9, result.daysRemaining)
        assertEquals(Money(3_509, "EUR"), result.recommendedDailySpend)
    }

    @Test fun overBudgetIsNotClampedInternallyButDailyAllowanceIsZero() {
        val result = calculate(100_000, listOf(transaction(1, 125_000, TransactionType.EXPENSE)))
        assertEquals(Money(-25_000, "EUR"), result.remaining)
        assertEquals(0, BigDecimal("125").compareTo(requireNotNull(result.percentageUsed)))
        assertEquals(Money(0, "EUR"), result.recommendedDailySpend)
    }

    @Test fun zeroBudgetHasDefinedPercentageSemantics() {
        val noSpending = calculate(0, emptyList())
        assertEquals(BigDecimal.ZERO, noSpending.percentageUsed)
        assertEquals(Money(0, "EUR"), noSpending.recommendedDailySpend)
        val withSpending = calculate(0, listOf(transaction(1, 100, TransactionType.EXPENSE)))
        assertNull(withSpending.percentageUsed)
        assertEquals(Money(-100, "EUR"), withSpending.remaining)
        assertEquals(Money(0, "EUR"), withSpending.recommendedDailySpend)
    }

    @Test fun finalDayDividesByOne() {
        val result = BudgetProgressCalculator.calculate(
            MonthlyBudget(month, Money(10_000, "EUR")), emptyList(), month,
            LocalDate.of(2026, 9, 30), zone,
        )
        assertEquals(1, result.daysRemaining)
        assertEquals(Money(10_000, "EUR"), result.recommendedDailySpend)
    }

    @Test fun incompatibleCurrencyFailsSafely() {
        assertThrows(IllegalArgumentException::class.java) {
            calculate(100_000, listOf(transaction(1, 100, TransactionType.EXPENSE).copy(amount = Money(100, "USD"))))
        }
    }

    private fun calculate(limit: Long, transactions: List<Transaction>) = BudgetProgressCalculator.calculate(
        MonthlyBudget(month, Money(limit, "EUR")), transactions, month, today, zone,
    )

    private fun transaction(
        id: Long,
        amountMinor: Long,
        type: TransactionType,
        at: String = "2026-09-20T10:00:00Z",
    ): Transaction {
        val instant = Instant.parse(at)
        return Transaction(
            id, Money(amountMinor, "EUR"), type, "Shop", null, 1, instant,
            TransactionSource.MANUAL, ImportStatus.CONFIRMED, null, null, null, instant, instant,
        )
    }
}
