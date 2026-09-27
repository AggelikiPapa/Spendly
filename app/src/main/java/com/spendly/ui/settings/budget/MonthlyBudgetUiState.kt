package com.spendly.ui.settings.budget

import com.spendly.domain.model.Money
import java.time.YearMonth

sealed interface MonthlyBudgetUiState {
    data object Loading : MonthlyBudgetUiState
    data class LoadError(val message: String) : MonthlyBudgetUiState
    data class Ready(
        val month: YearMonth,
        val currentLimit: Money? = null,
        val amountInput: String = "",
        val amountError: String? = null,
        val saveError: String? = null,
        val saveStatus: SaveStatus = SaveStatus.Idle,
        val hasUserEdited: Boolean = false,
    ) : MonthlyBudgetUiState
}

enum class SaveStatus { Idle, Saving, Saved }
