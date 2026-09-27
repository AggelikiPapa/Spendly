package com.spendly.domain.repository

import com.spendly.domain.model.MonthlyBudget
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface MonthlyBudgetRepository {
    suspend fun upsert(budget: MonthlyBudget)
    suspend fun getByMonth(yearMonth: YearMonth): MonthlyBudget?
    fun observeByMonth(yearMonth: YearMonth): Flow<MonthlyBudget?>
}
