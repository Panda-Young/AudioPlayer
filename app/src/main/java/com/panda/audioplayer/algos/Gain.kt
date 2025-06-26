package com.panda.audioplayer.algos

import com.panda.audioplayer.utils.Logger

class Gain {
    init {
        System.loadLibrary("gain_jni") 
        Logger.logi("Gain library loaded")
    }

    external fun getGainVersion(version: ByteArray): Int
    external fun gainInit(): Long
    external fun gainDeinit(gainHandle: Long)
    external fun gainSetParam(gainHandle: Long, cmd: Int, param: ByteArray, size: Int): Int
    external fun gainGetParam(gainHandle: Long, cmd: Int, param: ByteArray, size: Int): Int
    external fun gainProcess(gainHandle: Long, input: FloatArray, output: FloatArray, blockSize: Int): Int
}
