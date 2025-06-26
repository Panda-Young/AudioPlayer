cmake_minimum_required(VERSION 3.22.1)

project(gain_jni)

add_library(gain_jni SHARED
    gain_jni.cpp
    gain.c
)

target_link_libraries(gain_jni
    log
)
