package com.panda.audioplayer

import com.panda.audioplayer.utils.Logger

class Gain {
    init { System.loadLibrary("gain_jni") 
        Logger.logi("Gain library loaded")
    }

    external fun getAlgoVersion(version: ByteArray): Int
    external fun algoInit(): Long
    external fun algoDeinit(algoHandle: Long)
    external fun algoSetParam(algoHandle: Long, cmd: Int, param: ByteArray, size: Int): Int
    external fun algoGetParam(algoHandle: Long, cmd: Int, param: ByteArray, size: Int): Int
    external fun algoProcess(algoHandle: Long, input: FloatArray, output: FloatArray, blockSize: Int): Int
}
