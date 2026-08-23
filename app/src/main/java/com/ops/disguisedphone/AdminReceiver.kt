package com.ops.disguisedphone

import android.app.admin.DeviceAdminReceiver

/**
 * Registering this app as a Device Admin blocks direct uninstall from
 * Settings > Apps. To uninstall, the user must first go to
 * Settings > Security > Device admin apps > Phone > Deactivate,
 * then uninstall normally.
 */
class AdminReceiver : DeviceAdminReceiver()
