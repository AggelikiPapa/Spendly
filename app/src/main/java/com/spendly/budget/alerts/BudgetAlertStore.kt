package com.spendly.budget.alerts

import java.time.YearMonth
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface BudgetAlertStore {
    val enabled: Flow<Boolean>
    val paceEnabled: Flow<Boolean>
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean): Boolean
    fun isPaceEnabled(): Boolean
    fun setPaceEnabled(enabled: Boolean): Boolean
    fun delivered(month: YearMonth): Set<BudgetThreshold>
    /** Returns false when durable storage did not accept the update. */
    fun markDelivered(month: YearMonth, thresholds: Set<BudgetThreshold>): Boolean
    fun lastPaceAlertDate(): LocalDate?
    /** Also records a day suppressed by a higher-priority threshold alert. */
    fun markPaceAlertDate(date: LocalDate): Boolean
}

interface BudgetAlertPermission {
    fun canPost(): Boolean
}

interface BudgetAlertSender {
    fun send(month: YearMonth, decision: BudgetAlertDecision)
    fun sendPace(decision: SpendingPaceAlertDecision)
}
