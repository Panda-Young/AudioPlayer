package com.panda.audioplayer

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.panda.audioplayer.permission.PermissionManager
import com.panda.audioplayer.utils.Logger
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity(), PermissionManager.PermissionCallback {
    private lateinit var permissionManager: PermissionManager
    private lateinit var playlistRecyclerView: RecyclerView
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var audioFileManager: AudioFileManager
    private val playlist = mutableListOf<String>()
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        Logger.logd("MainActivity")

        // Initialize PermissionManager
        permissionManager = PermissionManager(this)
        permissionManager.setPermissionCallback(this)
        permissionManager.checkAndRequestPermissions()

        // Initialize AudioFileManager
        audioFileManager = AudioFileManager(contentResolver)

        // Initialize RecyclerView and Adapter
        playlistRecyclerView = findViewById(R.id.playlist_recycler_view)
        playlistRecyclerView.layoutManager = LinearLayoutManager(this)
        playlist.addAll(getAudioFileNames())
        playlistAdapter = PlaylistAdapter(playlist) { fileName ->
            playlistAdapter.removeItem(fileName)
        }
        playlistRecyclerView.adapter = playlistAdapter

        // Initialize SeekBar, current_time, and total_time
        seekBar = findViewById(R.id.seek_bar)
        currentTime = findViewById(R.id.current_time)
        totalTime = findViewById(R.id.total_time)
        seekBar.max = 100 // Set max value to 100 (percentage)




        // Set click listener for playlist_button
        findViewById<ImageView>(R.id.playlist_button).setOnClickListener {
            togglePlaylistVisibility()
        }
    }
    private fun getAudioFileNames(): List<String> {
        val audioFiles = audioFileManager.scanAllLocalFiles()
        return audioFiles.map { it.absolutePath } // Return full file paths
    }

    private fun togglePlaylistVisibility() {
        if (playlistRecyclerView.visibility == View.VISIBLE) {
            playlistRecyclerView.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    playlistRecyclerView.visibility = View.GONE
                }
        } else {
            playlistRecyclerView.alpha = 0f
            playlistRecyclerView.visibility = View.VISIBLE
            playlistRecyclerView.layoutParams.height = resources.getDimensionPixelSize(R.dimen.playlist_height)
            playlistRecyclerView.animate()
                .alpha(1f)
                .setDuration(300)
        }
        playlistRecyclerView.requestLayout()
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        if (playlistRecyclerView.isVisible) {
            togglePlaylistVisibility()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onAllPermissionsGranted() {
        // Proceed with app functionality
        Logger.logi("All permissions granted, proceeding with app functionality")
    }

    override fun onPermissionsDenied() {
        // Exit the app if permissions are denied
        Logger.logw("Permissions denied, exiting the app")
        finish()
    }

}
