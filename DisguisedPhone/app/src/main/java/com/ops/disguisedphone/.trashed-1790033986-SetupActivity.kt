package com.ops.disguisedphone

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Normal launcher-icon entry point. Use this once to:
 *  1. Grant Notification Access (system won't allow silent grant).
 *  2. Open the Home app chooser so you can set this app as your default launcher.
 * After that, DisguiseActivity is what you'll see on the home button.
 */
class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        layout.addView(TextView(this).apply {
            text = "Setup"
            textSize = 22f
        })

        layout.addView(Button(this).apply {
            text = "1. Grant notification access"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        })

        layout.addView(Button(this).apply {
            text = "2. Set as default Home app"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        })

        layout.addView(TextView(this).apply {
            text = "\nOnce both are set, press Home to see the disguise screen. " +
                "Double-tap the bottom edge of the screen and confirm your fingerprint to unlock."
            textSize = 14f
        })

        setContentView(layout)
    }
}
