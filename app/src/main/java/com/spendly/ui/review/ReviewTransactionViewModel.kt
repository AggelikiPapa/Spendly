package com.spendly.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.model.Category
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionVisibility
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import com.spendly.ui.transactions.form.TransactionFormValidator
import com.spendly.ui.transactions.form.TransactionFormValues
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.Currency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReviewTransactionViewModel(
    private val transactionId: Long,
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<ReviewTransactionUiState>(ReviewTransactionUiState.Loading)
    val uiState: StateFlow<ReviewTransactionUiState> = mutableUiState
    private val completedChannel = Channel<Unit>(Channel.BUFFERED)
    val completedEvents = completedChannel.receiveAsFlow()
    private var original: Transaction? = null
    private var originalCategory: Category? = null

    init {
        viewModelScope.launch {
            try {
                val transaction = transactions.getById(transactionId)
                if (transaction == null || !TransactionVisibility.needsWalletReview(transaction)) {
                    mutableUiState.value = ReviewTransactionUiState.NotFound
                    return@launch
                }
                original = transaction
                mutableUiState.value = ReviewTransactionUiState.Ready(
                    form = TransactionFormValues(
                        amountInput = BigDecimal.valueOf(transaction.amount.amountMinor, fractionDigits(transaction.amount.currencyCode)).toPlainString(),
                        currencyCode = transaction.amount.currencyCode,
                        transactionType = transaction.type,
                        selectedCategoryId = transaction.categoryId,
                        merchantInput = transaction.merchant.orEmpty(),
                        selectedDate = transaction.occurredAt.atZone(clock.zone).toLocalDate(),
                        categories = emptyList(),
                    ),
                    notesInput = transaction.notes.orEmpty(),
                    rawSourceText = transaction.rawSourceText,
                )
                categories.observeAll().collect { allCategories ->
                    originalCategory = allCategories.firstOrNull { it.id == transaction.categoryId }
                    updateReady { state ->
                        val selectedInactive = originalCategory?.takeIf { !it.isActive && state.form.selectedCategoryId == it.id }
                        state.copy(form = state.form.copy(
                            categories = allCategories.filter { it.isActive },
                            selectedInactiveCategoryName = selectedInactive?.let { "${it.name} (Inactive)" },
                            categoryLoadError = null,
                        ))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.value = ReviewTransactionUiState.LoadError("Review transaction could not be loaded.")
            }
        }
    }

    private fun updateReady(transform: (ReviewTransactionUiState.Ready) -> ReviewTransactionUiState.Ready) {
        mutableUiState.update { (it as? ReviewTransactionUiState.Ready)?.let(transform) ?: it }
    }

    fun onAmountChanged(value: String) = updateReady {
        if (it.isSaving) it else it.copy(form = it.form.copy(amountInput = value, amountError = null), operationError = null)
    }

    fun onMerchantChanged(value: String) = updateReady {
        if (it.isSaving) it else it.copy(form = it.form.copy(merchantInput = value), merchantError = null, operationError = null)
    }

    fun onCategorySelected(id: Long?) = updateReady {
        if (it.isSaving) it else it.copy(
            form = it.form.copy(selectedCategoryId = id, selectedInactiveCategoryName = null, categoryError = null),
            operationError = null,
        )
    }

    fun onDateSelected(value: LocalDate) = updateReady {
        if (it.isSaving) it else it.copy(form = it.form.copy(selectedDate = value), operationError = null)
    }

    fun onNotesChanged(value: String) = updateReady {
        if (it.isSaving) it else it.copy(notesInput = value, operationError = null)
    }

    fun confirm() {
        val state = uiState.value as? ReviewTransactionUiState.Ready ?: return
        val transaction = original ?: return
        if (state.isSaving || state.showIgnoreConfirmation) return
        val form = state.form
        val validationCategories = form.categories + listOfNotNull(originalCategory).filter {
            !it.isActive && it.id == transaction.categoryId && form.selectedCategoryId == it.id
        }
        val validation = TransactionFormValidator.validate(
            form.amountInput, form.currencyCode, fractionDigits(form.currencyCode),
            form.transactionType, form.selectedCategoryId, validationCategories,
            requireExpenseCategory = false,
        )
        val merchant = form.merchantInput.trim()
        if (!validation.isValid || merchant.isEmpty()) {
            updateReady { it.copy(
                form = it.form.copy(amountError = validation.amountError, categoryError = validation.categoryError),
                merchantError = if (merchant.isEmpty()) "Enter a merchant to confirm this purchase." else null,
                operationError = null,
            ) }
            return
        }
        val amount = validation.amount ?: return
        updateReady { it.copy(isSaving = true, operationError = null) }
        viewModelScope.launch {
            try {
                val current = transactions.getById(transactionId)
                if (current == null || !TransactionVisibility.needsWalletReview(current)) {
                    mutableUiState.value = ReviewTransactionUiState.NotFound
                    return@launch
                }
                val originalTime = current.occurredAt.atZone(clock.zone).toLocalTime()
                val updated = current.copy(
                    amount = amount,
                    merchant = merchant,
                    categoryId = form.selectedCategoryId,
                    occurredAt = form.selectedDate.atTime(originalTime).atZone(clock.zone).toInstant(),
                    notes = state.notesInput.trim().ifBlank { null },
                    importStatus = ImportStatus.CONFIRMED,
                    updatedAt = clock.instant(),
                )
                if (transactions.update(updated) == 0) mutableUiState.value = ReviewTransactionUiState.NotFound
                else completedChannel.send(Unit)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateReady { it.copy(isSaving = false, operationError = "Could not confirm the transaction. Please try again.") }
            }
        }
    }

    fun requestIgnore() = updateReady {
        if (it.isSaving) it else it.copy(showIgnoreConfirmation = true, operationError = null)
    }

    fun cancelIgnore() = updateReady { it.copy(showIgnoreConfirmation = false) }

    fun confirmIgnore() {
        val state = uiState.value as? ReviewTransactionUiState.Ready ?: return
        if (!state.showIgnoreConfirmation || state.isSaving) return
        updateReady { it.copy(showIgnoreConfirmation = false, isSaving = true, operationError = null) }
        viewModelScope.launch {
            try {
                val current = transactions.getById(transactionId)
                if (current == null || !TransactionVisibility.needsWalletReview(current)) {
                    mutableUiState.value = ReviewTransactionUiState.NotFound
                    return@launch
                }
                if (transactions.update(current.copy(importStatus = ImportStatus.IGNORED, updatedAt = clock.instant())) == 0) {
                    mutableUiState.value = ReviewTransactionUiState.NotFound
                } else completedChannel.send(Unit)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateReady { it.copy(isSaving = false, operationError = "Could not ignore the transaction. Please try again.") }
            }
        }
    }

    private fun fractionDigits(currencyCode: String): Int =
        runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }
            .getOrDefault(2).takeIf { it >= 0 } ?: 2
}
