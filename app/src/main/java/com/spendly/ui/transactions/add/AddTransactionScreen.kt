package com.spendly.ui.transactions.add

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.spendly.ui.transactions.form.TransactionFormValues

@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.savedEvents.collect { onBack() } }

    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !state.isSaving) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.add_transaction), style = MaterialTheme.typography.headlineSmall)
        }
        TransactionForm(
            values = TransactionFormValues(
                amountInput = state.amountInput,
                currencyCode = "EUR",
                transactionType = state.transactionType,
                selectedCategoryId = state.selectedCategoryId,
                merchantInput = state.merchantInput,
                selectedDate = state.selectedDate,
                categories = state.categories,
                amountError = state.amountError,
                categoryError = state.categoryError,
                categoryLoadError = state.categoryLoadError,
            ),
            enabled = !state.isSaving,
            onAmountChanged = viewModel::onAmountChanged,
            onTypeSelected = viewModel::onTypeSelected,
            onCategorySelected = viewModel::onCategorySelected,
            onMerchantChanged = viewModel::onMerchantChanged,
            onDateSelected = viewModel::onDateSelected,
        )
        state.saveError?.let { InlineError(it) }
        Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.isSaving) R.string.saving else R.string.save_transaction))
        }
    }
}
