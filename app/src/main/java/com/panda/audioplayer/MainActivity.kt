package com.panda.audioplayer

import android.os.Build
import android.os.Bundle
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
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity(), PermissionManager.PermissionCallback {

    private lateinit var viewInitializer: ViewInitializer
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var audioManager: AudioManager
    private lateinit var audioTrackManager: AudioTrackManager
    private val playlist = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Logger.logd("Start MainActivity. SDK Version: ${Build.VERSION.SDK_INT}")

        // Initialize components
        viewInitializer = ViewInitializer(this)
        permissionHandler = PermissionHandler(this)
        audioManager = AudioManager(this)
        audioTrackManager = AudioTrackManager(44100) // Initialize AudioTrackManager with sample rate

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
    }

    private fun loadAudioFiles() {
        playlist.clear()
        playlist.addAll(audioManager.getAudioFileNames())
        viewInitializer.playlistAdapter.notifyDataSetChanged()
        if (playlist.isNotEmpty()) {
            viewInitializer.playlistAdapter.setSelectedPosition(0)
        }
    }

    private fun setupListeners() {
        viewInitializer.rescanButton.setOnClickListener {
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
    }

    private fun togglePlayPause() {
        val playPauseButton = findViewById<ImageView>(R.id.play_pause_button)
        if (audioTrackManager.isPlaying) {
            audioTrackManager.pausePlay()
            playPauseButton.setImageResource(R.drawable.ic_play) // 切换到播放图标
            Logger.logi("Audio paused")
        } else {
            val selectedFilePath = playlist[viewInitializer.playlistAdapter.getSelectedPosition()]
            audioTrackManager.startPlay(selectedFilePath)
            playPauseButton.setImageResource(R.drawable.ic_pause) // 切换到暂停图标
            Logger.logi("Audio started playing")
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

class AudioManager(private val activity: MainActivity) {

    private lateinit var audioFileManager: AudioFileManager

    fun initializeAudioManager() {
        audioFileManager = AudioFileManager(activity.contentResolver)
    }

    fun getAudioFileNames(): List<String> {
        val audioFiles = audioFileManager.scanAllLocalFiles()
        return audioFiles.map { it.absolutePath }
    }
}
