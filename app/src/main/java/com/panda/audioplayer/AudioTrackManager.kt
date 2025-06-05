package com.panda.audioplayer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.panda.audioplayer.utils.Logger
import com.panda.audioplayer.utils.WavFile
import com.panda.audioplayer.utils.DataConverter
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioTrackManager(private val sampleRate: Int, private val channelConfig: Int) {

    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channelMask = when (channelConfig) {
        1 -> AudioFormat.CHANNEL_OUT_MONO
        2 -> AudioFormat.CHANNEL_OUT_STEREO
        else -> AudioFormat.CHANNEL_OUT_MONO
    }
    private var fileChanels: Int = 0
    private var fileBlockAlign: Int = 0
    private var fileBitDepth: Int = 0
    private var fileAudioFormat: Int = 0
    private val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelMask, audioFormat) * 3 // 3 for 24 bit pcm
    private var audioTrack: AudioTrack? = null
    private var audioFile: RandomAccessFile? = null
    internal var isPlaying = false
    internal var isCompleted = false
    private var isPaused = false
    private var audioFileLength: Long = 0
    private var dataChunkSize: Long = 0
    private var dataCurrentOffset: Long = 0
    private var dataPauseOffset: Long = 0
    private var dataChunkOffset: Long = 0
    private var filePath: String? = null
    private var jumpFlag = false
    var onPlaybackComplete: (() -> Unit)? = null
    private var algoHandle: Long = 0
    private val algo = AlgoExample()

    init {
        initializeAudioTrack()
    }

    private fun initAlgorithm() {
        val version = ByteArray(1024)
        if (algo.getAlgoVersion(version) != 0) {
            Logger.logf("Failed to get algo version")
            return
        }

        var endIndex = 0
        while (endIndex < version.size && version[endIndex].toInt() != 0) {
            endIndex++
        }
        val validVersionBytes = version.copyOfRange(0, endIndex)
        val versionString = String(validVersionBytes)

        Logger.logi("Algorithm version: $versionString")
        algoHandle = algo.algoInit()

        val param = ByteBuffer.allocate(4).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            putFloat(-5.0f)
        }.array()
        algo.algoSetParam(algoHandle, 2, param, param.size)
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
            if (resume) {
                audioFile?.seek(dataPauseOffset)
            } else {
                stopPlay()
                initAlgorithm()
                this.filePath = filePath
                audioFile = RandomAccessFile(filePath, "r")
                Logger.logi("Audio started playing $filePath")
                val wav = wavFile ?: WavFile(File(filePath))
                this.fileChanels = wav.getChannels()
                this.fileBlockAlign = wav.getBlockAlign()
                this.fileBitDepth = wav.getBitDepth()
                this.fileAudioFormat = wav.getAudioFormat()
                this.dataChunkOffset = wav.getDataStartOffset()?.toLong() ?: 0
                this.dataChunkSize = wav.getAudioDataSize()?.toLong() ?: 0
                if (!jumpFlag) {
                    dataCurrentOffset = dataChunkOffset
                }
                audioFileLength = audioFile?.length() ?: 0
                audioFile?.seek(dataCurrentOffset)
            }
            isPlaying = true
            isPaused = false
            isCompleted = false
            jumpFlag = false
            audioTrack?.play()

            Thread {
                try {
                    val buffer = ByteArray(minBufferSize)
                    val dataChunkEnd = dataChunkOffset + dataChunkSize
                    while (isPlaying && audioFile?.filePointer ?: 0L < dataChunkEnd) {
                        buffer.fill(0)
                        var read = 0
                        if ((audioFile?.filePointer ?: 0L) + buffer.size > dataChunkEnd) {
                            val remainingBytes = dataChunkEnd - (audioFile?.filePointer ?: 0)
                            read = audioFile?.read(buffer, 0, remainingBytes.toInt()) ?: 0
                        } else {
                            read = audioFile?.read(buffer) ?: 0
                        }
                        if (read > 0) {
                            val convertedBuffer = when (fileBitDepth) {
                                8 -> DataConverter.convert8BitTo16Bit(buffer)
                                24 -> DataConverter.convert24BitTo16Bit(buffer)
                                32 -> {
                                    when (fileAudioFormat) {
                                        1 -> DataConverter.convert32BitIntTo16Bit(buffer) // 32-bit int
                                        3 -> DataConverter.convert32BitFloatTo16Bit(buffer) // 32-bit float
                                        else -> buffer // use raw data
                                    }
                                }
                                else -> buffer // use raw data
                            }
                            // audioTrack?.write(convertedBuffer, 0, convertedBuffer.size)
                            val floatInput = DataConverter.byteArrayToFloatArray(convertedBuffer)
                            val floatOutput = FloatArray(floatInput.size)
                            algo.algoProcess(algoHandle, floatInput, floatOutput, floatOutput.size)
                            audioTrack?.write(DataConverter.floatArrayToByteArray(floatOutput), 0, convertedBuffer.size)
                        }

                        if (isPaused) {
                            dataPauseOffset = audioFile?.filePointer ?: 0
                            audioTrack?.pause()
                            break
                        }
                    }
                    if (isPlaying && audioFile?.filePointer ?: 0L >= dataChunkEnd) {
                        isPlaying = false
                        isCompleted = true
                        onPlaybackComplete?.invoke()
                        Logger.logi("Audio playback completed")
                        if (algoHandle != 0L) {
                            algo.algoDeinit(algoHandle)
                        }
                    }
                } catch (e: IOException) {
                    Logger.logf("Error during playback: ${e.message}")
                }
            }.start()
        } catch (e: IOException) {
            Logger.logf("Error starting playback: ${e.message}")
        }
    }

    fun stopPlay() {
        isPlaying = false
        if (algoHandle != 0L) {
            algo.algoDeinit(algoHandle)
        }
        audioTrack?.stop()
        audioTrack?.flush()
        try {
            audioFile?.close()
        } catch (e: IOException) {
            Logger.logf("Error closing audio file: ${e.message}")
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
        dataCurrentOffset = positionMillis * sampleRate / 1000 * fileBlockAlign
        dataCurrentOffset -= dataCurrentOffset % fileBlockAlign // align to fileBlockAlign-byte boundary
        dataCurrentOffset += dataChunkOffset
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
                (currentPosition / (sampleRate / 1000 * fileBlockAlign)).toInt()
            } catch (e: IOException) {
                Logger.logf("Error getting current position: ${e.message}")
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

    // Placeholder implementations
    fun setReverbEffect(enabled: Boolean) {
        // TODO: Implement reverb effect
    }

    fun setEqualizerEffect(enabled: Boolean) {
        // TODO: Implement equalizer
    }

    fun set3DEffect(enabled: Boolean) {
        // TODO: Implement 3D audio
    }
}
