package com.ops.disguisedphone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Best-effort only: ACTION_SHUTDOWN fires a moment before power actually
 * cuts, so there's no guarantee wipeData() finishes executing in time.
 * SIM-removal detection is far more reliable than this.
 */
class ShutdownReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!WipeModeState.isLiveMode(context)) return
        WipeExecutor.executeWipe(context)
    }
}
