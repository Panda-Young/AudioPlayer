package com.panda.audioplayer.algos

import com.panda.audioplayer.utils.Logger

class Mss {
    init {
        System.loadLibrary("mss_jni") 
        Logger.logi("Mss library loaded")
    }

    external fun getMssWrapperVersion(version: ByteArray): Int
    external fun mssWrapperInit(): Long
    external fun mssWrapperDeinit(mssHandle: Long)
    external fun mssWrapperSetParam(mssHandle: Long, cmd: Int, param: ByteArray, size: Int): Int
    external fun mssWrapperGetParam(mssHandle: Long, cmd: Int, param: ByteArray, size: Int): Int
    external fun mssWrapperProcess(mssHandle: Long, input: FloatArray, output: FloatArray, blockSize: Int): Int
}
