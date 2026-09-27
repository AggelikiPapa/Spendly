package com.spendly.ui.settings.categories

import com.spendly.domain.model.Category
import com.spendly.domain.repository.CategoryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagementViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeCategories()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun exposesAllCategoriesAndReactsToChanges() = runTest {
        val vm = CategoryManagementViewModel(repository)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(2, vm.uiState.value.categories.size)
        assertTrue(vm.uiState.value.categories.first { it.id == 1L }.isActive)
        assertFalse(vm.uiState.value.categories.first { it.id == 2L }.isActive)

        repository.items.value = repository.items.value + Category(3, "Travel", false, true)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.categories.size)
    }

    @Test fun creationWaitsForCategoryListToLoad() = runTest {
        val vm = CategoryManagementViewModel(repository)
        vm.startAdd()
        assertNull(vm.uiState.value.editor)
        advanceUntilIdle()
        vm.startAdd()
        assertNotNull(vm.uiState.value.editor)
    }

    @Test fun blankWhitespaceAndCaseInsensitiveDuplicatesAreRejected() = runTest {
        val vm = CategoryManagementViewModel(repository)
        advanceUntilIdle()
        vm.startAdd()
        for (input in listOf("", "   ", "Groceries", "  gRoCeRiEs ", " archived ")) {
            vm.changeName(input)
            vm.saveEditor()
            assertNotNull(vm.uiState.value.editor?.nameError)
        }
        assertTrue(repository.inserted.isEmpty())
    }

    @Test fun validCustomCategoryIsTrimmedInsertedAndReactive() = runTest {
        val vm = CategoryManagementViewModel(repository)
        advanceUntilIdle()
        vm.startAdd()
        vm.changeName("  Travel  ")
        vm.saveEditor()
        advanceUntilIdle()

        val inserted = repository.inserted.single()
        assertEquals("Travel", inserted.name)
        assertFalse(inserted.isSystem)
        assertTrue(inserted.isActive)
        assertNull(vm.uiState.value.editor)
        assertTrue(vm.uiState.value.categories.any { it.name == "Travel" })
    }

    @Test fun customCategoryCanBeRenamedButSystemCategoryCannot() = runTest {
        val vm = CategoryManagementViewModel(repository)
        advanceUntilIdle()
        vm.startRename(1)
        assertNull(vm.uiState.value.editor)
        vm.startRename(2)
        vm.changeName("  New name  ")
        vm.saveEditor()
        advanceUntilIdle()
        assertEquals("New name", repository.updated.single().name)
        assertEquals(2L, repository.updated.single().id)
        assertEquals("New name", vm.uiState.value.categories.first { it.id == 2L }.name)
    }

    @Test fun deactivationAndReactivationPreserveSystemIdentity() = runTest {
        val vm = CategoryManagementViewModel(repository)
        advanceUntilIdle()
        vm.toggleActive(1)
        advanceUntilIdle()
        assertFalse(repository.updated.last().isActive)
        assertTrue(repository.updated.last().isSystem)
        assertFalse(vm.uiState.value.categories.first { it.id == 1L }.isActive)

        vm.toggleActive(1)
        advanceUntilIdle()
        assertTrue(repository.updated.last().isActive)
        assertTrue(repository.updated.last().isSystem)
        assertTrue(vm.uiState.value.categories.first { it.id == 1L }.isActive)
    }

    @Test fun insertAndToggleFailuresShowNontechnicalErrors() = runTest {
        val vm = CategoryManagementViewModel(repository)
        advanceUntilIdle()
        repository.failure = IllegalStateException("database internals")
        vm.startAdd()
        vm.changeName("Travel")
        vm.saveEditor()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.editor!!.isSaving)
        assertNotNull(vm.uiState.value.editor!!.saveError)
        assertFalse(vm.uiState.value.editor!!.saveError!!.contains("database internals"))
        vm.dismissEditor()

        vm.toggleActive(1)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.operationError)
        assertFalse(vm.uiState.value.operationError!!.contains("database internals"))
    }

    @Test fun repeatedSaveWhilePendingInsertsOnlyOnce() = runTest {
        repository.gate = CompletableDeferred()
        val vm = CategoryManagementViewModel(repository)
        advanceUntilIdle()
        vm.startAdd()
        vm.changeName("Travel")
        vm.saveEditor()
        vm.saveEditor()
        runCurrent()
        vm.saveEditor()
        assertEquals(1, repository.insertCalls)
        repository.gate!!.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, repository.inserted.size)
    }

    private class FakeCategories : CategoryRepository {
        val items = MutableStateFlow(listOf(
            Category(1, "Groceries", true, true),
            Category(2, "Archived", false, false),
        ))
        val inserted = mutableListOf<Category>()
        val updated = mutableListOf<Category>()
        var insertCalls = 0
        var failure: Exception? = null
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun insert(category: Category): Long {
            insertCalls++
            gate?.await()
            failure?.let { throw it }
            inserted += category
            val id = (items.value.maxOfOrNull { it.id } ?: 0) + 1
            items.value = items.value + category.copy(id = id)
            return id
        }
        override suspend fun update(category: Category): Int {
            failure?.let { throw it }
            updated += category
            items.value = items.value.map { if (it.id == category.id) category else it }
            return 1
        }
        override suspend fun getById(id: Long): Category? = items.value.firstOrNull { it.id == id }
        override fun observeActive(): Flow<List<Category>> = items
        override fun observeAll(): Flow<List<Category>> = items
    }
}
