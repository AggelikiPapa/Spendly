package com.spendly.domain.model

import java.util.Locale

/** An amount in the currency's minor units, such as cents for EUR. */
class Money(amountMinor: Long, currencyCode: String) {
    val amountMinor: Long = amountMinor
    val currencyCode: String = currencyCode.trim().uppercase(Locale.ROOT)

    init {
        require(this.currencyCode.isNotBlank()) { "Currency code must not be blank" }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.addExact(amountMinor, other.amountMinor), currencyCode)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.subtractExact(amountMinor, other.amountMinor), currencyCode)
    }

    private fun requireSameCurrency(other: Money) {
        require(currencyCode == other.currencyCode) {
            "Cannot combine $currencyCode and ${other.currencyCode}"
        }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Money && amountMinor == other.amountMinor && currencyCode == other.currencyCode)

    override fun hashCode(): Int = 31 * amountMinor.hashCode() + currencyCode.hashCode()

    override fun toString(): String = "Money(amountMinor=$amountMinor, currencyCode=$currencyCode)"
}
