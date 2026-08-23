package com.ops.disguisedphone

import android.content.Context

object WipeCountdownState {
    private const val PREFS = "wipe_countdown_prefs"
    private const val KEY_PENDING = "pending"
    private const val KEY_CANCELLED = "cancelled"
    private const val KEY_START_TIME = "start_time"
    private const val KEY_DURATION_MS = "duration_ms"

    fun start(context: Context, durationMs: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PENDING, true)
            .putBoolean(KEY_CANCELLED, false)
            .putLong(KEY_START_TIME, System.currentTimeMillis())
            .putLong(KEY_DURATION_MS, durationMs)
            .apply()
    }

    fun isPending(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_PENDING, false)

    fun cancel(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_CANCELLED, true)
            .apply()
    }

    fun isCancelled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CANCELLED, false)

    fun remainingMs(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val start = prefs.getLong(KEY_START_TIME, 0L)
        val duration = prefs.getLong(KEY_DURATION_MS, 0L)
        val elapsed = System.currentTimeMillis() - start
        return (duration - elapsed).coerceAtLeast(0L)
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PENDING, false)
            .apply()
    }
}
