package com.spendly.ui.transactions.edit

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import com.spendly.wallet.importer.SaveMerchantCategoryRuleUseCase

fun editTransactionViewModelFactory(
    id: Long,
    transactions: TransactionRepository,
    categories: CategoryRepository,
    saveMerchantRule: SaveMerchantCategoryRuleUseCase,
) = viewModelFactory {
    initializer { EditTransactionViewModel(id, transactions, categories, saveMerchantRule = saveMerchantRule) }
}
