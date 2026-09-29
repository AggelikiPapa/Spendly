package com.spendly.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.model.TransactionSource
import com.spendly.domain.model.TransactionType
import com.spendly.domain.model.TransactionVisibility
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Clock
import java.time.YearMonth
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val selectedMonth = MutableStateFlow(YearMonth.now(clock))
    private val filters = MutableStateFlow(TransactionFilters())
    private val mutableUiState = MutableStateFlow(TransactionsUiState(selectedMonth = selectedMonth.value))
    val uiState: StateFlow<TransactionsUiState> = mutableUiState

    init {
        viewModelScope.launch {
            selectedMonth.flatMapLatest { month ->
                val start = month.atDay(1).atStartOfDay(clock.zone).toInstant()
                val end = month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toInstant()
                flow {
                    emitAll(combine(
                        transactions.observeInRange(start, end),
                        transactions.observeAll(),
                        categories.observeAll(),
                        filters,
                    ) { monthTransactions, allTransactions, allCategories, selectedFilters ->
                        val categoryNames = allCategories.associate { it.id to it.name }
                        val monthHistory = TransactionHistoryFilter.apply(
                            monthTransactions, month, clock.zone, TransactionFilters(),
                        )
                        val rows = TransactionHistoryFilter.apply(
                            monthHistory, month, clock.zone, selectedFilters,
                        ).map { transaction ->
                            TransactionRow(transaction, transaction.categoryId?.let {
                                categoryNames[it] ?: "Category unavailable"
                            })
                        }
                        val missingCategories = monthHistory.mapNotNull { it.categoryId }.distinct()
                            .filterNot(categoryNames::containsKey)
                            .map { CategoryOption(it, "Category unavailable") }
                        val availableCategories = (allCategories.map { CategoryOption(it.id, it.name) } + missingCategories)
                            .sortedWith(compareBy<CategoryOption> { it.name.lowercase(Locale.ROOT) }.thenBy { it.id })
                        TransactionsUiState(
                            rows = rows,
                            selectedMonth = month,
                            filters = selectedFilters,
                            availableCategories = availableCategories,
                            hasAnyHistory = allTransactions.any(TransactionVisibility::inHistory),
                            hasMonthHistory = monthHistory.isNotEmpty(),
                            reviewCount = allTransactions.count(TransactionVisibility::needsWalletReview),
                            isLoading = false,
                        )
                    })
                }.catch { error ->
                    if (error is CancellationException) throw error
                    mutableUiState.value = mutableUiState.value.copy(
                        selectedMonth = month, filters = filters.value,
                        isLoading = false, error = "Transactions could not be loaded.",
                    )
                }
            }.collect { mutableUiState.value = it }
        }
    }

    fun setSearchQuery(query: String) { filters.value = filters.value.copy(searchQuery = query) }
    fun setCategoryFilter(category: CategoryFilter) { filters.value = filters.value.copy(category = category) }
    fun setTypeFilter(type: TransactionType?) { filters.value = filters.value.copy(type = type) }
    fun setSourceFilter(source: TransactionSource?) { filters.value = filters.value.copy(source = source) }
    fun clearFilters() { filters.value = TransactionFilters() }

    fun previousMonth() {
        mutableUiState.value = mutableUiState.value.copy(isLoading = true)
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        if (!canGoNext()) return
        mutableUiState.value = mutableUiState.value.copy(isLoading = true)
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun canGoNext(): Boolean = selectedMonth.value < YearMonth.now(clock)
}
