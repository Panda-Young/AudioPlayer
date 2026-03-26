package com.panda.audioplayer.utils

import android.media.MediaMetadataRetriever
import java.io.File

data class AudioMetadata(
    val title: String,
    val artist: String,
    val duration: Int,
    val coverArt: ByteArray?
)

object MetadataReader {
    fun read(file: File): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Unknown Artist"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toIntOrNull() ?: 0
            val coverArt = try { retriever.embeddedPicture } catch (e: Exception) { null }
            AudioMetadata(title, artist, duration, coverArt)
        } catch (e: Exception) {
            Logger.loge("Failed to read metadata from ${file.name}: ${e.message}")
            AudioMetadata(file.nameWithoutExtension, "Unknown Artist", 0, null)
        } finally {
            try { retriever.release() } catch (e: Exception) { /* ignore */ }
        }
    }
}
