package com.spendly.ui.transactions

import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionType
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Presentation-only formatting; the stored Money is never changed. */
object MoneyDisplayFormatter {
    fun format(transaction: Transaction, locale: Locale = Locale.getDefault()): String {
        val currency = Currency.getInstance(transaction.amount.currencyCode)
        val fractionDigits = currency.defaultFractionDigits.takeIf { it >= 0 } ?: 2
        val decimal = BigDecimal.valueOf(transaction.amount.amountMinor, fractionDigits).abs()
        val number = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }.format(decimal)
        val prefix = when (transaction.type) {
            TransactionType.EXPENSE -> "-"
            TransactionType.INCOME -> "+"
            TransactionType.TRANSFER -> ""
        }
        return prefix + number
    }
}

