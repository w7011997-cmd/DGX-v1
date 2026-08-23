package com.ops.disguisedphone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SimRemovalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!WipeModeState.isLiveMode(context)) return

        val state = intent.getStringExtra("ss")
        if (state == "ABSENT") {
            WipeExecutor.executeWipe(context)
        }
    }
}
