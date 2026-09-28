package com.spendly.domain

import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionVisibility
import java.time.YearMonth
import java.time.ZoneId

/** The spending definition shared by Dashboard and budget alerts. */
object MonthlySpendingCalculator {
    fun spent(transactions: List<Transaction>, month: YearMonth, zone: ZoneId, currencyCode: String): Money {
        val start = month.atDay(1).atStartOfDay(zone).toInstant()
        val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
        return transactions.asSequence()
            .filter { TransactionVisibility.inHistory(it) && it.type.countsTowardMonthlyBudget && it.occurredAt >= start && it.occurredAt < end }
            .fold(Money(0, currencyCode)) { total, transaction ->
                require(transaction.amount.amountMinor >= 0) { "Negative expense amount" }
                total + transaction.amount
            }
    }
}
