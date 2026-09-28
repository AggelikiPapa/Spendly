package com.spendly.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.ui.transactions.form.InlineError
import com.spendly.ui.transactions.form.TransactionForm

@Composable
fun ReviewTransactionScreen(
    viewModel: ReviewTransactionViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.completedEvents.collect { onBack() } }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.review_transaction), style = MaterialTheme.typography.headlineSmall)
        when (val current = state) {
            ReviewTransactionUiState.Loading -> CircularProgressIndicator()
            ReviewTransactionUiState.NotFound -> Text(stringResource(R.string.transaction_not_found))
            is ReviewTransactionUiState.LoadError -> InlineError(current.message)
            is ReviewTransactionUiState.Ready -> {
                TransactionForm(
                    values = current.form,
                    enabled = !current.isSaving,
                    onAmountChanged = viewModel::onAmountChanged,
                    onTypeSelected = {},
                    onCategorySelected = viewModel::onCategorySelected,
                    onMerchantChanged = viewModel::onMerchantChanged,
                    onDateSelected = viewModel::onDateSelected,
                    showTransactionType = false,
                    allowUncategorizedExpense = true,
                )
                current.merchantError?.let { InlineError(it) }
                OutlinedTextField(
                    value = current.notesInput,
                    onValueChange = viewModel::onNotesChanged,
                    label = { Text(stringResource(R.string.notes_optional)) },
                    enabled = !current.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                current.rawSourceText?.takeIf { it.isNotBlank() }?.let { raw ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.original_wallet_notification), style = MaterialTheme.typography.titleSmall)
                        Text(raw, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                current.operationError?.let { InlineError(it) }
                Button(onClick = viewModel::confirm, enabled = !current.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (current.isSaving) R.string.saving else R.string.confirm))
                }
                OutlinedButton(onClick = viewModel::requestIgnore, enabled = !current.isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.ignore))
                }
            }
        }
    }

    val ready = state as? ReviewTransactionUiState.Ready
    if (ready?.showIgnoreConfirmation == true) {
        AlertDialog(
            onDismissRequest = viewModel::cancelIgnore,
            title = { Text(stringResource(R.string.ignore)) },
            text = { Text(stringResource(R.string.confirm_ignore_transaction)) },
            confirmButton = { TextButton(onClick = viewModel::confirmIgnore) { Text(stringResource(R.string.ignore)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelIgnore) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
