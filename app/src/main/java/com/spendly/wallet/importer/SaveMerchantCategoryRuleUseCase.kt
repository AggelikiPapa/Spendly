package com.spendly.wallet.importer

import com.spendly.domain.model.Category
import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shared by Wallet edit and Review. A single application instance serializes local rule writes. */
class SaveMerchantCategoryRuleUseCase(
    private val rules: MerchantCategoryRuleRepository,
    private val categories: CategoryRepository,
) {
    private val mutex = Mutex()

    suspend fun save(merchant: String, categoryId: Long) = mutex.withLock {
        val normalized = requireNotNull(MerchantNormalizer.normalize(merchant)) { "Merchant is required" }
        require(categories.getById(categoryId)?.isActive == true) { "An active category is required" }
        val existing = rules.getAll().sortedBy { it.id }
            .firstOrNull { MerchantNormalizer.normalize(it.merchantPattern) == normalized }
        if (existing == null) {
            rules.insert(MerchantCategoryRule(0, merchant.trim(), categoryId))
        } else if (rules.update(existing.copy(merchantPattern = merchant.trim(), categoryId = categoryId)) == 0) {
            throw IllegalStateException("Merchant rule no longer exists")
        }
    }

    companion object {
        fun isAvailable(source: TransactionSource, merchant: String, categoryId: Long?, activeCategories: List<Category>): Boolean =
            source == TransactionSource.GOOGLE_WALLET && MerchantNormalizer.normalize(merchant) != null &&
                activeCategories.any { it.id == categoryId && it.isActive }
    }
}
