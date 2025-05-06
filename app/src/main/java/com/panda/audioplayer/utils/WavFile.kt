package com.panda.audioplayer.utils

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import com.panda.audioplayer.utils.Logger

class WavFile(private val file: File) {

    private var parsed = false
    private var sampleRate = 44100 // Default sample rate
    private var channels = 2 // Default stereo
    private var blockAlign = 4 // Default block align
    private var bitDepth = 16 // Default bit depth
    private var dataSize = 0
    private var dataStartOffset = 0
    private var audioFormat = 1 // Default PCM format
    private var audioTitle: String? = null
    private var artist: String? = null

    init {
        parseWavHeader()
    }

    private fun parseWavHeader() {
        if (parsed) return // If already parsed, return directly
        parsed = true
        try {
            FileInputStream(file).use { fis ->
                // Read the first 12 bytes to check RIFF and WAVE identifiers
                val headerBuffer = ByteArray(12)
                fis.read(headerBuffer)

                // Check if the file is a valid WAV file
                if (String(headerBuffer, 0, 4) != "RIFF") {
                    Logger.logw("$file is not a valid RIFF file")
                    return
                }
                if (String(headerBuffer, 8, 4) != "WAVE") {
                    Logger.logw("$file is not a valid WAVE file")
                    return
                }
                Logger.logi("Parsing WAV file path $file")

                var fmtChunkFound = false
                var dataChunkFound = false

                // Loop through chunks until both fmt and data chunks are found
                while (fis.available() > 0) {
                    // Read the chunk header (8 bytes: 4 bytes for chunk ID + 4 bytes for chunk size)
                    val chunkHeader = ByteArray(8)
                    if (fis.read(chunkHeader) != 8) {
                        break // End of file reached
                    }

                    val chunkId = String(chunkHeader, 0, 4)
                    var chunkSize = chunkHeader[4].toInt() and 0xFF or
                            (chunkHeader[5].toInt() and 0xFF shl 8) or
                            (chunkHeader[6].toInt() and 0xFF shl 16) or
                            (chunkHeader[7].toInt() and 0xFF shl 24)
                    if (chunkSize % 2 == 1) {
                        chunkSize++ // Ensure chunk size is even
                    }

                    Logger.logi("Parsing chunk: id=$chunkId, size=$chunkSize")

                    when (chunkId) {
                        "fmt " -> {
                            // Parse the fmt chunk
                            val fmtData = ByteArray(chunkSize)
                            fis.read(fmtData, 0, chunkSize)
                            parseFmtChunk(fmtData)
                            fmtChunkFound = true
                        }
                        "data" -> {
                            // Parse data chunk
                            dataSize = chunkSize
                            dataStartOffset = fis.channel.position().toInt()
                            dataChunkFound = true
                            fis.skip(chunkSize.toLong())
                        }
                        "LIST" -> {
                            // Parse LIST chunk
                            val listData = ByteArray(chunkSize)
                            fis.read(listData, 0, chunkSize)
                            parseListChunk(listData)
                        }
                        "INFO" -> {
                            // Parse INFO chunk
                            val infoData = ByteArray(chunkSize)
                            fis.read(infoData, 0, chunkSize)
                            parseInfoChunk(infoData)
                        }
                        else -> {
                            // Skip unknown chunks
                            fis.skip(chunkSize.toLong())
                        }
                    }
                }

                if (!fmtChunkFound) {
                    Logger.logw("Invalid WAV file: fmt chunk not found.")
                    throw IOException("Invalid WAV file: fmt chunk not found")
                }

                if (!dataChunkFound) {
                    Logger.logw("Invalid WAV file: data chunk not found.")
                    throw IOException("Invalid WAV file: data chunk not found")
                }
            }
        } catch (e: IOException) {
            Logger.loge("Error parsing WAV file header: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun parseFmtChunk(fmtData: ByteArray) {
        audioFormat = fmtData[0].toInt() and 0xFF or (fmtData[1].toInt() and 0xFF shl 8)
        if (audioFormat == 0xFFFE) {
            // Parse extensible format
            val cbSize = fmtData[16].toInt() and 0xFF or (fmtData[17].toInt() and 0xFF shl 8)
            val validBits = fmtData[18].toInt() and 0xFF or (fmtData[19].toInt() and 0xFF shl 8)
            val channelMask = fmtData[20].toInt() and 0xFF or
                    (fmtData[21].toInt() and 0xFF shl 8) or
                    (fmtData[22].toInt() and 0xFF shl 16) or
                    (fmtData[23].toInt() and 0xFF shl 24)
            val subFormat = ByteArray(16)
            System.arraycopy(fmtData, 24, subFormat, 0, 16)
            val actualFormat = subFormat[0].toInt() and 0xFF or (subFormat[1].toInt() and 0xFF shl 8)
            if (actualFormat != 1 && actualFormat != 3) {
                Logger.logw("Unsupported WAV sub format: actualFormat=$actualFormat. Only PCM (1) and IEEE Float (3) are supported")
                throw IOException("Unsupported WAV sub format: only PCM and IEEE Float are supported")
            }
            audioFormat = actualFormat
        } else if (audioFormat != 1 && audioFormat != 3) {
            Logger.logw("Unsupported WAV format: audioFormat=$audioFormat. Only PCM (1) and IEEE Float (3) are supported")
            throw IOException("Unsupported WAV format: only PCM and IEEE Float are supported")
        }

        channels = fmtData[2].toInt() and 0xFF or (fmtData[3].toInt() and 0xFF shl 8)
        sampleRate = fmtData[4].toInt() and 0xFF or
                (fmtData[5].toInt() and 0xFF shl 8) or
                (fmtData[6].toInt() and 0xFF shl 16) or
                (fmtData[7].toInt() and 0xFF shl 24)
        blockAlign = fmtData[12].toInt() and 0xFF or (fmtData[13].toInt() and 0xFF shl 8)
        bitDepth = fmtData[14].toInt() and 0xFF or (fmtData[15].toInt() and 0xFF shl 8)
        Logger.logi("Parsed WAV header: audioFormat=$audioFormat, channels=$channels, sampleRate=$sampleRate, bitDepth=$bitDepth")
    }

    private fun parseListChunk(listData: ByteArray) {
        val listType = String(listData, 0, 4, Charsets.UTF_8)
        Logger.logd("Parsing LIST chunk: type=$listType, totalSize=${listData.size}")
        if (listType == "INFO") {
            var offset = 4
            while (offset < listData.size) {
                if (offset + 7 >= listData.size) {
                    Logger.loge("Invalid listData: not enough bytes to read subChunkId and subChunkSize")
                    break
                }

                val subChunkId = String(listData, offset, 4, Charsets.UTF_8)
                val subChunkSize = (listData[offset + 4].toUByte().toInt()) or
                                    (listData[offset + 5].toUByte().toInt() shl 8) or
                                    (listData[offset + 6].toUByte().toInt() shl 16) or
                                    (listData[offset + 7].toUByte().toInt() shl 24)
                Logger.logd("Parsing subChunk: id=$subChunkId, size=$subChunkSize")

                offset += 8

                if (!subChunkId.all { it.isLetterOrDigit() }) {
                    Logger.loge("Invalid subChunkId: $subChunkId, skipping chunk")
                    offset += subChunkSize
                    if (subChunkSize % 2 != 0) {
                        offset += 1
                    }
                    continue
                }

                if (subChunkSize <= 0 || offset + subChunkSize > listData.size) {
                    Logger.loge("Invalid subChunkSize: $subChunkSize, remaining bytes: ${listData.size - offset}")
                    offset = listData.size
                    continue
                }

                try {
                    val subChunkData = String(listData, offset, subChunkSize, Charsets.UTF_8)
                    when (subChunkId) {
                        "INAM" -> audioTitle = subChunkData
                        "IART" -> artist = subChunkData
                    }
                } catch (e: Exception) {
                    Logger.loge("Failed to decode subChunkData for id=$subChunkId: ${e.message}")
                }
                offset += subChunkSize
                if (subChunkSize % 2 != 0) {
                    offset += 1
                }
            }
        }
    }

    private fun parseInfoChunk(infoData: ByteArray) {
        var offset = 0
        while (offset < infoData.size) {
            val subChunkId = String(infoData, offset, 4)
            val subChunkSize = infoData[offset + 4].toInt() and 0xFF or
                    (infoData[offset + 5].toInt() and 0xFF shl 8) or
                    (infoData[offset + 6].toInt() and 0xFF shl 16) or
                    (infoData[offset + 7].toInt() and 0xFF shl 24)
            offset += 8

            val subChunkData = String(infoData, offset, subChunkSize)
            when (subChunkId) {
                "INAM" -> audioTitle = subChunkData // Title
                "IART" -> artist = subChunkData // Artist
            }
            offset += subChunkSize
        }
    }

    fun getAudioTitle(): String {
        return audioTitle ?: file.nameWithoutExtension
    }

    fun getArtist(): String {
        return artist ?: "Unknown Artist"
    }

    fun getAudioFormat(): Int {
        return audioFormat
    }

    fun getSampleRate(): Int {
        return sampleRate
    }

    fun getChannels(): Int {
        return channels
    }

    fun getBitDepth(): Int {
        return bitDepth
    }

    fun getBlockAlign(): Int {
        return blockAlign
    }

    fun getAudioData(): ByteArray {
        val audioData = ByteArray(dataSize)
        try {
            FileInputStream(file).use { fis ->
                fis.skip(dataStartOffset.toLong())
                fis.read(audioData)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return audioData
    }

    fun getFileName(): String {
        return file.name
    }

    fun getFile(): File {
        return file
    }

    fun getDataStartOffset(): Int {
        return dataStartOffset
    }

    fun getTotalDuration(): Int {
        // Calculate total duration in milliseconds
        return if (dataSize == 0 || sampleRate == 0) {
            0
        } else {
            (dataSize / (sampleRate / 1000 * channels * (bitDepth / 8))).toInt()
        }
    }

    fun getAudioDataSize(): Int {
        return dataSize
    }
}
