package com.panda.audioplayer.utils

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import com.panda.audioplayer.utils.Logger

class WavFile(private val file: File) {

    private var sampleRate = 0
    private var channels = 0
    private var bitDepth = 0
    private var dataSize = 0
    private var dataStartOffset = 0
    private var audioFormat = 0 // Add this variable

    init {
        parseWavHeader()
    }

    private fun parseWavHeader() {
        try {
            FileInputStream(file).use { fis ->
                // Read the first 12 bytes to check RIFF and WAVE identifiers
                val headerBuffer = ByteArray(12)
                fis.read(headerBuffer)

                // Check if the file is a valid WAV file
                if (String(headerBuffer, 0, 4) != "RIFF" || String(headerBuffer, 8, 4) != "WAVE") {
                    Logger.logw("Invalid RIFF file: $String(headerBuffer, 0, 4) or not WAVE identifier: $String(headerBuffer, 8, 4)")
                    throw IOException("Invalid WAV file")
                }

                var fmtChunkFound = false
                var dataChunkFound = false

                // Loop through chunks until both fmt and data chunks are found
                while (!fmtChunkFound || !dataChunkFound) {
                    // Read the chunk header (8 bytes: 4 bytes for chunk ID + 4 bytes for chunk size)
                    val chunkHeader = ByteArray(8)
                    if (fis.read(chunkHeader) != 8) {
                        break // End of file reached
                    }

                    val chunkId = String(chunkHeader, 0, 4)
                    val chunkSize = chunkHeader[4].toInt() and 0xFF or
                            (chunkHeader[5].toInt() and 0xFF shl 8) or
                            (chunkHeader[6].toInt() and 0xFF shl 16) or
                            (chunkHeader[7].toInt() and 0xFF shl 24)

                    Logger.logd("Parsing chunk: id=$chunkId, size=$chunkSize")

                    when (chunkId) {
                        "fmt " -> {
                            // Allocate fmtData based on the chunk size
                            val fmtData = ByteArray(chunkSize)
                            fis.read(fmtData, 0, chunkSize) // Read the entire fmt chunk

                            // Parse the fmt chunk data
                            audioFormat = fmtData[0].toInt() and 0xFF or (fmtData[1].toInt() and 0xFF shl 8)
                            if (audioFormat == 0xFFFE) {
                                // Parse the extra_info_chunk_t structure for extensible format
                                val cbSize = fmtData[16].toInt() and 0xFF or (fmtData[17].toInt() and 0xFF shl 8)
                                val validBits = fmtData[18].toInt() and 0xFF or (fmtData[19].toInt() and 0xFF shl 8)
                                val channelMask = fmtData[20].toInt() and 0xFF or
                                        (fmtData[21].toInt() and 0xFF shl 8) or
                                        (fmtData[22].toInt() and 0xFF shl 16) or
                                        (fmtData[23].toInt() and 0xFF shl 24)
                                val subFormat = ByteArray(16)
                                System.arraycopy(fmtData, 24, subFormat, 0, 16) // Copy the 16-byte sub format GUID

                                // Parse the sub format GUID to determine the actual audio format
                                val actualFormat = subFormat[0].toInt() and 0xFF or (subFormat[1].toInt() and 0xFF shl 8)
                                if (actualFormat != 1 && actualFormat != 3) { // Support PCM (1) and IEEE Float (3)
                                    Logger.logw("Unsupported WAV sub format: actualFormat=$actualFormat. Only PCM (1) and IEEE Float (3) are supported")
                                    throw IOException("Unsupported WAV sub format: only PCM and IEEE Float are supported")
                                }
                                audioFormat = actualFormat
                            } else if (audioFormat != 1 && audioFormat != 3) { // Support PCM (1) and IEEE Float (3)
                                Logger.logw("Unsupported WAV format: audioFormat=$audioFormat. Only PCM (1) and IEEE Float (3) are supported")
                                throw IOException("Unsupported WAV format: only PCM and IEEE Float are supported")
                            }

                            channels = fmtData[2].toInt() and 0xFF or (fmtData[3].toInt() and 0xFF shl 8)
                            sampleRate = fmtData[4].toInt() and 0xFF or
                                    (fmtData[5].toInt() and 0xFF shl 8) or
                                    (fmtData[6].toInt() and 0xFF shl 16) or
                                    (fmtData[7].toInt() and 0xFF shl 24)
                            bitDepth = fmtData[14].toInt() and 0xFF or (fmtData[15].toInt() and 0xFF shl 8)
                            fmtChunkFound = true
                            Logger.logd("Parsed WAV header: audioFormat=$audioFormat, channels=$channels, sampleRate=$sampleRate, bitDepth=$bitDepth")
                        }
                        "data" -> {
                            // Parse data chunk
                            dataSize = chunkSize
                            dataStartOffset = fis.channel.position().toInt()
                            dataChunkFound = true

                            // Skip the data chunk content (we only need its size and offset)
                            fis.skip(chunkSize.toLong())
                        }
                        else -> {
                            // Skip unknown chunks (e.g., JUNK, FLLR, etc.)
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

    fun ByteArray.toHexString(): String {
        return joinToString("") { "%02X".format(it) }
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
            // Formula: (dataSize * 1000) / (sampleRate * channels * (bitDepth / 8))
            (dataSize * 1000 / (sampleRate * channels * (bitDepth / 8))).toInt()
        }
    }
}
