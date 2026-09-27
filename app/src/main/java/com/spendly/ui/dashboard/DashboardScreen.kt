package com.spendly.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
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
import com.spendly.domain.model.Money
import com.spendly.domain.model.TransactionType
import com.spendly.ui.transactions.MoneyDisplayFormatter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    contentPadding: PaddingValues,
    onConfigureBudget: () -> Unit,
    onSeeAllTransactions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        DashboardUiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(contentPadding).padding(24.dp))
        is DashboardUiState.Error -> Text(
            current.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(contentPadding).padding(24.dp),
        )
        is DashboardUiState.Ready -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    current.month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy")),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp),
                )
            }
            item {
                val progress = current.progress
                if (progress == null) {
                    NoBudgetCard(onConfigureBudget)
                } else {
                    BudgetCard(progress)
                }
            }
            if (current.progress != null) {
                item { SpendingPaceCard(current.progress.spendingPace, current.progress.spent) }
                item { DailyAllowanceCard(current.progress) }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.recent_transactions), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onSeeAllTransactions) { Text(stringResource(R.string.see_all)) }
                }
            }
            if (current.recentTransactions.isEmpty()) {
                item { Text(stringResource(R.string.no_transactions), modifier = Modifier.padding(horizontal = 24.dp)) }
            } else {
                items(current.recentTransactions, key = { it.transaction.id }) { row -> RecentTransactionRow(row) }
            }
        }
    }
}

@Composable
private fun SpendingPaceCard(pace: SpendingPace, actualSpent: Money) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.spending_pace), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(when (pace.status) {
                SpendingPaceStatus.BELOW_PACE -> R.string.below_planned_pace
                SpendingPaceStatus.ON_PACE -> R.string.on_planned_pace
                SpendingPaceStatus.ABOVE_PACE -> R.string.above_planned_pace
            }))
            Text(stringResource(
                R.string.expected_by_today,
                MoneyDisplayFormatter.formatAmount(pace.expectedSpentByToday),
            ))
            Text(stringResource(R.string.actual_spending, MoneyDisplayFormatter.formatAmount(actualSpent)))
            when (pace.status) {
                SpendingPaceStatus.ABOVE_PACE -> Text(stringResource(
                    R.string.ahead_of_target,
                    MoneyDisplayFormatter.formatAmount(pace.paceDifference),
                ))
                SpendingPaceStatus.BELOW_PACE -> Text(stringResource(
                    R.string.below_target,
                    MoneyDisplayFormatter.formatAmount(pace.paceDifference),
                ))
                SpendingPaceStatus.ON_PACE -> Unit
            }
        }
    }
}

@Composable
private fun NoBudgetCard(onConfigureBudget: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.no_monthly_budget), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.set_budget_prompt))
            Button(onClick = onConfigureBudget) { Text(stringResource(R.string.set_spending_limit)) }
        }
    }
}

@Composable
private fun BudgetCard(progress: BudgetProgress) {
    val remaining = progress.remaining.amountMinor
    val percentageLabel = progress.percentageUsed?.setScale(1, RoundingMode.HALF_UP)?.toPlainString()
        ?.plus("%")
    val visualProgress = progress.percentageUsed?.divide(BigDecimal(100))?.toFloat()?.coerceIn(0f, 1f)
        ?: if (progress.spent.amountMinor > 0L) 1f else 0f

    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.monthly_budget), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.spent_amount, MoneyDisplayFormatter.formatAmount(progress.spent)),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(stringResource(R.string.of_budget_limit, MoneyDisplayFormatter.formatAmount(progress.budgetLimit)))
            LinearProgressIndicator(progress = { visualProgress }, modifier = Modifier.fillMaxWidth())
            Text(
                if (percentageLabel == null) stringResource(R.string.limit_exceeded)
                else stringResource(R.string.budget_used, percentageLabel),
            )
            Text(
                if (remaining < 0L) {
                    stringResource(R.string.over_budget_amount, MoneyDisplayFormatter.formatAmount(progress.remaining))
                } else {
                    stringResource(R.string.remaining_amount, MoneyDisplayFormatter.formatAmount(progress.remaining))
                },
                color = if (remaining < 0L) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun DailyAllowanceCard(progress: BudgetProgress) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.daily_allowance), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.amount_per_day,
                    MoneyDisplayFormatter.formatAmount(progress.recommendedDailySpend),
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(stringResource(R.string.days_remaining, progress.daysRemaining))
        }
    }
}

@Composable
private fun RecentTransactionRow(row: DashboardTransactionRow) {
    val transaction = row.transaction
    val typeLabel = stringResource(when (transaction.type) {
        TransactionType.EXPENSE -> R.string.expense
        TransactionType.INCOME -> R.string.income
        TransactionType.TRANSFER -> R.string.transfer
    })
    val merchant = transaction.merchant?.takeIf { it.isNotBlank() } ?: typeLabel
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.systemDefault()).format(transaction.occurredAt)
    val subtitle = listOfNotNull(typeLabel, row.categoryName, date).joinToString(" | ")
    ListItem(
        headlineContent = { Text(merchant) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Text(MoneyDisplayFormatter.format(transaction)) },
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}
