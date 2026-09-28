package com.spendly.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

@Composable
fun SettingsScreen(
    walletViewModel: GoogleWalletTrackingViewModel,
    contentPadding: PaddingValues,
    onOpenDestination: (SettingsDestination) -> Unit,
) {
    val walletState by walletViewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { walletViewModel.refreshAccess() }
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Text(
            text = stringResource(R.string.budget_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.monthly_spending_limit)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().clickable { onOpenDestination(SettingsDestination.MonthlyBudget) },
        )
        Text(
            text = stringResource(R.string.categories),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.categories)) },
            supportingContent = { Text(stringResource(R.string.manage_categories)) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().clickable { onOpenDestination(SettingsDestination.Categories) },
        )
        Text(
            text = stringResource(R.string.automatic_transaction_tracking),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
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
            modifier = Modifier.fillMaxWidth().clickable { onOpenDestination(SettingsDestination.GoogleWalletTracking) },
        )
    }
}
