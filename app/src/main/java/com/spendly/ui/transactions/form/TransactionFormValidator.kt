package com.spendly.ui.transactions.form

import com.spendly.domain.MoneyInputParser
import com.spendly.domain.model.Category
import com.spendly.domain.model.Money
import com.spendly.domain.model.TransactionType

data class TransactionFormValidation(
    val amount: Money?,
    val amountError: String?,
    val categoryError: String?,
) {
    val isValid: Boolean get() = amount != null && categoryError == null
}

object TransactionFormValidator {
    fun validate(
        amountInput: String,
        currencyCode: String,
        fractionDigits: Int,
        type: TransactionType,
        categoryId: Long?,
        categories: List<Category>,
    ): TransactionFormValidation {
        val amount = MoneyInputParser.parsePositive(amountInput, currencyCode, fractionDigits)
        val categoryValid = type != TransactionType.EXPENSE ||
            categories.any { it.id == categoryId }
        return TransactionFormValidation(
            amount = amount,
            amountError = if (amount == null) "Enter an amount greater than zero with valid decimal places." else null,
            categoryError = if (!categoryValid) "Choose a category for an expense." else null,
        )
    }
}
