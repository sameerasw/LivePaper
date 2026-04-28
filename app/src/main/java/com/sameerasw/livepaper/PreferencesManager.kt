package com.sameerasw.livepaper

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wallpaper_prefs", Context.MODE_PRIVATE)

    var selectedVideoName: String
        get() = prefs.getString("selected_video", "my_video") ?: "my_video"
        set(value) = prefs.edit().putString("selected_video", value).apply()

    fun getAvailableVideos(context: Context): List<String> {
        val videos = mutableListOf<String>()
        val fields = R.raw::class.java.fields
        for (field in fields) {
            try {
                videos.add(field.name)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return videos
    }
}
