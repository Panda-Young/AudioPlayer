#include <jni.h>
#include "algo_example.h"

extern "C" {
JNIEXPORT jint JNICALL
Java_com_panda_audioplayer_AlgoExample_getAlgoVersion(JNIEnv *env, jobject thiz, jstring version) {
    const char *versionStr = env->GetStringUTFChars(version, 0);
    int result = get_algo_version((char *)versionStr);
    env->ReleaseStringUTFChars(version, versionStr);
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
Java_com_panda_audioplayer_AlgoExample_algoProcess(JNIEnv *env, jobject thiz, jlong algo_handle, jfloatArray input, jfloatArray output, jint block_size) {
    jfloat *inputArray = env->GetFloatArrayElements(input, 0);
    jfloat *outputArray = env->GetFloatArrayElements(output, 0);
    int result = algo_process((void *)algo_handle, inputArray, outputArray, block_size);
    env->ReleaseFloatArrayElements(input, inputArray, 0);
    env->ReleaseFloatArrayElements(output, outputArray, 0);
    return result;
}
}
