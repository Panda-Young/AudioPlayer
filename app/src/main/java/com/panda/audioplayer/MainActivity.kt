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
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity(), PermissionManager.PermissionCallback {

    // Define loop modes
    enum class LoopMode {
        REPEAT_ONE, // Repeat the current song
        REPEAT_ALL, // Repeat the entire playlist
        SHUFFLE     // Shuffle the playlist
    }

    private var currentVisibleView: VisibleView = VisibleView.NONE

    enum class VisibleView {
        EFFECTS, PLAYLIST, NONE
    }

    private lateinit var viewInitializer: ViewInitializer
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var audioManager: AudioManager
    private lateinit var audioTrackManager: AudioTrackManager
    private lateinit var effectRecyclerView: RecyclerView
    private lateinit var effectAdapter: EffectAdapter
    private lateinit var effectManager: EffectManager
    private val playlist = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var currentLoopMode: LoopMode = LoopMode.REPEAT_ALL // Default to repeat all
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
            initializeAudioTrackManager(wavFile.getSampleRate(), wavFile.getChannels())
            updateAudioInfo(wavFile)
            handler.post(updateSeekBarRunnable)
        }

        effectManager = EffectManager(audioTrackManager)

        effectAdapter = EffectAdapter(effectManager) { name, state ->
            Logger.logi("Effect $name ${if (state) "enabled" else "disabled"}")
        }

        // Initialize effect recycler view
        effectRecyclerView = findViewById(R.id.effect_recycler_view)
        effectRecyclerView.layoutManager = LinearLayoutManager(this)
        effectRecyclerView.adapter = effectAdapter

        val effectButton: ImageView = findViewById(R.id.effect_button)
        effectButton.setOnClickListener {
            if (currentVisibleView == VisibleView.EFFECTS) {
                effectRecyclerView.visibility = View.GONE
                currentVisibleView = VisibleView.NONE
            } else {
                if (viewInitializer.playlistRecyclerView.isVisible) {
                    togglePlaylistVisibility()
                }
                effectRecyclerView.visibility = View.VISIBLE
                currentVisibleView = VisibleView.EFFECTS
            }
        }
    }

    private fun handleEffectToggle(effectName: String, enabled: Boolean) {
        when (effectName) {
            "Reverb" -> audioTrackManager.setReverbEffect(enabled)
            "Equalizer" -> audioTrackManager.setEqualizerEffect(enabled)
            "3D Audio" -> audioTrackManager.set3DEffect(enabled)
        }
        Logger.logi("Effect $effectName ${if (enabled) "enabled" else "disabled"}")
    }

    private fun loadAudioFiles() {
        playlist.clear()
        playlist.addAll(audioManager.getAudioFileNames())
        viewInitializer.playlistAdapter.notifyDataSetChanged()
        if (playlist.isNotEmpty()) {
            viewInitializer.playlistAdapter.setSelectedPosition(0)
        }
    }

    private fun initializeAudioTrackManager(sampleRate: Int, channelConfig: Int) {
        audioTrackManager = AudioTrackManager(sampleRate, channelConfig)
        audioTrackManager.onPlaybackComplete = {
            runOnUiThread {
                handlePlaybackCompletion()
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
                    val currentPosition = seekBar.progress
                    viewInitializer.currentTime.text = formatTime(currentPosition)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Set up playlist item click listener
        viewInitializer.playlistAdapter.setOnItemClickListener { filePath ->
            switchToNewAudio(filePath)
        }

        // Set up loop button listener
        findViewById<ImageView>(R.id.loop_button).setOnClickListener {
            toggleLoopMode()
        }

        // Set up previous button listener
        findViewById<ImageView>(R.id.prev_button).setOnClickListener {
            playPrevious()
        }

        // Set up next button listener
        findViewById<ImageView>(R.id.next_button).setOnClickListener {
            playNext()
        }
    }

    // Play the previous song based on the current loop mode
    private fun playPrevious() {
        when (currentLoopMode) {
            // LoopMode.REPEAT_ONE -> {
            //     // Restart the current song
            //     val selectedFilePath = playlist[viewInitializer.playlistAdapter.getSelectedPosition()]
            //     switchToNewAudio(selectedFilePath)
            // }
            LoopMode.REPEAT_ONE, LoopMode.REPEAT_ALL -> {
                // Move to the previous song in the playlist
                val currentPosition = viewInitializer.playlistAdapter.getSelectedPosition()
                val previousPosition = if (currentPosition - 1 < 0) playlist.size - 1 else currentPosition - 1
                viewInitializer.playlistAdapter.setSelectedPosition(previousPosition)
                val previousFilePath = playlist[previousPosition]
                switchToNewAudio(previousFilePath)
            }
            LoopMode.SHUFFLE -> {
                // Play a random song from the playlist
                val randomPosition = (0 until playlist.size).random()
                viewInitializer.playlistAdapter.setSelectedPosition(randomPosition)
                val randomFilePath = playlist[randomPosition]
                switchToNewAudio(randomFilePath)
            }
        }
    }

    // Play the next song based on the current loop mode
    private fun playNext() {
        when (currentLoopMode) {
            // LoopMode.REPEAT_ONE -> {
            //     // Restart the current song
            //     val selectedFilePath = playlist[viewInitializer.playlistAdapter.getSelectedPosition()]
            //     switchToNewAudio(selectedFilePath)
            // }
            LoopMode.REPEAT_ONE, LoopMode.REPEAT_ALL -> {
                // Move to the next song in the playlist
                val currentPosition = viewInitializer.playlistAdapter.getSelectedPosition()
                val nextPosition = (currentPosition + 1) % playlist.size
                viewInitializer.playlistAdapter.setSelectedPosition(nextPosition)
                val nextFilePath = playlist[nextPosition]
                switchToNewAudio(nextFilePath)
            }
            LoopMode.SHUFFLE -> {
                // Play a random song from the playlist
                val randomPosition = (0 until playlist.size).random()
                viewInitializer.playlistAdapter.setSelectedPosition(randomPosition)
                val randomFilePath = playlist[randomPosition]
                switchToNewAudio(randomFilePath)
            }
        }
    }

    // Toggle loop mode and update the icon
    private fun toggleLoopMode() {
        currentLoopMode = when (currentLoopMode) {
            LoopMode.REPEAT_ONE -> LoopMode.REPEAT_ALL
            LoopMode.REPEAT_ALL -> LoopMode.SHUFFLE
            LoopMode.SHUFFLE -> LoopMode.REPEAT_ONE
        }
        updateLoopButtonIcon()
        Logger.logi("Loop mode: $currentLoopMode")
    }

    // Update the loop button icon based on the current loop mode
    private fun updateLoopButtonIcon() {
        val loopButton = findViewById<ImageView>(R.id.loop_button)
        when (currentLoopMode) {
            LoopMode.REPEAT_ONE -> loopButton.setImageResource(R.drawable.ic_single_loop)
            LoopMode.REPEAT_ALL -> loopButton.setImageResource(R.drawable.ic_list_loop)
            LoopMode.SHUFFLE -> loopButton.setImageResource(R.drawable.ic_shuffle)
        }
    }


    // Handle playback completion based on the current loop mode
    private fun handlePlaybackCompletion() {
        when (currentLoopMode) {
            LoopMode.REPEAT_ONE -> {
                // Restart the current song
                val selectedFilePath = playlist[viewInitializer.playlistAdapter.getSelectedPosition()]
                switchToNewAudio(selectedFilePath)
            }
            LoopMode.REPEAT_ALL -> {
                // Move to the next song in the playlist
                val nextPosition = (viewInitializer.playlistAdapter.getSelectedPosition() + 1) % playlist.size
                viewInitializer.playlistAdapter.setSelectedPosition(nextPosition)
                val nextFilePath = playlist[nextPosition]
                switchToNewAudio(nextFilePath)
            }
            LoopMode.SHUFFLE -> {
                // Play a random song from the playlist
                val randomPosition = (0 until playlist.size).random()
                viewInitializer.playlistAdapter.setSelectedPosition(randomPosition)
                val randomFilePath = playlist[randomPosition]
                switchToNewAudio(randomFilePath)
            }
        }
    }

    private fun switchToNewAudio(filePath: String) {
        // Stop the current audio if it's playing
        if (::audioTrackManager.isInitialized && audioTrackManager.isPlaying) {
            audioTrackManager.stopPlay()
        }

        // Initialize the new audio track manager with the new file's sample rate
        val wavFile = WavFile(File(filePath))
        initializeAudioTrackManager(wavFile.getSampleRate(), wavFile.getChannels())

        // Start playing the new audio from the beginning
        audioTrackManager.startPlay(filePath, wavFile)

        // Update the UI with the new audio's information
        updateAudioInfo(wavFile)

        // Update the play/pause button icon to pause
        val playPauseButton = findViewById<ImageView>(R.id.play_pause_button)
        playPauseButton.setImageResource(R.drawable.ic_pause)

        handler.removeCallbacks(updateSeekBarRunnable)
        handler.post(updateSeekBarRunnable)
    }

    private fun updateAudioInfo(wavFile: WavFile) {
        val titleTextView = findViewById<TextView>(R.id.audio_title)
        val artistTextView = findViewById<TextView>(R.id.audio_artist)
        val totalDuration = wavFile.getTotalDuration()

        titleTextView.text = wavFile.getAudioTitle()
        artistTextView.text = wavFile.getArtist()
        viewInitializer.seekBar.max = totalDuration
        viewInitializer.totalTime.text = formatTime(totalDuration)

        val audioCover = findViewById<ImageView>(R.id.audio_cover)
        val coverArt = wavFile.getCoverArt()
        if (coverArt != null) {
            Glide.with(this)
                .load(coverArt)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(audioCover)
        } else {
            audioCover.setImageResource(R.drawable.ic_launcher_foreground)
        }
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
                Logger.logi("Resuming playback")
            } else {
                audioTrackManager.startPlay(selectedFilePath)
            }
            playPauseButton.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun togglePlaylistVisibility() {
        if (currentVisibleView == VisibleView.PLAYLIST) {
            viewInitializer.playlistRecyclerView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    viewInitializer.playlistRecyclerView.visibility = View.GONE
                    viewInitializer.playlistControlArea.visibility = View.GONE
                    currentVisibleView = VisibleView.NONE
                }
        } else {
            if (effectRecyclerView.isVisible) {
                effectRecyclerView.visibility = View.GONE
                currentVisibleView = VisibleView.NONE
            }

            viewInitializer.playlistRecyclerView.layoutParams.height =
                resources.getDimensionPixelSize(R.dimen.playlist_height)
            viewInitializer.playlistRecyclerView.alpha = 0f
            viewInitializer.playlistRecyclerView.visibility = View.VISIBLE
            viewInitializer.playlistControlArea.visibility = View.VISIBLE
            viewInitializer.playlistRecyclerView.animate()
                .alpha(1f)
                .setDuration(300)
            currentVisibleView = VisibleView.PLAYLIST
        }
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
        Logger.loge("Permissions denied, exiting the app")
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
