package com.spendly.domain

import com.spendly.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyInputParserTest {
    @Test
    fun wholeAndDecimalAmountsConvertToMinorUnits() {
        assertEquals(Money(1_200, "EUR"), MoneyInputParser.parsePositive("12", "EUR", 2))
        assertEquals(Money(1_250, "EUR"), MoneyInputParser.parsePositive("12.5", "EUR", 2))
        assertEquals(Money(1_250, "EUR"), MoneyInputParser.parsePositive("12.50", "EUR", 2))
        assertEquals(Money(1_250, "EUR"), MoneyInputParser.parsePositive("12,50", "EUR", 2))
    }

    @Test
    fun invalidZeroNegativeAndOverflowAmountsAreRejected() {
        listOf("", "abc", "12.345", "1.2.3", "0", "0.00", "-12", "999999999999999999999")
            .forEach { assertNull(MoneyInputParser.parsePositive(it, "EUR", 2)) }
    }

    @Test
    fun nonNegativeBudgetAmountsAllowZeroButRejectNegativeAndInvalidInput() {
        assertEquals(Money(0, "EUR"), MoneyInputParser.parseNonNegative("0.00", "EUR", 2))
        assertEquals(Money(100_000, "EUR"), MoneyInputParser.parseNonNegative("1000", "EUR", 2))
        assertEquals(Money(100_050, "EUR"), MoneyInputParser.parseNonNegative("1000.50", "EUR", 2))
        listOf("", "-1", "1000.123", "abc").forEach {
            assertNull(MoneyInputParser.parseNonNegative(it, "EUR", 2))
        }
    }
}
