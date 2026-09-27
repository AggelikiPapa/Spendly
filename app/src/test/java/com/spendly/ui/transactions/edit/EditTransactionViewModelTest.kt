package com.spendly.ui.transactions.edit

import com.spendly.domain.model.Category
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditTransactionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val zone = ZoneId.of("Europe/Athens")
    private val now = Instant.parse("2026-09-27T12:00:00Z")
    private val clock = Clock.fixed(now, zone)
    private val original = Transaction(
        42, Money(1240, "EUR"), TransactionType.EXPENSE, "Shop", "Original description", 1,
        Instant.parse("2026-09-15T09:30:00Z"), TransactionSource.MANUAL, ImportStatus.CONFIRMED,
        "external-id", "raw text", "Original notes", Instant.parse("2026-09-01T00:00:00Z"),
        Instant.parse("2026-09-02T00:00:00Z"),
    )
    private val transactions = FakeTransactions(original)
    private val categories = FakeCategories()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun loadsSelectedIdAndOriginalValues() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        val state = vm.uiState.value as EditTransactionUiState.Ready
        assertEquals(listOf(42L), transactions.requestedIds)
        assertEquals("12.40", state.form.amountInput)
        assertEquals("Shop", state.form.merchantInput)
        assertEquals(1L, state.form.selectedCategoryId)
        assertEquals(LocalDate.of(2026, 9, 15), state.form.selectedDate)
    }

    @Test fun validSaveUpdatesOnceAndPreservesUneditedFields() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        vm.onAmountChanged("23.45")
        vm.onTypeSelected(TransactionType.INCOME)
        vm.onMerchantChanged("  New shop  ")
        vm.onDateSelected(LocalDate.of(2026, 9, 16))
        vm.save()
        vm.save()
        advanceUntilIdle()

        val saved = transactions.updates.single()
        assertEquals(42L, saved.id)
        assertEquals(Money(2345, "EUR"), saved.amount)
        assertEquals(TransactionType.INCOME, saved.type)
        assertEquals("New shop", saved.merchant)
        assertEquals(LocalDate.of(2026, 9, 16).atTime(12, 30).atZone(zone).toInstant(), saved.occurredAt)
        assertEquals(original.createdAt, saved.createdAt)
        assertEquals(now, saved.updatedAt)
        assertEquals(original.source, saved.source)
        assertEquals(original.importStatus, saved.importStatus)
        assertEquals(original.externalReference, saved.externalReference)
        assertEquals(original.rawSourceText, saved.rawSourceText)
        assertEquals(original.description, saved.description)
        assertEquals(original.notes, saved.notes)
        assertEquals(Unit, vm.completedEvents.first())
    }

    @Test fun invalidAmountAndMissingExpenseCategoryDoNotUpdate() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        vm.onAmountChanged("0")
        vm.onCategorySelected(null)
        vm.save()
        val state = vm.uiState.value as EditTransactionUiState.Ready
        assertNotNull(state.form.amountError)
        assertNotNull(state.form.categoryError)
        assertTrue(transactions.updates.isEmpty())
    }

    @Test fun deletionRequiresConfirmationAndOccursOnce() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        vm.confirmDelete()
        assertTrue(transactions.deletedIds.isEmpty())
        vm.requestDelete()
        vm.cancelDelete()
        vm.confirmDelete()
        assertTrue(transactions.deletedIds.isEmpty())
        vm.requestDelete()
        vm.confirmDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        assertEquals(listOf(42L), transactions.deletedIds)
        assertEquals(Unit, vm.completedEvents.first())
    }

    @Test fun missingTransactionIsSafe() = runTest {
        transactions.item = null
        val vm = newViewModel()
        advanceUntilIdle()
        assertEquals(EditTransactionUiState.NotFound, vm.uiState.value)
        vm.save()
        vm.requestDelete()
        vm.confirmDelete()
        assertTrue(transactions.updates.isEmpty())
        assertTrue(transactions.deletedIds.isEmpty())
    }

    @Test fun saveAndDeleteFailuresStayOnFormWithUsefulErrors() = runTest {
        val vm = newViewModel()
        advanceUntilIdle()
        transactions.updateFailure = IllegalStateException("database internals")
        vm.save()
        advanceUntilIdle()
        val saveState = vm.uiState.value as EditTransactionUiState.Ready
        assertFalse(saveState.isSaving)
        assertNotNull(saveState.operationError)
        assertFalse(saveState.operationError!!.contains("database internals"))

        transactions.deleteFailure = IllegalStateException("database internals")
        vm.requestDelete()
        vm.confirmDelete()
        advanceUntilIdle()
        val deleteState = vm.uiState.value as EditTransactionUiState.Ready
        assertFalse(deleteState.isDeleting)
        assertNotNull(deleteState.operationError)
        assertFalse(deleteState.operationError!!.contains("database internals"))
    }

    private fun newViewModel() = EditTransactionViewModel(42, transactions, categories, clock)

    private class FakeTransactions(var item: Transaction?) : TransactionRepository {
        val requestedIds = mutableListOf<Long>()
        val updates = mutableListOf<Transaction>()
        val deletedIds = mutableListOf<Long>()
        var updateFailure: Exception? = null
        var deleteFailure: Exception? = null
        override suspend fun insert(transaction: Transaction): Long = error("Unused")
        override suspend fun update(transaction: Transaction): Int {
            updateFailure?.let { throw it }
            updates += transaction
            return 1
        }
        override suspend fun deleteById(id: Long): Int {
            deleteFailure?.let { throw it }
            deletedIds += id
            return 1
        }
        override suspend fun getById(id: Long): Transaction? { requestedIds += id; return item }
        override fun observeAll(): Flow<List<Transaction>> = emptyFlow()
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> = emptyFlow()
    }

    private class FakeCategories : CategoryRepository {
        val items = MutableStateFlow(listOf(Category(1, "Groceries", true, true)))
        override suspend fun insert(category: Category): Long = error("Unused")
        override suspend fun update(category: Category): Int = error("Unused")
        override suspend fun getById(id: Long): Category? = error("Unused")
        override fun observeActive(): Flow<List<Category>> = items
        override fun observeAll(): Flow<List<Category>> = items
    }
}
