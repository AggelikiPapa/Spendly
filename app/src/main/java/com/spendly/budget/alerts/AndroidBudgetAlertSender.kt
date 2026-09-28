package com.spendly.budget.alerts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.spendly.MainActivity
import com.spendly.R
import com.spendly.ui.transactions.MoneyDisplayFormatter
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class AndroidBudgetAlertSender(context: Context) : BudgetAlertSender {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun send(month: YearMonth, decision: BudgetAlertDecision) {
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, appContext.getString(R.string.budget_alert_channel), NotificationManager.IMPORTANCE_DEFAULT,
        ))
        val monthName = month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val firstLine = if (decision.highest == BudgetThreshold.ONE_HUNDRED && decision.spent.amountMinor == decision.limit.amountMinor) {
            appContext.getString(R.string.budget_alert_reached, monthName)
        } else {
            appContext.getString(R.string.budget_alert_used, decision.percentageUsed?.toPlainString() ?: "100+", monthName)
        }
        val difference = decision.limit - decision.spent
        val secondLine = if (difference.amountMinor >= 0) {
            appContext.getString(R.string.budget_alert_remaining, MoneyDisplayFormatter.formatAmount(difference))
        } else {
            appContext.getString(R.string.budget_alert_over, MoneyDisplayFormatter.formatAmount(decision.spent - decision.limit))
        }
        val notification = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(appContext.getString(R.string.app_name))
            .setContentText(firstLine)
            .setStyle(Notification.BigTextStyle().bigText("$firstLine\n$secondLine"))
            .setContentIntent(PendingIntent.getActivity(appContext, 0, dashboardIntent(appContext),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(true)
            .build()
        manager.notify(month.year * 10_000 + month.monthValue * 100 + decision.highest.percent, notification)
    }

    companion object {
        const val CHANNEL_ID = "budget_alerts"

        fun dashboardIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
