/**
 * Хранилище настроек приложения: адрес сервера, куки авторизации и история недавних комнат.
 */
package com.livekit.meetkit.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.livekit.meetkit.data.models.RecentRoom

class ServerPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("meetkit_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var serverUrl: String
        get() = prefs.getString("server_url", "") ?: ""
        set(value) {
            val formatted = formatUrl(value)
            prefs.edit().putString("server_url", formatted).apply()
        }

    fun clearServerUrl() {
        prefs.edit().remove("server_url").remove("saved_cookies").apply()
    }

    fun saveCookiesJson(json: String) {
        prefs.edit().putString("saved_cookies", json).apply()
    }

    fun getCookiesJson(): String? {
        return prefs.getString("saved_cookies", null)
    }

    fun getRecentRooms(): List<RecentRoom> {
        val json = prefs.getString("recent_rooms", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<RecentRoom>>() {}.type
            gson.fromJson<List<RecentRoom>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addRecentRoom(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val current = getRecentRooms().filter { it.name != trimmed }.toMutableList()
        current.add(0, RecentRoom(trimmed, System.currentTimeMillis()))
        if (current.size > 10) {
            current.subList(10, current.size).clear()
        }
        val json = gson.toJson(current)
        prefs.edit().putString("recent_rooms", json).apply()
    }

    fun removeRecentRoom(name: String) {
        val current = getRecentRooms().filter { it.name != name.trim() }
        val json = gson.toJson(current)
        prefs.edit().putString("recent_rooms", json).apply()
    }

    private fun formatUrl(url: String): String {
        var trimmed = url.trim()
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length - 1)
        }
        if (trimmed.isNotEmpty() && !trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }
}
