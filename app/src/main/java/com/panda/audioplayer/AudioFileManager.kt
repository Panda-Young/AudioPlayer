package com.panda.audioplayer

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.panda.audioplayer.utils.Logger
import java.io.File

class AudioFileManager(private val contentResolver: ContentResolver) {

    companion object {
        // Define excluded paths (relative to the external storage directory)
        private val EXCLUDED_PATHS = listOf(
            "/Music/ringtone",
            "/Music/notifications",
            "/Music/alarms"
        )
    }

    private var cachedAudioFiles: MutableList<File>? = null

    @SuppressLint("SdCardPath")
    fun scanAllLocalFiles(): MutableList<File> {
        return cachedAudioFiles ?: run {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            cachedAudioFiles = result
            result
        }
    }

    fun refreshAudioFiles() {
        cachedAudioFiles = null
        scanAllLocalFiles()
    }

    private fun listAudioFilesRecursively(directory: File, audioFiles: MutableList<File>, excludedPaths: List<String>) {
        if (isPathExcluded(directory.absolutePath)) {
            return
        }
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                listAudioFilesRecursively(file, audioFiles, excludedPaths)
            } else if (file.isFile && isAudioFile(file)) {
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
}
