package com.sameerasw.livepaper

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedVideoName: String
        get() = prefs.getString(KEY_SELECTED_VIDEO, DEFAULT_VIDEO) ?: DEFAULT_VIDEO
        set(value) = prefs.edit().putString(KEY_SELECTED_VIDEO, value).apply()

    fun getAvailableVideos(): List<String> {
        return R.raw::class.java.fields.mapNotNull { field ->
            try { field.name } catch (e: Exception) { null }
        }
    }

    companion object {
        const val PREFS_NAME = "wallpaper_prefs"
        const val KEY_SELECTED_VIDEO = "selected_video"
        const val DEFAULT_VIDEO = "my_video"
    }
}
