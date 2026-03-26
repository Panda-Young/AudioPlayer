package com.panda.audioplayer

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.panda.audioplayer.utils.Logger
import java.io.File

class ExoPlayerManager(private val context: Context) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    internal var isCompleted = false
    private var isPausedState = false
    private var currentFilePath: String? = null
    var onPlaybackComplete: (() -> Unit)? = null

    val isPlaying: Boolean get() = exoPlayer.isPlaying

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isCompleted = true
                    Logger.logi("Playback completed: $currentFilePath")
                    onPlaybackComplete?.invoke()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Logger.loge("ExoPlayer error for $currentFilePath : ${error.message}")
            }
        })
    }

    fun startPlay(filePath: String) {
        currentFilePath = filePath
        isCompleted = false
        isPausedState = false
        Logger.logi("ExoPlayer starting: $filePath")
        exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(filePath))))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun pausePlay() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPausedState = true
            Logger.logi("ExoPlayer paused")
        }
    }

    fun resumePlay() {
        if (isPausedState) {
            exoPlayer.play()
            isPausedState = false
            Logger.logi("ExoPlayer resumed")
        }
    }

    fun stopPlay() {
        exoPlayer.stop()
        isCompleted = false
        isPausedState = false
        Logger.logi("ExoPlayer stopped")
    }

    fun seekTo(positionMillis: Long) {
        exoPlayer.seekTo(positionMillis)
    }

    fun getCurrentPosition(): Int = exoPlayer.currentPosition.toInt()

    fun getDuration(): Int = exoPlayer.duration.let { if (it > 0) it.toInt() else 0 }

    fun isPaused(): Boolean = isPausedState

    fun isSameAudioFile(newFilePath: String?): Boolean = currentFilePath == newFilePath

    fun release() {
        exoPlayer.release()
        Logger.logi("ExoPlayer released")
    }

    // Stub effect hooks — DSP processing layer to be wired in a later step
    fun setGainEffect(enabled: Boolean) {
        Logger.logi("Gain effect ${if (enabled) "enabled" else "disabled"} (stub)")
    }

    fun setEqualizerEffect(enabled: Boolean) {
        Logger.logi("Equalizer effect ${if (enabled) "enabled" else "disabled"} (stub)")
    }

    fun set3DEffect(enabled: Boolean) {
        Logger.logi("3D audio effect ${if (enabled) "enabled" else "disabled"} (stub)")
    }
}
