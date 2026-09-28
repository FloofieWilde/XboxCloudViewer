package com.example.xboxcloudviewer.services

import android.util.Log
import android.webkit.CookieManager
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object ProfileScrapingService {

    fun scrapeAvatarUrl(gamertag: String): String? {
        // Nouvelle priorité 1 : Tenter le scraping silencieux via CookieManager authentifié !
        val authenticatedAvatar = scrapeFromOfficialXbox(gamertag)
        if (authenticatedAvatar != null) return authenticatedAvatar
        
        // Priorité 2 : Si on n'est pas loggé ou que le scraping a échoué, on tente xboxgamertag
        return scrapeFromXboxGamertag(gamertag)
    }

    private fun scrapeFromOfficialXbox(gamertag: String): String? {
        val encodedName = URLEncoder.encode(gamertag.trim(), "UTF-8").replace("+", "%20")
        try {
            val searchUrl = "https://www.xbox.com/fr-FR/play/user/$encodedName"
            Log.d("ProfileScraper", "[Official] Fetching $searchUrl")
            val connection = URL(searchUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            // Injecter les cookies partagés par la WebView principale !
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://www.xbox.com")
            if (cookies != null) {
                connection.setRequestProperty("Cookie", cookies)
                Log.d("ProfileScraper", "[Official] Cookies injected for auth.")
            }
            
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().readText()
                val regex = """https://images-[a-zA-Z0-9-]+\.xboxlive\.com/image\?url=[^"'\s>\\]+""".toRegex()
                val matchResult = regex.find(html)
                if (matchResult != null) {
                    val found = matchResult.value.replace("&amp;", "&").replace("\\u0026", "&")
                    Log.d("ProfileScraper", "[Official] Found: $found")
                    return found
                }
            }
        } catch (e: Exception) { 
            Log.e("ProfileScraper", "[Official] Exception", e)
        }
        return null
    }

    private fun scrapeFromXboxGamertag(gamertag: String): String? {
        val formattedName = gamertag.trim().replace(" ", "-")
        try {
            val searchUrl = "https://xboxgamertag.com/search/$formattedName"
            Log.d("ProfileScraper", "[Fallback] Fetching $searchUrl")
            val connection = URL(searchUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().readText()
                val regex = """https://images-[a-zA-Z0-9-]+\.xboxlive\.com/image\?url=[^"'\s>]+""".toRegex()
                val matchResult = regex.find(html)
                if (matchResult != null) {
                    val found = matchResult.value.replace("&amp;", "&")
                    Log.d("ProfileScraper", "[Fallback] Found: $found")
                    return found
                }
            }
        } catch (e: Exception) { }
        return null
    }
}