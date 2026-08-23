package com.ops.disguisedphone

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    private var authenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        if (PasswordStore.isSet(this) && !authenticated) {
            renderGate()
        } else if (!PasswordStore.isSet(this)) {
            renderCreatePassword()
        } else {
            renderMain()
        }
    }

    private fun renderGate() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 300, 64, 64)
            setBackgroundColor(0xFF000000.toInt())
        }

        layout.addView(TextView(this).apply {
            text = "Hello"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
        })

        val box = GradientDrawable().apply {
            setColor(0xFF1A1A1A.toInt())
            cornerRadius = 16f
            setStroke(2, 0x33FFFFFF)
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            transformationMethod = PasswordTransformationMethod.getInstance()
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
                    if (PasswordStore.verify(this@SetupActivity, v.text.toString())) {
                        authenticated = true
                        render()
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
    }

    private fun renderCreatePassword() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val wordInput = EditText(this).apply {
            hint = "Security word"
            inputType = InputType.TYPE_CLASS_TEXT
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        val confirmInput = EditText(this).apply {
            hint = "Confirm"
            inputType = InputType.TYPE_CLASS_TEXT
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        layout.addView(wordInput)
        layout.addView(confirmInput)

        layout.addView(Button(this).apply {
            text = "Save"
            setOnClickListener {
                val w = wordInput.text.toString()
                val c = confirmInput.text.toString()
                when {
                    w.isBlank() -> Toast.makeText(this@SetupActivity, "Enter a word first", Toast.LENGTH_SHORT).show()
                    w != c -> Toast.makeText(this@SetupActivity, "Doesn't match", Toast.LENGTH_SHORT).show()
                    else -> {
                        PasswordStore.setPassword(this@SetupActivity, w)
                        authenticated = true
                        render()
                    }
                }
            }
        })

        setContentView(layout)
    }

    private fun renderMain() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val oldInput = EditText(this).apply {
            hint = "Current word"
            inputType = InputType.TYPE_CLASS_TEXT
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        val newInput = EditText(this).apply {
            hint = "New word"
            inputType = InputType.TYPE_CLASS_TEXT
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        val confirmNewInput = EditText(this).apply {
            hint = "Confirm new word"
            inputType = InputType.TYPE_CLASS_TEXT
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        layout.addView(oldInput)
        layout.addView(newInput)
        layout.addView(confirmNewInput)

        layout.addView(Button(this).apply {
            text = "Change"
            setOnClickListener {
                val old = oldInput.text.toString()
                val newW = newInput.text.toString()
                val confirmNew = confirmNewInput.text.toString()
                if (!PasswordStore.verify(this@SetupActivity, old)) {
                    oldInput.text.clear()
                    return@setOnClickListener
                }
                when {
                    newW.isBlank() -> Toast.makeText(this@SetupActivity, "Enter a new word", Toast.LENGTH_SHORT).show()
                    newW != confirmNew -> Toast.makeText(this@SetupActivity, "New word doesn't match", Toast.LENGTH_SHORT).show()
                    else -> {
                        PasswordStore.setPassword(this@SetupActivity, newW)
                        Toast.makeText(this@SetupActivity, "Changed", Toast.LENGTH_SHORT).show()
                        oldInput.text.clear()
                        newInput.text.clear()
                        confirmNewInput.text.clear()
                    }
                }
            }
        })

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

        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, AdminReceiver::class.java)
        val isAdmin = dpm.isAdminActive(adminComponent)

        layout.addView(Button(this).apply {
            text = if (isAdmin) "Device admin: ON" else "Prevent uninstall"
            setOnClickListener {
                if (isAdmin) {
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                } else {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    }
                    startActivity(intent)
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Enable shade-block (Accessibility)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        setContentView(layout)
    }
}
