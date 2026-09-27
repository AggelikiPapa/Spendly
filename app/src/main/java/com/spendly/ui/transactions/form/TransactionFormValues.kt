package com.spendly.ui.transactions.form

import com.spendly.domain.model.Category
import com.spendly.domain.model.TransactionType
import java.time.LocalDate

data class TransactionFormValues(
    val amountInput: String,
    val currencyCode: String,
    val transactionType: TransactionType,
    val selectedCategoryId: Long?,
    val merchantInput: String,
    val selectedDate: LocalDate,
    val categories: List<Category>,
    val amountError: String? = null,
    val categoryError: String? = null,
    val categoryLoadError: String? = null,
)
