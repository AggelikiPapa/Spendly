package com.spendly.budget.alerts

import android.content.Context
import android.content.SharedPreferences
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow

/** Small durable store for an opt-in switch and month-scoped delivered thresholds. */
class SharedPreferencesBudgetAlertStore(context: Context) : BudgetAlertStore {
    private val preferences = context.applicationContext.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE)
    private val mutableEnabled = MutableStateFlow(preferences.getBoolean(ENABLED, false))
    override val enabled = mutableEnabled
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == ENABLED) mutableEnabled.value = prefs.getBoolean(ENABLED, false)
    }

    init { preferences.registerOnSharedPreferenceChangeListener(listener) }

    override fun isEnabled(): Boolean = preferences.getBoolean(ENABLED, false)

    override fun setEnabled(enabled: Boolean): Boolean = preferences.edit().putBoolean(ENABLED, enabled).commit().also {
        if (it) mutableEnabled.value = enabled
    }

    override fun delivered(month: YearMonth): Set<BudgetThreshold> = preferences.getStringSet(key(month), emptySet())
        .orEmpty().mapNotNull { name -> BudgetThreshold.entries.firstOrNull { it.name == name } }.toSet()

    override fun markDelivered(month: YearMonth, thresholds: Set<BudgetThreshold>): Boolean {
        val names = delivered(month).mapTo(mutableSetOf()) { it.name }
        names += thresholds.map { it.name }
        return preferences.edit().putStringSet(key(month), names).commit()
    }

    private fun key(month: YearMonth) = "delivered_$month"

    private companion object { const val ENABLED = "enabled" }
}
