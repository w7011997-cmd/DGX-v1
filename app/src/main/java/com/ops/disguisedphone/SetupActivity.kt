package com.ops.disguisedphone

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        layout.addView(TextView(this).apply {
            text = "Setup"
            textSize = 22f
        })

        if (!PasswordStore.isSet(this)) {
            layout.addView(TextView(this).apply {
                text = "\nSet your security word or sentence. You'll type this to unlock your phone later."
                textSize = 14f
            })

            val wordInput = EditText(this).apply {
                hint = "Security word/sentence"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            val confirmInput = EditText(this).apply {
                hint = "Confirm"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            layout.addView(wordInput)
            layout.addView(confirmInput)

            layout.addView(Button(this).apply {
                text = "Save security word"
                setOnClickListener {
                    val w = wordInput.text.toString()
                    val c = confirmInput.text.toString()
                    when {
                        w.isBlank() -> Toast.makeText(this@SetupActivity, "Enter a word first", Toast.LENGTH_SHORT).show()
                        w != c -> Toast.makeText(this@SetupActivity, "Doesn't match", Toast.LENGTH_SHORT).show()
                        else -> {
                            PasswordStore.setPassword(this@SetupActivity, w)
                            Toast.makeText(this@SetupActivity, "Saved", Toast.LENGTH_SHORT).show()
                            render()
                        }
                    }
                }
            })
        } else {
            layout.addView(TextView(this).apply {
                text = "\n✅ Security word is set."
                textSize = 14f
            })

            layout.addView(Button(this).apply {
                text = "Change security word"
                setOnClickListener {
                    getSharedPreferences("password_prefs", MODE_PRIVATE).edit().clear().apply()
                    render()
                }
            })
        }

        layout.addView(Button(this).apply {
            text = "Grant notification access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        layout.addView(Button(this).apply {
            text = "Set as default Home app"
            setOnClickListener {
                DisguiseState.setActive(this@SetupActivity, true)
                val component = ComponentName(this@SetupActivity, DisguiseActivity::class.java)
                packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        })

        layout.addView(TextView(this).apply {
            text = "\nOnce set up, press Home to see the black screen. Double-tap anywhere, type your security word, and press the keyboard's arrow key to unlock and get the Home-app picker back."
            textSize = 14f
        })

        setContentView(layout)
    }
}
