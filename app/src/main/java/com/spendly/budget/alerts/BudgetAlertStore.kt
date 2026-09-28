package com.spendly.budget.alerts

import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

interface BudgetAlertStore {
    val enabled: Flow<Boolean>
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean): Boolean
    fun delivered(month: YearMonth): Set<BudgetThreshold>
    /** Returns false when durable storage did not accept the update. */
    fun markDelivered(month: YearMonth, thresholds: Set<BudgetThreshold>): Boolean
}

interface BudgetAlertPermission {
    fun canPost(): Boolean
}

interface BudgetAlertSender {
    fun send(month: YearMonth, decision: BudgetAlertDecision)
}
