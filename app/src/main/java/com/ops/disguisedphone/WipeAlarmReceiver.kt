package com.ops.disguisedphone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WipeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (WipeCountdownState.isCancelled(context)) {
            WipeCountdownState.clear(context)
            return
        }
        WipeExecutor.executeWipe(context)
        WipeCountdownState.clear(context)
    }
}
