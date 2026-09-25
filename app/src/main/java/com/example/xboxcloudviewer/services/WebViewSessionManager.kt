package com.example.xboxcloudviewer.services

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.example.xboxcloudviewer.models.UserProfile

class WebViewSessionManager(
    private val webView: WebView,
    private val accountManager: AccountManager
) {

    fun saveCurrentSession(onComplete: () -> Unit) {
        val currentUser = accountManager.getCurrentUser()
        
        // Save cookies
        val cookies = CookieManager.getInstance().getCookie("https://play.xbox.com")
        currentUser.cookies = cookies

        // Save local storage
        webView.evaluateJavascript("JSON.stringify(window.localStorage);") { result ->
            val cleanResult = if (result != null && result != "null" && result.startsWith("\"")) {
                result.substring(1, result.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
            } else result
            
            currentUser.localStorage = cleanResult
            accountManager.saveUser(currentUser)
            
            // Run completion on main thread
            webView.post { onComplete() }
        }
    }

    fun switchProfile(newUser: UserProfile, onReady: () -> Unit) {
        // Clear current data
        CookieManager.getInstance().removeAllCookies(null)
        WebStorage.getInstance().deleteAllData()
        webView.clearCache(true)

        // Set new active user
        accountManager.setCurrentUserId(newUser.id)

        // Restore cookies
        if (!newUser.cookies.isNullOrEmpty()) {
            CookieManager.getInstance().setCookie("https://play.xbox.com", newUser.cookies)
        }

        // To restore LocalStorage, we must do it after the page starts loading.
        // For simplicity in this flow, we reload the page, and the MainActivity's
        // WebViewClient will inject the local storage when onPageFinished is called.
        webView.post { onReady() }
    }
    
    fun injectLocalStorage(user: UserProfile) {
        if (user.localStorage.isNullOrEmpty() || user.localStorage == "null") return
        
        val js = """
            try {
                var data = JSON.parse('${user.localStorage}');
                for (var key in data) {
                    window.localStorage.setItem(key, data[key]);
                }
            } catch(e) { console.error('Error injecting local storage', e); }
        """.trimIndent()
        
        webView.evaluateJavascript(js, null)
    }
}