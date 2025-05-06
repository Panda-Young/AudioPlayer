/***************************************************************************
 * Description: algorithm exxample
 * version: 0.1.0
 * Author: Panda-Young
 * Date: 2024-05-11 11:08:17
 * Copyright (c) 2024 by Panda-Young, All Rights Reserved.
 **************************************************************************/

#ifndef _ALGO_EXAMPLE_H
#define _ALGO_EXAMPLE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define E_OK 0
#define E_VERSION_BUFFER_NULL -1
#define E_ALGO_HANDLE_NULL -2
#define E_PARAM_BUFFER_NULL -3
#define E_PARAM_SIZE_INVALID -4
#define E_ALLOCATE_FAILED -5
#define E_PARAM_OUT_OF_RANGE -6

typedef enum algo_param {
    ALGO_PARAM1 = 1,
    ALGO_PARAM2,
    ALGO_PARAM3,
    ALGO_PARAM4,
} algo_param_t;

int get_algo_version(char *version);
void *algo_init();
void algo_deinit(void *algo_handle);
int algo_set_param(void *algo_handle, algo_param_t cmd, void *param, int param_size);
int algo_get_param(void *algo_handle, algo_param_t cmd, void *param, int param_size);
int algo_process(void *algo_handle, const float *input, float *output, int sample_count);

#ifdef __cplusplus
}
#endif

#endif
