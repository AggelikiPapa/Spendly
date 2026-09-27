package com.spendly.ui.dashboard

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MonthlyBudgetRepository
import com.spendly.domain.repository.TransactionRepository

fun dashboardViewModelFactory(
    transactions: TransactionRepository,
    budgets: MonthlyBudgetRepository,
    categories: CategoryRepository,
) = viewModelFactory {
    initializer { DashboardViewModel(transactions, budgets, categories) }
}
