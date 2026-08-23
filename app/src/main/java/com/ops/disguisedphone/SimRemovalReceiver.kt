package com.ops.disguisedphone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class SimRemovalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!WipeModeState.isLiveMode(context)) return

        val state = intent.getStringExtra("ss")
            ?: intent.getStringExtra(TelephonyManager.EXTRA_SIM_STATE)
        if (state == "ABSENT" || state == TelephonyManager.SIM_STATE_ABSENT.toString()) {
            WipeExecutor.executeWipe(context)
        }
    }
}
