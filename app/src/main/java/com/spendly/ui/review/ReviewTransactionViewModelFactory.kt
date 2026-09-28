package com.spendly.ui.review

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import com.spendly.wallet.importer.SaveMerchantCategoryRuleUseCase

fun reviewTransactionViewModelFactory(
    id: Long,
    transactions: TransactionRepository,
    categories: CategoryRepository,
    saveMerchantRule: SaveMerchantCategoryRuleUseCase,
) = viewModelFactory {
    initializer { ReviewTransactionViewModel(id, transactions, categories, saveMerchantRule = saveMerchantRule) }
}
