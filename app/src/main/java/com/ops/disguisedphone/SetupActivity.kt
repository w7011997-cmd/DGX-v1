package com.ops.disguisedphone

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SetupActivity : AppCompatActivity() {

    private var authenticated = false
    private var showingGatePrompt = false
    private var showingHomePrompt = false
    private var showingApps = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra("skip_gate", false) == true) {
            authenticated = true
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        SetupForeground.inForeground = true
    }

    override fun onPause() {
        super.onPause()
        SetupForeground.inForeground = false
    }

    private fun render() {
        when {
            PasswordStore.isSet(this) && !authenticated ->
                if (showingGatePrompt) {
                    renderPasswordPrompt { authenticated = true; render() }
                } else {
                    renderBlankGate { showingGatePrompt = true; render() }
                }
            !PasswordStore.isSet(this) -> renderCreatePassword()
            showingHomePrompt -> renderPasswordPrompt {
                activateAsHomeApp()
                showingHomePrompt = false
                render()
            }
            showingApps -> renderApps()
            else -> renderMain()
        }
    }

    private fun renderBlankGate(onDoubleTap: () -> Unit) {
        val root = FrameLayout(this).apply {
            setBackgroundColor(0xFF000000.toInt())
            isClickable = true
        }
        var lastDownTime = 0L
        root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val now = System.currentTimeMillis()
                if (now - lastDownTime <= 350) {
                    lastDownTime = 0L
                    onDoubleTap()
                } else {
                    lastDownTime = now
                }
                true
            } else {
                false
            }
        }
        setContentView(root)
    }

    /** Reusable "Hello" + numeric-first masked input. Double-tap Hello switches to full keyboard. */
    private fun renderPasswordPrompt(onSuccess: () -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 300, 64, 64)
            setBackgroundColor(0xFF000000.toInt())
        }

        val box = GradientDrawable().apply {
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
                    if (PasswordStore.verify(this@SetupActivity, v.text.toString())) {
                        onSuccess()
                    } else {
                        IntruderCapture.capture(this@SetupActivity, this@SetupActivity)
                        (v as EditText).text.clear()
                    }
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
        layout.addView(hello)
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

    private fun activateAsHomeApp() {
        DisguiseState.setActive(this, true)
        val component = ComponentName(this, DisguiseActivity::class.java)
        packageManager.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Hide our own launcher icon now that double-tap-on-Home is a working entry point.
        val alias = ComponentName(this, "com.ops.disguisedphone.SetupLauncherAlias")
        packageManager.setComponentEnabledSetting(
            alias,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    private fun renderApps() {
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 64, 32, 32)
        }

        val backRow = TextView(this).apply {
            text = "← Back"
            textSize = 16f
            setPadding(24, 24, 24, 24)
            setOnClickListener {
                showingApps = false
                render()
            }
        }
        list.addView(backRow)

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

    private fun renderMain() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        layout.addView(Button(this).apply {
            text = "Apps"
            setOnClickListener {
                showingApps = true
                render()
            }
        })

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
                    IntruderCapture.capture(this@SetupActivity, this@SetupActivity)
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
                showingHomePrompt = true
                render()
            }
        })

        layout.addView(Button(this).apply {
            text = "Give back my launcher"
            setOnClickListener {
                DisguiseState.setActive(this@SetupActivity, false)

                val component = ComponentName(this@SetupActivity, DisguiseActivity::class.java)
                packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )

                // Icon comes back since the double-tap entry point no longer exists.
                val alias = ComponentName(this@SetupActivity, "com.ops.disguisedphone.SetupLauncherAlias")
                packageManager.setComponentEnabledSetting(
                    alias,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )

                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
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
                    val i = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    }
                    startActivity(i)
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Enable shade-block (Accessibility)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        })

        layout.addView(Button(this).apply {
            text = "Enable screen pinning"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            }
        })

        val hasSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED
        layout.addView(Button(this).apply {
            text = if (hasSmsPermission) "SMS wipe trigger: ON" else "Grant SMS permission"
            setOnClickListener {
                if (!hasSmsPermission) {
                    ActivityCompat.requestPermissions(
                        this@SetupActivity,
                        arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_PHONE_STATE),
                        1002
                    )
                }
            }
        })

        layout.addView(Button(this).apply {
            text = "Grant exact alarm scheduling"
            setOnClickListener {
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } else {
                    Toast.makeText(this@SetupActivity, "Not needed on this Android version", Toast.LENGTH_SHORT).show()
                }
            }
        })

        val liveMode = WipeModeState.isLiveMode(this)
        layout.addView(Button(this).apply {
            text = if (liveMode) "WIPE MODE: LIVE (tap to switch to Test)" else "WIPE MODE: TEST (tap to switch to Live)"
            setOnClickListener {
                WipeModeState.setLiveMode(this@SetupActivity, !liveMode)
                render()
            }
        })

        val hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

        layout.addView(Button(this).apply {
            text = if (hasCameraPermission) "Camera access: ON" else "Grant camera access"
            setOnClickListener {
                if (!hasCameraPermission) {
                    ActivityCompat.requestPermissions(
                        this@SetupActivity,
                        arrayOf(Manifest.permission.CAMERA),
                        1001
                    )
                }
            }
        })

        setContentView(layout)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }
}
