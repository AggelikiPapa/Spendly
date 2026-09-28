package com.spendly.wallet.importer

import com.spendly.domain.model.Category
import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveMerchantCategoryRuleUseCaseTest {
    private val category = Category(1, "Groceries", true, true)
    private val categories = FakeCategories()
    private val rules = FakeRules()
    private val saver = SaveMerchantCategoryRuleUseCase(rules, categories)

    @Test fun availabilityRequiresWalletMerchantAndActiveCategory() {
        assertTrue(SaveMerchantCategoryRuleUseCase.isAvailable(TransactionSource.GOOGLE_WALLET, "Shop", 1, listOf(category)))
        assertFalse(SaveMerchantCategoryRuleUseCase.isAvailable(TransactionSource.MANUAL, "Shop", 1, listOf(category)))
        assertFalse(SaveMerchantCategoryRuleUseCase.isAvailable(TransactionSource.GOOGLE_WALLET, "  ", 1, listOf(category)))
        assertFalse(SaveMerchantCategoryRuleUseCase.isAvailable(TransactionSource.GOOGLE_WALLET, "Shop", null, listOf(category)))
        assertFalse(SaveMerchantCategoryRuleUseCase.isAvailable(TransactionSource.GOOGLE_WALLET, "Shop", 1, listOf(category.copy(isActive = false))))
    }

    @Test fun createsOnceThenUpdatesAcrossCaseAndWhitespaceDifferences() = runTest {
        saver.save("  GPK   MARKET IKE  ", 1)
        assertEquals("GPK   MARKET IKE", rules.items.value.single().merchantPattern)
        saver.save("gpk market ike", 2)
        assertEquals(1, rules.items.value.size)
        assertEquals(2L, rules.items.value.single().categoryId)
        assertEquals("gpk market ike", rules.items.value.single().merchantPattern)
        assertEquals("gpk market ike", MerchantNormalizer.normalize(" GPK \t MARKET  IKE "))
    }

    @Test fun rejectsInactiveCategoryWithoutWritingRule() = runTest {
        categories.items.value = listOf(category.copy(isActive = false))
        runCatching { saver.save("Shop", 1) }.onSuccess { error("Expected failure") }
        assertTrue(rules.items.value.isEmpty())
    }

    @Test fun deletingRuleStopsFutureMatching() = runTest {
        saver.save("Shop", 1)
        val matcher = MerchantCategoryMatcher(rules, categories)
        assertEquals(1L, matcher.categoryIdFor("shop"))
        rules.deleteById(rules.items.value.single().id)
        assertEquals(null, matcher.categoryIdFor("SHOP"))
    }

    private class FakeCategories : CategoryRepository {
        val items = MutableStateFlow(listOf(Category(1, "Groceries", true, true), Category(2, "Dining", false, true)))
        override suspend fun insert(category: Category): Long = error("Unused")
        override suspend fun update(category: Category): Int = error("Unused")
        override suspend fun getById(id: Long) = items.value.firstOrNull { it.id == id }
        override fun observeActive(): Flow<List<Category>> = items
        override fun observeAll(): Flow<List<Category>> = items
    }

    private class FakeRules : MerchantCategoryRuleRepository {
        val items = MutableStateFlow<List<MerchantCategoryRule>>(emptyList())
        override suspend fun insert(rule: MerchantCategoryRule): Long {
            val id = (items.value.maxOfOrNull { it.id } ?: 0) + 1
            items.value += rule.copy(id = id)
            return id
        }
        override suspend fun update(rule: MerchantCategoryRule): Int {
            if (items.value.none { it.id == rule.id }) return 0
            items.value = items.value.map { if (it.id == rule.id) rule else it }
            return 1
        }
        override suspend fun deleteById(id: Long): Int {
            items.value = items.value.filterNot { it.id == id }
            return 1
        }
        override suspend fun getById(id: Long) = items.value.firstOrNull { it.id == id }
        override suspend fun getAll() = items.value
        override fun observeAll(): Flow<List<MerchantCategoryRule>> = items
    }
}
