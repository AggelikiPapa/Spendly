package com.spendly.ui.settings.budgetalerts

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.budget.alerts.AndroidBudgetAlertPermission

@Composable
fun BudgetNotificationSettingsScreen(
    viewModel: BudgetNotificationSettingsViewModel,
    permission: AndroidBudgetAlertPermission,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var requestDenied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            requestDenied = false
            viewModel.enable()
        } else {
            requestDenied = true
            viewModel.refresh()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
            Text(stringResource(R.string.budget_notifications), style = MaterialTheme.typography.headlineSmall)
        }
        Text(stringResource(R.string.budget_notifications_description))
        Text(stringResource(when (state.status) {
            BudgetNotificationStatus.ENABLED -> R.string.budget_notifications_enabled
            BudgetNotificationStatus.DISABLED -> R.string.budget_notifications_disabled
            BudgetNotificationStatus.PERMISSION_REQUIRED -> R.string.budget_notifications_permission_required
        }), style = MaterialTheme.typography.titleMedium)
        when (state.status) {
            BudgetNotificationStatus.ENABLED -> OutlinedButton(onClick = viewModel::disable, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.disable_budget_notifications))
            }
            BudgetNotificationStatus.DISABLED -> Button(onClick = viewModel::enable, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.enable_budget_notifications))
            }
            BudgetNotificationStatus.PERMISSION_REQUIRED -> Button(onClick = {
                if (permission.needsRuntimePermission() && !requestDenied) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else permission.openSystemSettings()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (requestDenied) R.string.open_notification_settings else R.string.grant_budget_notification_permission))
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
