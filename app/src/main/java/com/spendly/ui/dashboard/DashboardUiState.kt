package com.spendly.ui.dashboard

import com.spendly.domain.model.Transaction
import java.time.YearMonth

data class DashboardTransactionRow(val transaction: Transaction, val categoryName: String?)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Ready(
        val month: YearMonth,
        val progress: BudgetProgress?,
        val recentTransactions: List<DashboardTransactionRow>,
    ) : DashboardUiState
}
