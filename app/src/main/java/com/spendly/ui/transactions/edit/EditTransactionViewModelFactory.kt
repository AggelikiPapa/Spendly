package com.spendly.ui.transactions.edit

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository

fun editTransactionViewModelFactory(
    id: Long,
    transactions: TransactionRepository,
    categories: CategoryRepository,
) = viewModelFactory {
    initializer { EditTransactionViewModel(id, transactions, categories) }
}
