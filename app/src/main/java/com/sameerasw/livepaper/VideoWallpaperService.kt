package com.sameerasw.livepaper

import android.os.Handler
import android.os.Looper
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
        private val handler = Handler(Looper.getMainLooper())
        private var isVisible = false

        private val reverseRunnable = object : Runnable {
            override fun run() {
                val player = exoPlayer ?: return
                val current = player.currentPosition
                if (current > 0) {
                    player.seekTo(maxOf(0, current - 33)) // Seek back ~30fps
                    handler.postDelayed(this, 33)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                handler.removeCallbacks(reverseRunnable)
                exoPlayer?.play()
            } else {
                exoPlayer?.pause()
                handler.post(reverseRunnable)
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
            handler.removeCallbacks(reverseRunnable)
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
