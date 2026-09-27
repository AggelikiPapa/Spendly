package com.spendly.ui.transactions.edit

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.ui.transactions.form.InlineError
import com.spendly.ui.transactions.form.TransactionForm

@Composable
fun EditTransactionScreen(
    viewModel: EditTransactionViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.completedEvents.collect { onBack() } }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.edit_transaction), style = MaterialTheme.typography.headlineSmall)
        }
        when (val current = state) {
            EditTransactionUiState.Loading -> CircularProgressIndicator()
            EditTransactionUiState.NotFound -> Text(stringResource(R.string.transaction_not_found))
            is EditTransactionUiState.LoadError -> InlineError(current.message)
            is EditTransactionUiState.Ready -> {
                val busy = current.isSaving || current.isDeleting
                TransactionForm(
                    values = current.form,
                    enabled = !busy,
                    onAmountChanged = viewModel::onAmountChanged,
                    onTypeSelected = viewModel::onTypeSelected,
                    onCategorySelected = viewModel::onCategorySelected,
                    onMerchantChanged = viewModel::onMerchantChanged,
                    onDateSelected = viewModel::onDateSelected,
                )
                current.operationError?.let { InlineError(it) }
                Button(onClick = viewModel::save, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (current.isSaving) R.string.saving else R.string.save_transaction))
                }
                OutlinedButton(onClick = viewModel::requestDelete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (current.isDeleting) R.string.deleting else R.string.delete_transaction))
                }
            }
        }
    }

    val ready = state as? EditTransactionUiState.Ready
    if (ready?.showDeleteConfirmation == true) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.delete_transaction)) },
            text = { Text(stringResource(R.string.confirm_delete_transaction)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.delete_transaction)) }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
