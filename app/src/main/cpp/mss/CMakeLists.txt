cmake_minimum_required(VERSION 3.22.1)

project(mss_jni)

add_library(mss_wrapper SHARED IMPORTED)
set_target_properties(mss_wrapper PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libmss_wrapper.so)

add_library(mss_jni SHARED mss_jni.cpp)

target_link_libraries(mss_jni
    mss_wrapper  
    log
)
