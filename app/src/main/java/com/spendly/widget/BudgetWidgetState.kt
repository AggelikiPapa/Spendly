package com.spendly.widget

import com.spendly.domain.model.Money
import com.spendly.ui.dashboard.BudgetProgress
import com.spendly.ui.transactions.MoneyDisplayFormatter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

sealed interface BudgetWidgetState {
    val month: YearMonth

    data class NoBudget(override val month: YearMonth) : BudgetWidgetState
    data class Error(override val month: YearMonth) : BudgetWidgetState

    data class Ready(
        override val month: YearMonth,
        val budgetLimit: Money,
        val spent: Money,
        val remaining: Money,
        val percentageUsed: BigDecimal?,
        val percentageLabel: String,
        val spentLimitLabel: String,
        val balanceLabel: String,
        val smallBalanceLabel: String,
        val visualProgress: Float,
        val isOverBudget: Boolean,
    ) : BudgetWidgetState
}

/** Formats the Dashboard calculation once, before Glance renders either widget size. */
internal object BudgetWidgetStateFactory {
    fun ready(month: YearMonth, progress: BudgetProgress, locale: Locale = Locale.getDefault()): BudgetWidgetState.Ready {
        val over = progress.remaining.amountMinor < 0L
        val percentage = progress.percentageUsed
        val percentageLabel = percentage?.setScale(0, RoundingMode.HALF_UP)?.toPlainString()?.plus("%")
            ?: "Limit exceeded"
        val visualProgress = percentage?.divide(BigDecimal(100))?.toFloat()?.coerceIn(0f, 1f)
            ?: if (progress.spent.amountMinor > 0L) 1f else 0f
        val balance = if (over) Money(Math.negateExact(progress.remaining.amountMinor), progress.remaining.currencyCode)
            else progress.remaining
        return BudgetWidgetState.Ready(
            month = month,
            budgetLimit = progress.budgetLimit,
            spent = progress.spent,
            remaining = progress.remaining,
            percentageUsed = percentage,
            percentageLabel = percentageLabel,
            spentLimitLabel = "${MoneyDisplayFormatter.formatAmount(progress.spent, locale)} / ${MoneyDisplayFormatter.formatAmount(progress.budgetLimit, locale)}",
            balanceLabel = "${MoneyDisplayFormatter.formatAmount(balance, locale)} ${if (over) "over" else "remaining"}",
            smallBalanceLabel = "${MoneyDisplayFormatter.formatAmount(balance, locale)} ${if (over) "over" else "left"}",
            visualProgress = visualProgress,
            isOverBudget = over,
        )
    }

    fun monthLabel(month: YearMonth, locale: Locale = Locale.getDefault()): String =
        month.month.getDisplayName(TextStyle.FULL, locale)
}
