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
}
