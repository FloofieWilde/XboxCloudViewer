package com.example.xboxcloudviewer

import android.annotation.SuppressLint
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private var secondaryPresentation: SecondaryDisplayPresentation? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Cacher la barre d'action si elle existe
        supportActionBar?.hide()

        // Mode plein écran immersif (sans barre de statut ni de navigation)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Création de la WebView prenant tout l'écran principal
        val webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // Activation de JavaScript et du stockage DOM (nécessaire pour Xbox Cloud)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                
                // Bloquer le zoom
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                
                // Adapter la vue à l'écran (respect du viewport)
                useWideViewPort = true
                loadWithOverviewMode = true
            }
            
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            
            loadUrl("https://play.xbox.com")
        }

        setContentView(webView)

        // Intercepter l'action "Retour" (souvent mappée au bouton B de la manette sur Android)
        // pour empêcher l'application de se fermer accidentellement.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // On ne fait volontairement rien. L'application restera ouverte.
                // La WebView traitera l'événement B en interne pour le jeu.
            }
        })
    }

    override fun onResume() {
        super.onResume()
        showPresentationOnSecondaryDisplay()
    }

    override fun onPause() {
        super.onPause()
        secondaryPresentation?.dismiss()
        secondaryPresentation = null
    }

    private fun showPresentationOnSecondaryDisplay() {
        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        
        // On récupère d'abord les écrans spécifiquement désignés pour les présentations
        val presentationDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        
        // Si aucun n'est taggé "PRESENTATION", on teste sur tous les écrans disponibles
        val allDisplays = if (presentationDisplays.isNotEmpty()) presentationDisplays else displayManager.displays

        for (display in allDisplays) {
            // On ignore l'écran principal
            if (display.displayId != Display.DEFAULT_DISPLAY) {
                secondaryPresentation = SecondaryDisplayPresentation(this, display)
                try {
                    secondaryPresentation?.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                break // On s'arrête au premier écran secondaire trouvé
            }
        }
    }
}
