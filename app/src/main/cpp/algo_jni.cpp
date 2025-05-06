#include <jni.h>
#include "algo_example.h"

extern "C" {
JNIEXPORT jint JNICALL
Java_com_panda_audioplayer_AlgoExample_getAlgoVersion(JNIEnv *env, jobject thiz, jbyteArray version) {
    jbyte* versionBytes = env->GetByteArrayElements(version, 0);
    int result = get_algo_version((char *)versionBytes);
    env->ReleaseByteArrayElements(version, versionBytes, 0);
    return result;
}


JNIEXPORT jlong JNICALL
Java_com_panda_audioplayer_AlgoExample_algoInit(JNIEnv *env, jobject thiz) {
    return (jlong)algo_init();
}


JNIEXPORT void JNICALL
Java_com_panda_audioplayer_AlgoExample_algoDeinit(JNIEnv *env, jobject thiz, jlong algo_handle) {
    algo_deinit((void *)algo_handle);
}


JNIEXPORT jint JNICALL
Java_com_panda_audioplayer_AlgoExample_algoSetParam(JNIEnv *env, jobject thiz, jlong algo_handle, jint cmd, jbyteArray param, jint param_size) {
    jbyte *paramArray = env->GetByteArrayElements(param, 0);
    int result = algo_set_param((void *)algo_handle, (algo_param_t)cmd, paramArray, param_size);
    env->ReleaseByteArrayElements(param, paramArray, 0);
    return result;
}


JNIEXPORT jint JNICALL
Java_com_panda_audioplayer_AlgoExample_algoGetParam(JNIEnv *env, jobject thiz, jlong algo_handle, jint cmd, jbyteArray param, jint param_size) {
    jbyte *paramArray = env->GetByteArrayElements(param, 0);
    int result = algo_get_param((void *)algo_handle, (algo_param_t)cmd, paramArray, param_size);
    env->ReleaseByteArrayElements(param, paramArray, 0);
    return result;
}


JNIEXPORT jint JNICALL
Java_com_panda_audioplayer_AlgoExample_algoProcess(JNIEnv *env, jobject thiz, jlong algo_handle, jfloatArray input, jfloatArray output, jint sample_count) {
    jfloat *inputArray = env->GetFloatArrayElements(input, 0);
    jfloat *outputArray = env->GetFloatArrayElements(output, 0);
    int result = algo_process((void *)algo_handle, inputArray, outputArray, sample_count);
    env->ReleaseFloatArrayElements(input, inputArray, 0);
    env->ReleaseFloatArrayElements(output, outputArray, 0);
    return result;
}
}
