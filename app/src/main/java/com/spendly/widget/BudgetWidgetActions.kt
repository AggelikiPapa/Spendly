package com.spendly.widget

import android.content.Context
import android.content.Intent
import com.spendly.MainActivity

object BudgetWidgetActions {
    fun dashboardIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
}
