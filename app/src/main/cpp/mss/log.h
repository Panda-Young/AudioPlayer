/***************************************************************************
 * Description: log
 * version: 0.1.0
 * Author: Panda-Young
 * Date: 2024-05-01 16:59:25
 * Copyright (c) 2024 by Panda-Young, All Rights Reserved.
 **************************************************************************/

 #ifndef _LOG_H
 #define _LOG_H
 
 #include <errno.h>
 #include <stdio.h>
 #include <string.h>
 #include <sys/stat.h>
 #include <time.h>
 
 #if defined(_WIN32) || defined(_WIN64) || defined(__MINGW32__) || defined(__MINGW64__)
 #include <windows.h>
 #endif
 
 typedef enum {
     LOG_LEVEL_OFF,
     LOG_LEVEL_ERROR,
     LOG_LEVEL_WARNING,
     LOG_LEVEL_INFO,
     LOG_LEVEL_DEBUG,
 } LOG_TYPE;
 
 #ifndef log_level
 #define log_level LOG_LEVEL_DEBUG
 #endif
 #if defined(__QNX__)
 #define USE_SLOGINFO 0
 #define USE_SLOG2INFO 0
 #endif
 #if defined(__linux__)
 #define USE_LINUX_SYSLOG 0
 #endif
 #if defined(__ANDROID__)
 #define USE_ANDROID_LOG 1
 #endif
 #define USE_FILELOG 0
 
 #ifndef __FILENAME__
 #define __FILENAME__ \
     (strrchr(__FILE__, '/') ? (strrchr(__FILE__, '/') + 1) : (strrchr(__FILE__, '\\') ? strrchr(__FILE__, '\\') + 1 : __FILE__))
 #endif
 
 extern const char *__PROGNAME;
 #if defined(__linux__) || defined(__MINGW32__) || defined(__MINGW64__) || defined(__QNX__)
 __attribute__((constructor)) void get_exe_path();
 __attribute__((destructor)) void free_exe_path();
 #endif
 
 #if USE_SLOG2INFO
 #include <sys/slog.h>
 #include <sys/slog2.h>
 #include <sys/slogcodes.h>
 #define CODE_MASK 900
 #define SLOG_PAGE_NUM 32
 #define BUFFER_SET_NAME "AudioPlayer"
 #define BUFFER_NAME "AudioPlayer"
 
 extern slog2_buffer_t slog_buffer;
 
 #define LOG(level, code, include_context, fmt, ...)                                                          \
     do {                                                                                                     \
         if (slog_buffer != NULL && log_level >= level) {                                                     \
             if (include_context) {                                                                           \
                 slog2f(slog_buffer, CODE_MASK, code, "%d @%s: " fmt, __LINE__, __FUNCTION__, ##__VA_ARGS__); \
             } else {                                                                                         \
                 slog2f(slog_buffer, CODE_MASK, code, fmt, ##__VA_ARGS__);                                    \
             }                                                                                                \
         }                                                                                                    \
     } while (0)
 
 #define LOGD2(fmt, ...) LOG(LOG_LEVEL_DEBUG, SLOG2_DEBUG2, 0, fmt, ##__VA_ARGS__)
 #define LOGD(fmt, ...) LOG(LOG_LEVEL_DEBUG, SLOG2_DEBUG1, 1, fmt, ##__VA_ARGS__)
 #define LOGI2(fmt, ...) LOG(LOG_LEVEL_INFO, SLOG2_INFO, 0, fmt, ##__VA_ARGS__)
 #define LOGI(fmt, ...) LOG(LOG_LEVEL_INFO, SLOG2_INFO, 1, fmt, ##__VA_ARGS__)
 #define LOGW2(fmt, ...) LOG(LOG_LEVEL_WARNING, SLOG2_WARNING, 0, fmt, ##__VA_ARGS__)
 #define LOGW(fmt, ...) LOG(LOG_LEVEL_WARNING, SLOG2_WARNING, 1, fmt, ##__VA_ARGS__)
 #define LOGE2(fmt, ...) LOG(LOG_LEVEL_ERROR, SLOG2_ERROR, 0, fmt, ##__VA_ARGS__)
 #define LOGE(fmt, ...) LOG(LOG_LEVEL_ERROR, SLOG2_ERROR, 1, fmt, ##__VA_ARGS__)
 
 void __attribute__((constructor)) slog_buffer_init(void);
 
 #elif USE_SLOGINFO
 #include <sys/slog.h>
 #include <sys/slogcodes.h>
 #define CODE_MASK 900
 #define _SLOGC_YOUNG _SLOG_SETCODE(_SLOGC_PRIVATE_START + CODE_MASK, 1)
 
 #define LOG(level, code, include_context, fmt, ...)                                               \
     do {                                                                                          \
         if (log_level >= level) {                                                                 \
             if (include_context) {                                                                \
                 slogf(_SLOGC_YOUNG, code, "%d @%s: " fmt, __LINE__, __FUNCTION__, ##__VA_ARGS__); \
             } else {                                                                              \
                 slogf(_SLOGC_YOUNG, code, fmt, ##__VA_ARGS__);                                    \
             }                                                                                     \
         }                                                                                         \
     } while (0)
 
 #define LOGD2(fmt, ...) LOG(LOG_LEVEL_DEBUG, _SLOG_DEBUG2, 0, fmt, ##__VA_ARGS__)
 #define LOGD(fmt, ...) LOG(LOG_LEVEL_DEBUG, _SLOG_DEBUG1, 1, fmt, ##__VA_ARGS__)
 #define LOGI2(fmt, ...) LOG(LOG_LEVEL_INFO, _SLOG_INFO, 0, fmt, ##__VA_ARGS__)
 #define LOGI(fmt, ...) LOG(LOG_LEVEL_INFO, _SLOG_INFO, 1, fmt, ##__VA_ARGS__)
 #define LOGW2(fmt, ...) LOG(LOG_LEVEL_WARNING, _SLOG_WARNING, 0, fmt, ##__VA_ARGS__)
 #define LOGW(fmt, ...) LOG(LOG_LEVEL_WARNING, _SLOG_WARNING, 1, fmt, ##__VA_ARGS__)
 #define LOGE2(fmt, ...) LOG(LOG_LEVEL_ERROR, _SLOG_ERROR, 0, fmt, ##__VA_ARGS__)
 #define LOGE(fmt, ...) LOG(LOG_LEVEL_ERROR, _SLOG_ERROR, 1, fmt, ##__VA_ARGS__)
 
 #elif USE_LINUX_SYSLOG
 #include <pthread.h>
 #include <sys/syscall.h>
 #include <syslog.h>
 #include <unistd.h>
 #define LOG(level, priority, include_context, fmt, ...)                                \
     do {                                                                               \
         if (log_level >= level) {                                                      \
             if (include_context) {                                                     \
                 int pid = (int)getpid();                                               \
                 int tid = (int)syscall(SYS_gettid);                                    \
                 syslog(priority, "[%d.%d] %s:%d @%s: " fmt,                            \
                        pid, tid, __FILENAME__, __LINE__, __FUNCTION__, ##__VA_ARGS__); \
             } else {                                                                   \
                 syslog(priority, fmt, ##__VA_ARGS__);                                  \
             }                                                                          \
         }                                                                              \
     } while (0)
 
 #define LOGD2(fmt, ...) LOG(LOG_LEVEL_DEBUG, LOG_LEVEL_DEBUG, 0, fmt, ##__VA_ARGS__)
 #define LOGD(fmt, ...) LOG(LOG_LEVEL_DEBUG, LOG_LEVEL_DEBUG, 1, fmt, ##__VA_ARGS__)
 #define LOGI2(fmt, ...) LOG(LOG_LEVEL_INFO, LOG_LEVEL_INFO, 0, fmt, ##__VA_ARGS__)
 #define LOGI(fmt, ...) LOG(LOG_LEVEL_INFO, LOG_LEVEL_INFO, 1, fmt, ##__VA_ARGS__)
 #define LOGW2(fmt, ...) LOG(LOG_LEVEL_WARNING, LOG_LEVEL_WARNING, 0, fmt, ##__VA_ARGS__)
 #define LOGW(fmt, ...) LOG(LOG_LEVEL_WARNING, LOG_LEVEL_WARNING, 1, fmt, ##__VA_ARGS__)
 #define LOGE2(fmt, ...) LOG(LOG_LEVEL_ERROR, LOG_ERR, 0, fmt, ##__VA_ARGS__)
 #define LOGE(fmt, ...) LOG(LOG_LEVEL_ERROR, LOG_ERR, 1, fmt, ##__VA_ARGS__)
 
 void __attribute__((constructor)) open_log_operation(void);
 void __attribute__((destructor)) close_log_operation(void);
 
 #elif USE_ANDROID_LOG
 #include <android/log.h>
 #include <sys/syscall.h>
 #include <unistd.h>
 #define LOG_TAG "AudioPlayer"
 #define LOG(level, priority, include_context, fmt, ...)                                   \
     do {                                                                                  \
         if (log_level >= level) {                                                         \
             if (include_context) {                                                        \
                 __android_log_print(priority, LOG_TAG, "%s:%d @%s: " fmt,                 \
                                     __FILENAME__, __LINE__, __FUNCTION__, ##__VA_ARGS__); \
             } else {                                                                      \
                 __android_log_print(priority, LOG_TAG, fmt, ##__VA_ARGS__);               \
             }                                                                             \
         }                                                                                 \
     } while (0)
 
 #define LOGD2(fmt, ...) LOG(LOG_LEVEL_DEBUG, ANDROID_LOG_DEBUG, 0, fmt, ##__VA_ARGS__)
 #define LOGD(fmt, ...) LOG(LOG_LEVEL_DEBUG, ANDROID_LOG_DEBUG, 1, fmt, ##__VA_ARGS__)
 #define LOGI2(fmt, ...) LOG(LOG_LEVEL_INFO, ANDROID_LOG_INFO, 0, fmt, ##__VA_ARGS__)
 #define LOGI(fmt, ...) LOG(LOG_LEVEL_INFO, ANDROID_LOG_INFO, 1, fmt, ##__VA_ARGS__)
 #define LOGW2(fmt, ...) LOG(LOG_LEVEL_WARNING, ANDROID_LOG_WARN, 0, fmt, ##__VA_ARGS__)
 #define LOGW(fmt, ...) LOG(LOG_LEVEL_WARNING, ANDROID_LOG_WARN, 1, fmt, ##__VA_ARGS__)
 #define LOGE2(fmt, ...) LOG(LOG_LEVEL_ERROR, ANDROID_LOG_ERROR, 0, fmt, ##__VA_ARGS__)
 #define LOGE(fmt, ...) LOG(LOG_LEVEL_ERROR, ANDROID_LOG_ERROR, 1, fmt, ##__VA_ARGS__)
 
 #elif USE_FILELOG
 extern FILE *log_file;
 
 #if defined(_WIN32) || defined(_WIN64)
 #if defined(__MINGW32__) || defined(__MINGW64__) || defined(_MSC_VER)
 #define LOG(level, include_context, level_str, fmt, ...)                                                             \
     do {                                                                                                             \
         if (log_file && log_level >= level) {                                                                        \
             SYSTEMTIME st = {0};                                                                                     \
             GetLocalTime(&st);                                                                                       \
             int pid = (int)GetCurrentProcessId();                                                                    \
             int tid = (int)GetCurrentThreadId();                                                                     \
             char _log_buf_[1024] = {0};                                                                              \
             if (include_context) {                                                                                   \
                 snprintf(_log_buf_, sizeof(_log_buf_), "%04d-%02d-%02d %02d:%02d:%02d.%03d %s [%d.%d] %s %s:%d @%s", \
                          st.wYear, st.wMonth, st.wDay,                                                               \
                          st.wHour, st.wMinute, st.wSecond, st.wMilliseconds,                                         \
                          __PROGNAME, pid, tid, level_str, __FILENAME__, __LINE__, __FUNCTION__);                     \
                 fprintf(log_file, "%-96s" fmt "\n", _log_buf_, ##__VA_ARGS__);                                       \
             } else {                                                                                                 \
                 fprintf(log_file, fmt "\n", ##__VA_ARGS__);                                                          \
             }                                                                                                        \
             fflush(log_file);                                                                                        \
         }                                                                                                            \
     } while (0)
 #endif
 #elif defined(__linux__) || defined(__QNX__) || defined(__ANDROID__)
 #include <pthread.h>
 #include <sys/syscall.h>
 #include <sys/time.h>
 #include <unistd.h>
 #define LOG(level, include_context, level_str, fmt, args...)                                                          \
     do {                                                                                                              \
         if (log_file && log_level >= level) {                                                                         \
             struct timeval tv = {0};                                                                                  \
             gettimeofday(&tv, NULL);                                                                                  \
             time_t time = tv.tv_sec;                                                                                  \
             struct tm *tm_info = localtime(&time);                                                                    \
             int pid = (int)getpid();                                                                                  \
             int tid = (int)syscall(SYS_gettid);                                                                       \
             char _log_buf_[1024] = {0};                                                                               \
             if (include_context) {                                                                                    \
                 snprintf(_log_buf_, sizeof(_log_buf_), "%04d-%02d-%02d %02d:%02d:%02d.%06ld %s [%d.%d] %s %s:%d @%s", \
                          tm_info->tm_year + 1900, tm_info->tm_mon + 1, tm_info->tm_mday,                              \
                          tm_info->tm_hour, tm_info->tm_min, tm_info->tm_sec, tv.tv_usec,                              \
                          __PROGNAME, pid, tid, level_str, __FILENAME__, __LINE__, __FUNCTION__);                      \
                 fprintf(log_file, "%-96s" fmt "\n", _log_buf_, ##args);                                               \
             } else {                                                                                                  \
                 fprintf(log_file, "%s" fmt "\n", _log_buf_, ##args);                                                  \
             }                                                                                                         \
             fflush(log_file);                                                                                         \
         }                                                                                                             \
     } while (0)
 #endif
 
 #if defined(__linux__) || defined(__MINGW32__) || defined(__MINGW64__) || defined(__QNX__) || defined(__ANDROID__)
 #define LOGD2(fmt, args...) LOG(LOG_LEVEL_DEBUG, 0, "", fmt, ##args)
 #define LOGD(fmt, args...) LOG(LOG_LEVEL_DEBUG, 1, "DEBUG", fmt, ##args)
 #define LOGI2(fmt, args...) LOG(LOG_LEVEL_INFO, 0, "INFO", fmt, ##args)
 #define LOGI(fmt, args...) LOG(LOG_LEVEL_INFO, 1, "INFO", fmt, ##args)
 #define LOGW2(fmt, args...) LOG(LOG_LEVEL_WARNING, 0, "WARN", fmt, ##args)
 #define LOGW(fmt, args...) LOG(LOG_LEVEL_WARNING, 1, "WARN", fmt, ##args)
 #define LOGE2(fmt, args...) LOG(LOG_LEVEL_ERROR, 0, "ERROR", fmt, ##args)
 #define LOGE(fmt, args...) LOG(LOG_LEVEL_ERROR, 1, "ERROR", fmt, ##args)
 
 void __attribute__((constructor)) open_log_file(void);
 void __attribute__((destructor)) close_log_file(void);
 #elif defined(_MSC_VER)
 #define LOGD2(fmt, ...) LOG(LOG_LEVEL_DEBUG, 0, "", fmt, __VA_ARGS__)
 #define LOGD(fmt, ...) LOG(LOG_LEVEL_DEBUG, 1, "DEBUG", fmt, __VA_ARGS__)
 #define LOGI2(fmt, ...) LOG(LOG_LEVEL_INFO, 0, "INFO", fmt, __VA_ARGS__)
 #define LOGI(fmt, ...) LOG(LOG_LEVEL_INFO, 1, "INFO", fmt, __VA_ARGS__)
 #define LOGW2(fmt, ...) LOG(LOG_LEVEL_WARNING, 0, "WARN", fmt, __VA_ARGS__)
 #define LOGW(fmt, ...) LOG(LOG_LEVEL_WARNING, 1, "WARN", fmt, __VA_ARGS__)
 #define LOGE2(fmt, ...) LOG(LOG_LEVEL_ERROR, 0, "ERROR", fmt, __VA_ARGS__)
 #define LOGE(fmt, ...) LOG(LOG_LEVEL_ERROR, 1, "ERROR", fmt, __VA_ARGS__)
 #endif
 
 #else // print log to console
 #if defined(_WIN32) || defined(_WIN64)
 #if defined(__MINGW32__) || defined(__MINGW64__)
 #define LOG(level, color, include_context, fmt, ...)                                                              \
     do {                                                                                                          \
         if (log_level >= level) {                                                                                 \
             SYSTEMTIME st = {0};                                                                                  \
             GetLocalTime(&st);                                                                                    \
             int pid = (int)GetCurrentProcessId();                                                                 \
             int tid = (int)GetCurrentThreadId();                                                                  \
             char _log_buf_[1024] = {0};                                                                           \
             if (include_context) {                                                                                \
                 snprintf(_log_buf_, sizeof(_log_buf_), "%04d-%02d-%02d %02d:%02d:%02d.%03d %s [%d.%d] %s:%d @%s", \
                          st.wYear, st.wMonth, st.wDay,                                                            \
                          st.wHour, st.wMinute, st.wSecond, st.wMilliseconds,                                      \
                          __PROGNAME, pid, tid, __FILENAME__, __LINE__, __FUNCTION__);                             \
                 printf(color "%-96s" fmt "\e[0m\n", _log_buf_, ##__VA_ARGS__);                                    \
             } else {                                                                                              \
                 printf(color fmt "\e[0m\n", ##__VA_ARGS__);                                                       \
             }                                                                                                     \
         }                                                                                                         \
     } while (0)
 #else
 #define LOG(level, color, include_context, fmt, ...)                                                              \
     do {                                                                                                          \
         if (log_level >= level) {                                                                                 \
             SYSTEMTIME st = {0};                                                                                  \
             GetLocalTime(&st);                                                                                    \
             int pid = (int)GetCurrentProcessId();                                                                 \
             int tid = (int)GetCurrentThreadId();                                                                  \
             char _log_buf_[1024] = {0};                                                                           \
             if (include_context) {                                                                                \
                 snprintf(_log_buf_, sizeof(_log_buf_), "%04d-%02d-%02d %02d:%02d:%02d.%03d %s [%d.%d] %s:%d @%s", \
                          st.wYear, st.wMonth, st.wDay,                                                            \
                          st.wHour, st.wMinute, st.wSecond, st.wMilliseconds,                                      \
                          __PROGNAME, pid, tid, __FILENAME__, __LINE__, __FUNCTION__);                             \
                 printf(color "%-96s" fmt "\x1b[0m\n", _log_buf_, ##__VA_ARGS__);                                  \
             } else {                                                                                              \
                 printf(color fmt "\x1b[0m\n", ##__VA_ARGS__);                                                     \
             }                                                                                                     \
         }                                                                                                         \
     } while (0)
 #endif
 #else
 #if defined(__linux__) || defined(__QNX__) || defined(__ANDROID__)
 #include <pthread.h>
 #include <sys/syscall.h>
 #include <sys/time.h>
 #include <unistd.h>
 #define LOG(level, color, include_context, fmt, args...)                                                           \
     do {                                                                                                           \
         if (log_level >= level) {                                                                                  \
             struct timeval tv = {0};                                                                               \
             gettimeofday(&tv, NULL);                                                                               \
             time_t time = tv.tv_sec;                                                                               \
             struct tm *tm_info = localtime(&time);                                                                 \
             int pid = (int)getpid();                                                                               \
             int tid = (int)syscall(SYS_gettid);                                                                    \
             char _log_buf_[1024] = {0};                                                                            \
             if (include_context) {                                                                                 \
                 snprintf(_log_buf_, sizeof(_log_buf_), "%04d-%02d-%02d %02d:%02d:%02d.%06ld %s [%d.%d] %s:%d @%s", \
                          tm_info->tm_year + 1900, tm_info->tm_mon + 1, tm_info->tm_mday,                           \
                          tm_info->tm_hour, tm_info->tm_min, tm_info->tm_sec, tv.tv_usec,                           \
                          __PROGNAME, pid, tid, __FILENAME__, __LINE__, __FUNCTION__);                              \
                 printf(color "%-96s" fmt "\e[0m\n", _log_buf_, ##args);                                            \
             } else {                                                                                               \
                 printf(color fmt "\e[0m\n", ##args);                                                               \
             }                                                                                                      \
         }                                                                                                          \
     } while (0)
 #endif
 #endif
 
 #if defined(__linux__) || defined(__MINGW32__) || defined(__MINGW64__) || defined(__QNX__) || defined(__ANDROID__)
 #define LOGD2(fmt, args...) LOG(LOG_LEVEL_DEBUG, "\e[1;36m", 0, fmt, ##args)
 #define LOGD(fmt, args...) LOG(LOG_LEVEL_DEBUG, "\e[1;36m", 1, fmt, ##args)
 #define LOGI2(fmt, args...) LOG(LOG_LEVEL_INFO, "\e[1;32m", 0, fmt, ##args)
 #define LOGI(fmt, args...) LOG(LOG_LEVEL_INFO, "\e[1;32m", 1, fmt, ##args)
 #define LOGW2(fmt, args...) LOG(LOG_LEVEL_WARNING, "\e[1;33m", 0, fmt, ##args)
 #define LOGW(fmt, args...) LOG(LOG_LEVEL_WARNING, "\e[1;33m", 1, fmt, ##args)
 #define LOGE2(fmt, args...) LOG(LOG_LEVEL_ERROR, "\e[1;31m", 0, fmt, ##args)
 #define LOGE(fmt, args...) LOG(LOG_LEVEL_ERROR, "\e[1;31m", 1, fmt, ##args)
 #elif defined(_MSC_VER)
 #define LOGD2(fmt, ...) LOG(LOG_LEVEL_DEBUG, "\x1b[1;36m", 0, fmt, __VA_ARGS__)
 #define LOGD(fmt, ...) LOG(LOG_LEVEL_DEBUG, "\x1b[1;36m", 1, fmt, __VA_ARGS__)
 #define LOGI2(fmt, ...) LOG(LOG_LEVEL_INFO, "\x1b[1;32m", 0, fmt, __VA_ARGS__)
 #define LOGI(fmt, ...) LOG(LOG_LEVEL_INFO, "\x1b[1;32m", 1, fmt, __VA_ARGS__)
 #define LOGW2(fmt, ...) LOG(LOG_LEVEL_WARNING, "\x1b[1;33m", 0, fmt, __VA_ARGS__)
 #define LOGW(fmt, ...) LOG(LOG_LEVEL_WARNING, "\x1b[1;33m", 1, fmt, __VA_ARGS__)
 #define LOGE2(fmt, ...) LOG(LOG_LEVEL_ERROR, "\x1b[1;31m", 0, fmt, __VA_ARGS__)
 #define LOGE(fmt, ...) LOG(LOG_LEVEL_ERROR, "\x1b[1;31m", 1, fmt, __VA_ARGS__)
 #endif
 #endif
 
 #endif // _LOG_H
 