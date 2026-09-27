package com.spendly.ui.transactions.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.domain.model.TransactionType
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.savedEvents.collect { onBack() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !state.isSaving) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            Text(stringResource(R.string.add_transaction), style = MaterialTheme.typography.headlineSmall)
        }

        OutlinedTextField(
            value = state.amountInput,
            onValueChange = viewModel::onAmountChanged,
            label = { Text(stringResource(R.string.amount)) },
            prefix = { Text("€") },
            textStyle = MaterialTheme.typography.headlineMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = state.amountError != null,
            supportingText = state.amountError?.let { error -> { InlineError(error) } },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.transaction_type), style = MaterialTheme.typography.titleMedium)
            val types = TransactionType.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.transactionType == type,
                        onClick = { viewModel.onTypeSelected(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, types.size),
                        enabled = !state.isSaving,
                        label = {
                            Text(
                                stringResource(
                                    when (type) {
                                        TransactionType.EXPENSE -> R.string.expense
                                        TransactionType.INCOME -> R.string.income
                                        TransactionType.TRANSFER -> R.string.transfer
                                    },
                                ),
                            )
                        },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleMedium)
            Box {
                val selectedName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name
                OutlinedButton(
                    onClick = { categoryMenuExpanded = true },
                    enabled = !state.isSaving && state.categories.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(selectedName ?: stringResource(R.string.choose_category))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                ) {
                    if (state.transactionType != TransactionType.EXPENSE) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_category)) },
                            onClick = {
                                viewModel.onCategorySelected(null)
                                categoryMenuExpanded = false
                            },
                        )
                    }
                    state.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                viewModel.onCategorySelected(category.id)
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
            }
            if (state.categories.isEmpty() && state.categoryLoadError == null) {
                Text(stringResource(R.string.no_active_categories), style = MaterialTheme.typography.bodySmall)
            }
            state.categoryLoadError?.let { InlineError(it) }
            state.categoryError?.let { InlineError(it) }
        }

        OutlinedTextField(
            value = state.merchantInput,
            onValueChange = viewModel::onMerchantChanged,
            label = { Text(stringResource(R.string.merchant_optional)) },
            singleLine = true,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.date), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { showDatePicker = true }, enabled = !state.isSaving) {
                Text(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(state.selectedDate))
            }
        }

        state.saveError?.let { InlineError(it) }
        Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.isSaving) R.string.saving else R.string.save_transaction))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate.toEpochDay() * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun InlineError(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
