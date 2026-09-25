package com.example.xboxcloudviewer

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton

class SecondaryDisplayPresentation(outerContext: Context, display: Display) : 
    Presentation(outerContext, display, R.style.Theme_XboxCloudViewer) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Empêcher l'écran secondaire de "voler" le focus de la manette
        // On retire FLAG_NOT_TOUCHABLE pour permettre le clic tactile sur le bouton de paramètres
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
        
        setContentView(R.layout.layout_secondary_screen)

        val btnSettings = findViewById<ImageButton>(R.id.btn_settings)
        val settingsView = findViewById<SettingsView>(R.id.settings_view)

        btnSettings?.setOnClickListener {
            settingsView?.visibility = View.VISIBLE
        }

        settingsView?.onCloseClicked = {
            settingsView.visibility = View.GONE
        }
    }
}
