package com.example.stressbustersimulator

import android.content.Context
import android.os.Bundle
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val cbSound = findViewById<CheckBox>(R.id.cbSound)
        val cbVibration = findViewById<CheckBox>(R.id.cbVibration)

        val prefs = getSharedPreferences(Prefs.PREFS_NAME, Context.MODE_PRIVATE)
        val soundOn = prefs.getBoolean(Prefs.KEY_SOUND, true)
        val vibOn = prefs.getBoolean(Prefs.KEY_VIBRATION, true)

        cbSound.isChecked = soundOn
        cbVibration.isChecked = vibOn

        cbSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Prefs.KEY_SOUND, isChecked).apply()
        }

        cbVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(Prefs.KEY_VIBRATION, isChecked).apply()
        }
    }
}
