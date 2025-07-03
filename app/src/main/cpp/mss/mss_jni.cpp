/* **************************************************************************
 * @Description:
 * @Version: 0.1.0
 * @Author: pandapan@aactechnologies.com
 * @Date: 2025-06-26 17:41:39
 * @Copyright (c) 2025 by @AAC Technologies, All Rights Reserved.
 **************************************************************************/

#include "mss_wrapper.h"
#include <jni.h>
#include <string>
#include <stdio.h>

extern "C" {
#include "log.h"

JNIEXPORT jint JNICALL Java_com_panda_audioplayer_algos_Mss_getMssWrapperVersion(JNIEnv *env, jobject obj, jbyteArray version)
{
    static char versionBuffer[256];
    int result = get_mss_wrapper_version(versionBuffer);
    if (result == E_OK) {
        env->SetByteArrayRegion(version, 0, strlen(versionBuffer), (jbyte *)versionBuffer);
    }
    return result;
}

JNIEXPORT jlong JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperInit(JNIEnv *env, jobject obj)
{
    LOGI("Mss::mssWrapperInit");
    void *handle = mss_wrapper_init("/sdcard/Download/causal_8frame_state_static_quant.onnx");

    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT void JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperDeinit(JNIEnv *env, jobject obj, jlong mssWrapperHandle)
{
    mss_wrapper_deinit(reinterpret_cast<void *>(mssWrapperHandle));
}

JNIEXPORT jint JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperSetParam(JNIEnv *env, jobject obj, jlong mssWrapperHandle, jint cmd, jbyteArray param, jint paramSize)
{
    void *paramPtr = env->GetByteArrayElements(param, nullptr);
    int result = mss_wrapper_set_param(reinterpret_cast<void *>(mssWrapperHandle), static_cast<mss_wrapper_param_t>(cmd), paramPtr, paramSize);
    env->ReleaseByteArrayElements(param, static_cast<jbyte *>(paramPtr), 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperGetParam(JNIEnv *env, jobject obj, jlong mssWrapperHandle, jint cmd, jbyteArray param, jint paramSize)
{
    void *paramPtr = env->GetByteArrayElements(param, nullptr);
    int result = mss_wrapper_get_param(reinterpret_cast<void *>(mssWrapperHandle), static_cast<mss_wrapper_param_t>(cmd), paramPtr, paramSize);
    env->ReleaseByteArrayElements(param, static_cast<jbyte *>(paramPtr), 0);
    return result;
}

JNIEXPORT jint JNICALL Java_com_panda_audioplayer_algos_Mss_mssWrapperProcess(
    JNIEnv *env, jobject obj, jlong mssWrapperHandle,
    jfloatArray input, jfloatArray output, jint blockSize)
{

    if (!mssWrapperHandle) {
        LOGE("Invalid MSS handle");
        return E_ALGO_HANDLE_NULL;
    }
    if (!input || !output) {
        LOGE("Input/output buffer is null");
        return E_PARAM_BUFFER_NULL;
    }
    if (blockSize <= 0) {
        LOGE("Invalid block size: %d", blockSize);
        return E_PARAM_SIZE_INVALID;
    }

    jfloat *inputPtr = env->GetFloatArrayElements(input, nullptr);
    jfloat *outputPtr = env->GetFloatArrayElements(output, nullptr);

    auto *inputLeft = new float[blockSize / 2];
    auto *inputRight = new float[blockSize / 2];
    auto *outputLeft = new float[blockSize / 2];
    auto *outputRight = new float[blockSize / 2];

    for (int i = 0, j = 0; i < blockSize; i += 2, j++) {
        inputLeft[j] = inputPtr[i];
        inputRight[j] = inputPtr[i + 1];
    }

    int result = mss_wrapper_process(
        reinterpret_cast<void *>(mssWrapperHandle),
        inputLeft, inputRight,
        outputLeft, outputRight,
        blockSize / 2);

    if (result == E_OK) {
        for (int i = 0, j = 0; i < blockSize; i += 2, j++) {
            outputPtr[i] = outputLeft[j];
            outputPtr[i + 1] = outputRight[j];
        }
    }

    env->ReleaseFloatArrayElements(input, inputPtr, JNI_ABORT);
    env->ReleaseFloatArrayElements(output, outputPtr, 0);
    delete[] inputLeft;
    delete[] inputRight;
    delete[] outputLeft;
    delete[] outputRight;

    return result;
}
}
