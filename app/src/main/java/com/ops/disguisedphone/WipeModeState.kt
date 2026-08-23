package com.ops.disguisedphone

import android.content.Context

/**
 * Test Mode (default): SMS trigger runs the full pipeline but never actually
 * wipes -- shows a visible countdown instead, for safe testing. SIM-removal
 * and power-off evasion detection are completely disabled in this mode.
 *
 * Live Mode: SMS trigger wipes silently after a short delay, no visible
 * countdown (so a thief doesn't get warned to pull the SIM or power off
 * first). SIM removal or a power-off attempt trigger an immediate wipe.
 */
object WipeModeState {
    private const val PREFS = "wipe_mode_prefs"
    private const val KEY_LIVE = "live_mode"

    fun isLiveMode(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LIVE, false) // default: Test Mode
    }

    fun setLiveMode(context: Context, live: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LIVE, live)
            .apply()
    }
}
