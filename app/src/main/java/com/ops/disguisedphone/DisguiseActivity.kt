package com.ops.disguisedphone

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

/**
 * This Activity is registered as a HOME/LAUNCHER category activity so it can
 * be selected as the default launcher (Settings > Apps > Default apps > Home app).
 * Once set as default, this is what shows instead of the real launcher.
 *
 * LOCKED (disguise active): near-empty screen, optionally a Phone shortcut.
 * A double-tap in the bottom zone (where a fingerprint sensor usually sits on
 * devices with a rear/front capacitive sensor) triggers a biometric prompt.
 * On success -> unlock.
 *
 * UNLOCKED: shows a plain list of installed apps so you can actually use the phone.
 * Double-tap the same zone again to re-lock instantly (no biometric needed to hide).
 */
class DisguiseActivity : AppCompatActivity() {

    private var lastTapTime = 0L
    private val doubleTapWindowMs = 350L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Fired when the Home button is pressed while another app is open.
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        if (DisguiseState.isActive(this)) {
            showLockedScreen()
        } else {
            showUnlockedScreen()
        }
    }

    // ---------- LOCKED UI ----------

    private fun showLockedScreen() {
        val root = FrameLayoutBottomZone(this) { handleZoneTap() }
        setContentView(root)
    }

    private fun handleZoneTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime <= doubleTapWindowMs) {
            lastTapTime = 0L
            promptUnlock()
        } else {
            lastTapTime = now
        }
    }

    private fun promptUnlock() {
        val canAuth = BiometricManager.from(this)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                DisguiseState.setActive(this@DisguiseActivity, false)
                render()
            }
        })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock")
            .setSubtitle("Confirm your fingerprint to continue")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(info)
    }

    // ---------- UNLOCKED UI ----------

    private fun showUnlockedScreen() {
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
        }

        val lockRow = TextView(this).apply {
            text = "🔒 Tap here to hide again"
            textSize = 16f
            setPadding(24, 24, 24, 24)
            setOnClickListener {
                DisguiseState.setActive(this@DisguiseActivity, true)
                render()
            }
        }
        list.addView(lockRow)

        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        for (app in apps) {
            val row = TextView(this).apply {
                text = app.loadLabel(pm)
                textSize = 18f
                setPadding(24, 32, 24, 32)
                setOnClickListener {
                    val launchIntent = pm.getLaunchIntentForPackage(app.activityInfo.packageName)
                    if (launchIntent != null) startActivity(launchIntent)
                }
            }
            list.addView(row)
        }

        scroll.addView(list)
        setContentView(scroll)
    }

    // Intercept the physical Back / Recents behavior isn't fully possible from
    // an Activity alone; this covers in-app back presses only.
    override fun onBackPressed() {
        // No-op while locked: swallow back presses so the disguise can't be
        // trivially dismissed by backing out. Unlocked mode behaves normally.
        if (!DisguiseState.isActive(this)) {
            super.onBackPressed()
        }
    }
}

/**
 * A near-empty full-screen view with an invisible tap zone across the bottom
 * ~15% of the screen, matching where a fingerprint sensor commonly sits.
 * Double-tapping inside that zone fires the callback.
 */
private class FrameLayoutBottomZone(
    context: android.content.Context,
    private val onZoneDoubleTap: () -> Unit
) : android.widget.FrameLayout(context) {

    init {
        setBackgroundColor(0xFF000000.toInt())
        isClickable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Whole-screen tap zone: this phone's fingerprint sensor is rear-mounted,
        // so no on-screen position corresponds to it. Any double-tap anywhere
        // on the screen triggers the biometric prompt, which then reads
        // whichever sensor the OS has (front, rear, or under-display).
        if (event.action == MotionEvent.ACTION_DOWN) {
            onZoneDoubleTap()
            return true
        }
        return super.onTouchEvent(event)
    }
}
