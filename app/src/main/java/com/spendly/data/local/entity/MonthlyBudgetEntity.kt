package com.spendly.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_budgets")
data class MonthlyBudgetEntity(
    @PrimaryKey val yearMonth: String,
    val limitMinor: Long,
    val currencyCode: String,
)
