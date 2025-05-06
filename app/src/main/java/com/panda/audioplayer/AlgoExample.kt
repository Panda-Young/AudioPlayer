package com.panda.audioplayer

import com.panda.audioplayer.utils.Logger

class AlgoExample {
    init { System.loadLibrary("algo_jni") 
        Logger.logi("AlgoExample library loaded")
    }

    external fun getAlgoVersion(version: ByteArray): Int
    external fun algoInit(): Long
    external fun algoDeinit(algoHandle: Long)
    external fun algoSetParam(algoHandle: Long, cmd: Int, param: ByteArray, size: Int): Int
    external fun algoGetParam(algoHandle: Long, cmd: Int, param: ByteArray, size: Int): Int
    external fun algoProcess(algoHandle: Long, input: FloatArray, output: FloatArray, blockSize: Int): Int
}
