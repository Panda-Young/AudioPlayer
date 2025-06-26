/* **************************************************************************
 * @Description: gain moodule jni interface
 * @Version: 0.1.0
 * @Author: pandapan@aactechnologies.com
 * @Date: 2025-06-26 16:09:38
 * @Copyright (c) 2025 by @AAC Technologies, All Rights Reserved.
 **************************************************************************/

#include "gain.h"
#include <jni.h>

extern "C" {
JNIEXPORT jint JNICALL
Java_com_panda_audioplayer_algos_Gain_getGainVersion(JNIEnv *env, jobject thiz, jbyteArray version)
{
    jbyte *versionBytes = env->GetByteArrayElements(version, 0);
    int result = get_algo_version((char *)versionBytes);
    env->ReleaseByteArrayElements(version, versionBytes, 0);
    return result;
}

JNIEXPORT jlong JNICALL
Java_com_panda_audioplayer_algos_Gain_gainInit(JNIEnv *env, jobject thiz)
{
    return (jlong)algo_init();
}

JNIEXPORT void JNICALL
Java_com_panda_audioplayer_algos_Gain_gainDeinit(JNIEnv *env, jobject thiz, jlong gain_handle)
{
    algo_deinit((void *)gain_handle);
}

JNIEXPORT jint JNICALL
Java_com_panda_audioplayer_algos_Gain_gainSetParam(JNIEnv *env, jobject thiz, jlong gain_handle, jint cmd, jbyteArray param, jint param_size)
{
    jbyte *paramArray = env->GetByteArrayElements(param, 0);
    int result = algo_set_param((void *)gain_handle, (algo_param_t)cmd, paramArray, param_size);
    env->ReleaseByteArrayElements(param, paramArray, 0);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_panda_audioplayer_algos_Gain_gainGetParam(JNIEnv *env, jobject thiz, jlong gain_handle, jint cmd, jbyteArray param, jint param_size)
{
    jbyte *paramArray = env->GetByteArrayElements(param, 0);
    int result = algo_get_param((void *)gain_handle, (algo_param_t)cmd, paramArray, param_size);
    env->ReleaseByteArrayElements(param, paramArray, 0);
    return result;
}

JNIEXPORT jint JNICALL
Java_com_panda_audioplayer_algos_Gain_gainProcess(JNIEnv *env, jobject thiz, jlong gain_handle, jfloatArray input, jfloatArray output, jint sample_count)
{
    jfloat *inputArray = env->GetFloatArrayElements(input, 0);
    jfloat *outputArray = env->GetFloatArrayElements(output, 0);
    int result = algo_process((void *)gain_handle, inputArray, outputArray, sample_count);
    env->ReleaseFloatArrayElements(input, inputArray, 0);
    env->ReleaseFloatArrayElements(output, outputArray, 0);
    return result;
}
}
