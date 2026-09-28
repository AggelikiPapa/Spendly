package com.spendly.ui.transactions.edit

import com.spendly.ui.transactions.form.TransactionFormValues
import com.spendly.domain.model.TransactionSource
import com.spendly.wallet.importer.SaveMerchantCategoryRuleUseCase

sealed interface EditTransactionUiState {
    data object Loading : EditTransactionUiState
    data object NotFound : EditTransactionUiState
    data class LoadError(val message: String) : EditTransactionUiState
    data class Ready(
        val form: TransactionFormValues,
        val source: TransactionSource = TransactionSource.MANUAL,
        val rememberMerchant: Boolean = false,
        val isSaving: Boolean = false,
        val isDeleting: Boolean = false,
        val showDeleteConfirmation: Boolean = false,
        val operationError: String? = null,
    ) : EditTransactionUiState {
        val canRememberMerchant: Boolean get() = SaveMerchantCategoryRuleUseCase.isAvailable(
            source, form.merchantInput, form.selectedCategoryId, form.categories,
        )
    }
}
