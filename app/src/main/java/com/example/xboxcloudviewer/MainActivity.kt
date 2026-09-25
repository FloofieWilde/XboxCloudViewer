package com.example.xboxcloudviewer

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.xboxcloudviewer.services.AccountManager
import com.example.xboxcloudviewer.services.WebViewSessionManager

class MainActivity : AppCompatActivity() {

    private var secondaryPresentation: SecondaryDisplayPresentation? = null
    private lateinit var webView: WebView

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
        webView = WebView(this).apply {
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
            
            webChromeClient = WebChromeClient()
            
            // WebViewClient spécifique pour gérer l'injection du LocalStorage
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val currentUser = AccountManager(this@MainActivity).getCurrentUser()
                    WebViewSessionManager(this@MainActivity.webView, AccountManager(this@MainActivity)).injectLocalStorage(currentUser)
                }
            }
            
            loadUrl("https://play.xbox.com")
        }

        setContentView(webView)
        
        // Forcer le focus sur la WebView pour être sûr qu'elle capte la manette
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()

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
                
                // --- AJOUT : GESTION DU CHANGEMENT DE COMPTE ---
                secondaryPresentation?.onProfileSwitchRequested = { newUser ->
                    val sessionManager = WebViewSessionManager(webView, AccountManager(this))
                    
                    // 1. Sauvegarder la session du compte actuel
                    sessionManager.saveCurrentSession {
                        // 2. Changer de compte, effacer les données WebView et charger le nouveau
                        sessionManager.switchProfile(newUser) {
                            webView.loadUrl("https://play.xbox.com")
                        }
                    }
                }

                try {
                    secondaryPresentation?.show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                break // On s'arrête au premier écran secondaire trouvé
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Intercepter uniquement le bouton "Back" d'Android (ou de la console)
        // pour le transformer en bouton central "Xbox Guide" (BUTTON_MODE)
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            val xboxGuideEvent = KeyEvent(
                event.downTime,
                event.eventTime,
                event.action,
                KeyEvent.KEYCODE_BUTTON_MODE,
                event.repeatCount,
                event.metaState,
                event.deviceId,
                event.scanCode,
                event.flags,
                event.source
            )
            return super.dispatchKeyEvent(xboxGuideEvent)
        }

        // Récupérer le paramètre sauvegardé pour savoir s'il faut inverser les boutons
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val swapButtons = prefs.getBoolean("use_xbox_button_display", false)

        if (swapButtons) {
            val newKeyCode = when (event.keyCode) {
                KeyEvent.KEYCODE_BUTTON_A -> KeyEvent.KEYCODE_BUTTON_B
                KeyEvent.KEYCODE_BUTTON_B -> KeyEvent.KEYCODE_BUTTON_A
                KeyEvent.KEYCODE_BUTTON_X -> KeyEvent.KEYCODE_BUTTON_Y
                KeyEvent.KEYCODE_BUTTON_Y -> KeyEvent.KEYCODE_BUTTON_X
                else -> event.keyCode
            }

            // Si on a intercepté un bouton à échanger
            if (newKeyCode != event.keyCode) {
                // On crée un nouvel événement identique mais avec le bon KeyCode
                val newEvent = KeyEvent(
                    event.downTime,
                    event.eventTime,
                    event.action,
                    newKeyCode,
                    event.repeatCount,
                    event.metaState,
                    event.deviceId,
                    event.scanCode,
                    event.flags,
                    event.source
                )
                return super.dispatchKeyEvent(newEvent)
            }
        }

        return super.dispatchKeyEvent(event)
    }
}
