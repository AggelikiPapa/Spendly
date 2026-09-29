package com.spendly.ui.analytics

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Ready(val summary: AnalyticsSummary) : AnalyticsUiState
    data class Empty(val summary: AnalyticsSummary) : AnalyticsUiState
    data object Error : AnalyticsUiState
}
