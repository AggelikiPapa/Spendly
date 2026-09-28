package com.spendly.ui.review

import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Money
import com.spendly.domain.model.TransactionSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewTransactionViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val transactions = FakeReviewTransactions()
    private val categories = FakeReviewCategories()
    private val now = Instant.parse("2026-09-20T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun viewModel() = ReviewTransactionViewModel(1, transactions, categories, clock)
    private fun ready(vm: ReviewTransactionViewModel) = vm.uiState.value as ReviewTransactionUiState.Ready

    @Test fun confirmEditsFieldsAndPreservesWalletIdentity() = runTest {
        val original = reviewTransaction(merchant = null)
        transactions.items.value = listOf(original)
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(original.rawSourceText, ready(vm).rawSourceText)
        vm.onAmountChanged("5.25")
        vm.onMerchantChanged("  Corrected Market  ")
        vm.onCategorySelected(1)
        vm.onDateSelected(LocalDate.of(2026, 9, 16))
        vm.onNotesChanged("  Checked receipt  ")

        vm.confirm()
        vm.confirm()
        advanceUntilIdle()

        assertEquals(1, transactions.updateCalls)
        val saved = transactions.items.value.single()
        assertEquals(1L, saved.id)
        assertEquals(ImportStatus.CONFIRMED, saved.importStatus)
        assertEquals(TransactionSource.GOOGLE_WALLET, saved.source)
        assertEquals(Money(525, "EUR"), saved.amount)
        assertEquals("Corrected Market", saved.merchant)
        assertEquals(1L, saved.categoryId)
        assertEquals(Instant.parse("2026-09-16T10:30:00Z"), saved.occurredAt)
        assertEquals("Checked receipt", saved.notes)
        assertEquals(original.createdAt, saved.createdAt)
        assertEquals(original.externalReference, saved.externalReference)
        assertEquals(original.rawSourceText, saved.rawSourceText)
        assertEquals(now, saved.updatedAt)
    }

    @Test fun categoryIsOptionalWhenConfirming() = runTest {
        transactions.items.value = listOf(reviewTransaction())
        val vm = viewModel()
        advanceUntilIdle()
        vm.confirm()
        advanceUntilIdle()
        assertEquals(ImportStatus.CONFIRMED, transactions.items.value.single().importStatus)
        assertNull(transactions.items.value.single().categoryId)
    }

    @Test fun missingMerchantAndInvalidAmountCannotBeConfirmed() = runTest {
        transactions.items.value = listOf(reviewTransaction(merchant = null))
        val vm = viewModel()
        advanceUntilIdle()
        vm.onAmountChanged("bad")
        vm.confirm()
        advanceUntilIdle()
        assertNotNull(ready(vm).form.amountError)
        assertNotNull(ready(vm).merchantError)
        assertEquals(0, transactions.updateCalls)
    }

    @Test fun confirmMakesItemDisappearFromReactiveReviewList() = runTest {
        transactions.items.value = listOf(reviewTransaction())
        val list = ReviewViewModel(transactions, categories)
        val detail = viewModel()
        advanceUntilIdle()
        assertEquals(1, list.uiState.value.rows.size)
        detail.confirm()
        advanceUntilIdle()
        assertTrue(list.uiState.value.rows.isEmpty())
    }

    @Test fun ignoreRequiresExplicitConfirmationAndPreservesRow() = runTest {
        val original = reviewTransaction()
        transactions.items.value = listOf(original)
        val list = ReviewViewModel(transactions, categories)
        val vm = viewModel()
        advanceUntilIdle()

        vm.confirmIgnore()
        advanceUntilIdle()
        assertEquals(0, transactions.updateCalls)
        vm.requestIgnore()
        vm.cancelIgnore()
        vm.confirmIgnore()
        advanceUntilIdle()
        assertEquals(0, transactions.updateCalls)

        vm.requestIgnore()
        assertTrue(ready(vm).showIgnoreConfirmation)
        vm.confirmIgnore()
        vm.confirmIgnore()
        advanceUntilIdle()
        assertEquals(1, transactions.updateCalls)
        val saved = transactions.items.value.single()
        assertEquals(ImportStatus.IGNORED, saved.importStatus)
        assertEquals(original.id, saved.id)
        assertEquals(original.source, saved.source)
        assertEquals(original.externalReference, saved.externalReference)
        assertEquals(original.rawSourceText, saved.rawSourceText)
        assertEquals(now, saved.updatedAt)
        assertTrue(list.uiState.value.rows.isEmpty())
    }

    @Test fun updateFailureKeepsUserOnReadyScreenWithError() = runTest {
        transactions.items.value = listOf(reviewTransaction())
        transactions.failUpdate = true
        val vm = viewModel()
        advanceUntilIdle()
        vm.confirm()
        advanceUntilIdle()
        assertEquals(1, transactions.updateCalls)
        assertFalse(ready(vm).isSaving)
        assertNotNull(ready(vm).operationError)
        assertEquals(ImportStatus.NEEDS_REVIEW, transactions.items.value.single().importStatus)
    }

    @Test fun nonReviewTransactionCannotBeOpenedForReview() = runTest {
        transactions.items.value = listOf(reviewTransaction(status = ImportStatus.CONFIRMED))
        val vm = viewModel()
        advanceUntilIdle()
        assertEquals(ReviewTransactionUiState.NotFound, vm.uiState.value)
    }
}
