package com.spendly.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.domain.model.TransactionType
import com.spendly.domain.model.TransactionSource
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.compose.foundation.rememberScrollState

@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    contentPadding: PaddingValues,
    onAddTransaction: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
    onReviewTransactions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.transactions), style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_transaction))
            }
        }

        TextButton(onClick = onReviewTransactions, modifier = Modifier.padding(start = 8.dp)) {
            Text(if (state.reviewCount > 0) stringResource(R.string.needs_review_count, state.reviewCount) else stringResource(R.string.needs_review))
        }

        OutlinedTextField(
            value = state.filters.searchQuery,
            onValueChange = viewModel::setSearchQuery,
            label = { Text(stringResource(R.string.search_transactions)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (state.filters.searchQuery.isNotEmpty()) ({
                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                }
            }) else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        MonthSelector(state.selectedMonth, viewModel.canGoNext(), viewModel::previousMonth, viewModel::nextMonth)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterMenu(
                label = when (val category = state.filters.category) {
                    CategoryFilter.All -> stringResource(R.string.all_categories)
                    CategoryFilter.Uncategorized -> stringResource(R.string.uncategorized)
                    is CategoryFilter.Specific -> state.availableCategories.firstOrNull { it.id == category.id }?.name
                        ?: stringResource(R.string.category_unavailable)
                },
                selected = state.filters.category != CategoryFilter.All,
                choices = listOf(
                    stringResource(R.string.all_categories) to CategoryFilter.All,
                    stringResource(R.string.uncategorized) to CategoryFilter.Uncategorized,
                ) + state.availableCategories.map { it.name to CategoryFilter.Specific(it.id) },
                onSelect = viewModel::setCategoryFilter,
            )
            FilterMenu(
                label = when (state.filters.type) {
                    TransactionType.EXPENSE -> stringResource(R.string.expense)
                    TransactionType.INCOME -> stringResource(R.string.income)
                    TransactionType.TRANSFER -> stringResource(R.string.transfer)
                    null -> stringResource(R.string.all_types)
                },
                selected = state.filters.type != null,
                choices = listOf(
                    stringResource(R.string.all_types) to null,
                    stringResource(R.string.expense) to TransactionType.EXPENSE,
                    stringResource(R.string.income) to TransactionType.INCOME,
                    stringResource(R.string.transfer) to TransactionType.TRANSFER,
                ),
                onSelect = viewModel::setTypeFilter,
            )
            FilterMenu(
                label = when (state.filters.source) {
                    TransactionSource.MANUAL -> stringResource(R.string.manual_source)
                    TransactionSource.GOOGLE_WALLET -> stringResource(R.string.wallet_source)
                    null -> stringResource(R.string.all_sources)
                },
                selected = state.filters.source != null,
                choices = listOf(
                    stringResource(R.string.all_sources) to null,
                    stringResource(R.string.manual_source) to TransactionSource.MANUAL,
                    stringResource(R.string.wallet_source) to TransactionSource.GOOGLE_WALLET,
                ),
                onSelect = viewModel::setSourceFilter,
            )
        }

        if (state.filters.hasActiveFilters) {
            TextButton(onClick = viewModel::clearFilters, modifier = Modifier.padding(start = 8.dp)) {
                Text(stringResource(R.string.clear_filters))
            }
        } else {
            Spacer(Modifier.height(4.dp))
        }

        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            state.error != null -> Text(state.error!!, modifier = Modifier.padding(24.dp))
            state.rows.isEmpty() -> Text(
                stringResource(when {
                    !state.hasAnyHistory -> R.string.no_transactions
                    !state.hasMonthHistory -> R.string.no_transactions_month
                    else -> R.string.no_transactions_match
                }),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.rows, key = { it.transaction.id }) { row ->
                    TransactionHistoryRow(row, onClick = { onOpenTransaction(row.transaction.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(month: YearMonth, canNext: Boolean, previous: () -> Unit, next: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = previous) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.previous_month))
        }
        Text(month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy")),
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = next, enabled = canNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.next_month))
        }
    }
}

@Composable
private fun <T> FilterMenu(label: String, selected: Boolean, choices: List<Pair<String, T>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(selected = selected, onClick = { expanded = true }, label = { Text(label, maxLines = 1) })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (choiceLabel, value) ->
                DropdownMenuItem(text = { Text(choiceLabel) }, onClick = {
                    onSelect(value)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun TransactionHistoryRow(row: TransactionRow, onClick: () -> Unit) {
    val transaction = row.transaction
    val typeLabel = stringResource(
        when (transaction.type) {
            TransactionType.EXPENSE -> R.string.expense
            TransactionType.INCOME -> R.string.income
            TransactionType.TRANSFER -> R.string.transfer
        },
    )
    val label = transaction.merchant?.takeIf { it.isNotBlank() } ?: typeLabel
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault())
        .format(transaction.occurredAt)
    val subtitle = listOfNotNull(typeLabel, row.categoryName, date).joinToString(" | ")
    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
    }

    ListItem(
        headlineContent = { Text(label) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Text(
                MoneyDisplayFormatter.format(transaction),
                style = MaterialTheme.typography.titleMedium,
                color = amountColor,
            )
        },
        modifier = Modifier.padding(horizontal = 8.dp).clickable(onClick = onClick),
    )
}
