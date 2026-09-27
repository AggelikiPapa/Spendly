package com.spendly.ui.transactions.add

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository

fun addTransactionViewModelFactory(
    transactions: TransactionRepository,
    categories: CategoryRepository,
) = viewModelFactory {
    initializer { AddTransactionViewModel(transactions, categories) }
}
