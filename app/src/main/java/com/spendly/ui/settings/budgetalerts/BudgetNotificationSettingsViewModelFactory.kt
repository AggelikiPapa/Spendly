package com.spendly.ui.settings.budgetalerts

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spendly.budget.alerts.BudgetAlertPermission
import com.spendly.budget.alerts.BudgetAlertStore

fun budgetNotificationSettingsViewModelFactory(store: BudgetAlertStore, permission: BudgetAlertPermission) = viewModelFactory {
    initializer { BudgetNotificationSettingsViewModel(store, permission) }
}
