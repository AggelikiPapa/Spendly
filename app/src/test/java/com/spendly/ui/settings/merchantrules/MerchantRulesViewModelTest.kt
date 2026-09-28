package com.spendly.ui.settings.merchantrules

import com.spendly.domain.model.Category
import com.spendly.domain.model.MerchantCategoryRule
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MerchantCategoryRuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MerchantRulesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val rules = FakeRules()
    private val categories = FakeCategories()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun joinsCategoriesOnceAndHandlesInactiveAndMissingReferences() = runTest {
        rules.items.value = listOf(MerchantCategoryRule(1, "Shop", 1), MerchantCategoryRule(2, "Other", 99))
        val vm = MerchantRulesViewModel(rules, categories)
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.rows.size)
        assertEquals("Groceries", vm.uiState.value.rows.first { it.rule.id == 1L }.categoryName)
        assertTrue(vm.uiState.value.rows.first { it.rule.id == 1L }.isCategoryInactive)
        assertNull(vm.uiState.value.rows.first { it.rule.id == 2L }.categoryName)
        assertEquals(0, categories.getByIdCalls)
        vm.openEditor(1)
        assertNull(vm.uiState.value.editor?.categoryId)
        vm.selectCategory(2)
        vm.saveEditor()
        advanceUntilIdle()
        assertEquals(2L, rules.items.value.first { it.id == 1L }.categoryId)
        assertEquals("Dining", vm.uiState.value.rows.first { it.rule.id == 1L }.categoryName)
    }

    @Test fun deletionRequiresConfirmationAndListUpdatesReactivelyWithoutChangingTransactions() = runTest {
        rules.items.value = listOf(MerchantCategoryRule(1, "Shop", 2))
        val vm = MerchantRulesViewModel(rules, categories)
        advanceUntilIdle()
        vm.confirmDelete()
        assertEquals(1, rules.items.value.size)
        vm.requestDelete(1)
        vm.cancelDelete()
        vm.confirmDelete()
        assertEquals(1, rules.items.value.size)
        vm.requestDelete(1)
        vm.confirmDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(1, rules.deleteCalls)
        assertTrue(vm.uiState.value.rows.isEmpty())
        assertFalse(vm.uiState.value.isSaving)
    }

    private class FakeRules : MerchantCategoryRuleRepository {
        val items = MutableStateFlow<List<MerchantCategoryRule>>(emptyList())
        var deleteCalls = 0
        override suspend fun insert(rule: MerchantCategoryRule): Long = error("Unused")
        override suspend fun update(rule: MerchantCategoryRule): Int {
            if (items.value.none { it.id == rule.id }) return 0
            items.value = items.value.map { if (it.id == rule.id) rule else it }
            return 1
        }
        override suspend fun deleteById(id: Long): Int {
            deleteCalls++
            if (items.value.none { it.id == id }) return 0
            items.value = items.value.filterNot { it.id == id }
            return 1
        }
        override suspend fun getById(id: Long) = items.value.firstOrNull { it.id == id }
        override suspend fun getAll() = items.value
        override fun observeAll(): Flow<List<MerchantCategoryRule>> = items
    }

    private class FakeCategories : CategoryRepository {
        val items = MutableStateFlow(listOf(Category(1, "Groceries", true, false), Category(2, "Dining", false, true)))
        var getByIdCalls = 0
        override suspend fun insert(category: Category): Long = error("Unused")
        override suspend fun update(category: Category): Int = error("Unused")
        override suspend fun getById(id: Long): Category? { getByIdCalls++; return items.value.firstOrNull { it.id == id } }
        override fun observeActive(): Flow<List<Category>> = items
        override fun observeAll(): Flow<List<Category>> = items
    }
}
