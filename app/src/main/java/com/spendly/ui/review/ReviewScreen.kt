package com.spendly.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.ui.transactions.MoneyDisplayFormatter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    contentPadding: PaddingValues,
    onOpenTransaction: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        Text(stringResource(R.string.needs_review), modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.headlineSmall)
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            state.error != null -> Text(state.error!!, modifier = Modifier.padding(24.dp))
            state.rows.isEmpty() -> Text(stringResource(R.string.no_review_transactions), modifier = Modifier.padding(24.dp))
            else -> LazyColumn {
                items(state.rows, key = { it.transaction.id }) { row ->
                    val transaction = row.transaction
                    val date = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                        .withZone(ZoneId.systemDefault()).format(transaction.occurredAt)
                    ListItem(
                        headlineContent = { Text(transaction.merchant?.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown_merchant)) },
                        supportingContent = {
                            Text(listOfNotNull(stringResource(R.string.needs_review), row.categoryName, date).joinToString(" | "))
                        },
                        trailingContent = { Text(MoneyDisplayFormatter.format(transaction)) },
                        modifier = Modifier.clickable { onOpenTransaction(transaction.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
