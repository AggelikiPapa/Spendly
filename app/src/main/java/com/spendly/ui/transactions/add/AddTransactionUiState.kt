package com.spendly.ui.transactions.add

import com.spendly.domain.model.Category
import com.spendly.domain.model.TransactionType
import java.time.LocalDate

data class AddTransactionUiState(
    val amountInput: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val selectedCategoryId: Long? = null,
    val merchantInput: String = "",
    val selectedDate: LocalDate,
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false,
    val amountError: String? = null,
    val categoryError: String? = null,
    val categoryLoadError: String? = null,
    val saveError: String? = null,
)
