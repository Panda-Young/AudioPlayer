package com.panda.audioplayer

import android.content.Context

object PlaybackEngine {
    @Volatile
    private var manager: ExoPlayerManager? = null

    fun getManager(context: Context): ExoPlayerManager {
        return manager ?: synchronized(this) {
            manager ?: ExoPlayerManager(context.applicationContext).also { manager = it }
        }
    }
}
