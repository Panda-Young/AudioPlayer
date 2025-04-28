package com.panda.audioplayer

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.constraintlayout.widget.ConstraintLayout
import com.panda.audioplayer.permission.PermissionManager
import com.panda.audioplayer.utils.Logger
import com.panda.audioplayer.utils.WavFile
import androidx.core.view.isVisible
import android.content.Context
import java.io.File

class MainActivity : AppCompatActivity(), PermissionManager.PermissionCallback {

    private lateinit var viewInitializer: ViewInitializer
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var audioManager: AudioManager
    private lateinit var audioTrackManager: AudioTrackManager
    private val playlist = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (::audioTrackManager.isInitialized) { // Check if audioTrackManager is initialized
                updateSeekBar()
            }
            handler.postDelayed(this, 1000) // Update every second
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Logger.logd("Start MainActivity. SDK Version: ${Build.VERSION.SDK_INT}")

        // Initialize components
        viewInitializer = ViewInitializer(this)
        permissionHandler = PermissionHandler(this)
        audioManager = AudioManager(this)

        // Initialize views
        viewInitializer.initializeViews()
        viewInitializer.initializeRecyclerView(playlist)

        // Initialize permissions
        permissionHandler.initializePermissions()

        // Initialize audio manager
        audioManager.initializeAudioManager()

        // Load audio files
        loadAudioFiles()

        // Set up listeners
        setupListeners()

        // Start updating SeekBar only after audioTrackManager is initialized
        if (playlist.isNotEmpty()) {
            val initialFilePath = playlist[0]
            val wavFile = WavFile(File(initialFilePath))
            initializeAudioTrackManager(wavFile.getSampleRate())
            handler.post(updateSeekBarRunnable)
        }
    }

    private fun loadAudioFiles() {
        playlist.clear()
        playlist.addAll(audioManager.getAudioFileNames())
        viewInitializer.playlistAdapter.notifyDataSetChanged()
        if (playlist.isNotEmpty()) {
            viewInitializer.playlistAdapter.setSelectedPosition(0)
        }
    }

    private fun initializeAudioTrackManager(sampleRate: Int) {
        audioTrackManager = AudioTrackManager(sampleRate)
        audioTrackManager.onPlaybackComplete = {
            runOnUiThread {
                val playPauseButton = findViewById<ImageView>(R.id.play_pause_button)
                playPauseButton.setImageResource(R.drawable.ic_play) // Switch to play icon

                // Reset the SeekBar and time labels
                viewInitializer.seekBar.progress = 0
                viewInitializer.currentTime.text = formatTime(0)
            }
        }
    }

    private fun setupListeners() {
        viewInitializer.rescanButton.setOnClickListener {
            audioManager.refreshAudioFiles()
            loadAudioFiles()
            Logger.logi("Rescanned audio files")
        }

        viewInitializer.closeButton.setOnClickListener {
            togglePlaylistVisibility()
            Logger.logi("Closed playlist")
        }

        findViewById<ImageView>(R.id.playlist_button).setOnClickListener {
            togglePlaylistVisibility()
        }

        // Set up play/pause button listener
        findViewById<ImageView>(R.id.play_pause_button).setOnClickListener {
            togglePlayPause()
        }

        // Set up SeekBar listener
        viewInitializer.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && ::audioTrackManager.isInitialized) { // Check if audioTrackManager is initialized
                    audioTrackManager.seekTo(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Set up playlist item click listener
        viewInitializer.playlistAdapter.setOnItemClickListener { filePath ->
            val wavFile = WavFile(File(filePath))
            initializeAudioTrackManager(wavFile.getSampleRate()) // Initialize with the correct sample rate
            updateAudioInfo(wavFile)
        }
    }

    private fun updateAudioInfo(wavFile: WavFile) {
        val titleTextView = findViewById<TextView>(R.id.audio_title)
        val artistTextView = findViewById<TextView>(R.id.audio_artist)
        val totalDuration = wavFile.getTotalDuration()

        titleTextView.text = wavFile.getAudioTitle()
        artistTextView.text = wavFile.getArtist()
        viewInitializer.seekBar.max = totalDuration
        viewInitializer.totalTime.text = formatTime(totalDuration)
    }

    private fun togglePlayPause() {
        if (!::audioTrackManager.isInitialized) return // Check if audioTrackManager is initialized

        val playPauseButton = findViewById<ImageView>(R.id.play_pause_button)
        val selectedFilePath = playlist[viewInitializer.playlistAdapter.getSelectedPosition()]

        if (audioTrackManager.isPlaying) {
            audioTrackManager.pausePlay()
            playPauseButton.setImageResource(R.drawable.ic_play)
            Logger.logi("Audio paused")
        } else {
            if (audioTrackManager.isSameAudioFile(selectedFilePath) && !audioTrackManager.isCompleted) {
                audioTrackManager.resumePlay()
                Logger.logi("Resuming playback from paused position")
            } else {
                audioTrackManager.startPlay(selectedFilePath)
                Logger.logi("Starting playback of new audio file: $selectedFilePath")
            }
            playPauseButton.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun togglePlaylistVisibility() {
        if (viewInitializer.playlistRecyclerView.visibility == View.VISIBLE) {
            viewInitializer.playlistRecyclerView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    viewInitializer.playlistRecyclerView.visibility = View.GONE
                    viewInitializer.playlistControlArea.visibility = View.GONE
                }
        } else {
            viewInitializer.playlistRecyclerView.alpha = 0f
            viewInitializer.playlistRecyclerView.visibility = View.VISIBLE
            viewInitializer.playlistControlArea.visibility = View.VISIBLE
            viewInitializer.playlistRecyclerView.layoutParams.height = resources.getDimensionPixelSize(R.dimen.playlist_height)
            viewInitializer.playlistRecyclerView.animate()
                .alpha(1f)
                .setDuration(300)
        }
        viewInitializer.playlistRecyclerView.requestLayout()
    }

    private fun updateSeekBar() {
        if (::audioTrackManager.isInitialized && (audioTrackManager.isPlaying || audioTrackManager.isPaused())) {
            val currentPosition = audioTrackManager.getCurrentPosition()
            viewInitializer.seekBar.progress = currentPosition
            viewInitializer.currentTime.text = formatTime(currentPosition)
        }
    }

    private fun formatTime(millis: Int): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionHandler.permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onAllPermissionsGranted() {
        Logger.logi("All permissions granted, proceeding with app functionality")
    }

    override fun onPermissionsDenied() {
        Logger.logw("Permissions denied, exiting the app")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBarRunnable)
    }
}

