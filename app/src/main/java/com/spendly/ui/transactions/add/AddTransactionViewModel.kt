package com.spendly.ui.transactions.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.model.ImportStatus
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import com.spendly.ui.transactions.form.TransactionFormValidator
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val initialLocalTime = LocalDateTime.now(clock)
    private val mutableUiState = MutableStateFlow(
        AddTransactionUiState(selectedDate = initialLocalTime.toLocalDate()),
    )
    val uiState: StateFlow<AddTransactionUiState> = mutableUiState

    private val savedChannel = Channel<Unit>(Channel.BUFFERED)
    val savedEvents = savedChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            categories.observeActive()
                .catch {
                    mutableUiState.update { state ->
                        state.copy(categoryLoadError = "Categories could not be loaded.")
                    }
                }
                .collect { activeCategories ->
                    mutableUiState.update { state ->
                        state.copy(
                            categories = activeCategories,
                            selectedCategoryId = state.selectedCategoryId?.takeIf { id ->
                                activeCategories.any { it.id == id }
                            },
                            categoryLoadError = null,
                        )
                    }
                }
        }
    }

    fun onAmountChanged(value: String) {
        if (uiState.value.isSaving) return
        mutableUiState.update { it.copy(amountInput = value, amountError = null, saveError = null) }
    }

    fun onTypeSelected(value: TransactionType) {
        if (uiState.value.isSaving) return
        if (uiState.value.transactionType == value) return
        mutableUiState.update {
            it.copy(transactionType = value, selectedCategoryId = null, categoryError = null, saveError = null)
        }
    }

    fun onCategorySelected(id: Long?) {
        if (uiState.value.isSaving) return
        mutableUiState.update { it.copy(selectedCategoryId = id, categoryError = null, saveError = null) }
    }

    fun onMerchantChanged(value: String) {
        if (uiState.value.isSaving) return
        mutableUiState.update { it.copy(merchantInput = value, saveError = null) }
    }

    fun onDateSelected(value: java.time.LocalDate) {
        if (uiState.value.isSaving) return
        mutableUiState.update { it.copy(selectedDate = value, saveError = null) }
    }

    fun save() {
        val state = uiState.value
        if (state.isSaving) return

        val validation = TransactionFormValidator.validate(
            state.amountInput, "EUR", 2, state.transactionType, state.selectedCategoryId, state.categories,
        )
        if (!validation.isValid) {
            mutableUiState.update {
                it.copy(
                    amountError = validation.amountError,
                    categoryError = validation.categoryError,
                    saveError = null,
                )
            }
            return
        }
        val amount = validation.amount ?: return

        mutableUiState.update { it.copy(isSaving = true, amountError = null, categoryError = null, saveError = null) }
        viewModelScope.launch {
            try {
                val now = clock.instant()
                val occurredAt = state.selectedDate.atTime(initialLocalTime.toLocalTime())
                    .atZone(clock.zone)
                    .toInstant()
                transactions.insert(
                    Transaction(
                        id = 0,
                        amount = amount,
                        type = state.transactionType,
                        merchant = state.merchantInput.trim().ifBlank { null },
                        description = null,
                        categoryId = state.selectedCategoryId,
                        occurredAt = occurredAt,
                        source = TransactionSource.MANUAL,
                        importStatus = ImportStatus.CONFIRMED,
                        externalReference = null,
                        rawSourceText = null,
                        notes = null,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
                savedChannel.send(Unit)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(isSaving = false, saveError = "Could not save the transaction. Please try again.")
                }
            }
        }
    }
}
