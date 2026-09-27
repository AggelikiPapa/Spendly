package com.spendly.domain.model

enum class TransactionType(val countsTowardMonthlyBudget: Boolean) {
    EXPENSE(true),
    INCOME(false),
    TRANSFER(false),
}
