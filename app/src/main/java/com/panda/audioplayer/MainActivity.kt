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
import com.panda.audioplayer.utils.AudioMetadata
import com.panda.audioplayer.utils.MetadataReader
import androidx.core.view.isVisible
import android.content.Context
import java.io.File
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity(), PermissionManager.PermissionCallback {
    companion object {
        enum class VisibleView { EFFECTS, PLAYLIST, NONE }
    }

    private lateinit var viewInitializer: ViewInitializer
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var audioManager: AudioManager
    private lateinit var exoPlayerManager: ExoPlayerManager
    private lateinit var effectRecyclerView: RecyclerView
    private lateinit var effectAdapter: EffectAdapter
    private lateinit var effectManager: EffectManager
    private var currentVisibleView: VisibleView = VisibleView.NONE
    private val handler = Handler(Looper.getMainLooper())
    lateinit var playlistManager: PlaylistManager
    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            if (::exoPlayerManager.isInitialized) {
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
        viewInitializer.initializeViews()
        viewInitializer.initializeRecyclerView(mutableListOf())

        playlistManager = PlaylistManager(this, viewInitializer.playlistAdapter)

        permissionHandler = PermissionHandler(this)
        audioManager = AudioManager(this)

        // Initialize permissions
        permissionHandler.initializePermissions()

        // Initialize audio manager
        audioManager.initializeAudioManager()

        // Load audio files
        loadAudioFiles()

        // Set up listeners
        setupListeners()

        initExoPlayerManager()
        if (playlistManager.getPlaylist().isNotEmpty()) {
            val initialFilePath = playlistManager.getPlaylist()[0]
            val metadata = MetadataReader.read(File(initialFilePath))
            updateAudioInfo(metadata)
            handler.post(updateSeekBarRunnable)
        }

        effectManager = EffectManager(exoPlayerManager)

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
            "Gain" -> exoPlayerManager.setGainEffect(enabled)
            "Equalizer" -> exoPlayerManager.setEqualizerEffect(enabled)
            "3D Audio" -> exoPlayerManager.set3DEffect(enabled)
        }
        Logger.logi("Effect $effectName ${if (enabled) "enabled" else "disabled"}")
    }

    fun loadAudioFiles() {
        val files = audioManager.getAudioFileNames()
        playlistManager.refreshPlaylist(files)
    }

    private fun initExoPlayerManager() {
        exoPlayerManager = ExoPlayerManager(this)
        exoPlayerManager.onPlaybackComplete = {
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
                if (fromUser && ::exoPlayerManager.isInitialized) {
                    exoPlayerManager.seekTo(progress.toLong())
                    viewInitializer.currentTime.text = formatTime(progress)
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

    private fun playPrevious() {
        val currentPosition = viewInitializer.playlistAdapter.getSelectedPosition()
        val newPosition = playlistManager.getButtonPreviousPosition(currentPosition)
        viewInitializer.playlistAdapter.setSelectedPosition(newPosition)
        switchToNewAudio(playlistManager.getPlaylist()[newPosition])
    }

    private fun playNext() {
        val currentPosition = viewInitializer.playlistAdapter.getSelectedPosition()
        val newPosition = playlistManager.getButtonNextPosition(currentPosition)
        viewInitializer.playlistAdapter.setSelectedPosition(newPosition)
        switchToNewAudio(playlistManager.getPlaylist()[newPosition])
    }

    private fun toggleLoopMode() {
        playlistManager.toggleLoopMode()
        updateLoopButtonIcon()
        Logger.logi("Loop mode: ${playlistManager.getCurrentLoopMode()}")
    }

    private fun updateLoopButtonIcon() {
        val loopButton = findViewById<ImageView>(R.id.loop_button)
        when (playlistManager.getCurrentLoopMode()) {
            PlaylistManager.LoopMode.REPEAT_ONE -> loopButton.setImageResource(R.drawable.ic_single_loop)
            PlaylistManager.LoopMode.REPEAT_ALL -> loopButton.setImageResource(R.drawable.ic_list_loop)
            PlaylistManager.LoopMode.SHUFFLE -> loopButton.setImageResource(R.drawable.ic_shuffle)
        }
    }

    // Handle playback completion based on the current loop mode
    private fun handlePlaybackCompletion() {
        val currentPosition = viewInitializer.playlistAdapter.getSelectedPosition()
        val newPosition = playlistManager.getAutoNextPosition(currentPosition)
        viewInitializer.playlistAdapter.setSelectedPosition(newPosition)
        switchToNewAudio(playlistManager.getPlaylist()[newPosition])
    }

    private fun switchToNewAudio(filePath: String) {
        if (::exoPlayerManager.isInitialized) {
            exoPlayerManager.stopPlay()
        }
        val metadata = MetadataReader.read(File(filePath))
        exoPlayerManager.startPlay(filePath)
        updateAudioInfo(metadata)
        val playPauseButton = findViewById<ImageView>(R.id.play_pause_button)
        playPauseButton.setImageResource(R.drawable.ic_pause)
        handler.removeCallbacks(updateSeekBarRunnable)
        handler.post(updateSeekBarRunnable)
    }

    private fun updateAudioInfo(metadata: AudioMetadata) {
        val titleTextView = findViewById<TextView>(R.id.audio_title)
        val artistTextView = findViewById<TextView>(R.id.audio_artist)
        titleTextView.text = metadata.title
        artistTextView.text = metadata.artist
        if (metadata.duration > 0) {
            viewInitializer.seekBar.max = metadata.duration
            viewInitializer.totalTime.text = formatTime(metadata.duration)
        }
        val audioCover = findViewById<ImageView>(R.id.audio_cover)
        if (metadata.coverArt != null) {
            Glide.with(this)
                .load(metadata.coverArt)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(audioCover)
        } else {
            audioCover.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    private fun togglePlayPause() {
        if (!::exoPlayerManager.isInitialized) return

        val playPauseButton = findViewById<ImageView>(R.id.play_pause_button)
        val selectedFilePath = playlistManager.getPlaylist()[viewInitializer.playlistAdapter.getSelectedPosition()]

        if (exoPlayerManager.isPlaying) {
            exoPlayerManager.pausePlay()
            playPauseButton.setImageResource(R.drawable.ic_play)
            Logger.logi("Audio paused")
        } else {
            if (exoPlayerManager.isSameAudioFile(selectedFilePath) && !exoPlayerManager.isCompleted) {
                exoPlayerManager.resumePlay()
                Logger.logi("Resuming playback")
            } else {
                exoPlayerManager.startPlay(selectedFilePath)
                updateAudioInfo(MetadataReader.read(File(selectedFilePath)))
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
        if (::exoPlayerManager.isInitialized && (exoPlayerManager.isPlaying || exoPlayerManager.isPaused())) {
            val currentPosition = exoPlayerManager.getCurrentPosition()
            viewInitializer.seekBar.progress = currentPosition
            viewInitializer.currentTime.text = formatTime(currentPosition)
            // Sync seekBar max from ExoPlayer once it becomes accurate
            val duration = exoPlayerManager.getDuration()
            if (duration > 0 && viewInitializer.seekBar.max != duration) {
                viewInitializer.seekBar.max = duration
                viewInitializer.totalTime.text = formatTime(duration)
            }
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
        if (::exoPlayerManager.isInitialized) {
            exoPlayerManager.release()
        }
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

    fun initializeRecyclerView(initialList: MutableList<String>) {
        playlistRecyclerView.layoutManager = LinearLayoutManager(activity)
        playlistAdapter = PlaylistAdapter(initialList) { fileName ->
            (activity as? MainActivity)?.playlistManager?.apply {
                val newList = getPlaylist().toMutableList().apply { remove(fileName) }
                refreshPlaylist(newList)
                playlistAdapter.removeItem(fileName)
            }
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
