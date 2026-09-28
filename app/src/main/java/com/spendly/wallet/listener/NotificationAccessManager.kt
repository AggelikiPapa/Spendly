package com.spendly.wallet.listener

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

interface NotificationAccessGateway {
    fun isAccessGranted(): Boolean
    fun openSettings(): Boolean
}

/** Reads the Android-owned listener permission and opens the system grant screen. */
class NotificationAccessManager(context: Context) : NotificationAccessGateway {
    private val appContext = context.applicationContext
    private val listenerComponent = ComponentName(appContext, SpendlyNotificationListenerService::class.java)

    override fun isAccessGranted(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.isNotificationListenerAccessGranted(listenerComponent)
        } else {
            // API 26 predates NotificationManager.isNotificationListenerAccessGranted.
            val enabled = Settings.Secure.getString(appContext.contentResolver, ENABLED_LISTENERS).orEmpty()
            enabledListenerComponentsContains(enabled, listenerComponent)
        }
    }.getOrDefault(false)

    override fun openSettings(): Boolean = runCatching {
        appContext.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    }.getOrDefault(false)

    private companion object {
        const val ENABLED_LISTENERS = "enabled_notification_listeners"
    }
}

internal fun enabledListenerComponentsContains(enabled: String?, listener: ComponentName): Boolean =
    enabled.orEmpty().split(':').any { ComponentName.unflattenFromString(it) == listener }
