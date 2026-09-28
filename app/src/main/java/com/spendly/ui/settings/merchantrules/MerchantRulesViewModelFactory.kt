package com.spendly.ui.settings.merchantrules

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository

fun merchantRulesViewModelFactory(rules: MerchantCategoryRuleRepository, categories: CategoryRepository) = viewModelFactory {
    initializer { MerchantRulesViewModel(rules, categories) }
}
