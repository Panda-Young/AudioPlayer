package com.panda.audioplayer.plugins

interface AudioAlgorithmPlugin {
    val id: String
    val displayName: String
    val description: String
    val version: String
    fun setEnabled(enabled: Boolean)
    fun isEnabled(): Boolean
}
