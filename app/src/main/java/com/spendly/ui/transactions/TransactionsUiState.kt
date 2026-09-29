package com.spendly.ui.transactions

import com.spendly.domain.model.Transaction
import java.time.YearMonth

data class TransactionRow(val transaction: Transaction, val categoryName: String?)

data class TransactionsUiState(
    val rows: List<TransactionRow> = emptyList(),
    val selectedMonth: YearMonth,
    val filters: TransactionFilters = TransactionFilters(),
    val availableCategories: List<CategoryOption> = emptyList(),
    val hasAnyHistory: Boolean = false,
    val hasMonthHistory: Boolean = false,
    val reviewCount: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class CategoryOption(val id: Long, val name: String)
