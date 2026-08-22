package com.ops.disguisedphone

import android.content.Context

/**
 * Single source of truth for whether disguise mode is currently ON.
 * Backed by SharedPreferences so the Activity and the NotificationListenerService
 * (which run in different lifecycles) stay in sync.
 */
object DisguiseState {
    private const val PREFS = "disguise_prefs"
    private const val KEY_ACTIVE = "disguise_active"

    fun isActive(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACTIVE, true) // default ON: safest default is "hidden"
    }

    fun setActive(context: Context, active: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACTIVE, active)
            .apply()
    }
}
