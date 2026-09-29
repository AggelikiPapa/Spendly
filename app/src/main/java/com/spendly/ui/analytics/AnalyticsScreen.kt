package com.spendly.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendly.R
import com.spendly.domain.model.Money
import com.spendly.ui.transactions.MoneyDisplayFormatter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel, contentPadding: PaddingValues) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.padding(contentPadding),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { MonthSelector(month, viewModel.canGoNext(), viewModel::previousMonth, viewModel::nextMonth) }
        when (val current = state) {
            AnalyticsUiState.Loading -> item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
            AnalyticsUiState.Error -> item { Text(stringResource(R.string.analytics_error)) }
            is AnalyticsUiState.Empty -> {
                item { SummaryCard(current.summary) }
                item { Text(stringResource(R.string.analytics_empty, monthName(month))) }
            }
            is AnalyticsUiState.Ready -> {
                val summary = current.summary
                item { SummaryCard(summary) }
                item { CategoryCard(summary) }
                item { DailyCard(summary) }
                item { LargestCard(summary) }
            }
        }
    }
}

@Composable
private fun MonthSelector(month: YearMonth, canNext: Boolean, previous: () -> Unit, next: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = previous) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.analytics_previous_month))
        }
        Text(month.atDay(1).format(DateTimeFormatter.ofPattern("LLLL yyyy")),
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = next, enabled = canNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.analytics_next_month))
        }
    }
}

@Composable
private fun SummaryCard(summary: AnalyticsSummary) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.analytics_total_spent), style = MaterialTheme.typography.titleMedium)
            Text(MoneyDisplayFormatter.formatAmount(summary.total), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.analytics_average_per_day, MoneyDisplayFormatter.formatAmount(summary.averageDaily)),
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            val previousName = monthName(summary.comparison.previousMonth)
            Text(stringResource(if (summary.isCurrentMonth) R.string.analytics_vs_previous_current
                else R.string.analytics_vs_previous, previousName), style = MaterialTheme.typography.labelLarge)
            Text("${signedMoney(summary.comparison.difference)}  ·  ${percentChange(summary.comparison.percentageChange)}",
                style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.analytics_previous_total, previousName,
                MoneyDisplayFormatter.formatAmount(summary.comparison.previousTotal)),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CategoryCard(summary: AnalyticsSummary) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.analytics_by_category), style = MaterialTheme.typography.titleMedium)
            summary.categories.forEach { category ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category.name, modifier = Modifier.weight(1f).padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyMedium)
                    Text(MoneyDisplayFormatter.formatAmount(category.amount), style = MaterialTheme.typography.bodyMedium)
                }
                val fraction = category.percentage.divide(BigDecimal(100)).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text("${category.percentage.stripTrailingZeros().toPlainString()}%",
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DailyCard(summary: AnalyticsSummary) {
    val maxMinor = summary.daily.maxOf { it.amount.amountMinor }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.analytics_daily_spending), style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 18.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                summary.daily.forEach { day ->
                    val fraction = if (maxMinor == 0L) 0f else BigDecimal.valueOf(day.amount.amountMinor)
                        .divide(BigDecimal.valueOf(maxMinor), 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
                    val label = "${day.date}: ${MoneyDisplayFormatter.formatAmount(day.amount)}"
                    Column(
                        modifier = Modifier.width(78.dp).height(132.dp).semantics { contentDescription = label },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.width(20.dp).height((72f * fraction).coerceAtLeast(2f).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (day.amount.amountMinor == 0L) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.height(5.dp))
                        Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
                        Text(MoneyDisplayFormatter.formatAmount(day.amount),
                            style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun LargestCard(summary: AnalyticsSummary) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.analytics_largest_expenses), style = MaterialTheme.typography.titleMedium)
            summary.largest.forEach { expense ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(expense.transaction.merchant?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.expense), style = MaterialTheme.typography.titleSmall)
                        Text("${expense.categoryName} · ${expense.date.format(DateTimeFormatter.ofPattern("d MMM"))}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Text(MoneyDisplayFormatter.formatAmount(expense.transaction.amount),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun monthName(month: YearMonth): String = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

private fun signedMoney(amount: Money): String = when {
    amount.amountMinor > 0L -> "+${MoneyDisplayFormatter.formatAmount(amount)}"
    amount.amountMinor < 0L -> "-${MoneyDisplayFormatter.formatAmount(amount)}"
    else -> MoneyDisplayFormatter.formatAmount(amount)
}

private fun percentChange(value: BigDecimal?): String = when {
    value == null -> "—"
    value.signum() > 0 -> "+${value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}%"
    else -> "${value.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()}%"
}
