package com.spendly.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

class AndroidBudgetWidgetUpdater(context: Context) : BudgetWidgetUpdater {
    private val appContext = context.applicationContext

    override suspend fun update() {
        val ids = GlanceAppWidgetManager(appContext).getGlanceIds(SpendlyBudgetWidget::class.java)
        if (ids.isNotEmpty()) SpendlyBudgetWidget().updateAll(appContext)
    }
}
