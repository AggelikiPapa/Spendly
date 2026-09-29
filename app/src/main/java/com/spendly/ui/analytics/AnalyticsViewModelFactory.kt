package com.spendly.ui.analytics

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository

fun analyticsViewModelFactory(transactions: TransactionRepository, categories: CategoryRepository) = viewModelFactory {
    initializer { AnalyticsViewModel(transactions, categories) }
}
