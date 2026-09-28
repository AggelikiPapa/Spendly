package com.spendly.wallet.importer

import com.spendly.domain.model.Category
import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MerchantCategoryMatcherTest {
    private val rules = FakeRules()
    private val categories = FakeCategories()
    private val matcher = MerchantCategoryMatcher(rules, categories)

    @Test fun exactNormalizedMerchantMatchesActiveCategory() = runTest {
        rules.items = listOf(MerchantCategoryRule(1, "  GPK   MARKET IKE  ", 7))
        categories.items = listOf(Category(7, "Groceries", false, true))

        assertEquals(7L, matcher.categoryIdFor("gpk market ike"))
        assertEquals(7L, matcher.categoryIdFor("  GPK   MARKET IKE "))
    }

    @Test fun normalizationUsesRootLocale() = runTest {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            rules.items = listOf(MerchantCategoryRule(1, "IKEA", 7))
            categories.items = listOf(Category(7, "Home", false, true))
            assertEquals(7L, matcher.categoryIdFor("ikea"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun noRuleDifferentMerchantAndPartialNamesDoNotMatch() = runTest {
        assertNull(matcher.categoryIdFor("WOLT"))
        rules.items = listOf(MerchantCategoryRule(1, "WOLT", 7))
        categories.items = listOf(Category(7, "Food", false, true))
        assertNull(matcher.categoryIdFor("WOLT MARKET"))
        assertNull(matcher.categoryIdFor("MY WOLT"))
        assertNull(matcher.categoryIdFor("WOL"))
        assertNull(matcher.categoryIdFor(null))
        assertNull(matcher.categoryIdFor("   "))
    }

    @Test fun inactiveOrMissingCategoryIsNeverAssigned() = runTest {
        rules.items = listOf(MerchantCategoryRule(1, "WOLT", 7))
        assertNull(matcher.categoryIdFor("WOLT"))
        categories.items = listOf(Category(7, "Food", false, false))
        assertNull(matcher.categoryIdFor("WOLT"))
    }

    @Test fun multipleMatchesUseLowestIdWithAnActiveCategory() = runTest {
        rules.items = listOf(
            MerchantCategoryRule(10, "WOLT", 10),
            MerchantCategoryRule(2, " wolt ", 2),
            MerchantCategoryRule(1, "WOLT", 1),
        )
        categories.items = listOf(
            Category(1, "Inactive", false, false),
            Category(2, "Food", false, true),
            Category(10, "Other", false, true),
        )
        assertEquals(2L, matcher.categoryIdFor("WOLT"))
    }

    private class FakeRules : MerchantCategoryRuleRepository {
        var items = emptyList<MerchantCategoryRule>()
        override suspend fun getAll(): List<MerchantCategoryRule> = items
        override suspend fun insert(rule: MerchantCategoryRule): Long = error("Unused")
        override suspend fun update(rule: MerchantCategoryRule): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): MerchantCategoryRule? = error("Unused")
        override fun observeAll(): Flow<List<MerchantCategoryRule>> = emptyFlow()
    }

    private class FakeCategories : CategoryRepository {
        var items = emptyList<Category>()
        override suspend fun getById(id: Long): Category? = items.firstOrNull { it.id == id }
        override suspend fun insert(category: Category): Long = error("Unused")
        override suspend fun update(category: Category): Int = error("Unused")
        override fun observeAll(): Flow<List<Category>> = emptyFlow()
        override fun observeActive(): Flow<List<Category>> = emptyFlow()
    }
}
