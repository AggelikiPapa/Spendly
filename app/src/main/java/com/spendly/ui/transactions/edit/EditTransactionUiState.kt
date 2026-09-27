package com.spendly.ui.transactions.edit

import com.spendly.ui.transactions.form.TransactionFormValues

sealed interface EditTransactionUiState {
    data object Loading : EditTransactionUiState
    data object NotFound : EditTransactionUiState
    data class LoadError(val message: String) : EditTransactionUiState
    data class Ready(
        val form: TransactionFormValues,
        val isSaving: Boolean = false,
        val isDeleting: Boolean = false,
        val showDeleteConfirmation: Boolean = false,
        val operationError: String? = null,
    ) : EditTransactionUiState
}
