/* **************************************************************************
 * @Description: 
 * @Version: 0.1.0
 * @Author: pandapan@aactechnologies.com
 * @Date: 2025-06-26 17:41:39
 * @Copyright (c) 2025 by @AAC Technologies, All Rights Reserved.
 **************************************************************************/

#include <jni.h>
#include "mss_wrapper.h"
#include <string>

extern "C" {
#include "log.h"

JNIEXPORT jint JNICALL Java_com_panda_audioplayer_algos_Mss_getMssWrapperVersion(JNIEnv *env, jobject obj, jbyteArray version) {
    static char versionBuffer[256];
    int result = get_mss_wrapper_version(versionBuffer);
    if (result == E_OK) {
        env->SetByteArrayRegion(version, 0, strlen(versionBuffer), (jbyte *)versionBuffer);
    }
    return result;
}

JNIEXPORT jlong JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperInit(JNIEnv *env, jobject obj) {
    LOGI("Mss::mssWrapperInit");
    void *handle = mss_wrapper_init("/sdcard/Download/epoch213_causal_8frame_state.onnx");
    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT void JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperDeinit(JNIEnv *env, jobject obj, jlong mssWrapperHandle) {
    mss_wrapper_deinit(reinterpret_cast<void *>(mssWrapperHandle));
}

JNIEXPORT jint JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperSetParam(JNIEnv *env, jobject obj, jlong mssWrapperHandle, jint cmd, jbyteArray param, jint paramSize) {
    void *paramPtr = env->GetByteArrayElements(param, nullptr);
    int result = mss_wrapper_set_param(reinterpret_cast<void *>(mssWrapperHandle), static_cast<mss_wrapper_param_t>(cmd), paramPtr, paramSize);
    env->ReleaseByteArrayElements(param, static_cast<jbyte *>(paramPtr), 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperGetParam(JNIEnv *env, jobject obj, jlong mssWrapperHandle, jint cmd, jbyteArray param, jint paramSize) {
    void *paramPtr = env->GetByteArrayElements(param, nullptr);
    int result = mss_wrapper_get_param(reinterpret_cast<void *>(mssWrapperHandle), static_cast<mss_wrapper_param_t>(cmd), paramPtr, paramSize);
    env->ReleaseByteArrayElements(param, static_cast<jbyte *>(paramPtr), 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperProcess(
    JNIEnv *env, jobject obj, jlong mssWrapperHandle,
    jfloatArray inputLeft, jfloatArray inputRight,
    jfloatArray outputLeft, jfloatArray outputRight, jint numberSamples) {

    // 获取输入数组的指针
    float *inputLeftPtr = env->GetFloatArrayElements(inputLeft, nullptr);
    float *inputRightPtr = env->GetFloatArrayElements(inputRight, nullptr);

    // 获取输出数组的指针
    float *outputLeftPtr = env->GetFloatArrayElements(outputLeft, nullptr);
    float *outputRightPtr = env->GetFloatArrayElements(outputRight, nullptr);

    // 调用 C 函数处理音频数据
    int result = mss_wrapper_process(
        reinterpret_cast<void *>(mssWrapperHandle),
        inputLeftPtr, inputRightPtr,
        outputLeftPtr, outputRightPtr,
        static_cast<int>(numberSamples)
    );

    // 释放数组资源
    env->ReleaseFloatArrayElements(inputLeft, inputLeftPtr, 0);
    env->ReleaseFloatArrayElements(inputRight, inputRightPtr, 0);
    env->ReleaseFloatArrayElements(outputLeft, outputLeftPtr, 0);
    env->ReleaseFloatArrayElements(outputRight, outputRightPtr, 0);

    return result;
}

} // extern "C"
