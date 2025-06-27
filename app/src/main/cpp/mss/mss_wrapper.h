#ifndef MSS_WRAPPER_H
#define MSS_WRAPPER_H

#ifdef __cplusplus
extern "C" {
#endif

#define E_OK 0
#define E_VERSION_BUFFER_NULL -1
#define E_ALGO_HANDLE_NULL -2
#define E_PARAM_BUFFER_NULL -3
#define E_PARAM_SIZE_INVALID -4
#define E_ALLOCATE_FAILED -5
#define E_PARAM_ID_OUT_OF_RANGE -6

typedef enum mss_wrapper_param {
  BYPASS_ENABLE = 0,
  VOCAL_ENABLE,
  DRUM_ENABLE,
  BASS_ENABLE,
  OTHER_ENABLE,
  VOCAL_VOLUME,
  BASS_VOLUME,
  DRUM_VOLUME,
  OTHER_VOLUME
} mss_wrapper_param_t;

int get_mss_wrapper_version(char *version);
void *mss_wrapper_init(const char *model_path);
void mss_wrapper_deinit(void *mss_wrapper_handle);
int mss_wrapper_set_param(void *mss_wrapper_handle, mss_wrapper_param_t cmd,
                          void *param, int param_size);
int mss_wrapper_get_param(void *mss_wrapper_handle, mss_wrapper_param_t cmd,
                          void *param, int param_size);
int mss_wrapper_process(void *mss_wrapper_handle, const float *input_left,
                        const float *input_right, float *output_left,
                        float *output_right, int number_samples);

#ifdef __cplusplus
}
#endif

#endif
