package com.example.xboxcloudviewer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch

class SettingsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onCloseClicked: (() -> Unit)? = null

    init {
        // Charge le layout XML dans ce FrameLayout
        LayoutInflater.from(context).inflate(R.layout.layout_settings_view, this, true)

        val btnClose = findViewById<ImageButton>(R.id.btn_close_settings)
        val tabGeneral = findViewById<Button>(R.id.tab_general)
        val tabAbout = findViewById<Button>(R.id.tab_about)
        val contentGeneral = findViewById<LinearLayout>(R.id.content_general)
        val contentAbout = findViewById<ScrollView>(R.id.content_about)
        val switchXboxButtons = findViewById<Switch>(R.id.switch_xbox_buttons)

        // Gestion de la sauvegarde
        val prefs = context.applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        switchXboxButtons.isChecked = prefs.getBoolean("use_xbox_button_display", false)

        switchXboxButtons.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("use_xbox_button_display", isChecked).apply()
        }

        // Événements d'interface
        btnClose.setOnClickListener {
            onCloseClicked?.invoke() // On délègue la fermeture à la classe parente
        }

        tabGeneral.setOnClickListener {
            contentGeneral.visibility = VISIBLE
            contentAbout.visibility = GONE
        }

        tabAbout.setOnClickListener {
            contentGeneral.visibility = GONE
            contentAbout.visibility = VISIBLE
        }
    }
}