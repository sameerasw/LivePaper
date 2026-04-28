package com.sameerasw.livepaper

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedVideoName: String
        get() = prefs.getString(KEY_SELECTED_VIDEO, DEFAULT_VIDEO) ?: DEFAULT_VIDEO
        set(value) = prefs.edit().putString(KEY_SELECTED_VIDEO, value).apply()

    var playbackTrigger: String
        get() = prefs.getString(KEY_PLAYBACK_TRIGGER, TRIGGER_UNLOCK) ?: TRIGGER_UNLOCK
        set(value) = prefs.edit().putString(KEY_PLAYBACK_TRIGGER, value).apply()

    fun getAvailableVideos(): List<String> {
        return R.raw::class.java.fields.mapNotNull { field ->
            try { field.name } catch (e: Exception) { null }
        }
    }

    companion object {
        const val PREFS_NAME = "wallpaper_prefs"
        const val KEY_SELECTED_VIDEO = "selected_video"
        const val KEY_PLAYBACK_TRIGGER = "playback_trigger"
        const val DEFAULT_VIDEO = "my_video"
        const val TRIGGER_UNLOCK = "unlock"
        const val TRIGGER_SCREEN_ON = "screen_on"
    }
}
