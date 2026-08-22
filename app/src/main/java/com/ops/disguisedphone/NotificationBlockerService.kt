package com.ops.disguisedphone

import android.service.notification.StatusBarNotification
import android.service.notification.NotificationListenerService

/**
 * Requires the user to manually grant "Notification access" in
 * Settings > Apps > Special app access > Notification access.
 * Android does not allow this permission to be granted silently by the app.
 *
 * While DisguiseState is active, every incoming notification is
 * cancelled immediately so nothing reaches the status bar / lock screen.
 */
class NotificationBlockerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (DisguiseState.isActive(applicationContext)) {
            // Don't cancel our own foreground/setup notifications if we add any later.
            if (sbn.packageName != applicationContext.packageName) {
                cancelNotification(sbn.key)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // If disguise was active before a reboot/app restart, clear the shade immediately.
        if (DisguiseState.isActive(applicationContext)) {
            try {
                activeNotifications?.forEach { sbn ->
                    if (sbn.packageName != applicationContext.packageName) {
                        cancelNotification(sbn.key)
                    }
                }
            } catch (_: SecurityException) {
                // Listener not fully connected yet on some OEM skins; safe to ignore.
            }
        }
    }
}
