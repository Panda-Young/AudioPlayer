package com.panda.audioplayer.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

data class AudioMetadata(
    val title: String,
    val artist: String,
    val duration: Int,
    val coverArt: ByteArray?
)

object MetadataReader {
    fun read(context: Context, source: String): AudioMetadata {
        val retriever = MediaMetadataRetriever()
        val fallbackName = if (source.startsWith("content://")) {
            Uri.parse(source).lastPathSegment ?: "Unknown"
        } else {
            File(source).nameWithoutExtension
        }
        return try {
            if (source.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(source))
            } else {
                retriever.setDataSource(source)
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: fallbackName
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Unknown Artist"
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toIntOrNull() ?: 0
            val coverArt = try { retriever.embeddedPicture } catch (e: Exception) { null }
            AudioMetadata(title, artist, duration, coverArt)
        } catch (e: Exception) {
            Logger.loge("Failed to read metadata from $source: ${e.message}")
            AudioMetadata(fallbackName, "Unknown Artist", 0, null)
        } finally {
            try { retriever.release() } catch (e: Exception) { /* ignore */ }
        }
    }
}
