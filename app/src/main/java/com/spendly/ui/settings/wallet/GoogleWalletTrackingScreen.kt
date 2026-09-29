package com.spendly.ui.settings.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R

@Composable
fun GoogleWalletTrackingScreen(
    viewModel: GoogleWalletTrackingViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.google_wallet_tracking), style = MaterialTheme.typography.headlineSmall)
        }
        Text(stringResource(R.string.wallet_tracking_future_description))
        Text(stringResource(R.string.notification_access), style = MaterialTheme.typography.titleMedium)
        if (state.isChecking) {
            CircularProgressIndicator()
        } else {
            Text(
                stringResource(
                    if (state.notificationAccessGranted) R.string.notification_access_enabled
                    else R.string.notification_access_required,
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            if (!state.notificationAccessGranted) {
                Button(onClick = viewModel::openSystemSettings, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.grant_notification_access))
                }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(stringResource(R.string.wallet_tracking_privacy), style = MaterialTheme.typography.bodyMedium)
    }
}
