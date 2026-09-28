package com.spendly.ui.review

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository

fun reviewViewModelFactory(transactions: TransactionRepository, categories: CategoryRepository) = viewModelFactory {
    initializer { ReviewViewModel(transactions, categories) }
}
