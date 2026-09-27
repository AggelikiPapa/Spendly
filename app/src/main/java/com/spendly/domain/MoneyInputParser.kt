package com.spendly.domain

import com.spendly.domain.model.Money
import java.math.BigDecimal

/** Converts decimal amounts to minor units without floating-point arithmetic. */
object MoneyInputParser {
    fun parsePositive(input: String, currencyCode: String, fractionDigits: Int): Money? =
        parse(input, currencyCode, fractionDigits, allowZero = false)

    fun parseNonNegative(input: String, currencyCode: String, fractionDigits: Int): Money? =
        parse(input, currencyCode, fractionDigits, allowZero = true)

    private fun parse(input: String, currencyCode: String, fractionDigits: Int, allowZero: Boolean): Money? {
        require(fractionDigits >= 0) { "Fraction digits must not be negative" }
        val text = input.trim()
        val pattern = if (fractionDigits == 0) {
            Regex("[0-9]+")
        } else {
            Regex("[0-9]+(?:[.,][0-9]{1,$fractionDigits})?")
        }
        if (!pattern.matches(text)) return null

        val minor = try {
            BigDecimal(text.replace(',', '.')).movePointRight(fractionDigits).longValueExact()
        } catch (_: ArithmeticException) {
            return null
        }
        return if (minor > 0 || (allowZero && minor == 0L)) Money(minor, currencyCode) else null
    }
}
