package com.spendly.ui.review

import com.spendly.domain.model.Transaction

data class ReviewRow(val transaction: Transaction, val categoryName: String?)

data class ReviewUiState(
    val rows: List<ReviewRow> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
