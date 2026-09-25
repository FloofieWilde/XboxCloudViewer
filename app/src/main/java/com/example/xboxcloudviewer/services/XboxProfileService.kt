package com.example.xboxcloudviewer.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.concurrent.thread

object XboxProfileService {
    
    // Fetch Xbox profile picture based on gamertag using an unofficial public API proxy.
    // This allows getting the real Xbox Live Gamerpic without needing an OAuth2 access token.
    // Générer un avatar de secours local avec les initiales
    fun getFallbackAvatarUrl(name: String): String {
        val encodedName = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
        return "https://ui-avatars.com/api/?name=$encodedName&background=107C10&color=fff&size=256"
    }

    // Récupérer la vraie photo de profil Xbox Live (doit être appelé dans un thread en arrière-plan)
    fun resolveRealXboxAvatarUrl(gamertag: String): String? {
        val formattedName = gamertag.trim().replace(" ", "-")
        
        try {
            // Tentative 1 : xboxgamertag.com
            val searchUrl = "https://xboxgamertag.com/search/$formattedName"
            val connection = URL(searchUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().readText()
                
                // On cherche n'importe quel lien vers le CDN d'image Xbox Live dans le code source
                val regex = """https://images-[a-zA-Z0-9-]+\.xboxlive\.com/image\?url=[^"'\s>]+""".toRegex()
                val matchResult = regex.find(html)
                
                if (matchResult != null) {
                    return matchResult.value.replace("&amp;", "&")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            // Tentative 3 : xbox-api.net (API publique, format JSON)
            // Gamertag encode de facon standard sans modification particuliere.
            val encodedName = URLEncoder.encode(gamertag.trim(), "UTF-8")
            val searchUrl = "https://xbl.io/api/v2/friends/search?gt=$encodedName"
            val connection = URL(searchUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200 || connection.responseCode == 302) {
                 // XBL.io envoie une 302 (redirect) vers l'image officielle.
                 // Soit on suit la redirection, soit le header "Location" contient la reponse.
                 val redirectUrl = connection.getHeaderField("Location")
                 if (!redirectUrl.isNullOrEmpty() && redirectUrl.contains("xboxlive.com")) {
                     return redirectUrl
                 }
                 
                 // Sinon, on cherche dans le JSON si c'est renvoye ainsi
                 val jsonResponse = connection.inputStream.bufferedReader().readText()
                 val regex = """"displayPicRaw"\s*:\s*"([^"]+)"""".toRegex()
                 val matchResult = regex.find(jsonResponse)
                 if (matchResult != null) {
                     return matchResult.groupValues[1]
                 }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            // Tentative 4 (Dernier recours) : TrueAchievements
            // Les URLs TrueAchievements remplacent les espaces par des tirets
            val formattedName = gamertag.trim().replace(" ", "-")
            val searchUrl = "https://www.trueachievements.com/gamer/$formattedName"
            val connection = URL(searchUrl).openConnection() as HttpURLConnection
            // TrueAchievements a un système Cloudflare lourd, on spoof un max
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().readText()
                
                // On cherche l'image de profil qui est dans un div class="gamerpic" ou og:image
                val regex1 = """<meta\s+property="og:image"\s+content="([^"]+)"""".toRegex()
                val matchResult1 = regex1.find(html)
                
                if (matchResult1 != null) {
                    val imgUrl = matchResult1.groupValues[1].replace("&amp;", "&")
                    if (imgUrl.contains("xboxlive.com") || imgUrl.contains("gamerpic")) {
                        return imgUrl
                    }
                }
                
                // Plan B sur TrueAchievements : parser la div directement
                val regex2 = """<img[^>]+src="([^"]+)"[^>]+class="[^"]*gamerpic[^"]*"""".toRegex()
                val matchResult2 = regex2.find(html)
                if (matchResult2 != null) {
                    val imgUrl = matchResult2.groupValues[1].replace("&amp;", "&")
                    if (imgUrl.contains("xboxlive.com") || imgUrl.contains("gamerpic")) {
                        return imgUrl
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    // Chargeur d'image universel
    fun loadImageInto(url: String, imageView: ImageView) {
        thread {
            try {
                val connectionImg = URL(url).openConnection() as HttpURLConnection
                connectionImg.setRequestProperty("User-Agent", "Mozilla/5.0")
                connectionImg.connect()
                
                val inputStream = connectionImg.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                Handler(Looper.getMainLooper()).post {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}