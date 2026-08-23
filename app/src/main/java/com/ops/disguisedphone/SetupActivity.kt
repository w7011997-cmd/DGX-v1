package com.ops.disguisedphone

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {

    private var showingHomePrompt = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        if (showingHomePrompt) {
            renderHomePrompt()
        } else {
            renderMain()
        }
    }

    private fun renderHomePrompt() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 300, 64, 64)
        }

        layout.addView(TextView(this).apply {
            text = "Hello"
            textSize = 20f
        })

        val box = GradientDrawable().apply {
            setColor(0xFFE8E8E8.toInt())
            cornerRadius = 16f
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_GO
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
                        activateAsHomeApp()
                        showingHomePrompt = false
                        render()
                    } else {
                        Toast.makeText(this@SetupActivity, "Incorrect", Toast.LENGTH_SHORT).show()
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

    private fun activateAsHomeApp() {
        DisguiseState.setActive(this, true)
        val component = ComponentName(this, DisguiseActivity::class.java)
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private fun renderMain() {
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

            val oldInput = EditText(this).apply {
                hint = "Current security word"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            val newInput = EditText(this).apply {
                hint = "New security word"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            val confirmNewInput = EditText(this).apply {
                hint = "Confirm new word"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
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
                    when {
                        !PasswordStore.verify(this@SetupActivity, old) ->
                            Toast.makeText(this@SetupActivity, "Current word is incorrect", Toast.LENGTH_SHORT).show()
                        newW.isBlank() ->
                            Toast.makeText(this@SetupActivity, "Enter a new word", Toast.LENGTH_SHORT).show()
                        newW != confirmNew ->
                            Toast.makeText(this@SetupActivity, "New word doesn't match", Toast.LENGTH_SHORT).show()
                        else -> {
                            PasswordStore.setPassword(this@SetupActivity, newW)
                            Toast.makeText(this@SetupActivity, "Changed", Toast.LENGTH_SHORT).show()
                            render()
                        }
                    }
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
                if (!PasswordStore.isSet(this@SetupActivity)) {
                    Toast.makeText(this@SetupActivity, "Set a security word first", Toast.LENGTH_SHORT).show()
                } else {
                    showingHomePrompt = true
                    render()
                }
            }
        })

        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, AdminReceiver::class.java)
        val isAdmin = dpm.isAdminActive(adminComponent)

        layout.addView(Button(this).apply {
            text = if (isAdmin) "Device admin: ON (tap to open settings)" else "Prevent uninstall (enable Device Admin)"
            setOnClickListener {
                if (isAdmin) {
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                } else {
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                        putExtra(
                            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Prevents this app from being uninstalled without deactivating admin first."
                        )
                    }
                    startActivity(intent)
                }
            }
        })

        setContentView(layout)
    }
}
