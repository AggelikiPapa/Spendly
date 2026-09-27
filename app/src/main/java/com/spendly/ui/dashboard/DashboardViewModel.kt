package com.spendly.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.model.Transaction
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.MonthlyBudgetRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DashboardViewModel(
    transactions: TransactionRepository,
    budgets: MonthlyBudgetRepository,
    categories: CategoryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = mutableUiState
    private val month = YearMonth.now(clock)

    init {
        viewModelScope.launch {
            combine(
                transactions.observeAll(),
                budgets.observeByMonth(month),
                categories.observeAll(),
            ) { allTransactions, budget, allCategories ->
                val names = allCategories.associate { it.id to it.name }
                val recent = allTransactions
                    .sortedWith(compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id })
                    .take(5)
                    .map { DashboardTransactionRow(it, it.categoryId?.let(names::get)) }
                val progress = budget?.let {
                    BudgetProgressCalculator.calculate(it, allTransactions, month, LocalDate.now(clock), clock.zone)
                }
                DashboardUiState.Ready(month, progress, recent)
            }.catch { error ->
                if (error is CancellationException) throw error
                mutableUiState.value = DashboardUiState.Error("Dashboard could not be loaded.")
            }.collect { ready ->
                mutableUiState.value = ready
            }
        }
    }
}
