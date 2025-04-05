package com.panda.audioplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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

    private var isPlaying: Boolean = false
    private var loopMode: LoopMode = LoopMode.NO_LOOP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission if not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_READ_EXTERNAL_STORAGE)
        } else {
            // Permission already granted, proceed to read audio files
            readAudioFiles()
        }

        // Initialize UI components
        playPauseButton = findViewById(R.id.play_pause_button)
        prevButton = findViewById(R.id.prev_button)
        nextButton = findViewById(R.id.next_button)
        loopButton = findViewById(R.id.loop_button)
        playlistButton = findViewById(R.id.playlist_button)
        seekBar = findViewById(R.id.seek_bar)
        audioTitle = findViewById(R.id.audio_title)
        audioArtist = findViewById(R.id.audio_artist)
        audioCover = findViewById(R.id.audio_cover)

        // Set up button listeners
        playPauseButton.setOnClickListener {
            togglePlayPause()
        }

        prevButton.setOnClickListener {
            playPrevious()
        }

        nextButton.setOnClickListener {
            playNext()
        }

        loopButton.setOnClickListener {
            toggleLoopMode()
        }

        playlistButton.setOnClickListener {
            openPlaylist()
        }

        // Set up seek bar listener
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
                // Permission granted, proceed to read audio files
                readAudioFiles()
            } else {
                // Permission denied, show a dialog to inform the user
                showPermissionDeniedDialog()
            }
        }
    }

    private fun readAudioFiles() {
        // Implement the logic to read audio files here
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("To read audio files, you need to grant the storage permission. Please enable it in the app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Open app settings to allow the user to manually enable the permission
                openAppSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAppSettings() {
        // Implement the logic to open app settings here
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            pausePlayback()
            playPauseButton.setImageResource(R.drawable.ic_play)
        } else {
            startPlayback()
            playPauseButton.setImageResource(R.drawable.ic_pause)
        }
        isPlaying = !isPlaying
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
        // TODO: Implement logic to open the playlist or queue
    }

    private fun seekTo(progress: Int) {
        // TODO: Implement logic to seek to a specific position in the track
    }

    private fun startPlayback() {
        // TODO: Implement logic to start playback
    }

    private fun pausePlayback() {
        // TODO: Implement logic to pause playback
    }

    private fun setLoopMode(mode: LoopMode) {
        loopMode = mode
        // TODO: Implement logic to apply the loop mode
    }

    enum class LoopMode {
        NO_LOOP, LOOP_SINGLE, LOOP_SHUFFLE
    }
}
