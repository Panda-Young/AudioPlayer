package com.panda.audioplayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.panda.audioplayer.utils.Logger
import com.panda.audioplayer.utils.WavFile
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class AudioTrackManager(private val sampleRate: Int) {

    private val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private var audioTrack: AudioTrack? = null
    private var audioFile: RandomAccessFile? = null
    internal var isPlaying = false
    internal var isCompleted = false
    private var isPaused = false
    private var audioFileLength: Long = 0
    private var positionOffset: Long = 0
    private var pauseOffset: Long = 0
    private var filePath: String? = null
    private var flagJump = false
    var onPlaybackComplete: (() -> Unit)? = null

    init {
        initializeAudioTrack()
    }

    private fun initializeAudioTrack() {
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .setEncoding(audioFormat)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()
    }

    fun startPlay(filePath: String, resume: Boolean = false) {
        try {
            stopPlay()
            this.filePath = filePath
            audioFile = RandomAccessFile(filePath, "r")
            Logger.logi("Audio started playing $filePath")
            val wavFile = WavFile(File(filePath))
            positionOffset = wavFile.getDataStartOffset().toLong()
            audioFileLength = audioFile?.length() ?: 0

            isPlaying = true
            isPaused = false
            isCompleted = false

            if (resume) {
                audioFile?.seek(pauseOffset)
            } else {
                audioFile?.seek(positionOffset)
            }

            audioTrack?.play()

            Thread {
                try {
                    val buffer = ByteArray(bufferSize)
                    while (isPlaying && audioFile?.filePointer ?: 0 < audioFileLength) {
                        val read = audioFile?.read(buffer) ?: 0
                        if (read > 0) {
                            audioTrack?.write(buffer, 0, read)
                        }

                        if (isPaused) {
                            pauseOffset = audioFile?.filePointer ?: 0
                            audioTrack?.pause()
                            break
                        }
                    }
                    if (isPlaying && audioFile?.filePointer ?: 0 >= audioFileLength) {
                        isPlaying = false
                        isCompleted = true
                        onPlaybackComplete?.invoke()
                    }
                } catch (e: IOException) {
                    Logger.loge("Error during playback: ${e.message}")
                }
            }.start()
        } catch (e: IOException) {
            Logger.loge("Error starting playback: ${e.message}")
        }
    }

    fun stopPlay() {
        isPlaying = false
        audioTrack?.stop()
        audioTrack?.flush()
        try {
            audioFile?.close()
        } catch (e: IOException) {
            Logger.loge("Error closing audio file: ${e.message}")
        }
    }

    fun pausePlay() {
        if (isPlaying && !isPaused) {
            isPaused = true
            isPlaying = false
            audioTrack?.pause()
            pauseOffset = audioFile?.filePointer ?: 0
        }
    }

    fun resumePlay() {
        if (isPaused) {
            filePath?.let {
                startPlay(it, resume = true)
            }
        }
    }

    fun seekTo(positionMillis: Long) {
        try {
            positionOffset = (positionMillis * sampleRate * 2 * (if (channelConfig == AudioFormat.CHANNEL_OUT_STEREO) 2 else 1)) / 1000
            positionOffset -= positionOffset % 4
            positionOffset = positionOffset.coerceIn(0, audioFileLength - 1)
            flagJump = true

            if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                stopPlay()
                startPlay(filePath ?: return)
            } else {
                audioFile?.seek(positionOffset)
            }
        } catch (e: IOException) {
            Logger.loge("Error seeking to position: ${e.message}")
        }
    }

    fun getCurrentPosition(): Int {
        return if (audioFile == null) {
            0
        } else {
            try {
                val currentPosition = if (isPaused) pauseOffset else audioFile?.filePointer ?: 0
                (currentPosition / (sampleRate / 1000 * 2 * (if (channelConfig == AudioFormat.CHANNEL_OUT_STEREO) 2 else 1))).toInt()
            } catch (e: IOException) {
                Logger.loge("Error getting current position: ${e.message}")
                0
            }
        }
    }

    fun isPaused(): Boolean {
        return isPaused
    }

    fun release() {
        stopPlay()
        audioTrack?.release()
    }

    fun isSameAudioFile(newFilePath: String?): Boolean {
        return filePath == newFilePath
    }
}
