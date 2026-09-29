package com.spendly.budget.alerts

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpendingPaceAlertEvaluatorTest {
    private val month = YearMonth.of(2026, 9)
    private val today = LocalDate.of(2026, 9, 15)
    private val budget = MonthlyBudget(month, Money(10_000, "EUR"))

    @Test fun abovePaceUsesExactDashboardToleranceAndDifference() {
        // Day 15 expects 50 EUR; the 5 EUR floor is inclusive.
        assertNull(evaluate(budget, listOf(expense(5_500))))
        assertEquals(Money(501, "EUR"), evaluate(budget, listOf(expense(5_501)))?.aheadOfTarget)
        assertEquals(Money(1_000, "EUR"), evaluate(budget, listOf(expense(6_000)))?.aheadOfTarget)
        assertNull(evaluate(budget, listOf(expense(4_000))))
    }

    @Test fun manualAndWalletConfirmedExpensesCountButReviewAndIgnoredDoNot() {
        assertEquals(Money(1_000, "EUR"), evaluate(budget, listOf(expense(6_000)))?.aheadOfTarget)
        assertEquals(Money(1_000, "EUR"), evaluate(budget, listOf(expense(6_000, source = TransactionSource.GOOGLE_WALLET)))?.aheadOfTarget)
        assertNull(evaluate(budget, listOf(expense(6_000, status = ImportStatus.NEEDS_REVIEW))))
        assertNull(evaluate(budget, listOf(expense(6_000, status = ImportStatus.IGNORED))))
        assertEquals(Money(1_000, "EUR"), evaluate(budget, listOf(expense(6_000, status = ImportStatus.CONFIRMED)))?.aheadOfTarget)
    }

    @Test fun budgetEditAndReviewConfirmationCanMovePaceAboveTarget() {
        assertNull(evaluate(budget, listOf(expense(5_000))))
        assertEquals(Money(1_000, "EUR"), evaluate(MonthlyBudget(month, Money(8_000, "EUR")), listOf(expense(5_000)))?.aheadOfTarget)
        assertNull(evaluate(budget, listOf(expense(6_000, status = ImportStatus.NEEDS_REVIEW))))
        assertEquals(Money(1_000, "EUR"), evaluate(budget, listOf(expense(6_000, status = ImportStatus.CONFIRMED)))?.aheadOfTarget)
    }

    @Test fun noBudgetAndZeroBudgetProduceNoSeparatePaceAlert() {
        assertNull(evaluate(null, listOf(expense(6_000))))
        val zero = MonthlyBudget(month, Money(0, "EUR"))
        assertNull(evaluate(zero, emptyList()))
        assertNull(evaluate(zero, listOf(expense(1))))
    }

    private fun evaluate(budget: MonthlyBudget?, transactions: List<Transaction>) =
        SpendingPaceAlertEvaluator.evaluate(month, today, budget, transactions, ZoneOffset.UTC)

    private fun expense(amountMinor: Long, source: TransactionSource = TransactionSource.MANUAL,
                        status: ImportStatus = ImportStatus.CONFIRMED): Transaction {
        val at = Instant.parse("2026-09-15T12:00:00Z")
        return Transaction(0, Money(amountMinor, "EUR"), TransactionType.EXPENSE, "Shop", null, null,
            at, source, status, null, null, null, at, at)
    }
}
