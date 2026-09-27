package com.spendly.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {
    @Test
    fun addsAmountsInTheSameCurrency() {
        val result = Money(1_234, "EUR") + Money(-34, "eur")

        assertEquals(Money(1_200, "EUR"), result)
    }

    @Test
    fun subtractsAmountsInTheSameCurrency() {
        val result = Money(1_234, "EUR") - Money(34, "EUR")

        assertEquals(Money(1_200, "EUR"), result)
    }

    @Test
    fun rejectsArithmeticAcrossCurrencies() {
        val euros = Money(100, "EUR")
        val dollars = Money(100, "USD")

        assertThrows(IllegalArgumentException::class.java) { euros + dollars }
        assertThrows(IllegalArgumentException::class.java) { euros - dollars }
    }

    @Test
    fun normalizesCurrencyCodesWithoutDependingOnTheSystemLocale() {
        assertEquals("EUR", Money(100, " eur ").currencyCode)
    }

    @Test
    fun rejectsBlankCurrencyCodes() {
        assertThrows(IllegalArgumentException::class.java) { Money(0, "  ") }
    }

    @Test
    fun rejectsMinorUnitOverflow() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE, "EUR") + Money(1, "EUR")
        }
    }
}
