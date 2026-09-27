package com.spendly.ui.settings.budget

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.MonthlyBudgetRepository

fun monthlyBudgetViewModelFactory(repository: MonthlyBudgetRepository) = viewModelFactory {
    initializer { MonthlyBudgetViewModel(repository) }
}
