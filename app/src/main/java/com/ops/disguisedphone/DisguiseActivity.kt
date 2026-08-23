package com.ops.disguisedphone

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class DisguiseActivity : AppCompatActivity() {

    private var lastTapTime = 0L
    private val doubleTapWindowMs = 350L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { render() } catch (t: Throwable) { showCrashInfo(t) }
    }

    private fun showCrashInfo(t: Throwable) {
        val tv = TextView(this).apply {
            text = "CRASH:\n\n" + android.util.Log.getStackTraceString(t)
            textSize = 12f
            setPadding(24, 64, 24, 24)
            setTextColor(0xFFFFFFFF.toInt())
        }
        val scroll = ScrollView(this)
        scroll.addView(tv)
        setContentView(scroll)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try { render() } catch (t: Throwable) { showCrashInfo(t) }
    }

    override fun onResume() {
        super.onResume()
        try { render() } catch (t: Throwable) { showCrashInfo(t) }
    }

    private fun render() {
        if (DisguiseState.isActive(this)) showLockedScreen() else showUnlockedScreen()
    }

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
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            val reason = when (canAuth) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No fingerprint enrolled in phone Settings yet"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No fingerprint hardware detected"
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Fingerprint sensor temporarily unavailable"
                else -> "Fingerprint unlock unavailable (code $canAuth)"
            }
            android.widget.Toast.makeText(this, reason, android.widget.Toast.LENGTH_LONG).show()
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
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(info)
    }

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

    override fun onBackPressed() {
        if (!DisguiseState.isActive(this)) {
            super.onBackPressed()
        }
    }
}

private class FrameLayoutBottomZone(
    context: android.content.Context,
    private val onZoneDoubleTap: () -> Unit
) : android.widget.FrameLayout(context) {

    init {
        setBackgroundColor(0xFF000000.toInt())
        isClickable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            onZoneDoubleTap()
            return true
        }
        return super.onTouchEvent(event)
    }
}
