package com.example.xboxcloudviewer.services

import android.content.Context
import android.content.SharedPreferences
import com.example.xboxcloudviewer.models.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AccountManager(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("Accounts", Context.MODE_PRIVATE)

    fun getUsers(): List<UserProfile> {
        val usersJson = prefs.getString("users", "[]")
        val array = JSONArray(usersJson)
        val users = mutableListOf<UserProfile>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            users.add(UserProfile(
                id = obj.getString("id"),
                name = obj.getString("name"),
                avatarUrl = obj.optString("avatarUrl", "").takeIf { it.isNotEmpty() && it != "null" },
                cookies = obj.optString("cookies", "").takeIf { it.isNotEmpty() && it != "null" },
                localStorage = obj.optString("localStorage", "").takeIf { it.isNotEmpty() && it != "null" }
            ))
        }
        if (users.isEmpty()) {
            val guest = UserProfile(UUID.randomUUID().toString(), "Guest")
            users.add(guest)
            saveUsers(users)
            setCurrentUserId(guest.id)
        }
        return users
    }

    private fun saveUsers(users: List<UserProfile>) {
        val array = JSONArray()
        users.forEach { user ->
            val obj = JSONObject()
            obj.put("id", user.id)
            obj.put("name", user.name)
            obj.put("avatarUrl", user.avatarUrl)
            obj.put("cookies", user.cookies)
            obj.put("localStorage", user.localStorage)
            array.put(obj)
        }
        prefs.edit().putString("users", array.toString()).apply()
    }

    fun saveUser(user: UserProfile) {
        val users = getUsers().toMutableList()
        val index = users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            users[index] = user
        } else {
            users.add(user)
        }
        saveUsers(users)
    }

    fun getCurrentUserId(): String {
        return prefs.getString("current_user_id", "") ?: ""
    }

    fun setCurrentUserId(id: String) {
        prefs.edit().putString("current_user_id", id).apply()
    }
    
    fun getCurrentUser(): UserProfile {
        val users = getUsers()
        val currentId = getCurrentUserId()
        return users.find { it.id == currentId } ?: users.first()
    }
}