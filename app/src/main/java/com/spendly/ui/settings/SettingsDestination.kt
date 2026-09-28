package com.spendly.ui.settings

import com.spendly.ui.navigation.SpendlyRoutes

enum class SettingsDestination(val route: String) {
    MonthlyBudget(SpendlyRoutes.MonthlyBudget),
    Categories(SpendlyRoutes.Categories),
    MerchantRules(SpendlyRoutes.MerchantRules),
    GoogleWalletTracking(SpendlyRoutes.GoogleWalletTracking),
}
