package com.example.xboxcloudviewer.models

data class UserProfile(
    val id: String,
    var name: String,
    var avatarUrl: String? = null,
    var cookies: String? = null,
    var localStorage: String? = null
)
