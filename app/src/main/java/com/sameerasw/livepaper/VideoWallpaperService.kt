package com.sameerasw.livepaper

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbAdjustment
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var exoPlayer: ExoPlayer? = null
        private var currentAlpha = 1f
        private var fadeAnimator: ValueAnimator? = null
        
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        // User unlocked the phone
                        exoPlayer?.play()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        // User locked the phone (or screen timed out)
                        startFade(from = currentAlpha, to = 0f) {
                            exoPlayer?.pause()
                            exoPlayer?.seekTo(0)
                        }
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        // Screen turned on but might be locked
                        startFade(from = 0f, to = 1f)
                    }
                }
            }
        }

        @OptIn(UnstableApi::class)
        private fun startFade(from: Float, to: Float, onComplete: (() -> Unit)? = null) {
            fadeAnimator?.cancel()
            fadeAnimator = ValueAnimator.ofFloat(from, to).apply {
                duration = 500 // 500ms fade
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    currentAlpha = value
                    applyEffects()
                }
                onComplete?.let {
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            it()
                        }
                    })
                }
                start()
            }
        }

        @OptIn(UnstableApi::class)
        private fun applyEffects() {
            val rgbAdjustment = RgbAdjustment.Builder()
                .setRedScale(currentAlpha)
                .setGreenScale(currentAlpha)
                .setBlueScale(currentAlpha)
                .build()
            exoPlayer?.setVideoEffects(listOf(rgbAdjustment as Effect))
        }

        @OptIn(UnstableApi::class)
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            registerReceiver(receiver, filter)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (!visible) {
                exoPlayer?.pause()
            }
        }

        @OptIn(UnstableApi::class)
        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            
            val renderersFactory = DefaultRenderersFactory(applicationContext)
            val prefs = PreferencesManager(applicationContext)
            
            exoPlayer = ExoPlayer.Builder(applicationContext, renderersFactory).build().apply {
                setVideoSurfaceHolder(holder)
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 0f // Mute by default for wallpaper
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                
                val videoName = prefs.selectedVideoName
                val videoResId = resources.getIdentifier(videoName, "raw", packageName)
                if (videoResId != 0) {
                    val mediaItem = MediaItem.fromUri("android.resource://$packageName/$videoResId")
                    setMediaItem(mediaItem)
                    prepare()
                }
                
                // Initial state is black
                currentAlpha = 0f
                applyEffects()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            fadeAnimator?.cancel()
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if not registered
            }
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
