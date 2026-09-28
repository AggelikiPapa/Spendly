package com.spendly.ui.settings

import com.spendly.ui.navigation.SpendlyRoutes

enum class SettingsDestination(val route: String) {
    MonthlyBudget(SpendlyRoutes.MonthlyBudget),
    BudgetNotifications(SpendlyRoutes.BudgetNotifications),
    Categories(SpendlyRoutes.Categories),
    MerchantRules(SpendlyRoutes.MerchantRules),
    GoogleWalletTracking(SpendlyRoutes.GoogleWalletTracking),
}
