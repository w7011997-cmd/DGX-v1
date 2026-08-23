package com.ops.disguisedphone

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

object WipeExecutor {

    /** Set right before an actual wipe fires, so a Test Mode countdown UI can show "would have wiped" if visible. */
    @Volatile
    var lastWipeWasSimulated: Boolean = false

    fun executeWipe(context: Context) {
        if (!WipeModeState.isLiveMode(context)) {
            lastWipeWasSimulated = true
            return
        }
        lastWipeWasSimulated = false
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val admin = ComponentName(context, AdminReceiver::class.java)
            if (dpm.isAdminActive(admin)) {
                dpm.wipeData(0)
            }
        } catch (e: Exception) {
            // Nothing further we can do if this fails; there's no safe way to
            // surface an error without potentially alerting whoever has the phone.
        }
    }
}
