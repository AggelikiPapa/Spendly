package com.spendly.ui.review

import com.spendly.ui.transactions.form.TransactionFormValues

sealed interface ReviewTransactionUiState {
    data object Loading : ReviewTransactionUiState
    data object NotFound : ReviewTransactionUiState
    data class LoadError(val message: String) : ReviewTransactionUiState
    data class Ready(
        val form: TransactionFormValues,
        val notesInput: String,
        val rawSourceText: String?,
        val merchantError: String? = null,
        val isSaving: Boolean = false,
        val showIgnoreConfirmation: Boolean = false,
        val operationError: String? = null,
    ) : ReviewTransactionUiState
}
