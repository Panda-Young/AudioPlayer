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

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 100

    private lateinit var playPauseButton: ImageView
    private lateinit var prevButton: ImageView
    private lateinit var nextButton: ImageView
    private lateinit var loopButton: ImageView
    private lateinit var playlistButton: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var audioTitle: TextView
    private lateinit var audioArtist: TextView
    private lateinit var audioCover: ImageView

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false
    private var loopMode: LoopMode = LoopMode.NO_LOOP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        initializeUIComponents()

        setButtonListeners()

        setSeekBarListener()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_READ_EXTERNAL_STORAGE)
        } else {
            readAudioFiles()
        }
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
                    seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Do nothing
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Do nothing
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                readAudioFiles()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    @SuppressLint("SdCardPath")
    private fun readAudioFiles(): List<File> {
        val audioPaths = listOf(
            "/sdcard/Music/",
            "/sdcard/Download/",
            "/data/local/tmp/"
        )

        val audioFiles = mutableListOf<File>()
        for (path in audioPaths) {
            val directory = File(path)
            if (directory.exists() && directory.isDirectory) {
                listAudioFilesRecursively(directory, audioFiles)
            }
        }

        return audioFiles
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
        mediaPlayer?.let {
            if (isPlaying) {
                pausePlayback()
            } else {
                if (it.isPlaying) {
                    pausePlayback()
                } else {
                    startPlayback()
                }
            }
        }
    }

    private fun playPrevious() {
        // TODO: Implement logic to play the previous track
    }

    private fun playNext() {
        // TODO: Implement logic to play the next track
    }

    private fun toggleLoopMode() {
        when (loopMode) {
            LoopMode.NO_LOOP -> {
                setLoopMode(LoopMode.LOOP_SINGLE)
                loopButton.setImageResource(R.drawable.ic_single_loop)
            }
            LoopMode.LOOP_SINGLE -> {
                setLoopMode(LoopMode.LOOP_SHUFFLE)
                loopButton.setImageResource(R.drawable.ic_shuffle)
            }
            LoopMode.LOOP_SHUFFLE -> {
                setLoopMode(LoopMode.NO_LOOP)
                loopButton.setImageResource(R.drawable.ic_loop)
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

        val audioFileNames = audioFiles.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Audio Files")
            .setItems(audioFileNames) { _, which ->
                val selectedFile = audioFiles[which]
                playAudioFile(selectedFile)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun playAudioFile(file: File) {
        if (mediaPlayer == null) {
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
                } catch (e: IOException) {
                    e.printStackTrace()
                    this@MainActivity.isPlaying = false
                    playPauseButton.setImageResource(R.drawable.ic_play)
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Playback Error")
                        .setMessage("Failed to play the audio file.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        } else {
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        it.stop()
                        it.reset()
                    }
                    it.setDataSource(file.absolutePath)
                    it.prepare()
                    it.start()
                    this@MainActivity.isPlaying = true
                    playPauseButton.setImageResource(R.drawable.ic_pause)
                    audioTitle.text = file.name
                    val artistName = getArtistName(file)
                    audioArtist.text = artistName
                    audioArtist.visibility = if (artistName.isNullOrBlank()) View.GONE else View.VISIBLE
                } catch (e: IOException) {
                    e.printStackTrace()
                    this@MainActivity.isPlaying = false
                    playPauseButton.setImageResource(R.drawable.ic_play)
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Playback Error")
                        .setMessage("Failed to play the audio file.")
                        .setPositiveButton("OK", null)
                        .show()
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
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                playPauseButton.setImageResource(R.drawable.ic_play)
            }
        }
    }

    private fun setLoopMode(mode: LoopMode) {
        loopMode = mode
        // TODO: Implement logic to apply the loop mode
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    enum class LoopMode {
        NO_LOOP, LOOP_SINGLE, LOOP_SHUFFLE
    }
}
