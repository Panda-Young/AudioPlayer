package com.panda.audioplayer

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.panda.audioplayer.utils.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class AudioFileManager(private val contentResolver: ContentResolver, private val context: Context) {

    companion object {
        // Define excluded paths (relative to the external storage directory)
        private val EXCLUDED_PATHS = listOf(
            "/Music/ringtone",
            "/Music/notifications",
            "/Music/alarms"
        )
        private const val CACHE_FILE_NAME = "audio_files_cache.dat"
    }

    private var cachedAudioFiles: MutableList<File>? = null

    @SuppressLint("SdCardPath")
    private fun scanFiles(): MutableList<File> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryAudioFiles().toMutableList()
        } else {
            val audioPaths = listOf(
                Environment.getExternalStorageDirectory().absolutePath + "/Music/",
                Environment.getExternalStorageDirectory().absolutePath + "/Download/"
            )
            val audioFiles = mutableListOf<File>()
            for (path in audioPaths) {
                val directory = File(path)
                if (directory.exists() && directory.isDirectory) {
                    listAudioFilesRecursively(directory, audioFiles, EXCLUDED_PATHS)
                }
            }
            audioFiles
        }
    }

    @SuppressLint("SdCardPath")
    fun scanAllLocalFiles(): MutableList<File> {
        // Check if cachedAudioFiles is null or empty, and proceed with scanning if necessary
        return if (cachedAudioFiles.isNullOrEmpty()) {
            val result = scanFiles()
            // Update the cache with the new result
            cachedAudioFiles = result
            saveCacheToFile(result) // Save cache to file
            result
        } else {
            Logger.logd("Using cached audio files")
            cachedAudioFiles!!
        }
    }

    fun refreshAudioFiles() {
        cachedAudioFiles = null // Clear the cache
        val result = scanFiles()
        cachedAudioFiles = result // Update the cache with the new result
        saveCacheToFile(result) // Save the new scan result to cache file
    }

    private fun listAudioFilesRecursively(directory: File, audioFiles: MutableList<File>, excludedPaths: List<String>) {
        if (isPathExcluded(directory.absolutePath)) {
            return
        }
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                listAudioFilesRecursively(file, audioFiles, excludedPaths)
            } else if (file.isFile && isAudioFile(file)) {
                Logger.logd("Found audio file: ${file.absolutePath}")
                audioFiles.add(file)
            }
        }
    }

    private fun queryAudioFiles(): List<File> {
        val audioFiles = mutableListOf<File>()
        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val filePath = cursor.getString(dataColumn)
                if (!isPathExcluded(filePath)) {
                    val file = File(filePath)
                    if (file.exists()) {
                        Logger.logd("Found audio file: ${file.absolutePath}")
                        audioFiles.add(file)
                    }
                }
            }
        }

        return audioFiles
    }

    private fun isPathExcluded(filePath: String): Boolean {
        val externalStoragePath = Environment.getExternalStorageDirectory().absolutePath
        return EXCLUDED_PATHS.any { filePath.startsWith("$externalStoragePath$it") }
    }

    private fun isAudioFile(file: File): Boolean {
        val audioExtensions = listOf(".mp3", ".wav", ".ogg", ".m4a", ".flac")
        return audioExtensions.any { file.name.endsWith(it, ignoreCase = true) }
    }

    private fun saveCacheToFile(audioFiles: List<File>) {
        try {
            val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
            FileOutputStream(cacheFile).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(audioFiles.map { it.absolutePath })
                }
            }
            Logger.logd("Cache saved to file: ${cacheFile.absolutePath}")
        } catch (e: Exception) {
            Logger.logf("Error saving cache to file: ${e.message}")
        }
    }

    private fun loadCacheFromFile(): List<File>? {
        return try {
            val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                FileInputStream(cacheFile).use { fis ->
                    ObjectInputStream(fis).use { ois ->
                        val filePaths = ois.readObject() as List<String>
                        filePaths.map { File(it) }
                    }
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.logf("Error loading cache from file: ${e.message}")
            null
        }
    }

    init {
        cachedAudioFiles = loadCacheFromFile()?.toMutableList()
    }
}
