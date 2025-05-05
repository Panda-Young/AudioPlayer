package com.panda.audioplayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.panda.audioplayer.utils.Logger
import com.panda.audioplayer.utils.WavFile
import com.panda.audioplayer.utils.AudioDataConverter
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class AudioTrackManager(private val sampleRate: Int, private val channelConfig: Int) {

    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channelMask = when (channelConfig) {
        1 -> AudioFormat.CHANNEL_OUT_MONO
        2 -> AudioFormat.CHANNEL_OUT_STEREO
        else -> AudioFormat.CHANNEL_OUT_MONO
    }
    private var blockAlign: Int = 0
    private val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, audioFormat) * 3 // 3 for 24 bit pcm
    private var audioTrack: AudioTrack? = null
    private var audioFile: RandomAccessFile? = null
    internal var isPlaying = false
    internal var isCompleted = false
    private var isPaused = false
    private var audioFileLength: Long = 0
    private var dataCurrentOffset: Long = 0
    private var dataPauseOffset: Long = 0
    private var filePath: String? = null
    private var jumpFlag = false
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
                    .setChannelMask(channelMask)
                    .setEncoding(audioFormat)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .build()
    }

fun startPlay(filePath: String, wavFile: WavFile? = null, resume: Boolean = false) {
        try {
            stopPlay()
            this.filePath = filePath
            audioFile = RandomAccessFile(filePath, "r")
            Logger.logi("Audio started playing $filePath")
            val wav = wavFile ?: WavFile(File(filePath))
            this.blockAlign = wav.getBlockAlign()
            if (!jumpFlag) {
                dataCurrentOffset = wav.getDataStartOffset().toLong()
            }
            audioFileLength = audioFile?.length() ?: 0

            isPlaying = true
            isPaused = false
            isCompleted = false
            jumpFlag = false

            if (resume) {
                audioFile?.seek(dataPauseOffset)
            } else {
                audioFile?.seek(dataCurrentOffset)
            }

            audioTrack?.play()

            Thread {
                try {
                    val buffer = ByteArray(minBufferSize)
                    while (isPlaying && audioFile?.filePointer ?: 0 < audioFileLength) {
                        val read = audioFile?.read(buffer) ?: 0
                        if (read > 0) {
                            val convertedBuffer = when (wav.getBitDepth()) {
                                8 -> AudioDataConverter.convert8BitTo16Bit(buffer)
                                24 -> AudioDataConverter.convert24BitTo16Bit(buffer)
                                32 -> {
                                    when (wav.getAudioFormat()) {
                                        1 -> AudioDataConverter.convert32BitIntTo16Bit(buffer) // 32-bit int
                                        3 -> AudioDataConverter.convert32BitFloatTo16Bit(buffer) // 32-bit float
                                        else -> buffer // use raw data
                                    }
                                }
                                else -> buffer // use raw data
                            }
                            audioTrack?.write(convertedBuffer, 0, convertedBuffer.size)
                        }

                        if (isPaused) {
                            dataPauseOffset = audioFile?.filePointer ?: 0
                            audioTrack?.pause()
                            break
                        }
                    }
                    if (isPlaying && audioFile?.filePointer ?: 0 >= audioFileLength) {
                        isPlaying = false
                        isCompleted = true
                        onPlaybackComplete?.invoke()
                        Logger.logi("Audio playback completed")
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
            dataPauseOffset = audioFile?.filePointer ?: 0
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
        dataCurrentOffset = positionMillis * sampleRate / 1000 * blockAlign
        dataCurrentOffset -= dataCurrentOffset % 4 // align to 4-byte boundary
        if (isPlaying || isPaused) {
            dataCurrentOffset = dataCurrentOffset.coerceIn(0, audioFileLength - 1) // ensure offset in valid range
        } else {
            jumpFlag = true
        }
        audioFile?.seek(dataCurrentOffset)
        dataPauseOffset = dataCurrentOffset
    }

    fun getCurrentPosition(): Int {
        return if (audioFile == null) {
            0
        } else {
            try {
                val currentPosition = if (isPaused) dataPauseOffset else audioFile?.filePointer ?: 0
                (currentPosition / (sampleRate / 1000 * blockAlign)).toInt()
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
