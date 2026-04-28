package com.sameerasw.livepaper

import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
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

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            exoPlayer = ExoPlayer.Builder(applicationContext).build().apply {
                setVideoSurfaceHolder(holder)
                repeatMode = Player.REPEAT_MODE_OFF
                
                // Note: User needs to add my_video.mp4 to res/raw/
                // For now, this will fail to compile if the resource is missing.
                // We could use a try-catch or check resource existence if needed.
                val videoResId = resources.getIdentifier("my_video", "raw", packageName)
                if (videoResId != 0) {
                    val mediaItem = MediaItem.fromUri("android.resource://$packageName/$videoResId")
                    setMediaItem(mediaItem)
                    prepare()
                }

                addListener(object : Player.Listener {
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        updateSurfaceSize(holder, videoSize)
                    }
                })
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            exoPlayer?.videoSize?.let { updateSurfaceSize(holder, it) }
        }

        private fun updateSurfaceSize(holder: SurfaceHolder, videoSize: VideoSize) {
            if (videoSize.width <= 0 || videoSize.height <= 0) return

            val surfaceFrame = holder.surfaceFrame
            val surfaceWidth = surfaceFrame.width()
            val surfaceHeight = surfaceFrame.height()

            if (surfaceWidth <= 0 || surfaceHeight <= 0) return

            val videoWidth = videoSize.width.toFloat()
            val videoHeight = videoSize.height.toFloat()

            val scaleX = surfaceWidth / videoWidth
            val scaleY = surfaceHeight / videoHeight
            val scale = maxOf(scaleX, scaleY)

            val scaledWidth = (videoWidth * scale).toInt()
            val scaledHeight = (videoHeight * scale).toInt()

            // Center Crop logic: adjust the surface size or use a Matrix if possible.
            // In WallpaperService, we usually just adjust the surface holder size 
            // or let ExoPlayer handle it if we provide the right aspect ratio.
            // However, a common trick is to use holder.setFixedSize() to match the aspect ratio
            // but that might not "fill" the screen. 
            // A better way is to handle it via a custom surface layout or just accept the fit.
            // For true center crop in a wallpaper, we might need a custom renderer or 
            // just set the fixed size to a larger value and let it clip.
            
            // For now, we'll keep it simple as per the plan, but note that 
            // setVideoSurfaceHolder already handles some scaling.
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(reverseRunnable)
            exoPlayer?.release()
            exoPlayer = null
        }
    }
}
