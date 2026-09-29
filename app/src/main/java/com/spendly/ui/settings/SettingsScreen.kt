package com.spendly.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.ui.settings.wallet.GoogleWalletTrackingViewModel
import com.spendly.ui.settings.budgetalerts.BudgetNotificationSettingsViewModel
import com.spendly.ui.settings.budgetalerts.BudgetNotificationStatus

@Composable
fun SettingsScreen(
    walletViewModel: GoogleWalletTrackingViewModel,
    budgetNotificationsViewModel: BudgetNotificationSettingsViewModel,
    contentPadding: PaddingValues,
    onOpenDestination: (SettingsDestination) -> Unit,
) {
    val walletState by walletViewModel.uiState.collectAsStateWithLifecycle()
    val budgetNotificationState by budgetNotificationsViewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { walletViewModel.refreshAccess() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { budgetNotificationsViewModel.refresh() }
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState())
        .padding(bottom = 24.dp)) {
        Text(
            text = stringResource(R.string.budget_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.monthly_spending_limit)) },
            supportingContent = { Text(stringResource(R.string.monthly_spending_limit_summary)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clickable { onOpenDestination(SettingsDestination.MonthlyBudget) },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.budget_notifications)) },
            supportingContent = { Text(stringResource(when (budgetNotificationState.status) {
                BudgetNotificationStatus.ENABLED -> R.string.budget_notifications_enabled
                BudgetNotificationStatus.DISABLED -> R.string.budget_notifications_disabled
                BudgetNotificationStatus.PERMISSION_REQUIRED -> R.string.budget_notifications_permission_required
            })) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clickable { onOpenDestination(SettingsDestination.BudgetNotifications) },
        )
        Text(
            text = stringResource(R.string.transactions),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.categories)) },
            supportingContent = { Text(stringResource(R.string.manage_categories)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clickable { onOpenDestination(SettingsDestination.Categories) },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.merchant_rules)) },
            supportingContent = { Text(stringResource(R.string.merchant_rules_summary)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clickable { onOpenDestination(SettingsDestination.MerchantRules) },
        )
        Text(
            text = stringResource(R.string.automatic_transaction_tracking),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.google_wallet_tracking)) },
            supportingContent = {
                Text(stringResource(
                    if (walletState.notificationAccessGranted) R.string.notification_access_enabled
                    else R.string.notification_access_required,
                ))
            },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clickable { onOpenDestination(SettingsDestination.GoogleWalletTracking) },
        )
    }
}
