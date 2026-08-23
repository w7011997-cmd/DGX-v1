package com.ops.disguisedphone

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Watches for System UI window-state changes (notification shade, quick
 * settings, recents overview, etc.) and immediately triggers a back action
 * to collapse them, but only while DisguiseState is active.
 *
 * This cannot distinguish exactly which System UI surface opened -- it
 * treats any System UI window-state change the same way -- so it's a blunt
 * instrument. There will likely be a brief visible flicker before it closes.
 * Requires manual activation in Settings > Accessibility (Android does not
 * allow apps to silently grant themselves this permission).
 */
class ShadeBlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!DisguiseState.isActive(applicationContext)) return

        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun onInterrupt() {}
}
