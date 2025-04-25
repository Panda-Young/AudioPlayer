package com.panda.audioplayer.utils

import android.util.Log

object Logger {
    private const val TAG = "AudioPlayer"

    private fun getLogPrefix(): String {
        val stackTrace = Thread.currentThread().stackTrace
        val element = stackTrace[4]
        val fileName = element.fileName ?: "UnknownFile"
        val lineNumber = element.lineNumber
        val methodName = element.methodName
        return "$fileName:$lineNumber @$methodName".padEnd(64, ' ')
    }

    fun logd(message: String) { Log.d(TAG, "${getLogPrefix()} $message") }
    fun logi(message: String) { Log.i(TAG, "${getLogPrefix()} $message") }
    fun logw(message: String) { Log.w(TAG, "${getLogPrefix()} $message") }
    fun loge(message: String) { Log.e(TAG, "${getLogPrefix()} $message") }
}
