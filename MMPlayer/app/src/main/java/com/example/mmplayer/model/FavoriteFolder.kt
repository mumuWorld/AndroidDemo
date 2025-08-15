package com.example.mmplayer.model

import java.util.Date

data class FavoriteFolder(
    val id: String,
    val path: String,
    val name: String,
    val addedTime: Date = Date()
) {
    val displayName: String
        get() = name.ifEmpty { path.substringAfterLast("/") }
}