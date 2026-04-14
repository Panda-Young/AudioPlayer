package com.panda.audioplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.panda.audioplayer.permission.PermissionManager
import com.panda.audioplayer.utils.Logger
import com.panda.audioplayer.utils.AudioMetadata
import com.panda.audioplayer.utils.MetadataReader
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import java.io.File
import java.util.Locale
import android.widget.Toast

class MainActivity : AppCompatActivity(), PermissionManager.PermissionCallback {
    companion object {
        enum class VisibleView { PLAYLIST, NONE }
    }

    private lateinit var viewInitializer: ViewInitializer
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var audioManager: AudioManager
    private lateinit var exoPlayerManager: ExoPlayerManager
    private var currentVisibleView: VisibleView = VisibleView.NONE
    private val handler = Handler(Looper.getMainLooper())
    lateinit var playlistManager: PlaylistManager
    private var ignorePlaybackCompletion = false

    private val pickAudioLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        persistUriPermissions(uris)
        audioManager.importSafUris(uris)
        loadAudioFiles()
    }

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

        audioManager.initializeAudioManager()
        permissionHandler.initializePermissions()
        loadAudioFiles()
        setupListeners()

        initExoPlayerManager()
        restoreLastPlaybackIfAvailable()

        val effectButton: ImageView = findViewById(R.id.effect_button)
        effectButton.setOnClickListener {
            startActivity(Intent(this, EffectLabActivity::class.java))
        }

        handler.post(updateSeekBarRunnable)
    }

    private fun persistUriPermissions(uris: List<Uri>) {
        for (uri in uris) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Logger.logw("Persist permission failed for $uri: ${e.message}")
            }
        }
    }

    fun loadAudioFiles() {
        val sources = audioManager.getAudioSources()
        playlistManager.refreshPlaylist(sources)
    }

    private fun initExoPlayerManager() {
        exoPlayerManager = PlaybackEngine.getManager(this)
        exoPlayerManager.onPlaybackComplete = {
            runOnUiThread {
                if (ignorePlaybackCompletion) {
                    Logger.logd("Completion arrived during manual switch; run smart check")
                    handler.postDelayed({
                        val progressed = exoPlayerManager.getCurrentPosition() > 300
                        if (progressed || exoPlayerManager.isPlaying) {
                            Logger.logd("Likely stale completion callback, keep current track")
                        } else {
                            Logger.logw("Track made no progress after switch; fallback to next track")
                            handlePlaybackCompletion()
                        }
                        ignorePlaybackCompletion = false
                    }, 250)
                    return@runOnUiThread
                }
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

        viewInitializer.importButton.setOnClickListener {
            pickAudioLauncher.launch(arrayOf("audio/*"))
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
        viewInitializer.playlistAdapter.setOnItemClickListener { source ->
            switchToNewAudio(source)
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
        if (playlistManager.getPlaylist().isEmpty()) return
        val currentPosition = viewInitializer.playlistAdapter.getSelectedPosition()
        val newPosition = playlistManager.getButtonPreviousPosition(currentPosition)
        viewInitializer.playlistAdapter.setSelectedPosition(newPosition)
        switchToNewAudio(playlistManager.getPlaylist()[newPosition])
    }

    private fun playNext() {
        if (playlistManager.getPlaylist().isEmpty()) return
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
        if (playlistManager.getPlaylist().isEmpty()) return
        val currentPosition = viewInitializer.playlistAdapter.getSelectedPosition()
        val newPosition = playlistManager.getAutoNextPosition(currentPosition)
        viewInitializer.playlistAdapter.setSelectedPosition(newPosition)
        switchToNewAudio(playlistManager.getPlaylist()[newPosition])
    }

    private fun switchToNewAudio(source: String, autoPlay: Boolean = true, seekTo: Long = 0L) {
        ignorePlaybackCompletion = true
        val metadata = MetadataReader.read(this, source)
        exoPlayerManager.startPlay(source, autoPlay)
        if (seekTo > 0) {
            exoPlayerManager.seekTo(seekTo)
        }
        updateAudioInfo(metadata)
        val playPauseButton = findViewById<ImageView>(R.id.play_pause_button)
        playPauseButton.setImageResource(if (autoPlay) R.drawable.ic_pause else R.drawable.ic_play)
        handler.postDelayed({
            if (ignorePlaybackCompletion) {
                ignorePlaybackCompletion = false
            }
        }, 600)
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

    private fun restoreLastPlaybackIfAvailable() {
        val snapshot = exoPlayerManager.restorePlaybackState() ?: run {
            if (playlistManager.getPlaylist().isNotEmpty()) {
                val first = playlistManager.getPlaylist().first()
                updateAudioInfo(MetadataReader.read(this, first))
                viewInitializer.playlistAdapter.setSelectedPosition(0)
            }
            return
        }

        var playlist = playlistManager.getPlaylist().toMutableList()
        if (!playlist.contains(snapshot.source)) {
            playlist.add(0, snapshot.source)
            playlistManager.refreshPlaylist(playlist)
        }

        val index = playlistManager.getPlaylist().indexOf(snapshot.source).coerceAtLeast(0)
        viewInitializer.playlistAdapter.setSelectedPosition(index)
        // Product requirement: app launch restores track + position, but starts in paused state.
        switchToNewAudio(snapshot.source, false, snapshot.position)
    }

    private fun togglePlayPause() {
        if (!::exoPlayerManager.isInitialized) return
        if (playlistManager.getPlaylist().isEmpty()) return

        val playPauseButton = findViewById<ImageView>(R.id.play_pause_button)
        val selectedSource = playlistManager.getPlaylist()[viewInitializer.playlistAdapter.getSelectedPosition()]

        if (exoPlayerManager.isPlaying) {
            exoPlayerManager.pausePlay()
            playPauseButton.setImageResource(R.drawable.ic_play)
            Logger.logi("Audio paused")
        } else {
            if (exoPlayerManager.isSameAudioFile(selectedSource) && !exoPlayerManager.isCompleted) {
                exoPlayerManager.resumePlay()
                Logger.logi("Resuming playback")
            } else {
                exoPlayerManager.startPlay(selectedSource)
                updateAudioInfo(MetadataReader.read(this, selectedSource))
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
        loadAudioFiles()
    }

    override fun onPermissionsDenied() {
        Logger.logw("Permissions denied, continue with SAF import mode")
        Toast.makeText(this, "Storage permission denied. You can still import audio via SAF.", Toast.LENGTH_LONG).show()
        loadAudioFiles()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateSeekBarRunnable)
    }

    override fun onStop() {
        super.onStop()
        if (::exoPlayerManager.isInitialized) {
            exoPlayerManager.persistPlaybackState(exoPlayerManager.isPlaying)
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
    lateinit var importButton: TextView
    lateinit var closeButton: TextView
    lateinit var playlistControlArea: ConstraintLayout

    fun initializeViews() {
        activity.setContentView(R.layout.activity_main)
        playlistRecyclerView = activity.findViewById(R.id.playlist_recycler_view)
        seekBar = activity.findViewById(R.id.seek_bar)
        currentTime = activity.findViewById(R.id.current_time)
        totalTime = activity.findViewById(R.id.total_time)
        rescanButton = activity.findViewById(R.id.rescanButton)
        importButton = activity.findViewById(R.id.importButton)
        closeButton = activity.findViewById(R.id.closeButton)
        playlistControlArea = activity.findViewById(R.id.playlist_control_area)
    }

    fun initializeRecyclerView(initialList: MutableList<String>) {
        playlistRecyclerView.layoutManager = LinearLayoutManager(activity)
        playlistAdapter = PlaylistAdapter(initialList)
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

    private val safPrefs = context.getSharedPreferences("saf_audio_sources", Context.MODE_PRIVATE)
    private val keySafSources = "saf_sources"
    private lateinit var audioFileManager: AudioFileManager

    fun initializeAudioManager() {
        audioFileManager = AudioFileManager(context.contentResolver)
    }

    fun getAudioSources(): List<String> {
        ensureAudioManagerInitialized()
        val audioFiles = audioFileManager.scanAllLocalFiles()
        val local = audioFiles.map { it.absolutePath }
        val saf = safPrefs.getStringSet(keySafSources, emptySet())?.toList() ?: emptyList()

        val all = (local + saf)
            .distinct()
            .filter { isSourceReadable(it) }
            .sortedBy { displayLabel(it).lowercase(Locale.getDefault()) }

        Logger.logd("Audio sources summary: local=${local.size}, saf=${saf.size}, visible=${all.size}")

        cleanupInvalidSaf(all)
        return all
    }

    fun refreshAudioFiles() {
        ensureAudioManagerInitialized()
        audioFileManager.refreshAudioFiles()
        Logger.logi("Audio files refreshed")
    }

    fun importSafUris(uris: List<Uri>) {
        val old = safPrefs.getStringSet(keySafSources, emptySet())?.toMutableSet() ?: mutableSetOf()
        uris.forEach { old.add(it.toString()) }
        safPrefs.edit().putStringSet(keySafSources, old).apply()
        Logger.logi("Imported ${uris.size} audio URIs via SAF")
    }

    private fun isSourceReadable(source: String): Boolean {
        return try {
            if (source.startsWith("content://")) {
                context.contentResolver.openAssetFileDescriptor(Uri.parse(source), "r")?.use { true } ?: false
            } else {
                File(source).exists()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun displayLabel(source: String): String {
        return if (source.startsWith("content://")) {
            Uri.parse(source).lastPathSegment ?: source
        } else {
            File(source).nameWithoutExtension
        }
    }

    private fun cleanupInvalidSaf(validSources: List<String>) {
        val saf = safPrefs.getStringSet(keySafSources, emptySet())?.toMutableSet() ?: mutableSetOf()
        val validSet = validSources.toSet()
        val originalSize = saf.size
        saf.removeIf { it !in validSet }
        if (saf.size != originalSize) {
            safPrefs.edit().putStringSet(keySafSources, saf).apply()
        }
    }

    private fun ensureAudioManagerInitialized() {
        if (!::audioFileManager.isInitialized) {
            initializeAudioManager()
        }
    }
}
