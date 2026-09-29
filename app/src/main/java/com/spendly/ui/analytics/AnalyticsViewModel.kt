package com.spendly.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.domain.repository.CategoryRepository
import com.spendly.domain.repository.TransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
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
class AnalyticsViewModel(
    private val transactions: TransactionRepository,
    private val categories: CategoryRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val mutableMonth = MutableStateFlow(YearMonth.now(clock))
    val selectedMonth: StateFlow<YearMonth> = mutableMonth

    private val mutableUiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = mutableUiState

    init {
        viewModelScope.launch {
            mutableMonth.flatMapLatest { month ->
                val start = month.minusMonths(1).atDay(1).atStartOfDay(clock.zone).toInstant()
                val end = month.plusMonths(1).atDay(1).atStartOfDay(clock.zone).toInstant()
                flow<AnalyticsUiState> {
                    emitAll(combine(transactions.observeInRange(start, end), categories.observeAll()) { rows, allCategories ->
                        val summary = AnalyticsCalculator.calculate(month, LocalDate.now(clock), clock.zone, rows, allCategories)
                        if (summary.isEmpty) AnalyticsUiState.Empty(summary) else AnalyticsUiState.Ready(summary)
                    })
                }.catch { error ->
                    if (error is CancellationException) throw error
                    emit(AnalyticsUiState.Error)
                }
            }.collect { mutableUiState.value = it }
        }
    }

    fun previousMonth() {
        mutableUiState.value = AnalyticsUiState.Loading
        mutableMonth.value = mutableMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        if (!canGoNext()) return
        mutableUiState.value = AnalyticsUiState.Loading
        mutableMonth.value = mutableMonth.value.plusMonths(1)
    }

    fun canGoNext(): Boolean = mutableMonth.value < YearMonth.now(clock)
}
