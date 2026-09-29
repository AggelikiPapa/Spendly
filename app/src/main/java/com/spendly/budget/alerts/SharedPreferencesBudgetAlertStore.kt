package com.spendly.budget.alerts

import android.content.Context
import android.content.SharedPreferences
import java.time.YearMonth
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow

/** Small durable store for an opt-in switch and month-scoped delivered thresholds. */
class SharedPreferencesBudgetAlertStore(context: Context) : BudgetAlertStore {
    private val preferences = context.applicationContext.getSharedPreferences("budget_alerts", Context.MODE_PRIVATE)
    private val mutableEnabled = MutableStateFlow(preferences.getBoolean(ENABLED, false))
    private val mutablePaceEnabled = MutableStateFlow(preferences.getBoolean(PACE_ENABLED, false))
    override val enabled = mutableEnabled
    override val paceEnabled = mutablePaceEnabled
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == ENABLED) mutableEnabled.value = prefs.getBoolean(ENABLED, false)
        if (key == PACE_ENABLED) mutablePaceEnabled.value = prefs.getBoolean(PACE_ENABLED, false)
    }

    init { preferences.registerOnSharedPreferenceChangeListener(listener) }

    override fun isEnabled(): Boolean = preferences.getBoolean(ENABLED, false)

    override fun setEnabled(enabled: Boolean): Boolean = preferences.edit().putBoolean(ENABLED, enabled).commit().also {
        if (it) mutableEnabled.value = enabled
    }

    override fun isPaceEnabled(): Boolean = preferences.getBoolean(PACE_ENABLED, false)

    override fun setPaceEnabled(enabled: Boolean): Boolean = preferences.edit().putBoolean(PACE_ENABLED, enabled).commit().also {
        if (it) mutablePaceEnabled.value = enabled
    }

    override fun delivered(month: YearMonth): Set<BudgetThreshold> = preferences.getStringSet(key(month), emptySet())
        .orEmpty().mapNotNull { name -> BudgetThreshold.entries.firstOrNull { it.name == name } }.toSet()

    override fun markDelivered(month: YearMonth, thresholds: Set<BudgetThreshold>): Boolean {
        val names = delivered(month).mapTo(mutableSetOf()) { it.name }
        names += thresholds.map { it.name }
        return preferences.edit().putStringSet(key(month), names).commit()
    }

    override fun lastPaceAlertDate(): LocalDate? = preferences.getString(LAST_PACE_DATE, null)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    override fun markPaceAlertDate(date: LocalDate): Boolean =
        preferences.edit().putString(LAST_PACE_DATE, date.toString()).commit()

    private fun key(month: YearMonth) = "delivered_$month"

    private companion object {
        const val ENABLED = "enabled"
        const val PACE_ENABLED = "pace_enabled"
        const val LAST_PACE_DATE = "last_pace_alert_date"
    }
}
