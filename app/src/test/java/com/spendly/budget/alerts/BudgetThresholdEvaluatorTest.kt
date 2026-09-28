package com.spendly.budget.alerts

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetThresholdEvaluatorTest {
    private val month = YearMonth.of(2026, 9)
    private val budget = MonthlyBudget(month, Money(10_000, "EUR"))
    private val zone = ZoneOffset.UTC

    @Test fun exactThresholdCrossings() {
        val cases = listOf(
            Triple(6_900L, 7_100L, BudgetThreshold.SEVENTY),
            Triple(7_900L, 8_000L, BudgetThreshold.EIGHTY),
            Triple(8_900L, 9_100L, BudgetThreshold.NINETY),
            Triple(9_900L, 10_000L, BudgetThreshold.ONE_HUNDRED),
        )
        cases.forEach { (before, after, threshold) ->
            val previouslyDelivered = BudgetThreshold.entries.filter { it.percent < threshold.percent }.toSet()
            assertNull(evaluate(budget, listOf(transaction(before)), previouslyDelivered))
            assertEquals(threshold, evaluate(budget, listOf(transaction(after)), previouslyDelivered)?.highest)
        }
    }

    @Test fun largeJumpSelectsHighestAndMarksEveryCrossedThreshold() {
        val decision = evaluate(budget, listOf(transaction(9_200)), emptySet())!!
        assertEquals(BudgetThreshold.NINETY, decision.highest)
        assertEquals(setOf(BudgetThreshold.SEVENTY, BudgetThreshold.EIGHTY, BudgetThreshold.NINETY), decision.newlyCrossed)
        assertEquals("92", decision.percentageUsed?.toPlainString())
    }

    @Test fun deliveredThresholdNeverRepeatsEvenAfterSpendingFallsAndRises() {
        val delivered = setOf(BudgetThreshold.SEVENTY, BudgetThreshold.EIGHTY)
        assertNull(evaluate(budget, listOf(transaction(6_500)), delivered))
        assertNull(evaluate(budget, listOf(transaction(8_200)), delivered))
    }

    @Test fun onlyConfirmedExpensesInLocalMonthCountRegardlessOfSource() {
        val excluded = listOf(
            transaction(10_000, type = TransactionType.INCOME),
            transaction(10_000, type = TransactionType.TRANSFER),
            transaction(10_000, status = ImportStatus.NEEDS_REVIEW),
            transaction(10_000, status = ImportStatus.IGNORED),
            transaction(10_000, at = Instant.parse("2026-10-01T00:00:00Z")),
        )
        assertNull(evaluate(budget, excluded, emptySet()))
        assertEquals(BudgetThreshold.SEVENTY, evaluate(budget, excluded + transaction(7_100), emptySet())?.highest)
        assertEquals(BudgetThreshold.SEVENTY, evaluate(budget, excluded + transaction(7_100, source = TransactionSource.GOOGLE_WALLET), emptySet())?.highest)
        assertEquals(BudgetThreshold.SEVENTY, evaluate(budget, excluded + transaction(7_100, source = TransactionSource.GOOGLE_WALLET, status = ImportStatus.CONFIRMED), emptySet())?.highest)
    }

    @Test fun reviewConfirmationCanCrossThreshold() {
        assertNull(evaluate(budget, listOf(transaction(7_100, status = ImportStatus.NEEDS_REVIEW)), emptySet()))
        assertEquals(BudgetThreshold.SEVENTY, evaluate(budget, listOf(transaction(7_100, status = ImportStatus.CONFIRMED)), emptySet())?.highest)
    }

    @Test fun changingBudgetCanCrossUndeliveredThreshold() {
        val spending = listOf(transaction(7_000))
        val delivered = setOf(BudgetThreshold.SEVENTY)
        assertNull(evaluate(budget, spending, delivered))
        assertEquals(BudgetThreshold.EIGHTY, evaluate(MonthlyBudget(month, Money(8_000, "EUR")), spending, delivered)?.highest)
    }

    @Test fun noBudgetAndZeroBudgetAreSafe() {
        assertNull(evaluate(null, listOf(transaction(7_000)), emptySet()))
        val zero = MonthlyBudget(month, Money(0, "EUR"))
        assertNull(evaluate(zero, emptyList(), emptySet()))
        val decision = evaluate(zero, listOf(transaction(1)), emptySet())!!
        assertEquals(BudgetThreshold.ONE_HUNDRED, decision.highest)
        assertEquals(setOf(BudgetThreshold.ONE_HUNDRED), decision.newlyCrossed)
        assertNull(evaluate(zero, listOf(transaction(1)), decision.newlyCrossed))
    }

    @Test fun preciseCentBoundaryUsesDecimalArithmetic() {
        assertNull(evaluate(budget, listOf(transaction(7_999)), setOf(BudgetThreshold.SEVENTY)))
        assertEquals(BudgetThreshold.EIGHTY, evaluate(budget, listOf(transaction(8_000)), setOf(BudgetThreshold.SEVENTY))?.highest)
    }

    private fun evaluate(budget: MonthlyBudget?, transactions: List<Transaction>, delivered: Set<BudgetThreshold>) =
        BudgetThresholdEvaluator.evaluate(month, budget, transactions, delivered, zone)

    private fun transaction(
        amountMinor: Long,
        type: TransactionType = TransactionType.EXPENSE,
        status: ImportStatus = ImportStatus.CONFIRMED,
        source: TransactionSource = TransactionSource.MANUAL,
        at: Instant = Instant.parse("2026-09-15T12:00:00Z"),
    ) = Transaction(0, Money(amountMinor, "EUR"), type, "Shop", null, null, at, source, status,
        null, null, null, at, at)
}
