package com.spendly.ui.transactions.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spendly.R
import com.spendly.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionForm(
    values: TransactionFormValues,
    enabled: Boolean,
    onAmountChanged: (String) -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onMerchantChanged: (String) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val currencySymbol = remember(values.currencyCode) {
        runCatching { Currency.getInstance(values.currencyCode).symbol }.getOrDefault(values.currencyCode)
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(
            value = values.amountInput,
            onValueChange = onAmountChanged,
            label = { Text(stringResource(R.string.amount)) },
            prefix = { Text(currencySymbol) },
            textStyle = MaterialTheme.typography.headlineMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = values.amountError != null,
            supportingText = values.amountError?.let { error -> { InlineError(error) } },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.transaction_type), style = MaterialTheme.typography.titleMedium)
            val types = TransactionType.entries
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = values.transactionType == type,
                        onClick = { onTypeSelected(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, types.size),
                        enabled = enabled,
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
                val selectedName = values.categories.firstOrNull { it.id == values.selectedCategoryId }?.name
                    ?: values.selectedInactiveCategoryName
                OutlinedButton(
                    onClick = { categoryMenuExpanded = true },
                    enabled = enabled && values.categories.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(selectedName ?: stringResource(R.string.choose_category))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                ) {
                    if (values.transactionType != TransactionType.EXPENSE) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.no_category)) },
                            onClick = {
                                onCategorySelected(null)
                                categoryMenuExpanded = false
                            },
                        )
                    }
                    values.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                onCategorySelected(category.id)
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
            }
            if (values.categories.isEmpty() && values.categoryLoadError == null) {
                Text(stringResource(R.string.no_active_categories), style = MaterialTheme.typography.bodySmall)
            }
            values.categoryLoadError?.let { InlineError(it) }
            values.categoryError?.let { InlineError(it) }
        }

        OutlinedTextField(
            value = values.merchantInput,
            onValueChange = onMerchantChanged,
            label = { Text(stringResource(R.string.merchant_optional)) },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.date), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { showDatePicker = true }, enabled = enabled) {
                Text(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(values.selectedDate))
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = values.selectedDate.toEpochDay() * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
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
fun InlineError(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}
