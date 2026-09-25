package com.example.xboxcloudviewer.services

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import java.net.URL
import kotlin.concurrent.thread

object XboxProfileService {
    
    // Simulate fetching Xbox profile picture based on gamertag.
    // In a real scenario, you would call the Xbox API here.
    fun getAvatarUrlForName(name: String): String {
        val encodedName = name.replace(" ", "+")
        return "https://ui-avatars.com/api/?name=$encodedName&background=107C10&color=fff&size=256"
    }

    // Simple async image loader without third-party libraries (Glide/Picasso)
    fun loadImageInto(url: String, imageView: ImageView) {
        thread {
            try {
                val inputStream = URL(url).openStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                Handler(Looper.getMainLooper()).post {
                    imageView.setImageBitmap(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}