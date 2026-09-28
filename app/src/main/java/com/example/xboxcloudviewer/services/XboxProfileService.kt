package com.example.xboxcloudviewer.services

import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

object XboxProfileService {
    
    // Générer un avatar de secours local avec les initiales
    fun getFallbackAvatarUrl(name: String): String {
        val encodedName = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
        return "https://ui-avatars.com/api/?name=$encodedName&background=107C10&color=fff&size=256"
    }

    // Récupérer la vraie photo de profil Xbox Live (doit être appelé dans un thread en arrière-plan)
    fun resolveRealXboxAvatarUrl(gamertag: String): String? {
        // Tenter le scraping silencieux via CookieManager authentifié délégué au ProfileScrapingService
        return ProfileScrapingService.scrapeAvatarUrl(gamertag)
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