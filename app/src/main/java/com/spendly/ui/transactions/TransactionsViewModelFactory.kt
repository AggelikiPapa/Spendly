package com.spendly.ui.transactions

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository

fun transactionsViewModelFactory(
    transactions: TransactionRepository,
    categories: CategoryRepository,
) = viewModelFactory {
    initializer { TransactionsViewModel(transactions, categories) }
}
