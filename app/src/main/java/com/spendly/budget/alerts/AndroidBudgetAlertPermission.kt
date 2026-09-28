package com.spendly.budget.alerts

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** Posting permission is separate from Wallet notification-listener access. */
class AndroidBudgetAlertPermission(context: Context) : BudgetAlertPermission {
    private val appContext = context.applicationContext

    override fun canPost(): Boolean = runCatching {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val runtimeGranted = !needsRuntimePermission()
        runtimeGranted && manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(AndroidBudgetAlertSender.CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }.getOrDefault(false)

    fun needsRuntimePermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        appContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED

    fun openSystemSettings(): Boolean = runCatching {
        appContext.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
