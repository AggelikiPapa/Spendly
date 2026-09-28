package com.spendly.wallet.importer

import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository

/** merchantPattern is treated as normalized exact merchant text, not a substring or regex. */
class MerchantCategoryMatcher(
    private val rules: MerchantCategoryRuleRepository,
    private val categories: CategoryRepository,
) {
    suspend fun categoryIdFor(merchant: String?): Long? {
        val normalized = MerchantNormalizer.normalize(merchant) ?: return null
        for (rule in rules.getAll().sortedBy { it.id }) {
            if (MerchantNormalizer.normalize(rule.merchantPattern) != normalized) continue
            if (categories.getById(rule.categoryId)?.isActive == true) return rule.categoryId
        }
        return null
    }
}
