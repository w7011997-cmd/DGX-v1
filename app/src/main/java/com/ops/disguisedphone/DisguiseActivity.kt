package com.ops.disguisedphone

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DisguiseActivity : AppCompatActivity() {

    private var showingPrompt = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { render() } catch (t: Throwable) { showCrashInfo(t) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showingPrompt = false
        try { render() } catch (t: Throwable) { showCrashInfo(t) }
    }

    override fun onResume() {
        super.onResume()
        try { render() } catch (t: Throwable) { showCrashInfo(t) }
    }

    private fun showCrashInfo(t: Throwable) {
        val tv = TextView(this).apply {
            text = "CRASH:\n\n" + android.util.Log.getStackTraceString(t)
            textSize = 12f
            setPadding(24, 64, 24, 24)
            setTextColor(0xFFFFFFFF.toInt())
        }
        setContentView(tv)
    }

    private fun render() {
        if (showingPrompt) showPromptScreen() else showBlankScreen()
    }

    private fun showBlankScreen() {
        val root = FullScreenDoubleTap(this) {
            showingPrompt = true
            render()
        }
        setContentView(root)
    }

    private fun showPromptScreen() {
        if (!PasswordStore.isSet(this)) {
            showBlankScreen()
            return
        }

        val container = FrameLayout(this).apply { setBackgroundColor(0xFF000000.toInt()) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 300, 64, 64)
        }

        val box = android.graphics.drawable.GradientDrawable().apply {
            setColor(0xFF1A1A1A.toInt())
            cornerRadius = 16f
            setStroke(2, 0x33FFFFFF)
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            imeOptions = EditorInfo.IME_ACTION_GO
            setTextColor(0xFFFFFFFF.toInt())
            background = box
            setPadding(24, 24, 24, 24)
            setSingleLine(true)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 32
            layoutParams = lp
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    handleAttempt(v.text.toString())
                    true
                } else {
                    false
                }
            }
            transformationMethod = PasswordTransformationMethod.getInstance()
        }

        var lastHelloTap = 0L
        val hello = TextView(this).apply {
            text = "Hello"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            isClickable = true
            setOnClickListener {
                val now = System.currentTimeMillis()
                if (now - lastHelloTap <= 350) {
                    lastHelloTap = 0L
                    input.inputType = InputType.TYPE_CLASS_TEXT
                    input.transformationMethod = PasswordTransformationMethod.getInstance()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.restartInput(input)
                } else {
                    lastHelloTap = now
                }
            }
        }
        column.addView(hello)
        column.addView(input)

        container.addView(column)
        setContentView(container)
        input.requestFocus()
    }

    private fun handleAttempt(attempt: String) {
        if (PasswordStore.verify(this, attempt)) {
            unlockAndOpenSetup()
        } else {
            IntruderCapture.capture(this, this)
            showingPrompt = false
            render()
        }
    }

    private fun unlockAndOpenSetup() {
        val intent = Intent(this, SetupActivity::class.java).apply {
            putExtra("skip_gate", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (showingPrompt) {
            showingPrompt = false
            render()
        }
    }
}

private class FullScreenDoubleTap(
    context: android.content.Context,
    private val onDoubleTap: () -> Unit
) : FrameLayout(context) {

    init {
        setBackgroundColor(0xFF000000.toInt())
        isClickable = true
    }

    private var lastDownTime = 0L

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val now = System.currentTimeMillis()
            if (now - lastDownTime <= 350) {
                lastDownTime = 0L
                onDoubleTap()
            } else {
                lastDownTime = now
            }
            return true
        }
        return super.onTouchEvent(event)
    }
}
