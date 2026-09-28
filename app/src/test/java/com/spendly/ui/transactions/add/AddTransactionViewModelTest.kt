package com.spendly.ui.transactions.add

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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val transactions = FakeTransactionRepository()
    private val categories = FakeCategoryRepository()
    private val zone = ZoneId.of("Europe/Athens")
    private val fixedInstant = Instant.parse("2026-09-15T09:30:00Z")
    private val clock = Clock.fixed(fixedInstant, zone)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsAsExpenseWithCurrentLocalDate() = runTest {
        val viewModel = newViewModel()

        assertEquals(TransactionType.EXPENSE, viewModel.uiState.value.transactionType)
        assertEquals(LocalDate.of(2026, 9, 15), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun expenseWithoutCategoryIsRejected() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onAmountChanged("12.50")

        viewModel.save()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.categoryError)
        assertEquals(0, transactions.insertCalls)
    }

    @Test
    fun invalidAndZeroAmountsDoNotSave() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onCategorySelected(1)

        for (input in listOf("bad", "0", "-1")) {
            viewModel.onAmountChanged(input)
            viewModel.save()
            assertNotNull(viewModel.uiState.value.amountError)
        }
        assertEquals(0, transactions.insertCalls)
    }

    @Test
    fun incomeCanSaveWithoutCategory() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onTypeSelected(TransactionType.INCOME)
        viewModel.onAmountChanged("12.50")

        viewModel.save()
        advanceUntilIdle()

        assertEquals(TransactionType.INCOME, transactions.saved.single().type)
        assertNull(transactions.saved.single().categoryId)
    }

    @Test
    fun transferCanSaveWithoutCategory() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onTypeSelected(TransactionType.TRANSFER)
        viewModel.onAmountChanged("12.50")

        viewModel.save()
        advanceUntilIdle()

        assertEquals(TransactionType.TRANSFER, transactions.saved.single().type)
        assertNull(transactions.saved.single().categoryId)
    }

    @Test
    fun successfulSaveCreatesOneConfirmedManualTransaction() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onAmountChanged("12.50")
        viewModel.onCategorySelected(1)
        viewModel.onTypeSelected(TransactionType.EXPENSE)
        viewModel.onMerchantChanged("  Market  ")
        viewModel.onDateSelected(LocalDate.of(2026, 9, 14))

        viewModel.save()
        advanceUntilIdle()

        val saved = transactions.saved.single()
        assertEquals(1, transactions.insertCalls)
        assertEquals(0L, saved.id)
        assertEquals(Money(1_250, "EUR"), saved.amount)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals(1L, saved.categoryId)
        assertEquals("Market", saved.merchant)
        assertNull(saved.description)
        assertEquals(TransactionSource.MANUAL, saved.source)
        assertEquals(ImportStatus.CONFIRMED, saved.importStatus)
        assertNull(saved.externalReference)
        assertNull(saved.rawSourceText)
        assertEquals(fixedInstant, saved.createdAt)
        assertEquals(fixedInstant, saved.updatedAt)
        assertEquals(LocalDate.of(2026, 9, 14).atTime(12, 30).atZone(zone).toInstant(), saved.occurredAt)
        assertNull(viewModel.uiState.value.saveError)
        assertEquals(Unit, viewModel.savedEvents.first())
    }

    @Test
    fun repeatedSaveWhilePendingDoesNotDuplicate() = runTest {
        transactions.gate = CompletableDeferred()
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onAmountChanged("12.50")
        viewModel.onCategorySelected(1)

        viewModel.save()
        viewModel.save()
        runCurrent()
        viewModel.save()
        assertTrue(viewModel.uiState.value.isSaving)
        assertEquals(1, transactions.insertCalls)

        transactions.gate?.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, transactions.saved.size)
    }

    @Test
    fun repositoryFailureStaysOnFormWithUsefulError() = runTest {
        transactions.failure = IllegalStateException("database details")
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onAmountChanged("12.50")
        viewModel.onCategorySelected(1)

        viewModel.save()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertNotNull(viewModel.uiState.value.saveError)
        assertFalse(viewModel.uiState.value.saveError!!.contains("database details"))
        assertTrue(transactions.saved.isEmpty())
    }

    @Test
    fun categoriesArrivingLaterAppearInState() = runTest {
        categories.active.value = emptyList()
        val viewModel = newViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.categories.isEmpty())

        categories.active.value = listOf(Category(5, "Travel", true, true))
        advanceUntilIdle()
        assertEquals(listOf("Travel"), viewModel.uiState.value.categories.map { it.name })
    }

    private fun newViewModel() = AddTransactionViewModel(transactions, categories, clock)

    private class FakeTransactionRepository : TransactionRepository {
        val saved = mutableListOf<Transaction>()
        var insertCalls = 0
        var gate: CompletableDeferred<Unit>? = null
        var failure: Exception? = null

        override suspend fun insert(transaction: Transaction): Long {
            insertCalls++
            gate?.await()
            failure?.let { throw it }
            saved += transaction
            return saved.size.toLong()
        }

        override suspend fun update(transaction: Transaction): Int = error("Unused")
        override suspend fun deleteById(id: Long): Int = error("Unused")
        override suspend fun getById(id: Long): Transaction? = error("Unused")
        override suspend fun getBySourceAndExternalReference(source: com.spendly.domain.model.TransactionSource, externalReference: String): Transaction? = error("Unused")
        override suspend fun getBySourceInTimeRange(source: com.spendly.domain.model.TransactionSource, startInclusive: Instant, endInclusive: Instant): List<Transaction> = error("Unused")
        override fun observeAll(): Flow<List<Transaction>> = emptyFlow()
        override fun observeInRange(startInclusive: Instant, endExclusive: Instant): Flow<List<Transaction>> = emptyFlow()
    }

    private class FakeCategoryRepository : CategoryRepository {
        val active = MutableStateFlow(listOf(Category(1, "Groceries", isSystem = true, isActive = true)))

        override suspend fun insert(category: Category): Long = error("Unused")
        override suspend fun update(category: Category): Int = error("Unused")
        override suspend fun getById(id: Long): Category? = error("Unused")
        override fun observeActive(): Flow<List<Category>> = active
        override fun observeAll(): Flow<List<Category>> = active
    }
}
