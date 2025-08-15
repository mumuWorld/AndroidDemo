package com.example.mmplayer.service

import android.content.Context
import android.content.SharedPreferences
import com.example.mmplayer.model.FavoriteFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class FavoriteManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    
    suspend fun addFavorite(path: String, name: String = ""): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val id = UUID.randomUUID().toString()
                val favorites = getFavorites().toMutableList()
                
                // 检查是否已经收藏
                if (favorites.any { it.path == path }) {
                    return@withContext false
                }
                
                val favoriteFolder = FavoriteFolder(id, path, name)
                favorites.add(favoriteFolder)
                
                saveFavorites(favorites)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    suspend fun removeFavorite(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val favorites = getFavorites().toMutableList()
                val removed = favorites.removeAll { it.path == path }
                
                if (removed) {
                    saveFavorites(favorites)
                }
                removed
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    suspend fun getFavorites(): List<FavoriteFolder> {
        return withContext(Dispatchers.IO) {
            try {
                val favoritesJson = sharedPreferences.getString("favorites_list", "[]")
                parseFavoritesJson(favoritesJson ?: "[]")
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    suspend fun isFavorite(path: String): Boolean {
        return getFavorites().any { it.path == path }
    }
    
    private fun saveFavorites(favorites: List<FavoriteFolder>) {
        val json = favoritesToJson(favorites)
        sharedPreferences.edit()
            .putString("favorites_list", json)
            .apply()
    }
    
    private fun favoritesToJson(favorites: List<FavoriteFolder>): String {
        val sb = StringBuilder("[")
        favorites.forEachIndexed { index, favorite ->
            if (index > 0) sb.append(",")
            sb.append("{")
            sb.append("\"id\":\"${favorite.id}\",")
            sb.append("\"path\":\"${favorite.path}\",")
            sb.append("\"name\":\"${favorite.name}\",")
            sb.append("\"addedTime\":${favorite.addedTime.time}")
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }
    
    private fun parseFavoritesJson(json: String): List<FavoriteFolder> {
        val favorites = mutableListOf<FavoriteFolder>()
        
        try {
            // 简单的JSON解析（实际项目中建议使用Gson或Moshi）
            val content = json.trim().removePrefix("[").removeSuffix("]")
            if (content.isBlank()) return favorites
            
            val items = content.split("},{")
            items.forEach { item ->
                val cleanItem = item.removePrefix("{").removeSuffix("}")
                val fields = cleanItem.split(",")
                
                var id = ""
                var path = ""
                var name = ""
                var addedTime = Date()
                
                fields.forEach { field ->
                    val parts = field.split(":")
                    if (parts.size >= 2) {
                        val key = parts[0].trim().removePrefix("\"").removeSuffix("\"")
                        val value = parts.drop(1).joinToString(":").trim().removePrefix("\"").removeSuffix("\"")
                        
                        when (key) {
                            "id" -> id = value
                            "path" -> path = value
                            "name" -> name = value
                            "addedTime" -> {
                                try {
                                    addedTime = Date(value.toLong())
                                } catch (e: Exception) {
                                    addedTime = Date()
                                }
                            }
                        }
                    }
                }
                
                if (id.isNotEmpty() && path.isNotEmpty()) {
                    favorites.add(FavoriteFolder(id, path, name, addedTime))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return favorites
    }
}