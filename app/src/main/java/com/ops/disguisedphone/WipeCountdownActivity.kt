package com.ops.disguisedphone

import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WipeCountdownActivity : AppCompatActivity() {

    private var timer: CountDownTimer? = null
    private lateinit var timeLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 300, 64, 64)
            setBackgroundColor(0xFFB00020.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "TEST MODE\nWipe would trigger in:"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
        })

        timeLabel = TextView(this).apply {
            textSize = 40f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 32, 0, 32)
        }
        layout.addView(timeLabel)

        val input = EditText(this).apply {
            hint = "Enter security word to cancel"
            inputType = InputType.TYPE_CLASS_TEXT
            transformationMethod = PasswordTransformationMethod.getInstance()
            imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    if (PasswordStore.verify(this@WipeCountdownActivity, v.text.toString())) {
                        WipeCountdownState.cancel(this@WipeCountdownActivity)
                        timer?.cancel()
                        finish()
                    } else {
                        (v as EditText).text.clear()
                    }
                    true
                } else {
                    false
                }
            }
        }
        layout.addView(input)

        setContentView(layout)
        input.requestFocus()
        startTimer()
    }

    private fun startTimer() {
        val remaining = WipeCountdownState.remainingMs(this)
        timer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(msLeft: Long) {
                timeLabel.text = "${msLeft / 1000}s"
            }
            override fun onFinish() {
                timeLabel.text = "0s"
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}
