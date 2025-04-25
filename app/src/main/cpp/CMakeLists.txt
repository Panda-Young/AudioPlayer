# CMakeLists.txt
cmake_minimum_required(VERSION 3.22.1)

project(algo_jni)

add_library(algo_jni SHARED
    algo_jni.cpp
    algo_example.c
)

target_link_libraries(algo_jni
    log
)
