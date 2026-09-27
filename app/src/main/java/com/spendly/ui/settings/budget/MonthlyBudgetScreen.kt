package com.spendly.ui.settings.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

@Composable
fun MonthlyBudgetScreen(
    viewModel: MonthlyBudgetViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.monthly_budget), style = MaterialTheme.typography.headlineSmall)
        }
        when (val current = state) {
            MonthlyBudgetUiState.Loading -> CircularProgressIndicator()
            is MonthlyBudgetUiState.LoadError -> Text(current.message, color = MaterialTheme.colorScheme.error)
            is MonthlyBudgetUiState.Ready -> {
                Text(
                    current.month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                )
                current.currentLimit?.let { limit ->
                    Text(
                        stringResource(R.string.current_spending_limit) + " €" +
                            BigDecimal.valueOf(limit.amountMinor, 2).toPlainString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedTextField(
                    value = current.amountInput,
                    onValueChange = viewModel::onAmountChanged,
                    label = { Text(stringResource(R.string.spending_limit)) },
                    prefix = { Text("€") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = current.saveStatus != SaveStatus.Saving,
                    isError = current.amountError != null,
                    supportingText = current.amountError?.let { error -> { Text(error) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                current.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (current.saveStatus == SaveStatus.Saved) {
                    Text(stringResource(R.string.budget_saved), color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = viewModel::save,
                    enabled = current.saveStatus != SaveStatus.Saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (current.saveStatus == SaveStatus.Saving) R.string.saving else R.string.save))
                }
            }
        }
    }
}
