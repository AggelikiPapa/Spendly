package com.spendly.ui.transactions.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.model.Category
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import com.spendly.ui.transactions.form.TransactionFormValidator
import com.spendly.ui.transactions.form.TransactionFormValues
import com.spendly.wallet.importer.SaveMerchantCategoryRuleUseCase
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.util.Currency
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditTransactionViewModel(
    private val transactionId: Long,
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val saveMerchantRule: SaveMerchantCategoryRuleUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<EditTransactionUiState>(EditTransactionUiState.Loading)
    val uiState: StateFlow<EditTransactionUiState> = mutableUiState

    private val completedChannel = Channel<Unit>(Channel.BUFFERED)
    val completedEvents = completedChannel.receiveAsFlow()
    private var original: Transaction? = null
    private var originalCategory: Category? = null

    init {
        viewModelScope.launch {
            try {
                val transaction = transactions.getById(transactionId)
                if (transaction == null) {
                    mutableUiState.value = EditTransactionUiState.NotFound
                    return@launch
                }
                original = transaction
                val occurredLocal = transaction.occurredAt.atZone(clock.zone)
                val fractionDigits = fractionDigits(transaction.amount.currencyCode)
                mutableUiState.value = EditTransactionUiState.Ready(
                    source = transaction.source,
                    form = TransactionFormValues(
                        amountInput = BigDecimal.valueOf(transaction.amount.amountMinor, fractionDigits).abs().toPlainString(),
                        currencyCode = transaction.amount.currencyCode,
                        transactionType = transaction.type,
                        selectedCategoryId = transaction.categoryId,
                        merchantInput = transaction.merchant.orEmpty(),
                        selectedDate = occurredLocal.toLocalDate(),
                        categories = emptyList(),
                    ),
                )
                categories.observeAll().catch {
                    updateReady { it.copy(form = it.form.copy(categoryLoadError = "Categories could not be loaded.")) }
                }.collect { allCategories ->
                    originalCategory = allCategories.firstOrNull { it.id == transaction.categoryId }
                    updateReady { state ->
                        val activeCategories = allCategories.filter { it.isActive }
                        val selectedInactive = originalCategory?.takeIf { category ->
                            !category.isActive && state.form.selectedCategoryId == category.id
                        }
                        state.copy(form = state.form.copy(
                            categories = activeCategories,
                            selectedInactiveCategoryName = selectedInactive?.let { "${it.name} (Inactive)" },
                            categoryLoadError = null,
                        ))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.value = EditTransactionUiState.LoadError("Transaction could not be loaded.")
            }
        }
    }

    private fun updateReady(transform: (EditTransactionUiState.Ready) -> EditTransactionUiState.Ready) {
        mutableUiState.update { (it as? EditTransactionUiState.Ready)?.let(transform) ?: it }
    }

    fun onAmountChanged(value: String) = updateReady {
        if (it.isSaving || it.isDeleting) it else it.copy(form = it.form.copy(amountInput = value, amountError = null), operationError = null)
    }

    fun onTypeSelected(value: TransactionType) = updateReady {
        if (it.isSaving || it.isDeleting || it.form.transactionType == value) it else it.copy(
            form = it.form.copy(
                transactionType = value,
                selectedCategoryId = null,
                selectedInactiveCategoryName = null,
                categoryError = null,
            ),
            rememberMerchant = false,
            operationError = null,
        )
    }

    fun onCategorySelected(id: Long?) = updateReady {
        if (it.isSaving || it.isDeleting) it else it.copy(
            form = it.form.copy(selectedCategoryId = id, selectedInactiveCategoryName = null, categoryError = null),
            rememberMerchant = it.rememberMerchant && it.form.categories.any { category -> category.id == id },
            operationError = null,
        )
    }

    fun onMerchantChanged(value: String) = updateReady {
        if (it.isSaving || it.isDeleting) it else it.copy(
            form = it.form.copy(merchantInput = value),
            rememberMerchant = it.rememberMerchant && value.isNotBlank(),
            operationError = null,
        )
    }

    fun onDateSelected(value: LocalDate) = updateReady {
        if (it.isSaving || it.isDeleting) it else it.copy(form = it.form.copy(selectedDate = value), operationError = null)
    }

    fun onRememberMerchantChanged(value: Boolean) = updateReady {
        if (it.isSaving || it.isDeleting || !it.canRememberMerchant) it else it.copy(rememberMerchant = value, operationError = null)
    }

    fun save() {
        val state = uiState.value as? EditTransactionUiState.Ready ?: return
        val transaction = original ?: return
        if (state.isSaving || state.isDeleting || state.showDeleteConfirmation) return
        val form = state.form
        val validationCategories = form.categories + listOfNotNull(originalCategory).filter {
            !it.isActive && it.id == transaction.categoryId && form.selectedCategoryId == it.id
        }
        val validation = TransactionFormValidator.validate(
            form.amountInput, form.currencyCode, fractionDigits(form.currencyCode),
            form.transactionType, form.selectedCategoryId, validationCategories,
        )
        if (!validation.isValid) {
            updateReady { it.copy(form = it.form.copy(amountError = validation.amountError, categoryError = validation.categoryError), operationError = null) }
            return
        }
        val amount = validation.amount ?: return
        val shouldSaveRule = state.rememberMerchant && state.canRememberMerchant
        if (state.rememberMerchant && !shouldSaveRule) {
            updateReady { it.copy(operationError = "Choose an active category and enter a merchant to remember it.") }
            return
        }
        updateReady { it.copy(isSaving = true, operationError = null) }
        viewModelScope.launch {
            var ruleSaved = false
            try {
                val originalTime = transaction.occurredAt.atZone(clock.zone).toLocalTime()
                val updated = transaction.copy(
                    amount = amount,
                    type = form.transactionType,
                    categoryId = form.selectedCategoryId,
                    merchant = form.merchantInput.trim().ifBlank { null },
                    occurredAt = form.selectedDate.atTime(originalTime).atZone(clock.zone).toInstant(),
                    updatedAt = clock.instant(),
                )
                if (shouldSaveRule) {
                    saveMerchantRule.save(updated.merchant.orEmpty(), requireNotNull(updated.categoryId))
                    ruleSaved = true
                }
                if (transactions.update(updated) == 0) {
                    if (ruleSaved) updateReady { it.copy(isSaving = false, operationError = "Transaction could not be found. The merchant rule was saved.") }
                    else mutableUiState.value = EditTransactionUiState.NotFound
                } else {
                    completedChannel.send(Unit)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateReady { it.copy(isSaving = false, operationError = when {
                    ruleSaved -> "Could not save the transaction. The merchant rule was saved. Please try again."
                    shouldSaveRule -> "Could not save the merchant rule. The transaction was not saved. Please try again."
                    else -> "Could not save the transaction. Please try again."
                }) }
            }
        }
    }

    fun requestDelete() = updateReady {
        if (it.isSaving || it.isDeleting) it else it.copy(showDeleteConfirmation = true, operationError = null)
    }

    fun cancelDelete() = updateReady { it.copy(showDeleteConfirmation = false) }

    fun confirmDelete() {
        val state = uiState.value as? EditTransactionUiState.Ready ?: return
        if (!state.showDeleteConfirmation || state.isSaving || state.isDeleting) return
        updateReady { it.copy(showDeleteConfirmation = false, isDeleting = true, operationError = null) }
        viewModelScope.launch {
            try {
                if (transactions.deleteById(transactionId) == 0) {
                    mutableUiState.value = EditTransactionUiState.NotFound
                } else {
                    completedChannel.send(Unit)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateReady { it.copy(isDeleting = false, operationError = "Could not delete the transaction. Please try again.") }
            }
        }
    }

    private fun fractionDigits(currencyCode: String): Int =
        runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }
            .getOrDefault(2).takeIf { it >= 0 } ?: 2
}
