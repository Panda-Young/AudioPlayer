package com.panda.audioplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.provider.MediaStore
import android.os.Build
import android.os.Environment

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 100
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var playPauseButton: ImageView
    private lateinit var prevButton: ImageView
    private lateinit var nextButton: ImageView
    private lateinit var loopButton: ImageView
    private lateinit var playlistButton: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var audioTitle: TextView
    private lateinit var audioArtist: TextView
    private lateinit var audioCover: ImageView
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false
    private var loopMode: LoopMode = LoopMode.LOOP_LIST
    private var currentPlaylist: List<File> = emptyList()
    private var currentIndex: Int = -1
    private var unplayedSongs: MutableList<File> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeUIComponents()
        checkAndRequestPermissions()

        setButtonListeners()
        setSeekBarListener()
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            println("READ_EXTERNAL_STORAGE permission not granted, requesting permission")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_READ_EXTERNAL_STORAGE)
        } else {
            println("READ_EXTERNAL_STORAGE permission already granted")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    println("MANAGE_EXTERNAL_STORAGE permission not granted")
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${packageName}")
                    startActivity(intent)
                } else {
                    println("MANAGE_EXTERNAL_STORAGE permission granted")
                    readAudioFiles()
                    playFirstAudioFileIfAvailable()
                }
            } else {
                readAudioFiles()
                playFirstAudioFileIfAvailable()
            }
        }
    }

    private fun playFirstAudioFileIfAvailable() {
        currentPlaylist = readAudioFiles()
        if (currentPlaylist.isNotEmpty()) {
            if (!::audioTitle.isInitialized) {
                println("audioTitle is not initialized")
                return
            }
            println("Playing first audio file")
            val firstFile = currentPlaylist[0]
            updateUIForSelectedFile(firstFile)
            currentIndex = 0
        } else {
            println("No audio files found")
        }
    }

    private fun updateUIForSelectedFile(file: File) {
        audioTitle.text = file.name
        val artistName = getArtistName(file)
        audioArtist.text = artistName
        audioArtist.visibility = if (artistName.isNullOrBlank()) View.GONE else View.VISIBLE
        playPauseButton.setImageResource(R.drawable.ic_play)
    }

    private fun initializeUIComponents() {
        playPauseButton = findViewById(R.id.play_pause_button)
        prevButton = findViewById(R.id.prev_button)
        nextButton = findViewById(R.id.next_button)
        loopButton = findViewById(R.id.loop_button)
        playlistButton = findViewById(R.id.playlist_button)
        seekBar = findViewById(R.id.seek_bar)
        audioTitle = findViewById(R.id.audio_title)
        audioArtist = findViewById(R.id.audio_artist)
        audioCover = findViewById(R.id.audio_cover)
        currentTime = findViewById(R.id.current_time)
        totalTime = findViewById(R.id.total_time)
    }

    private fun setButtonListeners() {
        playPauseButton.setOnClickListener { togglePlayPause() }
        prevButton.setOnClickListener { playPrevious() }
        nextButton.setOnClickListener { playNext() }
        loopButton.setOnClickListener { toggleLoopMode() }
        playlistButton.setOnClickListener { openPlaylist() }
    }

    private fun setSeekBarListener() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopUpdatingSeekBar()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                startUpdatingSeekBar()
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                println("READ_EXTERNAL_STORAGE permission granted")
                checkAndRequestPermissions()
            } else {
                println("READ_EXTERNAL_STORAGE permission denied")
                showPermissionDeniedDialog()
            }
        }
    }

    @SuppressLint("SdCardPath")
    private fun readAudioFiles(): List<File> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryAudioFiles()
        } else {
            // old version Android 10 and below to directly access the audio files
            val audioPaths = listOf(
                Environment.getExternalStorageDirectory().absolutePath + "/Music/",
                Environment.getExternalStorageDirectory().absolutePath + "/Download/"
            )
            val audioFiles = mutableListOf<File>()
            for (path in audioPaths) {
                val directory = File(path)
                if (directory.exists() && directory.isDirectory) {
                    listAudioFilesRecursively(directory, audioFiles)
                }
            }
            audioFiles
        }
    }

    private fun listAudioFilesRecursively(directory: File, audioFiles: MutableList<File>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                listAudioFilesRecursively(file, audioFiles)
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
                val file = File(filePath)
                if (file.exists()) {
                    println("Found audio file: ${file.absolutePath}")
                    audioFiles.add(file)
                }
            }
        }

        return audioFiles
    }

    private fun isAudioFile(file: File): Boolean {
        val audioExtensions = listOf(".mp3", ".wav", ".ogg", ".m4a")
        return audioExtensions.any { file.name.endsWith(it, ignoreCase = true) }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("To read audio files, you need to grant the storage permission. Please enable it in the app settings.")
            .setPositiveButton("Go to Settings") { _, _ -> openAppSettings() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        // Implement the logic to open app settings here
    }

    private fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (isPlaying) {
                pausePlayback()
            } else {
                if (player.isPlaying) {
                    pausePlayback()
                } else {
                    if (currentIndex != -1) {
                        playAudioFile(currentPlaylist[currentIndex])
                    } else {
                        println("No audio file selected")
                    }
                }
            }
        } ?: run {
            if (currentIndex != -1) {
                playAudioFile(currentPlaylist[currentIndex])
            } else {
                println("No audio file selected")
            }
        }
    }

    private fun playPrevious() {
        if (currentPlaylist.isEmpty() || currentIndex == -1) return

        val previousIndex = if (currentIndex == 0) currentPlaylist.size - 1 else currentIndex - 1
        playAudioFile(currentPlaylist[previousIndex])
        currentIndex = previousIndex
    }

    private fun playNext() {
        if (currentPlaylist.isEmpty() || currentIndex == -1) return

        val nextIndex = if (currentIndex == currentPlaylist.size - 1) 0 else currentIndex + 1
        playAudioFile(currentPlaylist[nextIndex])
        currentIndex = nextIndex
    }

    private fun setLoopMode(mode: LoopMode) {
        loopMode = mode
        when (loopMode) {
            LoopMode.LOOP_SINGLE -> {
                mediaPlayer?.isLooping = true
                loopButton.setImageResource(R.drawable.ic_single_loop)
            }
            LoopMode.LOOP_LIST -> {
                mediaPlayer?.isLooping = false
                loopButton.setImageResource(R.drawable.ic_list_loop)
            }
            LoopMode.LOOP_SHUFFLE -> {
                mediaPlayer?.isLooping = false
                loopButton.setImageResource(R.drawable.ic_shuffle)
                unplayedSongs = currentPlaylist.toMutableList()
            }
        }
    }

    private fun toggleLoopMode() {
        when (loopMode) {
            LoopMode.LOOP_SINGLE -> {
                setLoopMode(LoopMode.LOOP_LIST)
                loopButton.setImageResource(R.drawable.ic_list_loop)
            }
            LoopMode.LOOP_LIST -> {
                setLoopMode(LoopMode.LOOP_SHUFFLE)
                loopButton.setImageResource(R.drawable.ic_shuffle)
            }
            LoopMode.LOOP_SHUFFLE -> {
                setLoopMode(LoopMode.LOOP_SINGLE)
                loopButton.setImageResource(R.drawable.ic_single_loop)
            }
        }
    }

    private fun openPlaylist() {
        val audioFiles = readAudioFiles()
        if (audioFiles.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No Audio Files")
                .setMessage("No audio files found in the specified directories.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        currentPlaylist = audioFiles
        val audioFileNames = audioFiles.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Audio Files")
            .setItems(audioFileNames) { _, which ->
                val selectedFile = audioFiles[which]
                playAudioFile(selectedFile)
                currentIndex = which
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun playAudioFile(file: File) {
        mediaPlayer?.let { player ->
            try {
                if (player.isPlaying || isPlaying) {
                    player.stop()
                }
                player.reset()
                player.setDataSource(file.absolutePath)
                player.prepare()
                player.start()
                isPlaying = true
                playPauseButton.setImageResource(R.drawable.ic_pause)
                audioTitle.text = file.name
                val artistName = getArtistName(file)
                audioArtist.text = artistName
                audioArtist.visibility = if (artistName.isNullOrBlank()) View.GONE else View.VISIBLE
                seekBar.max = player.duration
                totalTime.text = formatTime(player.duration)
                startUpdatingSeekBar()

                player.setOnCompletionListener {
                    when (loopMode) {
                        LoopMode.LOOP_SINGLE -> {
                            player.seekTo(0)
                            player.start()
                        }
                        LoopMode.LOOP_LIST -> {
                            playNext()
                        }
                        LoopMode.LOOP_SHUFFLE -> {
                            if (unplayedSongs.isNotEmpty()) {
                                unplayedSongs.remove(file)
                                if (unplayedSongs.isNotEmpty()) {
                                    val nextFile = unplayedSongs.random()
                                    playAudioFile(nextFile)
                                } else {
                                    unplayedSongs = currentPlaylist.toMutableList()
                                    playAudioFile(unplayedSongs.random())
                                }
                            }
                        }
                    }
                }

                println("Playing audio file: ${file.name}")
            } catch (e: IOException) {
                e.printStackTrace()
                isPlaying = false
                playPauseButton.setImageResource(R.drawable.ic_play)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Playback Error")
                    .setMessage("Failed to play the audio file.")
                    .setPositiveButton("OK", null)
                    .show()
                println("Failed to play audio file: ${file.name}")
            }
        } ?: run {
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(file.absolutePath)
                    prepare()
                    start()
                    this@MainActivity.isPlaying = true
                    playPauseButton.setImageResource(R.drawable.ic_pause)
                    audioTitle.text = file.name
                    val artistName = getArtistName(file)
                    audioArtist.text = artistName
                    audioArtist.visibility = if (artistName.isNullOrBlank()) View.GONE else View.VISIBLE
                    seekBar.max = this.duration
                    totalTime.text = formatTime(this.duration)
                    startUpdatingSeekBar()

                    setOnCompletionListener {
                        when (loopMode) {
                            LoopMode.LOOP_SINGLE -> {
                                seekTo(0)
                                start()
                            }
                            LoopMode.LOOP_SHUFFLE -> {
                                if (unplayedSongs.isNotEmpty()) {
                                    unplayedSongs.remove(file)
                                    if (unplayedSongs.isNotEmpty()) {
                                        val nextFile = unplayedSongs.random()
                                        playAudioFile(nextFile)
                                    } else {
                                        unplayedSongs = currentPlaylist.toMutableList()
                                        playAudioFile(unplayedSongs.random())
                                    }
                                }
                            }
                            LoopMode.LOOP_LIST -> {
                                playNext()
                            }
                        }
                    }

                    println("Playing audio file: ${file.name}")
                } catch (e: IOException) {
                    e.printStackTrace()
                    this@MainActivity.isPlaying = false
                    playPauseButton.setImageResource(R.drawable.ic_play)
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Playback Error")
                        .setMessage("Failed to play the audio file.")
                        .setPositiveButton("OK", null)
                        .show()
                    println("Failed to play audio file: ${file.name}")
                }
            }
        }
    }

    private fun getArtistName(file: File): String? {
        return null
    }

    private fun seekTo(progress: Int) {
        mediaPlayer?.seekTo(progress)
    }

    private val updateSeekBar = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                seekBar.progress = player.currentPosition
                currentTime.text = formatTime(player.currentPosition)
                totalTime.text = formatTime(player.duration)
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun formatTime(milliseconds: Int): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun startUpdatingSeekBar() {
        handler.post(updateSeekBar)
    }

    private fun stopUpdatingSeekBar() {
        handler.removeCallbacks(updateSeekBar)
    }

    private fun startPlayback() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                try {
                    if (!it.isPlaying && it.currentPosition > 0) {
                        it.start()
                    } else {
                        it.prepare()
                        it.start()
                    }
                    isPlaying = true
                    playPauseButton.setImageResource(R.drawable.ic_pause)
                } catch (e: IOException) {
                    e.printStackTrace()
                    isPlaying = false
                    playPauseButton.setImageResource(R.drawable.ic_play)
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Playback Error")
                        .setMessage("Failed to start playback.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPlaying = false
                playPauseButton.setImageResource(R.drawable.ic_play)
                stopUpdatingSeekBar()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    enum class LoopMode {
        LOOP_SINGLE, LOOP_LIST, LOOP_SHUFFLE
    }
}
