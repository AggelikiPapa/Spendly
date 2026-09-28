package com.spendly.ui.review

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.TransactionSource
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val transactions = FakeReviewTransactions()
    private val categories = FakeReviewCategories()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun reviewListContainsOnlyWalletNeedsReviewNewestFirst() = runTest {
        transactions.items.value = listOf(
            reviewTransaction(1, Instant.parse("2026-09-13T10:00:00Z"), categoryId = 1),
            reviewTransaction(2, Instant.parse("2026-09-15T10:00:00Z"), merchant = null),
            reviewTransaction(3, status = ImportStatus.CONFIRMED),
            reviewTransaction(4, status = ImportStatus.IGNORED),
            reviewTransaction(5, source = TransactionSource.MANUAL),
        )
        val vm = ReviewViewModel(transactions, categories)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf(2L, 1L), vm.uiState.value.rows.map { it.transaction.id })
        assertEquals(listOf(null, "Groceries"), vm.uiState.value.rows.map { it.categoryName })
    }

    @Test fun listReactsToConfirmIgnoreAndNewImports() = runTest {
        val vm = ReviewViewModel(transactions, categories)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.rows.isEmpty())
        transactions.items.value = listOf(reviewTransaction())
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.rows.size)
        transactions.items.value = listOf(reviewTransaction(status = ImportStatus.CONFIRMED))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.rows.isEmpty())
        transactions.items.value = listOf(reviewTransaction(status = ImportStatus.IGNORED))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.rows.isEmpty())
    }

    @Test fun repositoryFailureShowsSafeError() = runTest {
        val failing = FakeReviewTransactions()
        failing.failObserve = true
        val vm = ReviewViewModel(failing, categories)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Review transactions could not be loaded.", vm.uiState.value.error)
    }
}
