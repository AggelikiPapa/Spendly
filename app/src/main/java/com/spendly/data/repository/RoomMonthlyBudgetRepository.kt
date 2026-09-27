package com.spendly.data.repository

import com.spendly.data.local.dao.MonthlyBudgetDao
import com.spendly.data.local.mapper.toDomain
import com.spendly.data.local.mapper.toEntity
import com.spendly.domain.model.MonthlyBudget
import com.spendly.domain.repository.MonthlyBudgetRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.map

class RoomMonthlyBudgetRepository(private val dao: MonthlyBudgetDao) : MonthlyBudgetRepository {
    override suspend fun upsert(budget: MonthlyBudget) = dao.upsert(budget.toEntity())
    override suspend fun getByMonth(yearMonth: YearMonth) = dao.getByMonth(yearMonth.toString())?.toDomain()
    override fun observeByMonth(yearMonth: YearMonth) =
        dao.observeByMonth(yearMonth.toString()).map { it?.toDomain() }
}
