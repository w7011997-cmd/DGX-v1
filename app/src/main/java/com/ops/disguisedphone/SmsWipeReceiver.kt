package com.ops.disguisedphone

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Telephony

class SmsWipeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!PasswordStore.isSet(context)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }.trim()

        if (!PasswordStore.verify(context, body)) return

        val live = WipeModeState.isLiveMode(context)
        val durationMs = if (live) 10_000L else 180_000L

        WipeCountdownState.start(context, durationMs)
        scheduleAlarm(context, durationMs)

        if (!live) {
            // Test Mode: show the visible countdown screen.
            val activityIntent = Intent(context, WipeCountdownActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(activityIntent)
        }
        // Live Mode: nothing shown at all.
    }

    private fun scheduleAlarm(context: Context, delayMs: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WipeAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayMs,
                pi
            )
        } catch (e: Exception) {
            // Missing "schedule exact alarms" permission; falls back to inexact.
            am.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delayMs,
                pi
            )
        }
    }
}
