package com.panda.audioplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlayer
import com.panda.audioplayer.utils.Logger

class ExoPlayerManager(private val context: Context) {

    data class PlaybackSnapshot(
        val source: String,
        val position: Long,
        val shouldPlay: Boolean
    )

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("playback_state", Context.MODE_PRIVATE)
    private var codecRecoveryAttempted = false
    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setRenderersFactory(
            DefaultRenderersFactory(appContext)
                .setEnableDecoderFallback(true)
        )
        .setHandleAudioBecomingNoisy(true)
        .build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
        }

    internal var isCompleted = false
    private var isPausedState = false
    private var currentSource: String? = null
    var onPlaybackComplete: (() -> Unit)? = null

    val isPlaying: Boolean get() = exoPlayer.isPlaying

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isCompleted = true
                    persistPlaybackState(shouldPlay = false)
                    Logger.logi("Playback completed: $currentSource")
                    onPlaybackComplete?.invoke()
                } else if (playbackState == Player.STATE_READY) {
                    codecRecoveryAttempted = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Logger.loge("ExoPlayer error for $currentSource : ${error.message}")
                if (shouldRecoverFromCodecError(error) && !codecRecoveryAttempted) {
                    codecRecoveryAttempted = true
                    val source = currentSource
                    if (source != null) {
                        val recoverPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
                        Logger.logw("Codec renderer failure detected, attempting auto-recovery at $recoverPosition ms")
                        recoverPlayback(source, recoverPosition)
                    }
                }
            }
        })
    }

    fun getPlayer(): ExoPlayer = exoPlayer

    fun startPlay(source: String, autoPlay: Boolean = true) {
        currentSource = source
        isCompleted = false
        isPausedState = !autoPlay
        startPlaybackServiceIfNeeded()
        Logger.logi("ExoPlayer starting: $source")
        exoPlayer.setMediaItem(MediaItem.fromUri(parseSourceToUri(source)))
        exoPlayer.prepare()
        if (autoPlay) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
        persistPlaybackState(shouldPlay = autoPlay)
    }

    fun pausePlay() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            isPausedState = true
            persistPlaybackState(shouldPlay = false)
            Logger.logi("ExoPlayer paused")
        }
    }

    fun resumePlay() {
        if (isPausedState) {
            exoPlayer.play()
            isPausedState = false
            persistPlaybackState(shouldPlay = true)
            Logger.logi("ExoPlayer resumed")
        }
    }

    fun stopPlay() {
        persistPlaybackState(shouldPlay = false)
        exoPlayer.stop()
        isCompleted = false
        isPausedState = false
        Logger.logi("ExoPlayer stopped")
    }

    fun seekTo(positionMillis: Long) {
        exoPlayer.seekTo(positionMillis)
        persistPlaybackState(shouldPlay = exoPlayer.isPlaying)
    }

    fun getCurrentPosition(): Int = exoPlayer.currentPosition.toInt()

    fun getDuration(): Int = exoPlayer.duration.let { if (it > 0) it.toInt() else 0 }

    fun isPaused(): Boolean = isPausedState

    fun isSameAudioFile(newSource: String?): Boolean = currentSource == newSource

    fun getCurrentSource(): String? = currentSource

    fun persistPlaybackState(shouldPlay: Boolean) {
        val source = currentSource ?: return
        prefs.edit()
            .putString(KEY_SOURCE, source)
            .putLong(KEY_POSITION, exoPlayer.currentPosition)
            .putBoolean(KEY_SHOULD_PLAY, shouldPlay)
            .apply()
    }

    fun restorePlaybackState(): PlaybackSnapshot? {
        val source = prefs.getString(KEY_SOURCE, null) ?: return null
        val position = prefs.getLong(KEY_POSITION, 0L)
        val shouldPlay = prefs.getBoolean(KEY_SHOULD_PLAY, false)
        return PlaybackSnapshot(source, position, shouldPlay)
    }

    private fun startPlaybackServiceIfNeeded() {
        val intent = Intent(appContext, PlaybackService::class.java)
        try {
            // App playback starts from foreground UI, regular startService avoids
            // ForegroundServiceDidNotStartInTimeException timeout crashes.
            appContext.startService(intent)
        } catch (t: Throwable) {
            Logger.logw("Failed to start PlaybackService safely: ${t.message}")
        }
    }

    private fun parseSourceToUri(source: String): Uri {
        return if (source.startsWith("content://")) {
            Uri.parse(source)
        } else {
            Uri.fromFile(java.io.File(source))
        }
    }

    fun shutdown() {
        exoPlayer.release()
        Logger.logi("ExoPlayer released")
    }

    private fun shouldRecoverFromCodecError(error: PlaybackException): Boolean {
        val exoError = error as? ExoPlaybackException ?: return false
        return exoError.type == ExoPlaybackException.TYPE_RENDERER &&
            exoError.rendererName?.contains("MediaCodec", ignoreCase = true) == true
    }

    private fun recoverPlayback(source: String, positionMs: Long) {
        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(parseSourceToUri(source)))
            exoPlayer.prepare()
            if (positionMs > 0) {
                exoPlayer.seekTo(positionMs)
            }
            exoPlayer.play()
            persistPlaybackState(shouldPlay = true)
            Logger.logi("Playback auto-recovery completed for source: $source")
        } catch (t: Throwable) {
            Logger.loge("Playback auto-recovery failed: ${t.message}")
        }
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

    companion object {
        private const val KEY_SOURCE = "key_source"
        private const val KEY_POSITION = "key_position"
        private const val KEY_SHOULD_PLAY = "key_should_play"
    }
}
