package com.spendly.ui.review

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository

fun reviewTransactionViewModelFactory(
    id: Long,
    transactions: TransactionRepository,
    categories: CategoryRepository,
) = viewModelFactory {
    initializer { ReviewTransactionViewModel(id, transactions, categories) }
}
