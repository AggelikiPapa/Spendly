package com.spendly.ui.review

import com.spendly.ui.transactions.form.TransactionFormValues
import com.spendly.domain.model.TransactionSource
import com.spendly.wallet.importer.SaveMerchantCategoryRuleUseCase

sealed interface ReviewTransactionUiState {
    data object Loading : ReviewTransactionUiState
    data object NotFound : ReviewTransactionUiState
    data class LoadError(val message: String) : ReviewTransactionUiState
    data class Ready(
        val form: TransactionFormValues,
        val notesInput: String,
        val rawSourceText: String?,
        val source: TransactionSource = TransactionSource.GOOGLE_WALLET,
        val rememberMerchant: Boolean = false,
        val merchantError: String? = null,
        val isSaving: Boolean = false,
        val showIgnoreConfirmation: Boolean = false,
        val operationError: String? = null,
    ) : ReviewTransactionUiState {
        val canRememberMerchant: Boolean get() = SaveMerchantCategoryRuleUseCase.isAvailable(
            source, form.merchantInput, form.selectedCategoryId, form.categories,
        )
    }
}