class ViewInitializer(private val activity: MainActivity) {

    lateinit var playlistRecyclerView: RecyclerView
    lateinit var playlistAdapter: PlaylistAdapter
    lateinit var seekBar: SeekBar
    lateinit var currentTime: TextView
    lateinit var totalTime: TextView
    lateinit var rescanButton: TextView
    lateinit var closeButton: TextView
    lateinit var playlistControlArea: ConstraintLayout

    fun initializeViews() {
        activity.setContentView(R.layout.activity_main)
        playlistRecyclerView = activity.findViewById(R.id.playlist_recycler_view)
        seekBar = activity.findViewById(R.id.seek_bar)
        currentTime = activity.findViewById(R.id.current_time)
        totalTime = activity.findViewById(R.id.total_time)
        rescanButton = activity.findViewById(R.id.rescanButton)
        closeButton = activity.findViewById(R.id.closeButton)
        playlistControlArea = activity.findViewById(R.id.playlist_control_area)
    }

    fun initializeRecyclerView(playlist: MutableList<String>) {
        playlistRecyclerView.layoutManager = LinearLayoutManager(activity)
        playlistAdapter = PlaylistAdapter(playlist) { fileName ->
            playlistAdapter.removeItem(fileName)
        }
        playlistRecyclerView.adapter = playlistAdapter
    }
}

class PermissionHandler(private val activity: MainActivity) {

    internal lateinit var permissionManager: PermissionManager

    fun initializePermissions() {
        permissionManager = PermissionManager(activity)
        permissionManager.setPermissionCallback(activity)
        permissionManager.checkAndRequestPermissions()
    }
}

class AudioManager(private val context: Context) {

    private lateinit var audioFileManager: AudioFileManager

    fun initializeAudioManager() {
        audioFileManager = AudioFileManager(context.contentResolver, context)
    }

    fun getAudioFileNames(): List<String> {
        val audioFiles = audioFileManager.scanAllLocalFiles()
        return audioFiles.map { it.absolutePath }
    }

    fun refreshAudioFiles() {
        audioFileManager.refreshAudioFiles()
    }
}
