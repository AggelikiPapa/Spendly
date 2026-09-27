package com.spendly.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.model.Transaction
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TransactionsViewModel(
    transactions: TransactionRepository,
    categories: CategoryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(transactions.observeAll(), categories.observeAll()) { allTransactions, allCategories ->
                val categoryNames = allCategories.associate { it.id to it.name }
                allTransactions
                    .sortedWith(compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id })
                    .map { TransactionRow(it, it.categoryId?.let(categoryNames::get)) }
            }.catch {
                mutableUiState.value = TransactionsUiState(isLoading = false, error = "Transactions could not be loaded.")
            }.collect { rows ->
                mutableUiState.value = TransactionsUiState(rows = rows, isLoading = false)
            }
        }
    }
}
