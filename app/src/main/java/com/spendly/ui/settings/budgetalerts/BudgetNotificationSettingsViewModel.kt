package com.spendly.ui.settings.budgetalerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.budget.alerts.BudgetAlertPermission
import com.spendly.budget.alerts.BudgetAlertStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BudgetNotificationStatus { ENABLED, DISABLED, PERMISSION_REQUIRED }

data class BudgetNotificationSettingsUiState(
    val status: BudgetNotificationStatus = BudgetNotificationStatus.DISABLED,
    val paceEnabled: Boolean = false,
    val error: String? = null,
)

class BudgetNotificationSettingsViewModel(
    private val store: BudgetAlertStore,
    private val permission: BudgetAlertPermission,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(BudgetNotificationSettingsUiState())
    val uiState: StateFlow<BudgetNotificationSettingsUiState> = mutableUiState

    init {
        viewModelScope.launch { store.enabled.collect { refresh() } }
        viewModelScope.launch { store.paceEnabled.collect { refresh() } }
        refresh()
    }

    fun refresh() = mutableUiState.update { it.copy(status = status(), paceEnabled = paceEnabledSafely()) }

    fun enable() {
        if (!permission.canPost()) {
            refresh()
            return
        }
        updatePreference(true)
    }

    fun disable() = updatePreference(false)

    fun setPaceEnabled(enabled: Boolean) {
        if (status() != BudgetNotificationStatus.ENABLED) return
        val saved = runCatching { store.setPaceEnabled(enabled) }.getOrDefault(false)
        mutableUiState.update { it.copy(
            paceEnabled = paceEnabledSafely(),
            error = if (saved) null else "Could not save the spending pace alert setting.",
        ) }
    }

    private fun updatePreference(enabled: Boolean) {
        val saved = runCatching { store.setEnabled(enabled) }.getOrDefault(false)
        mutableUiState.update { it.copy(
            status = status(),
            paceEnabled = paceEnabledSafely(),
            error = if (saved) null else "Could not save the budget notification setting.",
        ) }
    }

    private fun status(): BudgetNotificationStatus = when {
        !permission.canPost() -> BudgetNotificationStatus.PERMISSION_REQUIRED
        runCatching { store.isEnabled() }.getOrDefault(false) -> BudgetNotificationStatus.ENABLED
        else -> BudgetNotificationStatus.DISABLED
    }

    private fun paceEnabledSafely(): Boolean = runCatching { store.isPaceEnabled() }.getOrDefault(false)
}
