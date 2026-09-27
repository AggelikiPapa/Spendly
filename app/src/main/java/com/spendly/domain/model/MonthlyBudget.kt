package com.spendly.domain.model

import java.time.YearMonth

data class MonthlyBudget(
    val yearMonth: YearMonth,
    val limit: Money,
) {
    init {
        require(limit.amountMinor >= 0) { "Monthly budget limit must not be negative" }
    }
}
