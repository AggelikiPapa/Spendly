package com.spendly.ui.navigation

object SpendlyRoutes {
    const val AddTransaction = "add_transaction"
    const val Categories = "categories"
    const val MerchantRules = "merchant_rules"
    const val MonthlyBudget = "monthly_budget"
    const val GoogleWalletTracking = "google_wallet_tracking"
    const val TransactionId = "transactionId"
    const val EditTransaction = "edit_transaction/{$TransactionId}"
    const val ReviewTransaction = "review_transaction/{$TransactionId}"

    fun editTransaction(id: Long): String = "edit_transaction/$id"
    fun reviewTransaction(id: Long): String = "review_transaction/$id"
}
