package com.sameerasw.livepaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

class VideoWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {
        private var exoPlayer: ExoPlayer? = null
        
        private val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_USER_PRESENT -> {
                        // User unlocked the phone
                        exoPlayer?.play()
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        // User locked the phone (or screen timed out)
                        exoPlayer?.pause()
                        exoPlayer?.seekTo(0) // Snap back to the first frame
                    }
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_OFF)
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
            exoPlayer = ExoPlayer.Builder(applicationContext).build().apply {
                setVideoSurfaceHolder(holder)
                repeatMode = Player.REPEAT_MODE_OFF
                volume = 0f // Mute by default for wallpaper
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                
                val videoResId = resources.getIdentifier("my_video", "raw", packageName)
                if (videoResId != 0) {
                    val mediaItem = MediaItem.fromUri("android.resource://$packageName/$videoResId")
                    setMediaItem(mediaItem)
                    prepare()
                }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
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
