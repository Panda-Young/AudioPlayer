/***************************************************************************
 * Description: algorithm exxample
 * version: 0.1.0
 * Author: Panda-Young
 * Date: 2024-05-11 11:08:17
 * Copyright (c) 2024 by Panda-Young, All Rights Reserved.
 **************************************************************************/

#include "gain.h"
#include "log.h"
#include <math.h>
#include <stdlib.h>

#define VERSION "0.1.2"
#define MAX_BUF_SIZE 1024

typedef struct algo_handle {
    char param1;
    float param2;
    char param3[MAX_BUF_SIZE];
    float *param4;
} algo_handle_t, *p_algo_handle_t;

static int validate_param_size(int received_size, int expected_size, const char *param_name)
{
    if (received_size != expected_size) {
        LOGE("Received param size %d Bytes for %s is not correct. Expected size is %d",
             received_size, param_name, expected_size);
        return E_PARAM_SIZE_INVALID;
    }
    return E_OK;
}

int get_algo_version(char *version)
{
    if (version == NULL) {
        LOGE("version is NULL");
        return E_VERSION_BUFFER_NULL;
    }
    strcpy(version, VERSION);
    return E_OK;
}

void *algo_init()
{
    p_algo_handle_t algo_handle = (p_algo_handle_t)malloc(sizeof(algo_handle_t));
    if (algo_handle == NULL) {
        LOGE("allocate for algo_handle_t failed");
        return NULL;
    }
    memset(algo_handle, 0, sizeof(algo_handle_t));
    LOGI("algo_init OK");
    return algo_handle;
}

void algo_deinit(void *algo_handle)
{
    if (!algo_handle) {
        LOGE("algo_handle is NULL");
        return;
    }
    p_algo_handle_t algo_handle_ptr = (p_algo_handle_t)algo_handle;
    if (algo_handle_ptr->param4 != NULL) {
        free(algo_handle_ptr->param4);
        algo_handle_ptr->param4 = NULL;
    }
    free(algo_handle);
    LOGI("algo_deinit OK");
}

int algo_set_param(void *algo_handle, algo_param_t cmd, void *param, int param_size)
{
    if (algo_handle == NULL) {
        LOGE("algo_handle is NULL");
        return E_ALGO_HANDLE_NULL;
    }
    if (param == NULL) {
        LOGE("param is NULL");
        return E_PARAM_BUFFER_NULL;
    }

    p_algo_handle_t algo_handle_ptr = (p_algo_handle_t)algo_handle;
    int ret = E_OK;
    switch (cmd) {
    case ALGO_PARAM1:
        ret = validate_param_size(param_size, sizeof(char), "param1");
        if (ret == E_OK) {
            algo_handle_ptr->param1 = *(char *)param;
            LOGI("set param1: %c", algo_handle_ptr->param1);
        }
        break;
    case ALGO_PARAM2:
        ret = validate_param_size(param_size, sizeof(float), "param2");
        if (ret == E_OK) {
            algo_handle_ptr->param2 = *(float *)param;
            LOGI("set param2: %.3f", algo_handle_ptr->param2);
        }
        break;
    case ALGO_PARAM3:
        if (param_size > MAX_BUF_SIZE) {
            LOGE("Received param size: %d Bytes is too large. Max size is %u",
                 param_size, MAX_BUF_SIZE);
            return E_PARAM_SIZE_INVALID;
        }
        memset(algo_handle_ptr->param3, 0, MAX_BUF_SIZE);
        memcpy(algo_handle_ptr->param3, param, param_size);
        LOGI("set param3: %s", algo_handle_ptr->param3);
        break;
    case ALGO_PARAM4:
        if (param_size > MAX_BUF_SIZE) {
            LOGE("Received param size: %d Bytes is too large. Max size is %u",
                 param_size, MAX_BUF_SIZE);
            return E_PARAM_SIZE_INVALID;
        }
        algo_handle_ptr->param4 = (float *)malloc(param_size);
        if (algo_handle_ptr->param4 == NULL) {
            LOGE("allocate %d Bytes for param4 failed", param_size);
            return E_ALLOCATE_FAILED;
        }
        memcpy(algo_handle_ptr->param4, param, param_size);
        break;
    default:
        LOGE("cmd %d is invalid", cmd);
        return E_PARAM_OUT_OF_RANGE;
    }
    return ret;
}

int algo_get_param(void *algo_handle, algo_param_t cmd, void *param, int param_size)
{
    if (algo_handle == NULL) {
        LOGE("algo_handle is NULL");
        return E_ALGO_HANDLE_NULL;
    }
    if (param == NULL) {
        LOGE("param is NULL");
        return E_PARAM_BUFFER_NULL;
    }

    p_algo_handle_t algo_handle_ptr = (p_algo_handle_t)algo_handle;
    int ret = E_OK;
    switch (cmd) {
    case ALGO_PARAM1:
        ret = validate_param_size(param_size, sizeof(char), "param1");
        if (ret == E_OK) {
            *(char *)param = algo_handle_ptr->param1;
            LOGI("get param1: %c", algo_handle_ptr->param1);
        }
        break;
    case ALGO_PARAM2:
        ret = validate_param_size(param_size, sizeof(float), "param2");
        if (ret == E_OK) {
            *(float *)param = algo_handle_ptr->param2;
            LOGI("get param2: %.1f", algo_handle_ptr->param2);
        }
        break;
    case ALGO_PARAM3:
        if (param_size > MAX_BUF_SIZE) {
            LOGE("param_size is too large");
            return E_PARAM_SIZE_INVALID;
        }
        memcpy(param, algo_handle_ptr->param3, param_size);
        LOGI("get param3: %s", algo_handle_ptr->param3);
        break;
    case ALGO_PARAM4:
        if (param_size > MAX_BUF_SIZE) {
            LOGE("param_size: %d is too large than MAX_BUF_SIZE %d", param_size, MAX_BUF_SIZE);
            return E_PARAM_SIZE_INVALID;
        }
        memcpy(param, algo_handle_ptr->param4, param_size);
        break;
    default:
        LOGE("cmd is invalid");
        return E_PARAM_OUT_OF_RANGE;
    }
    return ret;
}

float dBtoGain(float dB)
{
    return powf(10.0f, dB / 20.0f);
}

int algo_process(void *algo_handle, const float *input, float *output, int sample_count)
{
    if (algo_handle == NULL) {
        return E_ALGO_HANDLE_NULL;
    }
    if (input == NULL) {
        LOGE("input is NULL");
        return E_PARAM_BUFFER_NULL;
    }
    if (output == NULL) {
        LOGE("output is NULL");
        return E_PARAM_BUFFER_NULL;
    }
    if (sample_count <= 0) {
        LOGE("sample_count is not correct");
        return E_PARAM_SIZE_INVALID;
    }
    p_algo_handle_t algo_handle_ptr = (p_algo_handle_t)algo_handle;

    for (int i = 0; i < sample_count; i++) {
        output[i] = input[i] * dBtoGain(algo_handle_ptr->param2);
    }

    return E_OK;
}
