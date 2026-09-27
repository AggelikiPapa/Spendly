package com.spendly.ui.navigation

object SpendlyRoutes {
    const val AddTransaction = "add_transaction"
    const val Categories = "categories"
    const val TransactionId = "transactionId"
    const val EditTransaction = "edit_transaction/{$TransactionId}"

    fun editTransaction(id: Long): String = "edit_transaction/$id"
}
