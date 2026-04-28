package com.sameerasw.livepaper

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var exoPlayer: ExoPlayer? = null
        private lateinit var prefs: PreferencesManager
        private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }

        private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PreferencesManager.KEY_SELECTED_VIDEO) loadSelectedVideo()
        }

        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT -> exoPlayer?.play()
                    Intent.ACTION_SCREEN_OFF -> {
                        exoPlayer?.pause()
                        exoPlayer?.seekTo(0)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        if (isPreview || !keyguardManager.isKeyguardLocked) {
                            exoPlayer?.play()
                        } else {
                            exoPlayer?.pause()
                            exoPlayer?.seekTo(0)
                        }
                    }
                }
            }
        }

        @OptIn(UnstableApi::class)
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            prefs = PreferencesManager(applicationContext)
            
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(receiver, filter)

            applicationContext.getSharedPreferences(PreferencesManager.PREFS_NAME, Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(prefsListener)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                if (isPreview || !keyguardManager.isKeyguardLocked) exoPlayer?.play()
            } else {
                exoPlayer?.pause()
            }
        }

        @OptIn(UnstableApi::class)
        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            val renderersFactory = DefaultRenderersFactory(applicationContext)
            val player = ExoPlayer.Builder(applicationContext, renderersFactory).build()
            exoPlayer = player

            player.apply {
                setVideoSurfaceHolder(holder)
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 0f
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                loadSelectedVideo()
                playWhenReady = isPreview || !keyguardManager.isKeyguardLocked
            }
        }

        @OptIn(UnstableApi::class)
        private fun loadSelectedVideo() {
            val videoName = prefs.selectedVideoName
            val resId = resources.getIdentifier(videoName, "raw", packageName)
            if (resId != 0) {
                val mediaItem = MediaItem.fromUri("android.resource://$packageName/$resId")
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {
            super.onSurfaceChanged(holder, f, w, h)
            exoPlayer?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }

        override fun onDestroy() {
            super.onDestroy()
            try { unregisterReceiver(receiver) } catch (e: Exception) { }
            applicationContext.getSharedPreferences(PreferencesManager.PREFS_NAME, Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(prefsListener)
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
