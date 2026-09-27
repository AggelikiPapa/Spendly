package com.spendly.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTypeTest {
    @Test
    fun expensesConsumeTheMonthlyBudget() {
        assertTrue(TransactionType.EXPENSE.countsTowardMonthlyBudget)
    }

    @Test
    fun incomeDoesNotConsumeTheMonthlyBudget() {
        assertFalse(TransactionType.INCOME.countsTowardMonthlyBudget)
    }

    @Test
    fun transfersDoNotConsumeTheMonthlyBudget() {
        assertFalse(TransactionType.TRANSFER.countsTowardMonthlyBudget)
    }
}
