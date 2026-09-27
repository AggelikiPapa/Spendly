package com.spendly.ui.transactions

import com.spendly.domain.model.Transaction

data class TransactionRow(val transaction: Transaction, val categoryName: String?)

data class TransactionsUiState(
    val rows: List<TransactionRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
