package com.example.xboxcloudviewer

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.WindowManager

class SecondaryDisplayPresentation(outerContext: Context, display: Display) : 
    Presentation(outerContext, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Empêcher l'écran secondaire de "voler" le focus de la manette ou du tactile
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
        
        setContentView(R.layout.layout_secondary_screen)
    }
}