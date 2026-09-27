package com.spendly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.spendly.data.local.entity.MonthlyBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Upsert
    suspend fun upsert(budget: MonthlyBudgetEntity)

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    suspend fun getByMonth(yearMonth: String): MonthlyBudgetEntity?

    @Query("SELECT * FROM monthly_budgets WHERE yearMonth = :yearMonth")
    fun observeByMonth(yearMonth: String): Flow<MonthlyBudgetEntity?>
}
