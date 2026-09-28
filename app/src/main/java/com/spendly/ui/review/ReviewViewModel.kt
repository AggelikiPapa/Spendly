package com.spendly.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.model.Transaction
import com.spendly.domain.model.TransactionVisibility
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReviewViewModel(
    transactions: TransactionRepository,
    categories: CategoryRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = mutableUiState

    init {
        viewModelScope.launch {
            combine(transactions.observeAll(), categories.observeAll()) { allTransactions, allCategories ->
                val names = allCategories.associate { it.id to it.name }
                allTransactions.asSequence()
                    .filter(TransactionVisibility::needsWalletReview)
                    .sortedWith(compareByDescending<Transaction> { it.occurredAt }.thenByDescending { it.id })
                    .map { ReviewRow(it, it.categoryId?.let(names::get)) }
                    .toList()
            }.catch { error ->
                if (error is CancellationException) throw error
                mutableUiState.value = ReviewUiState(isLoading = false, error = "Review transactions could not be loaded.")
            }.collect { rows -> mutableUiState.value = ReviewUiState(rows = rows, isLoading = false) }
        }
    }
}
