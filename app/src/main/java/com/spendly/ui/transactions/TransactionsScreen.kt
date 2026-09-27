package com.spendly.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.domain.model.TransactionType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.transactions), style = MaterialTheme.typography.headlineSmall)
            FilledTonalButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_transaction))
            }
        }

        TextButton(onClick = onReviewTransactions, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
            Text(stringResource(R.string.needs_review))
        }

        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            state.error != null -> Text(state.error!!, modifier = Modifier.padding(24.dp))
            state.rows.isEmpty() -> Text(
                stringResource(R.string.no_transactions),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            else -> LazyColumn {
                items(state.rows, key = { it.transaction.id }) { row ->
                    TransactionHistoryRow(row, onClick = { onOpenTransaction(row.transaction.id) })
                    HorizontalDivider()
                }
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
        modifier = Modifier.clickable(onClick = onClick),
    )
}
